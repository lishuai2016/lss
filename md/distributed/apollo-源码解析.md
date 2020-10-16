

<!-- TOC -->autoauto- [1、core包核心类[包含读取appid和metaserver地址]](#1core包核心类包含读取appid和metaserver地址)auto    - [1、MetaDomainConsts提供了获取用户配置的metaserver地址](#1metadomainconsts提供了获取用户配置的metaserver地址)auto    - [2、MetaServerProvider](#2metaserverprovider)auto    - [3、jdk的spi加载类](#3jdk的spi加载类)auto    - [4、Foundation抽象类](#4foundation抽象类)auto- [2、metaserver服务](#2metaserver服务)auto- [3、client](#3client)auto    - [1、读取appid和metaserver配置信息](#1读取appid和metaserver配置信息)auto    - [2、ConfigService使用配置服务的入口](#2configservice使用配置服务的入口)auto    - [3、ConfigManager](#3configmanager)auto    - [4、ConfigFactory](#4configfactory)auto    - [5、ConfigRepository](#5configrepository)auto        - [1、RemoteConfigRepository](#1remoteconfigrepository)auto        - [2、LocalFileConfigRepository](#2localfileconfigrepository)auto        - [3、PropertiesCompatibleFileConfigRepository](#3propertiescompatiblefileconfigrepository)auto    - [6、RepositoryChangeListener仓库监听器](#6repositorychangelistener仓库监听器)auto- [通过使用流程分析源码](#通过使用流程分析源码)autoauto<!-- /TOC -->


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




# 通过使用流程分析源码



```java
interface ConfigFactory {//创建配置对象和配置文件对象接口，默认实现DefaultConfigFactory
Config create(String namespace);//默认实现DefaultConfig
//基于文件格式不同构建Properties\xml\json\yaml\yam\txt对应的对象
ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
```

ConfigFile和Config构建的必须参数namespace（名称）和ConfigRepository（数据来源）







- 1、ConfigService.getConfig(namesapce)
- 2、DefaultConfigManager#getConfig(namesapce)创建configFile\Config并缓存（维护namespace和configFile\Config映射）
  - 2.1、获取配置工厂：DefaultConfigFactoryManager#getFactory(namesapce)（从ConfigRegistry注册管理或者本地缓存创建的DefaultConfigFactory）
  - 2.2、ConfigFactory功能：创建configFile\Config（ConfigFactoryManager维护namespace和ConfigFactory映射）
  - 2.3、通过ConfigFactory创建Config对象，获取ConfigFile类似；（在这里实例化仓库对象，比如new RemoteConfigRepository(namespace)）

备注：

- 1、每一个namespace一一对应一个configFile\Config、一个ConfigFactory、一个RemoteConfigRepository；
- 2、设计方面：工厂模式创建对象、每一层都有一个对应的manager缓存




> 问题

- 1、如何从远端拉取数据并更新的流程；
- 2、本地缓存文件；
- 3、长轮训机制；
- 4、spring整合时数据属性更新问题；
- 5、直接HTTP方式访问的区别；

## RemoteConfigRepository初始化




在初始化构造函数的时候会去远端拉取配置信息并更新到本地；



this.trySync(); //抽象类中的方法，对应模板方法sync()--->loadApolloConfig()
this.schedulePeriodicRefresh();//定时刷新，也是调用trySync()
this.scheduleLongPollingRefresh();//长轮询刷新，最后还是调用trySync()



配置内容的统一抽象：
```java
public class ApolloConfig {
  private String appId;//唯一标识
  private String cluster;//默认default
  private String namespaceName;//配置文件的名称
  private Map<String, String> configurations;//key-value键值对，如果是文本文件则会存在一个content的key，value是文本对应的字符串
  private String releaseKey;//版本
```




url格式：

http://10.181.163.3:80/configs/apollo-test/default/application.yaml?ip=10.12.223.239&messages=%7B%22details%22%3A%7B%22apollo-test%2Bdefault%2Bapplication.yaml%22%3A413%7D%7D&releaseKey=20200923105324-0d93bc58cc899b0a


http://10.181.163.3:80/configs/apollo-test/default/application?ip=10.12.223.239


当仓库从远端通过HTTP拉取过了的信息会封装成ApolloConfig对象。后面会把ApolloConfig中的Map<String, String> configurations转换为Properties向下传递。

在仓库层通过原子引用AtomicReference<ApolloConfig>缓存，在配置对象config层通过原子引用AtomicReference<Properties>缓存，每一层都会把本地缓存和通过HTTP拉下下来最新的数据对比，只有发现不一致的时候才会下发数据（调用对应的监听器实现）




数据流向：

ConfigRepository中数据有更新会触发注册到其上的监听器RepositoryChangeListener，一般这个监听器的实现类是config/cofigfile的实现类，完成第一层数据的下发（仓库到配置对象）；在config/cofigfile中也可以添加对应配置对象变化的监听器，这里的监听器是开放给用户来使用的，如果有数据跟新会在这里进行第二层的数据下发（配置对象到用户的代码）



## LocalFileConfigRepository




- this.setLocalCacheDir(findLocalCacheDir(), false);
- this.setUpstreamRepository(upstream);
- this.trySync();