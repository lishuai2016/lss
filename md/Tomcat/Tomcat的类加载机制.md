
# Tomcat的类加载机制

在Tomcat的代码实现中，为了优化内存空间以及不同应用间的类隔离，Tomcat通过内置的一些类加载器来完成了这些功能。

在Java语言中，ClassLoader是以父子关系存在的，Java本身也有一定的类加载规范。在Tomcat中基本的ClassLoader层级关系如下图所示：

![](../../pic/2020-10-25/2020-10-25-15-30-09.png)

Tomcat启动的时候，会初始化图示所示的类加载器。而上面的三个类加载器：CommonClassLoader、CatalinaClassLoader和SharedClassLoader是与具体部署的Web应用无关的，而WebappClassLoader则对应Web应用，每个Web应用都会有独立的类加载器，从而实现类的隔离。

在每个Web应用初始化的时候，StandardContext对象代表每个Web应用，它会使用WebappLoader类来加载Web应用，而WebappLoader中会初始化org.apache.catalina.loader.WebappClassLoader来为每个Web应用创建单独的类加载器，当处理请求时，容器会根据请求的地址解析出由哪个Web应用来进行对应的处理，进而将当前线程的类加载器设置为请求Web应用的类加载器。


standEngine, StandHost, StandContext及StandWrapper是容器，他们之间有互相的包含关系。例如，StandEngine是StandHost的父容器，StandHost是StandEngine的子容器。在StandService内还包含一个Executor及Connector。

1） Executor是线程池，它的具体实现是java的concurrent包实现的executor，这个不是必须的，如果没有配置，则使用自写的worker thread线程池

2） Connector是网络socket相关接口模块，它包含两个对象，ProtocolHandler及Adapter

- ProtocolHandler是接收socket请求，并将其解析成HTTP请求对象，可以配置成nio模式或者传统io模式
- Adapter是处理HTTP请求对象，它就是从StandEngine的valve一直调用到StandWrapper的valve








# 99、Tomcat的类加载机制

破坏了原有的双亲委派类加载机制。

首先Tomcat的 类加载器体系如下图所示，他是自定义了很多类加载器的。

![](../../pic/2019-08-03-23-30-50.png)

Tomcat自定义了Common、Catalina、Shared等类加载器，其实就是用来加载Tomcat自己的一些核心基础类库的。

然后Tomcat为每个部署在里面的Web应用都有一个对应的WebApp类加载器，负责加载我们部署的这个Web应用的类

至于Jsp类加载器，则是给每个JSP都准备了一个Jsp类加载器。

而且大家一定要记得，Tomcat是打破了双亲委派机制的

每个WebApp负责加载自己对应的那个Web应用的class文件，也就是我们写好的某个系统打包好的war包中的所有class文件，不会传导给上层类加载器去加载。


在tomcat中类的加载稍有不同，如下图：

![](../../pic/2020-03-13-22-22-19.png)

- 1 Bootstrap 引导类加载器：加载JVM启动所需的类，以及标准扩展类（位于jre/lib/ext下）

- 2 System 系统类加载器 ：加载tomcat启动的类，比如bootstrap.jar，通常在catalina.bat或者catalina.sh中指定。位于CATALINA_HOME/bin下

- 3 Common 通用类加载器 ：加载tomcat使用以及应用通用的一些类，位于CATALINA_HOME/lib下，比如servlet-api.jar

- 4 webapp 应用类加载器：每个应用在部署后，都会创建一个唯一的类加载器。该类加载器会加载位于 WEB-INF/lib下的jar文件中的class 和 WEB-INF/classes下的class文件。

当应用需要到某个类时，则会按照下面的顺序进行类加载：

- 1 使用bootstrap引导类加载器加载

- 2 使用system系统类加载器加载

- 3 使用应用类加载器在WEB-INF/classes中加载

- 4 使用应用类加载器在WEB-INF/lib中加载

- 5 使用common类加载器在CATALINA_HOME/lib中加载


> 参考

- [图解Tomcat类加载机制](https://www.cnblogs.com/xing901022/p/4574961.html)



https://juejin.cn/post/6844903550300979214


```java
private void doInvoke(String path) {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            JarLoader jarLoader = new JarLoader(new String[]{path});
            Thread.currentThread().setContextClassLoader(jarLoader);
            Iterator<Hook> hookIt = ServiceLoader.load(Hook.class).iterator();
            if (!hookIt.hasNext()) {
                LOG.warn("No hook defined under path: " + path);
            } else {
                Hook hook = hookIt.next();
                LOG.info("Invoke hook [{}], path: {}", hook.getName(), path);
                hook.invoke(conf, msg);
            }
        } catch (Exception e) {
            LOG.error("Exception when invoke hook", e);
            throw DataXException.asDataXException(
                    CommonErrorCode.HOOK_INTERNAL_ERROR, "Exception when invoke hook", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
}
```
