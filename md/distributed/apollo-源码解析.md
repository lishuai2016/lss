

<!-- TOC -->

- [1、core包核心类[包含读取appid和metaserver地址]](#1core包核心类包含读取appid和metaserver地址)
    - [1、MetaDomainConsts提供了获取用户配置的metaserver地址](#1metadomainconsts提供了获取用户配置的metaserver地址)
    - [2、MetaServerProvider](#2metaserverprovider)
    - [3、jdk的spi加载类](#3jdk的spi加载类)
    - [4、Foundation抽象类](#4foundation抽象类)
- [2、metaserver服务](#2metaserver服务)
- [3、client](#3client)
    - [1、读取appid和metaserver配置信息](#1读取appid和metaserver配置信息)
    - [2、ConfigService使用配置服务的入口](#2configservice使用配置服务的入口)
    - [3、ConfigManager](#3configmanager)
    - [4、ConfigFactory](#4configfactory)
    - [5、ConfigRepository](#5configrepository)
        - [1、RemoteConfigRepository](#1remoteconfigrepository)
        - [2、LocalFileConfigRepository](#2localfileconfigrepository)
        - [3、PropertiesCompatibleFileConfigRepository](#3propertiescompatiblefileconfigrepository)
    - [6、RepositoryChangeListener仓库监听器](#6repositorychangelistener仓库监听器)

<!-- /TOC -->


> 模块依赖图

![](../../pic/2020-04-08-21-58-54.png)








# 1、core包核心类[包含读取appid和metaserver地址]

## 1、MetaDomainConsts提供了获取用户配置的metaserver地址

![](../../pic/2020-04-08-22-03-53.png)

具体的解析过程交给MetaServerProvider接口的实现类去实现具体的加载，该接口采用spi机制，可以有多个实现类。同时该接口实现了order接口，可以自定义多个实现类加载顺序，order值越小，优先级越高，当从高优先级的实现类加载到地址后就不再使用低优先级实现类去加载了。

## 2、MetaServerProvider

```java
public interface MetaServerProvider extends Ordered {
//metaserver地址返回值建议是个域名，也可以使用逗号分隔多个IP
String getMetaServerAddress(Env targetEnv);
}
```

```java
public interface Ordered {
int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;//最高优先级
int LOWEST_PRECEDENCE = Integer.MAX_VALUE;//最低优先级
int getOrder();//自定义优先级，越小优先级越高
}
```


![](../../pic/2020-04-08-22-19-08.png)


有两个实现类：

- 1、DefaultMetaServerProvider：最低优先级，在core包，portal service服务会使用metaserver找到可以使用的admin服务；[请求：metaserver域名/services/admin获取]

- 2、LegacyMetaServerProvider：在client包，使用client包时候按照加载这个实现类，我们接入的应该通过这个metaserver找config service服务地址；[请求：metaserver域名/services/config获取]


## 3、jdk的spi加载类

原理：通过jdk的类ServiceLoader.load(xxx接口)


```java
public class ServiceBootstrap {
  //获得这个接口的第一个实现类
  public static <S> S loadFirst(Class<S> clazz) {
    Iterator<S> iterator = loadAll(clazz);
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format(
          "No implementation defined in /META-INF/services/%s, please check whether the file exists and has the right implementation class!",
          clazz.getName()));
    }
    return iterator.next();
  }

 //核心方法:根据jdk的spi机制，加载一个接口的全部实现类
  public static <S> Iterator<S> loadAll(Class<S> clazz) {
    ServiceLoader<S> loader = ServiceLoader.load(clazz);

    return loader.iterator();
  }
  //先加载然后按照order排序
  public static <S extends Ordered> List<S> loadAllOrdered(Class<S> clazz) {
    Iterator<S> iterator = loadAll(clazz);

    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format(
          "No implementation defined in /META-INF/services/%s, please check whether the file exists and has the right implementation class!",
          clazz.getName()));
    }

    List<S> candidates = Lists.newArrayList(iterator);
    Collections.sort(candidates, new Comparator<S>() {
      @Override
      public int compare(S o1, S o2) {
        // the smaller order has higher priority
        return Integer.compare(o1.getOrder(), o2.getOrder());
      }
    });

    return candidates;
  }
  //这个是加载排序后的第一个
  public static <S extends Ordered> S loadPrimary(Class<S> clazz) {
    List<S> candidates = loadAllOrdered(clazz);

    return candidates.get(0);
  }
}

```

> 当前包下的spi接口以及默认实现



- 1、MetaServerProvider：提供如何找到配置的metaserver地址，默认实现为LegacyMetaServerProvider

- 2、MessageProducerManager：默认实现DefaultMessageProducerManager

- 3、ProviderManager：默认实现DefaultProviderManager，统一管理接口Provider的实现类

![](../../pic/2020-04-08-23-00-20.png)

这里主要用来加载一些配置文件信息

```java
public interface ProviderManager {
  public String getProperty(String name, String defaultValue);

  public <T extends Provider> T provider(Class<T> clazz);
}
```



## 4、Foundation抽象类

全部为静态方法和属性，可以直接引用并使用，不需要实例化。这个类的功能主要用来暴露ProviderManager接口的中的方法的

对外提供如下的四个方法：

![](../../pic/2020-04-08-23-09-12.png)

- 1、DefaultApplicationProvider：Load per-application configuration, like app id, from classpath://META-INF/app.properties[核心：加载应用的唯一标识]

- 2、DefaultNetworkProvider：Load network parameters

- 3、DefaultServerProvider：Load environment (fat, fws, uat, prod ...) and dc, from /opt/settings/server.properties, JVM property and/or OS environment variables.


# 2、metaserver服务

在apollo-configservice包下

主要逻辑在DiscoveryService类中

![](../../pic/2020-04-08-22-35-48.png)

有三个方法：

- 1、getConfigServiceInstances：对应client中的/services/config返回configservice服务列表，让客户端拉取配置信息以及维持http长轮询；

- 2、getAdminServiceInstances：对应portal服务中的/services/admin请求返回adminservice服务列表，让portal服务进行调用；

- 3、getMetaServiceInstances：咱们貌似没有；

实现原理：configservice和adminservice都是注册到eureka，这里直接通过eureka的客户端从注册中心获取。调用eurekaClient.getApplication（服务名），服务名包含在ServiceNameConsts类中。


```java

public interface ServiceNameConsts {

  String APOLLO_METASERVICE = "apollo-metaservice";

  String APOLLO_CONFIGSERVICE = "apollo-configservice";

  String APOLLO_ADMINSERVICE = "apollo-adminservice";

  String APOLLO_PORTAL = "apollo-portal";
}
```


# 3、client

## 1、读取appid和metaserver配置信息

client端启动读取核心配置的两个类：

- apollo-core包的类DefaultApplicationProvider读取设置的唯一标识appid

- apollo-core包的类LegacyMetaServerProvider读取meta server地址

备注：上面两个类加载配置文件均是采用spi机制实现。因此可以添加自己的类继承对应的接口来扩展自己的逻辑。


## 2、ConfigService使用配置服务的入口


内部包含两个类：

- 1、ConfigManager

- 2、ConfigRegistry

## 3、ConfigManager

```java
public interface ConfigManager {
  //根据文件名获取配置信息
  public Config getConfig(String namespace);
  //根据配置文件名称和格式获取一个对象
  public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
```

默认实现类DefaultConfigManager，内部通过ConfigFactoryManager委托给接口ConfigFactory创建Config/ConfigFile对象


## 4、ConfigFactory

对应ConfigManager接口的两个方法

```java
public interface ConfigFactory {
  public Config create(String namespace);

  public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat);
}

```

默认的实现类DefaultConfigFactory，包含一个ConfigUtil对象



## 5、ConfigRepository

![](../../pic/2020-04-08-23-40-44.png)

```java
public interface ConfigRepository {
  //从仓库中获取配置
  public Properties getConfig();

  //Set the fallback repo for this repository.
  public void setUpstreamRepository(ConfigRepository upstreamConfigRepository);

  //添加监听器
  public void addChangeListener(RepositoryChangeListener listener);

  //移除监听器
  public void removeChangeListener(RepositoryChangeListener listener);

  //Return the config's source type, i.e. where is the config loaded from
  public ConfigSourceType getSourceType();
}

```

AbstractConfigRepository实现监听器的添加、移除、触发，已经定于一个抽象的数据同步方法，让子类去实现。


### 1、RemoteConfigRepository


### 2、LocalFileConfigRepository

这里会把RemoteConfigRepository仓库的内容作为自己的上游数据源，把仓库的内容同步到本地磁盘文件一份



### 3、PropertiesCompatibleFileConfigRepository






## 6、RepositoryChangeListener仓库监听器

![](../../pic/2020-04-08-23-45-58.png)

```java
public interface RepositoryChangeListener {
  //Invoked when config repository changes.
  public void onRepositoryChange(String namespace, Properties newProperties);
}

```












ConfigUtil



ConfigServiceLocator

LocalFileConfigRepository

RemoteConfigLongPollService

RemoteConfigRepository


com.ctrip.framework.apollo.ConfigService#getAppConfig
com.ctrip.framework.apollo.internals.DefaultConfigManager#getConfig
com.ctrip.framework.apollo.spi.DefaultConfigFactory#create
com.ctrip.framework.apollo.spi.DefaultConfigFactory#createRemoteConfigRepository

RemoteConfigRepository包含核心类：

- ConfigUtil
- HttpUtil：客户端通过这个http请求从meta server获得config service服务等；
- ConfigServiceLocator：封装怎么找到meta server，然后发生请求；





1、client端如何通过配置的meta server地址找到服务的？