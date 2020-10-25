

<!-- TOC -->

- [1、一个\conf\server.xml](#1一个\conf\serverxml)
    - [1.1、Server](#11server)
    - [1.2、Service](#12service)
    - [1.3、Connector](#13connector)
    - [1.4、Engine](#14engine)
    - [1.5、Host](#15host)
    - [1.6、Context](#16context)
- [2、Tomcat Server处理一个http请求的过程](#2tomcat-server处理一个http请求的过程)
- [资料](#资料)

<!-- /TOC -->



# 1、一个\conf\server.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener"/>
  <Listener SSLEngine="on" className="org.apache.catalina.core.AprLifecycleListener"/>
  <Listener className="org.apache.catalina.core.JasperListener"/>

  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener"/>
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener"/>
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener"/>
  
  <GlobalNamingResources>    
    <Resource auth="Container" description="User database that can be updated and saved" factory="org.apache.catalina.users.MemoryUserDatabaseFactory" name="UserDatabase" pathname="conf/tomcat-users.xml" type="org.apache.catalina.UserDatabase"/>
  </GlobalNamingResources>
  
  <Service name="Catalina">
    <!--The connectors can use a shared executor, you can define one or more named thread pools-->
    <!--
    <Executor name="tomcatThreadPool" namePrefix="catalina-exec-"
        maxThreads="150" minSpareThreads="4"/>
    -->
   
    <Connector connectionTimeout="20000" port="8080" protocol="HTTP/1.1" redirectPort="8443"/>
    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443"/>
    <Engine defaultHost="localhost" name="Catalina">
      
      <Realm className="org.apache.catalina.realm.LockOutRealm">
        
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm" resourceName="UserDatabase"/>
      </Realm>
      <Host appBase="webapps" autoDeploy="true" name="localhost" unpackWARs="true">
        
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs" pattern="%h %l %u %t &quot;%r&quot; %s %b" prefix="localhost_access_log." suffix=".txt"/>
      <Context docBase="didi" path="/didi" reloadable="true" source="org.eclipse.jst.jee.server:didi"/>
      </Host>
    </Engine>
  </Service>
</Server>

```

从上面的配置我们中，位于配置文件顶层的是Server和Service元素，其中Server元素是整个配置文件的根元素，而Service元素则是配置服务器的核心元素。在Service元素内部，定义了一系列的连接器和内部容器类的组件。

## 1.1、Server

<Server>元素对应的是整个Servlet容器，是整个配置的顶层元素，由org.apache.catalina.Server接口来定义，默认的实现类是org.apache.catalina.core.StandardServer。该元素可配置的属性主要是port和shutdown，分别指的是监听shutdown命令的端口和命令。在该元素中可以定义一个或多个<Service>元素，除此以外还可以定义一些全局的资源或监听器。

## 1.2、Service

<Service>元素由org.apache.catalina.Service接口定义，默认的实现类为org.apache.catalina.core.StandardService。在该元素中可以定义一个<Engine>元素、一个或多个<Connector>元素，这些<Connector>元素共享同一个<Engine>元素来进行请求的处理。

## 1.3、Connector

< Connector >元素由org.apache.catalina.connector.Connector类来定义。< Connector>是接受客户端浏览器请求并向用户最终返回响应结果的组件。该元素位于< Service>元素中，可以定义多个，在我们的示例中配置了两个，分别接受AJP请求和HTTP请求，在配置中，需要为其制定服务的协议和端口号。

## 1.4、Engine

<Engine>元素由org.apache.catalina.Engine元素来定义，默认的实现类是org.apache.catalina.core.StandardEngine。<Engine>元素会用来处理<Service>中所有<Connector>接收到的请求，在<Engine>中可以定义多个<Host>元素作为虚拟主机。<Engine>是Tomcat配置中第一个实现org.apache.catalina.Container的接口，因此可以在其中定义一系列的子元素如<Realm>、<Valve>。

## 1.5、Host

<Host>元素由org.apache.catalina.Host接口来定义，默认实现为org.apache.catalina.core.StandardHost，
该元素定义在<Engine>中，可以定义多个。每个<Host>元素定义了一个虚拟主机，它可以包含一个或多个Web应用（通过<Context>元素来进行定义）。因为<Host>也是容器类元素，所以可以在其中定义子元素如<Realm>、<Valve>。

## 1.6、Context
 <Context>元素由org.apache.catalina.Context接口来定义，默认实现类为org.apache.catalina.core.StandardContext。
 该元素也许是大家用的最多的元素，在其中定义的是Web应用。一个<Host>中可以定义多个<Context>元素，分别对应不同的Web应用。该元素的属性，大家经常会用到如path、reloadable等，可以在<Context>中定义子元素如<Realm>、<Valve>。





# 2、Tomcat Server处理一个http请求的过程

假设来自客户的请求为：http://localhost:8080/wsota/wsota_index.jsp
- 1) 请求被发送到本机端口8080，被在那里侦听的CoyoteHTTP/1.1 Connector获得

- 2) Connector把该请求交给它所在的Service的Engine来处理，并等待来自Engine的回应

- 3) Engine获得请求localhost/wsota/wsota_index.jsp，匹配它所拥有的所有虚拟主机Host

- 4) Engine匹配到名为localhost的Host（即使匹配不到也把请求交给该Host处理，因为该Host被定义为该Engine的默认主机）

- 5) localhost Host获得请求/wsota/wsota_index.jsp，匹配它所拥有的所有Context

- 6) Host匹配到路径为/wsota的Context（如果匹配不到就把该请求交给路径名为""的Context去处理）

- 7) path="/wsota"的Context获得请求/wsota_index.jsp，在它的mapping table中寻找对应的servlet

- 8) Context匹配到URLPATTERN为*.jsp的servlet，对应于JspServlet类

- 9) 构造HttpServletRequest对象和HttpServletResponse对象，作为参数调用JspServlet的doGet或doPost方法

- 10) Context把执行完了之后的HttpServletResponse对象返回给Host

- 11) Host把HttpServletResponse对象返回给Engine

- 12) Engine把HttpServletResponse对象返回给Connector

- 13) Connector把HttpServletResponse对象返回给客户browser


> Context的部署配置文件web.xml的说明

一个Context对应于一个Web App，每个Web App是由一个或者多个servlet组成的
当一个Web App被初始化的时候，它将用自己的ClassLoader对象载入“部署配置文件web.xml”中定义的每个servlet类
- 它首先载入在$CATALINA_HOME/conf/web.xml中部署的servlet类
- 然后载入在自己的Web App根目录下的WEB-INF/web.xml中部署的servlet类

web.xml文件有两部分：servlet类定义和servlet映射定义
每个被载入的servlet类都有一个名字，且被填入该Context的映射表(mappingtable)中，和某种URLPATTERN对应当该Context获得请求时，将查询mappingtable，找到被请求的servlet，并执行以获得请求回应

分析一下所有的Context共享的web.xml文件，在其中定义的servlet被所有的Web App载入


# 资料

- [详解Tomcat 配置文件server.xml](https://www.cnblogs.com/kismetv/p/7228274.html)