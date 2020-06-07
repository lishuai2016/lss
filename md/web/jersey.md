
- [官方文档](https://howtodoinjava.com/jersey/jersey2-hello-world-example/)


- [Jersey框架一：Jersey RESTful WebService框架简介](https://www.cnblogs.com/chen-lhx/p/6138495.html)

开发RESTful WebService意味着支持在多种媒体类型以及抽象底层的客户端-服务器通信细节，如果没有一个好的工具包可用，这将是一个困难的任务

为了简化使用Java开发RESTful WebService及其客户端，一个轻量级的标准被提出：JAX-RS API

Jersey RESTful WebService框架是一个开源的、产品级别的JAVA框架，支持JAX-RS API并且是一个JAX-RS(JSR 311和 JSR 339)的参考实现

Jersey不仅仅是一个JAX-RS的参考实现，Jersey提供自己的API，其API继承自JAX-RS，提供更多的特性和功能以进一步简化RESTful service和客户端的开发


基于springboot构建Jersey服务

> 0、pom.xml配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ls</groupId>
    <artifactId>springboot-jersey</artifactId>
    <version>1.0-SNAPSHOT</version>


    <!-- Spring Boot 父依赖 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.1.RELEASE</version>
    </parent>

    <dependencies>
        <!-- Spring Boot web依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Junit -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jersey</artifactId>
        </dependency>

        <!-- 客户端 -->
        <dependency>
            <groupId>org.glassfish.jersey.core</groupId>
            <artifactId>jersey-client</artifactId>
            <version>2.25.1</version>
        </dependency>

    </dependencies>


</project>

```




> 1、server端，类似于springmvc的controller，提供rest格式的http服务，通过http浏览器或者Jersey client端进行http请求。

```java
package demo.web;

import demo.City;
import demo.Employee;
import demo.Employees;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;

/**
 * @program: springboot-jersey
 * @author: lishuai
 * @create: 2019-08-29 10:36
 */
@Component
@Path("/demo/rest")
public class DemoResource {

    //path注解指定路径,get注解指定访问方式,produces注解指定了返回值类型，这里返回JSON
    @Path("/city")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public City get(){
        City city = new City();
        city.setId(1L);
        city.setCityName("beijing");
        city.setCityCode("001");
        System.out.println(city.toString());
        return city;
    }


    @GET
    @Path("/employees")
    @Produces(MediaType.APPLICATION_XML)
    public Employees getAllEmployees()
    {
        Employees list = new Employees();
        list.setEmployeeList(new ArrayList<Employee>());

        list.getEmployeeList().add(new Employee(1, "Lokesh Gupta"));
        list.getEmployeeList().add(new Employee(2, "Alex Kolenchiskey"));
        list.getEmployeeList().add(new Employee(3, "David Kameron"));

        return list;
    }

}


```

> 2、配置，否则访问不到

```java
package demo;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 * @program: springboot-jersey
 * @author: lishuai
 * @create: 2019-08-29 10:34
 */
@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        //构造函数，在这里注册需要使用的内容，（过滤器，拦截器，API等）
        //注册包的方式
        packages("demo.web");//请求路径。上面的DemoResource就是在demo.web包路径下
    }
}


```

> 3、springboot的主类

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args){
        //springboot 入口
        SpringApplication.run(Application.class,args);
    }
}

```

server:
  port: 8081

启动后通过http即可访问


> 4、通过Jersey客户端来访问

```java
package demo;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * @program: springboot-jersey
 * @author: lishuai
 * @create: 2019-08-29 20:24
 */
public class MyClient {
    public static void main(String[] args) {
        list();
    }


    public static void list() {
        Client client = ClientBuilder.newClient( new ClientConfig().register( LoggingFilter.class ) );
        WebTarget webTarget = client.target("http://localhost:8081/demo/rest").path("employees");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_XML);
        Response response = invocationBuilder.get();

        Employees employees = response.readEntity(Employees.class);
        List<Employee> listOfEmployees = employees.getEmployeeList();

        System.out.println(response.getStatus());
        System.out.println(Arrays.toString( listOfEmployees.toArray(new Employee[listOfEmployees.size()]) ));
    }
}


```


执行结果：

```
八月 29, 2019 8:42:39 下午 org.glassfish.jersey.filter.LoggingFilter log
信息: 1 * Sending client request on thread main
1 > GET http://localhost:8081/demo/rest/employees
1 > Accept: application/xml

八月 29, 2019 8:42:41 下午 org.glassfish.jersey.filter.LoggingFilter log
信息: 1 * Client response received on thread main
1 < 200
1 < Content-Length: 258
1 < Content-Type: application/xml
1 < Date: Thu, 29 Aug 2019 12:42:41 GMT

200
[Employee [id=1, name=Lokesh Gupta], Employee [id=2, name=Alex Kolenchiskey], Employee [id=3, name=David Kameron]]

```

![](../../pic/2019-08-29-20-52-18.png)


http://localhost:8081/demo/rest/city

![](../../pic/2019-08-29-20-52-56.png)
