# spring-cloud-netflix-Eureka

<!-- TOC -->

- [spring-cloud-netflix-Eureka](#spring-cloud-netflix-eureka)
    - [1、服务发现: Eureka Clients](#1服务发现-eureka-clients)
        - [1.2、通过Eureka注册服务](#12通过eureka注册服务)
        - [1.3、Eureka Server的安全认证](#13eureka-server的安全认证)
        - [1.4、状态页和健康指标](#14状态页和健康指标)
        - [1.5、注册一个安全的应用](#15注册一个安全的应用)
        - [1.6、Eureka的健康检查](#16eureka的健康检查)
        - [1.7、Eureka服务提供者元数据和服务消费者](#17eureka服务提供者元数据和服务消费者)
        - [1.8、使用EurekaClient](#18使用eurekaclient)
        - [1.9、替代原生的Netflix EurekaClient](#19替代原生的netflix-eurekaclient)
        - [1.10、为什么服务注册比较慢？](#110为什么服务注册比较慢)
        - [1.11、zones 区](#111zones-区)
    - [2、服务发现：Eureka Server](#2服务发现eureka-server)
        - [2.1.如何引入 Eureka Server](#21如何引入-eureka-server)
        - [2.2.如何运行一个 Eureka Server](#22如何运行一个-eureka-server)
        - [2.3、高可用、Zones and Regions](#23高可用zones-and-regions)
        - [2.4. 单例模式部署server](#24-单例模式部署server)
        - [2.5、双服务配置模式（同伴意识）](#25双服务配置模式同伴意识)
        - [2.6、何时通过IP来进行服务的注册](#26何时通过ip来进行服务的注册)
        - [2.7、Eureka Server的安全设置](#27eureka-server的安全设置)
    - [3、配置属性](#3配置属性)

<!-- /TOC -->





翻译参考：https://spring.io/projects/spring-cloud-netflix

Spring Cloud Netflix通过自动配置、绑定spring环境和其他spring编程模型习惯，基于Netflix提供了对springboot应用的整合组件。在你的应用中通过很少简单的注解可以开启和配置一般通用模式，并且可以使用经过实战检验Netflix组件构建大型分布式系统。

这个模式包含服务发现Eureka、断路器Hystrix、智能路由zuul、和客户端负载均衡Ribbon等等。

> Spring Cloud Netflix特色

- 服务发现：Eureka服务实例进行注册，clients通过使用spring管理的beans可以发现这个注册的服务实例。通过声明式配置一个内嵌的Eureka server服务可以被开启。

- 断路器：Hystrix clients可以通过在方法上添加注解的方式被创建。同样，可以通过声明是配置启动内嵌的Hystrix dashboard。

- 声明式REST Client：Feign针对被JAX-RS 和 Spring MVC注解修饰的接口创建一个动态代理。

- 客户端负载均衡：Ribbon

- External Configuration: a bridge from the Spring Environment to Archaius（通过springboot的约定开启Netflix组件原生配置）

- 路由和过滤器：动态的注册zuul的过滤器，并通过配置的方式简单的约定，以反向代理的创建。

> 入门

只有你的springboot应用依赖引入了Spring Cloud Netflix and Eureka Core，通过添加注解@EnableEurekaClient，你的应用将尝试着链接地址在http://localhost:8761上Eureka server  (这个地址时eureka.client.serviceUrl.defaultZone的默认值)。

通过在你的springboot应用中添加依赖spring-cloud-starter-netflix-eureka-server和使用注解@EnableEurekaServer开启服务，你可以运行你自己的Eureka server。



参考：https://cloud.spring.io/spring-cloud-netflix/reference/html/

## 1、服务发现: Eureka Clients

服务发现是微服务基础框架中的重要一环。尝试手动配置每一个客户端或者通过某种方式的约定去配置是很麻烦的，并且通用性不高。Eureka是Netflix提供的服务发现组件，包含server服务和client。这个server服务可以通过配置和部署为高可用模式，并且每个服务实例备份有整个注册服务的全部信息。 


1.1、怎么引入Eureka Client？

为了在你的项目中引入Eureka Client，通过引入springcloud的相关starter依赖即可。如
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 1.2、通过Eureka注册服务

当一个client服务提供者注册服务到Eureka server上时，它提供自身的元数据诸如host、端口、健康检查URL、home页和其他信息。Eureka server服务端从每个client服务提供者接收心跳信息。如果心跳超过了配置的时间间隔，这个实例将被从服务注册列表中移除。

下面的这个例子展示一个最简化的 Eureka client应用。

```java
@SpringBootApplication
@RestController
public class Application {

    @RequestMapping("/")
    public String home() {
        return "Hello world";
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

}

```

备注：这其实就是一个基本的web应用，只不过在配置文件开启eureka服务注册功能，把服务注册到eureka server中。

注意，上面的例子展示了一个标准的springboot应用。通过在你的应用中引入spring-cloud-starter-netflix-eureka-client依赖在类的classpath路径下，你的应用将会自动注册到Eureka Server中。需要在配置文件中指定Eureka Server的地址，如下面的配置：

application.yml

```yml
eureka:
  client:
    serviceUrl:
        defaultZone: http://localhost:8761/eureka/

```

在上面的例子中，defaultZone值是一个有用必须配置，通过它任何clients可以找到服务URL。

defaultZone值是大小写敏感的并且需要使用驼峰标识，它在内部使用Map<String, String>存储的。因此，the defaultZone property does not follow the normal Spring Boot snake-case convention of default-zone.

默认的应用名称（服务id）：${spring.application.name}，虚拟主机：${spring.application.name}，非安全端口：${server.port}。

通过在项目中引入spring-cloud-starter-netflix-eureka-client依赖，可以使得该应用作为一个服务提供者注册它自身到服务注册中心，也可以从注册中心调用其他注册的服务。你可以通过配置eureka.instance.*相关的属性影响服务实例的行为。

### 1.3、Eureka Server的安全认证

当你的eureka client设置eureka.client.serviceUrl.defaultZone的URL为需要认证的格式，如：（user:password@localhost:8761/eureka），这样的设置就开启了基本的http认证功能。针对复杂的需求，你可以创建一个类型为DiscoveryClientOptionalArgs的bean对象并且注入到ClientFilter实例中，当client端调用server的时候就会被应用到。

提示：Because of a limitation in Eureka, it is not possible to support per-server basic auth credentials, so only the first set that are found is used.

### 1.4、状态页和健康指标

Eureka服务提供者的状态页和健康指标路径分别是/info和/health，他们是有用的默认位置，Spring Boot Actuator应用通过他们作为一个端点采集信息。如果你使用的不是默认的上下文或者servlet路径，比如（server.servletPath=/custom），你需要设置下面这样的配置，以便于Actuator应用采集信息。


```yml
eureka:
  instance:
    statusPageUrlPath: ${server.servletPath}/info
    healthCheckUrlPath: ${server.servletPath}/health
```

上面配置的元信息将会被clients使用，在一些场景下被用来觉得是否发送请求给你的应用服务，所以他们是有用的配置，需要配置准确。



### 1.5、注册一个安全的应用

如果你的应用想用https通信，你可以在EurekaInstanceConfig中配置下面两个属性：

eureka.instance.[nonSecurePortEnabled]=[false]

eureka.instance.[securePortEnabled]=[true]

如果一个服务使用上面的配置使得Eureka发布服务的时候选择一种安全的通信方式。Spring Cloud DiscoveryClient将总是返回以https开头的URL。同样服务健康检查的URL也是以https开头的。

一般情况状态和健康检查URL是非https的，除非你进行显示的设置，比如下面这样：


```yml
eureka:
  instance:
    statusPageUrl: https://${eureka.hostname}/info
    healthCheckUrl: https://${eureka.hostname}/health
    homePageUrl: https://${eureka.hostname}/

```

### 1.6、Eureka的健康检查

默认，eureka server端通过client端的心跳来判断一个服务提供者是否在线。除非另有说明，服务调用者不会广播被调用服务提供者的状态信息，每一个Spring Boot Actuator应用。于是，在服务成功注册后，Eureka服务端总数认为服务提供者是存活状态的。这个行为可以通过开启健康check改变，它的功能传播应用的状态到eureka server。因此，请求流量将不再发送到不是存活状态的服务上。下面的例子展示了如何开启client服务提供者健康检查：


application.yml
```yml
eureka:
  client:
    healthcheck:
      enabled: true
```
eureka.client.healthcheck.enabled=true应该在application.yml中进行设置。如果在bootstrap.yml中设置可能会出现预想不到的影响。

如果你想控制更多的健康检测，可以实现你自己的com.netflix.appinfo.HealthCheckHandler。




### 1.7、Eureka服务提供者元数据和服务消费者

为了你可以以某一种方式在你的平台上使用它，花一点时间弄明白eureka元数据如何工作的是值得的。标准的元数据信息包括：主键hostname、IP地址、端口号、状态页和健康检测。这些是在服务注册的时候被发布的，并且在clients进行通信时直接使用的。附加的元数据信息可以通过eureka.instance.metadataMap在服务注册的时候进行添加，这些信息可以被远端的clients使用。大体上，附加的元数据新不会影响client的行为，除非client感知到这个元数据的含义。




In general, additional metadata does not change the behavior of the client, unless the client is made aware of the meaning of the metadata. There are a couple of special cases, described later in this document, where Spring Cloud already assigns meaning to the metadata map.


> 改变Eureka Instance ID

Spring Cloud Eureka 提供了合理的默认，格式如下：

${spring.cloud.client.hostname}:${spring.application.name}:${spring.application.instance_id:${server.port}}}

一个实例：myhost:myappname:8080

你可以通过eureka.instance.instanceId覆盖这个值并指定一个唯一的标识，如：

application.yml

```yml
eureka:
  instance:
    instanceId: ${spring.application.name}:${vcap.application.instance_id:${spring.application.instance_id:${random.value}}}
```

### 1.8、使用EurekaClient

一旦你有一个应用作为服务消费者，你需要一个EurekaClient从Eureka Server中发现服务提供者实例。一种方式是使用原生的EurekaClient（或者使用Spring Cloud提供的DiscoveryClient），如下所示：

```java
@Autowired
private EurekaClient discoveryClient;

public String serviceUrl() {
    InstanceInfo instance = discoveryClient.getNextServerFromEureka("STORES", false);
    return instance.getHomePageUrl();
}
```

不要使用EurekaClient在@PostConstruct方法或者@Scheduled方法。（在ApplicationContext还没有启动完成的地方都不可以使用）。


默认EurekaClient使用Jersey进行http通信的。如果你希望避免依赖jersey，你可以去除依赖。Spring Cloud将自动配置一个client端的传输对象RestTemplate。下面展示如何去除依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    <exclusions>
        <exclusion>
            <groupId>com.sun.jersey</groupId>
            <artifactId>jersey-client</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.sun.jersey</groupId>
            <artifactId>jersey-core</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.sun.jersey.contribs</groupId>
            <artifactId>jersey-apache-client4</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```


### 1.9、替代原生的Netflix EurekaClient

你不需要用原生的Netflix EurekaClient。此外，一般通过某种方式进行包装后可以更加便利的使用。Spring Cloud提供Feign（一种rest 请求客户端构建器）和Spring RestTemplate通过eureka服务识别码而不是物理URL进行服务的调用。

你可以使用org.springframework.cloud.client.discovery.DiscoveryClient，它提供了简单的API用于服务发现，如下：

```java
@Autowired
private DiscoveryClient discoveryClient;

public String serviceUrl() {
    List<ServiceInstance> list = discoveryClient.getInstances("STORES");
    if (list != null && list.size() > 0 ) {
        return list.get(0).getUri();
    }
    return null;
}
```


### 1.10、为什么服务注册比较慢？

一个服务提供者通过serviceUrl，按照默认每隔30秒发送一次心跳到注册中心。当发现一个服务不可用的时候会发给3个心跳间隔，直到服务提供者、注册中心、消费者的元数据变成一致，在他们各自的本地缓存中。你可以改变这个心跳间隔周期，通过设置参数eureka.instance.leaseRenewalIntervalInSeconds实现。把这个值设置小于30秒可以达到加速client链接其他的服务。在生产环境下，最好严格保持默认值，注册中心的内部关于续租期计算会消耗一部分时间。


### 1.11、zones 区


1.11. Zones
If you have deployed Eureka clients to multiple zones, you may prefer that those clients use services within the same zone before trying services in another zone. To set that up, you need to configure your Eureka clients correctly.

First, you need to make sure you have Eureka servers deployed to each zone and that they are peers of each other. See the section on zones and regions for more information.

Next, you need to tell Eureka which zone your service is in. You can do so by using the metadataMap property. For example, if service 1 is deployed to both zone 1 and zone 2, you need to set the following Eureka properties in service 1:

Service 1 in Zone 1

```
eureka.instance.metadataMap.zone = zone1
eureka.client.preferSameZoneEureka = true
```

Service 1 in Zone 2

```
eureka.instance.metadataMap.zone = zone2
eureka.client.preferSameZoneEureka = true
```





## 2、服务发现：Eureka Server

### 2.1.如何引入 Eureka Server

添加下面的依赖即可。


```xml
<!-- Eureka注册中心依赖 -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<!-- 注意名称的改变 和F之前的版本所有区别 -->
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
		</dependency>
```

提示：如果你的项目使用Thymeleaf模板引擎，Eureka server使用的Freemarker模板引擎也许不能正确的加载。在这种情况下需要配置Freemarker模板引擎配置。


application.yml

```yml

spring:
  freemarker:
    template-loader-path: classpath:/templates/
    prefer-file-system-access: false
```


### 2.2.如何运行一个 Eureka Server

最简化的形式如下：

```java
@SpringBootApplication
@EnableEurekaServer
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

}

```

这个server服务有一个主页和一个路径为/eureka/*的http api。


### 2.3、高可用、Zones and Regions

这个Eureka server没有后端的存储，因此所有的注册服务提供者实例都需要通过心跳机制来保证服务的注册状态，通过在内存中更新来实现。clients在自己的本地也会缓存中服务注册中心提供的服务，这样就不需要每次发送请求都需要从注册中心查询服务提供者列表。


默认情况，每一个Eureka server本身也是一个Eureka client，并且需要至少配置一个服务URL去定位一个同样的服务实例。如果你没有配置，这个服务也会运行并且工作，但是会出现一些错误日志输出，不能够注册到对端。

### 2.4. 单例模式部署server

client和server两个缓存的组合以及心跳机制使得单例模式在一定程度上可以弹性不可用，只要在一定程度上进行监视或者弹性运行时保持活着。在单例模式时，你最好关闭client端的行为，因为它不能发送请求到其他对等的server。下面的例子展示如何关闭client行为：

```yml
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/

```
注意：serviceUrl被设置为指向本地的host。


### 2.5、双服务配置模式（同伴意识）


Eureka可以通过部署多个服务实例并要求他们之间相互注册来保证server的高可用性。事实上，这是默认行为，我们需要配置一个serviceUrl指向对方服务器，如下所示：


application.yml (Two Peer Aware Eureka Servers)

```yml
---
spring:
  profiles: peer1
eureka:
  instance:
    hostname: peer1
  client:
    serviceUrl:
      defaultZone: https://peer2/eureka/

---
spring:
  profiles: peer2
eureka:
  instance:
    hostname: peer2
  client:
    serviceUrl:
      defaultZone: https://peer1/eureka/
```

上面例子中，我们通过配置一个YAML文件运行在两个主机上。


集群部署配置

application.yml (Three Peer Aware Eureka Servers)

```yml
eureka:
  client:
    serviceUrl:
      defaultZone: https://peer1/eureka/,http://peer2/eureka/,http://peer3/eureka/

---
spring:
  profiles: peer1
eureka:
  instance:
    hostname: peer1

---
spring:
  profiles: peer2
eureka:
  instance:
    hostname: peer2

---
spring:
  profiles: peer3
eureka:
  instance:
    hostname: peer3
```


### 2.6、何时通过IP来进行服务的注册

在一些情况下，Eureka可以使用IP来表示服务提供者的地址而不是主机名。当设置 eureka.instance.preferIpAddress=true，当服务提供者注册服务的时候就是使用IP而不是域名。

备注：在服务提供者那边设置即可。

### 2.7、Eureka Server的安全设置

引入:spring-boot-starter-security

```yml

server:
  port: 8761

spring:
  security:
    user:
      name: admin
      password: pwd

logging:
  level:
    org:
      springframework:
        security: DEBUG

eureka:
  client:
    registerWithEureka: false
    fetchRegistry: false
    service-url:
      defaultZone:  http://admin:pwd@localhost:8761/eureka/
  server:
    waitTimeInMsWhenSyncEmpty: 0
```



```java

@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(EurekaApplication.class, args);
	}

	@EnableWebSecurity
	static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.csrf().ignoringAntMatchers("/eureka/**");
			super.configure(http);
		}
	}

}
```




## 3、配置属性

https://cloud.spring.io/spring-cloud-netflix/reference/html/appendix.html












