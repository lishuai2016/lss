# resteasy简介

一个Jboss开发的框架，目的是使用java语言开发client端和server端服务restful应用，并具有生产级别的可用性。它主要是JAX-RS的实现，并进行了扩展。




## 1、一个简单的例子

项目结构

![](../../pic/2020-02-04-10-05-36.png)


接收请求的入口一般是xxxResource，类比springmvc的controller。比如FooResource类的定义：

```java
package org.jboss.resteasy.examples.springbasic;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

@Path("/rest/foo")
public class FooResource {

   @Autowired
   FooService fooService;

   @GET
   public String getFoo(@Context ServletContext context) {
      return context.getInitParameter("foo");
   }

   @GET
   @Path("/hello")
   public String hello() {
      return fooService.hello();
   }
}

```

业务服务FooService

```java
package org.jboss.resteasy.examples.springbasic;

import org.springframework.stereotype.Component;

@Component
public class FooService {
    public String hello() {
        return "Hello, world!";
    }
}

```

说明：

- 1、@Path("/rest/foo")定义了请求的URI；

- 2、@GET定义了请求方式；

> 1、web.xml配置

```xml
<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
    <display-name>resteasy-spring-basic</display-name>

    <context-param>
        <param-name>foo</param-name>
        <param-value>bar</param-value>
    </context-param>

    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:resteasy-spring-basic.xml</param-value>
    </context-param>

    <!--    This must be present, or `SpringContextLoaderListener` will throw error: -->
    <!--    java.lang.RuntimeException: RESTEASY013095: RESTeasy Deployment is null, do you have the ResteasyBootstrap listener configured?-->
    <listener>
        <listener-class>org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap</listener-class>
    </listener>

    <listener>
        <listener-class>org.jboss.resteasy.plugins.spring.SpringContextLoaderListener</listener-class>
    </listener>

    <servlet>
        <servlet-name>resteasy-dispatcher</servlet-name>
        <servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher</servlet-class>
    </servlet>

    <servlet-mapping>
        <servlet-name>resteasy-dispatcher</servlet-name>
        <url-pattern>/rest/*</url-pattern>
    </servlet-mapping>

    <!--    You can use `FilterDispatcher` instead of `HttpServletDispatcher`. -->
    <!--    The advantage is that if a JAX-RS resource is not found under the URL requested, -->
    <!--    RESTEasy will delegate back to the base servlet container to resolve URLs.-->
    <!--    <filter>-->
    <!--        <filter-name>resteasy-filter</filter-name>-->
    <!--        <filter-class>-->
    <!--            org.jboss.resteasy.plugins.server.servlet.FilterDispatcher-->
    <!--        </filter-class>-->
    <!--    </filter>-->

    <!--    <filter-mapping>-->
    <!--        <filter-name>resteasy-filter</filter-name>-->
    <!--        <url-pattern>/rest/*</url-pattern>-->
    <!--    </filter-mapping>-->
</web-app>

```


说明：初步来看感觉和springmvc的分发器一样的原理，有待进一步研究。

org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher



> 2、spring配置文件resteasy-spring-basic.xml

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:p="http://www.springframework.org/schema/p"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd

		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <context:component-scan base-package="org.jboss.resteasy.examples.springbasic">
        <context:include-filter type="annotation" expression="javax.ws.rs.Path"/>
    </context:component-scan>
    <context:annotation-config/>

</beans>

```

说明：配置多了需要扫描Path注解。


> 3、pom.xml


```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.jboss.resteasy</groupId>
        <artifactId>testable-examples-pom</artifactId>
        <version>4.0.1.Final-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <groupId>org.jboss.resteasy.examples</groupId>
    <artifactId>examples-resteasy-spring-basic</artifactId>
    <name>Spring RestContact Maven Webapp</name>
    <packaging>war</packaging>
    <dependencies>
        <dependency>
            <groupId>org.jboss.resteasy</groupId>
            <artifactId>resteasy-spring</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.undertow</groupId>
            <artifactId>undertow-servlet</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.undertow</groupId>
            <artifactId>undertow-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jboss.resteasy</groupId>
            <artifactId>resteasy-undertow</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jboss.resteasy</groupId>
            <artifactId>resteasy-undertow-spring</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    <build>
        <finalName>resteasy-spring-example-basic</finalName>
    </build>
</project>


```


> 4、发送请求验证

http://localhost:8080/rest/foo/hello

返回值：Hello, world!




# 参考

- [github源码](https://github.com/lishuai2016/Resteasy)
- [官方样例](https://github.com/lishuai2016/resteasy-examples)


- [关于使用spring mvc或者resteasy构建restful服务的差别与比较](https://www.cnblogs.com/zhjh256/p/6883417.html)

- [Spring MVC与JAX-RS比较与分析](https://blog.csdn.net/dalinaidalin/article/details/45920311)

- [Spring Boot 集成 resteasy篇 — jax-rs初步介绍和spring boot集成](https://blog.csdn.net/u011410529/article/details/77503918)