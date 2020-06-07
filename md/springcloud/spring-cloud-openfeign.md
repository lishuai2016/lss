# spring-cloud-openfeign

翻译：https://cloud.spring.io/spring-cloud-openfeign/reference/html/

<!-- TOC -->

- [spring-cloud-openfeign](#spring-cloud-openfeign)
    - [1.声明式 REST Client: Feign](#1声明式-rest-client-feign)
        - [1.1、怎么引入Feign](#11怎么引入feign)
        - [1.2、覆盖feign的默认值](#12覆盖feign的默认值)
        - [1.3、手动创建Feign Clients](#13手动创建feign-clients)
        - [1.4、Feign的 Hystrix 支持](#14feign的-hystrix-支持)
        - [1.5、Feign Hystrix Fallbacks快速失败](#15feign-hystrix-fallbacks快速失败)
        - [1.6. Feign and @Primary](#16-feign-and-primary)
        - [1.7、Feign的继承支持](#17feign的继承支持)
        - [1.8. Feign request/response compression压缩](#18-feign-requestresponse-compression压缩)
        - [1.9、Feign logging](#19feign-logging)
        - [1.10. Feign @QueryMap support](#110-feign-querymap-support)
        - [1.11. HATEOAS support](#111-hateoas-support)
        - [1.12. Troubleshooting](#112-troubleshooting)

<!-- /TOC -->

## 1.声明式 REST Client: Feign

Feign是一个声明式 web service client。它使得写web service客户端变得更加便利。使用feign创建一个接口并添加注解。他提供可插拔的注解包括feign的注解和JAX-RS规范注解。feign同样也支持可插拔的编解码。Spring Cloud添加了支持 Spring MVC的注解并且在Spring Web环境中使用HttpMessageConverters。Spring Cloud整合Ribbon 和 Eureka，同样当client端使用feign时，Spring Cloud提供一个的负载均衡。


### 1.1、怎么引入Feign

引入依赖spring-cloud-starter-openfeign。然后开启

```java
@SpringBootApplication
@EnableFeignClients
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```
StoreClient.java 接口定义

```java
@FeignClient("stores")
public interface StoreClient {
    @RequestMapping(method = RequestMethod.GET, value = "/stores")
    List<Store> getStores();

    @RequestMapping(method = RequestMethod.POST, value = "/stores/{storeId}", consumes = "application/json")
    Store update(@PathVariable("storeId") Long storeId, Store store);
}
```

注解@FeignClient中的值“stores”是一个随意的一个client的名字，这个名字被用来创建一个ribbon负载均衡器或者Spring Cloud负载均衡器。你也可以指定URL属性。

上面负载均衡客户端可以找到“stores”服务提供者的物理地址（域名或者IP）。如果你的应用是一个eureka client，它将会从eureka的注册中心获得服务提供者的地址。如果你不想使用eureka，你可以配置一些服务地址列表。


### 1.2、覆盖feign的默认值

Spring Cloud’s Feign支持的一个核心概念是命名的client。每一个feign client是组件集合中的一部分，这个集合的名称是通过开发者在@FeignClient 中设置的。

Spring Cloud通过FeignClientsConfiguration让你完全控制@FeignClient，例如：

```java
@FeignClient(name = "stores", configuration = FooConfiguration.class)
public interface StoreClient {
    //..
}
```
在name和URL属性上支持占位符设置，例如：

```java

@FeignClient(name = "${feign.name}", url = "${feign.url}")
public interface StoreClient {
    //..
}
```

Spring Cloud Netflix为feign提供了下面这些默认值，通过(BeanType beanName: ClassName)：


- Decoder feignDecoder: ResponseEntityDecoder (which wraps a SpringDecoder)

- Encoder feignEncoder: SpringEncoder

- Logger feignLogger: Slf4jLogger

- Contract feignContract: SpringMvcContract

- Feign.Builder feignBuilder: HystrixFeign.Builder

- Client feignClient: if Ribbon is in the classpath and is enabled it is a LoadBalancerFeignClient, otherwise if Spring Cloud LoadBalancer is in the classpath, FeignBlockingLoadBalancerClient is used. If none of them is in the classpath, the default feign client is used.首先看下Ribbon是否配置，有的话使用LoadBalancerFeignClient；再看 Spring Cloud LoadBalancer是否配置，有的话使用FeignBlockingLoadBalancerClient，否则使用默认的。

注意:

spring-cloud-starter-openfeign contains both spring-cloud-starter-netflix-ribbon and spring-cloud-starter-loadbalancer.

说明依赖包已经存在，只要进行配置即可使用。


你可以指定feign clients具体的http通信实现方式，引入相关依赖，设置feign.okhttp.enabled=true使用OkHttpClient；设置feign.httpclient.enabled=true使用ApacheHttpClient。你同样可以通过org.apache.http.impl.client.CloseableHttpClient或者okhttp3.OkHttpClient自定义你自己的client的实现。

Spring Cloud Netflix 没有提供下面这些beans类型的默认实现，但是在创建feign client时，从spring容器中查找。


- Logger.Level

- Retryer

- ErrorDecoder

- Request.Options

- Collection<RequestInterceptor>

- SetterFactory

- QueryMapEncoder


创建上述类型的bean实例，并且把它放到@FeignClient的配置中去，比如FooConfiguration，这样可以实现覆盖默认值,比如：

```java
@Configuration
public class FooConfiguration {
    @Bean
    public Contract feignContract() {
        return new feign.Contract.Default();
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor("user", "password");
    }
}

```

这里替换了feign.Contract.Default为feign.Contract.Default的值，往RequestInterceptor拦截器集合中添加了一个BasicAuthRequestInterceptor。

@FeignClient 也可以通过属性进行配置。例如：

```yml
feign:
  client:
    config:
      feignName:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: full
        errorDecoder: com.example.SimpleErrorDecoder
        retryer: com.example.SimpleRetryer
        requestInterceptors:
          - com.example.FooRequestInterceptor
          - com.example.BarRequestInterceptor
        decode404: false
        encoder: com.example.SimpleEncoder
        decoder: com.example.SimpleDecoder
        contract: com.example.SimpleContract
```

默认配置也可以再@EnableFeignClients 注解的属性中进行设置。

如果你想使用属性配置全部的@FeignClient，你可以在配置的时候不指定feign的名字。

If you prefer using configuration properties to configured all @FeignClient, you can create configuration properties with default feign name.

```yml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: basic
```

如果你同时使用@Configuration和属性文件进行配置，那么配置文件的有效。它会覆盖@Configuration中相同的配置。如果你想改变他们的优先级，可以配置feign.client.default-to-properties=false

如果你需要在RequestInterceptor中使用ThreadLocal绑定变量，你需要设置线程的隔离策略针对Hystrix的信号量或者禁用Hystrix。

```yml
# To disable Hystrix in Feign
feign:
  hystrix:
    enabled: false

# To set thread isolation to SEMAPHORE
hystrix:
  command:
    default:
      execution:
        isolation:
          strategy: SEMAPHORE
```

我们希望针对同一个服务提供者创建多个使用不同的配置feign clients ，我们可以使用
@FeignClient的属性contextId来避免配置同名的冲突。例如：

```java
@FeignClient(contextId = "fooClient", name = "stores", configuration = FooConfiguration.class)
public interface FooClient {
    //..
}
@FeignClient(contextId = "barClient", name = "stores", configuration = BarConfiguration.class)
public interface BarClient {
    //..
}
```

### 1.3、手动创建Feign Clients

当我们需要高度自定义自己的Feign Clients而上面的方式没法满足。这种情况下，可以使用Feign Builder API进行手动创建。下面的例子配置了实现同样接口的两个Feign Clients，但是各自配置了一个请求拦截器。

```java

@Import(FeignClientsConfiguration.class)
class FooController {

    private FooClient fooClient;

    private FooClient adminClient;

        @Autowired
    public FooController(Decoder decoder, Encoder encoder, Client client, Contract contract) {
        this.fooClient = Feign.builder().client(client)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptor(new BasicAuthRequestInterceptor("user", "user"))
                .target(FooClient.class, "https://PROD-SVC");

        this.adminClient = Feign.builder().client(client)
                .encoder(encoder)
                .decoder(decoder)
                .contract(contract)
                .requestInterceptor(new BasicAuthRequestInterceptor("admin", "admin"))
                .target(FooClient.class, "https://PROD-SVC");
    }
}
```
提示：

- 1、上面的FeignClientsConfiguration类由Spring Cloud Netflix提供。

- 2、PROD-SVC为服务提供者地址；

### 1.4、Feign的 Hystrix 支持

引入了Hystrix的依赖并且设置了 feign.hystrix.enabled=true，feign将使用一个断路器包装全部的方法。返回一个com.netflix.hystrix.HystrixCommand。这让你选择使用哪种模式：调用toObservable()、.observe()或者异步调用.queue()。

通过使用Feign.Builder构建一个配置对象来禁用Hystrix支持。？？？

To disable Hystrix support on a per-client basis create a vanilla Feign.Builder with the "prototype" scope, e.g.:

```java
@Configuration
public class FooConfiguration {
        @Bean
    @Scope("prototype")
    public Feign.Builder feignBuilder() {
        return Feign.builder();
    }
}
```

### 1.5、Feign Hystrix Fallbacks快速失败

Hystrix支持fallback快速失败机制，当熔断器打开或者出现了错误，执行一段默认的代码。可以通过设置@FeignClient的属性来开启快速失败机制。你需要声明这个实现为一个springbean。


```java
@FeignClient(name = "hello", fallback = HystrixClientFallback.class)
protected interface HystrixClient {
    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    Hello iFailSometimes();
}

static class HystrixClientFallback implements HystrixClient {
    @Override
    public Hello iFailSometimes() {
        return new Hello("fallback");
    }
```

如果你需要触发fallback的原因，可以使用@FeignClient的fallbackFactory属性进行设置。

```java
@FeignClient(name = "hello", fallbackFactory = HystrixClientFallbackFactory.class)
protected interface HystrixClient {
    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    Hello iFailSometimes();
}

@Component
static class HystrixClientFallbackFactory implements FallbackFactory<HystrixClient> {
    @Override
    public HystrixClient create(Throwable cause) {
        return new HystrixClient() {
            @Override
            public Hello iFailSometimes() {
                return new Hello("fallback; reason was: " + cause.getMessage());
            }
        };
    }
}
```




### 1.6. Feign and @Primary

当Feign使用Hystrix的快速失败机制时，在spring容器中会存在多个相同类型的对象。当使用@Autowired进行设置时将没法确定注入哪一个导致失败。

When using Feign with Hystrix fallbacks, there are multiple beans in the ApplicationContext of the same type. This will cause @Autowired to not work because there isn’t exactly one bean, or one marked as primary. To work around this, Spring Cloud Netflix marks all Feign instances as @Primary, so Spring Framework will know which bean to inject. In some cases, this may not be desirable. To turn off this behavior set the primary attribute of @FeignClient to false.

```java
@FeignClient(name = "hello", primary = false)
public interface HelloClient {
    // methods here
}
```

### 1.7、Feign的继承支持

Feign supports boilerplate apis via single-inheritance interfaces. This allows grouping common operations into convenient base interfaces.


```java
UserService.java
public interface UserService {

    @RequestMapping(method = RequestMethod.GET, value ="/users/{id}")
    User getUser(@PathVariable("id") long id);
}


UserResource.java
@RestController
public class UserResource implements UserService {

}


UserClient.java
package project.user;

@FeignClient("users")
public interface UserClient extends UserService {

}
```

It is generally not advisable to share an interface between a server and a client. It introduces tight coupling, and also actually doesn’t work with Spring MVC in its current form (method parameter mapping is not inherited).

不推荐client和server端公用接口的方式，并且造成耦合。



### 1.8. Feign request/response compression压缩

如果你想对Feign请求和响应对象使用GZIP进行压缩。可以设置下面的配置：

```yml
feign.compression.request.enabled=true
feign.compression.response.enabled=true
```

可以在服务提供者进行设置:

```yml
feign.compression.request.enabled=true
feign.compression.request.mime-types=text/xml,application/xml,application/json
feign.compression.request.min-request-size=2048
```


### 1.9、Feign logging

每个Feign client创建一个logger。默认的logger名称为Feign client接口的全路径。日志级别为debug。

logging.level.project.user.UserClient: DEBUG

Logger.Level对象告诉每个client如何进行日志输出：


- 1、NONE, No logging (DEFAULT).

- 2、BASIC, Log only the request method and URL and the response status code and execution time.

- 3、HEADERS, Log the basic information along with request and response headers.

- 4、FULL, Log the headers, body, and metadata for both requests and responses.

For example, the following would set the Logger.Level to FULL:

```java
@Configuration
public class FooConfiguration {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
```


### 1.10. Feign @QueryMap support


The OpenFeign @QueryMap annotation provides support for POJOs to be used as GET parameter maps. Unfortunately, the default OpenFeign QueryMap annotation is incompatible with Spring because it lacks a value property.

Spring Cloud OpenFeign provides an equivalent @SpringQueryMap annotation, which is used to annotate a POJO or Map parameter as a query parameter map.

For example, the Params class defines parameters param1 and param2:

```java
// Params.java
public class Params {
    private String param1;
    private String param2;

    // [Getters and setters omitted for brevity]
}
```

The following feign client uses the Params class by using the @SpringQueryMap annotation:

```java
@FeignClient("demo")
public interface DemoTemplate {

    @GetMapping(path = "/demo")
    String demoEndpoint(@SpringQueryMap Params params);
}
```

If you need more control over the generated query parameter map, you can implement a custom QueryMapEncoder bean.

### 1.11. HATEOAS support


### 1.12. Troubleshooting

