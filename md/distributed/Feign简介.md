# Feign


## 1、Feign的工作原理

feign是一个伪客户端，即它不做任何的请求处理。Feign通过处理注解生成request，从而实现简化HTTP API开发的目的，即开发人员可以使用注解的方式定制request api模板，在发送http request请求之前，feign通过处理注解的方式替换掉request模板中的参数，这种实现方式显得更为直接、可理解。

> 总结

Feign的源码实现的过程如下：

- 1、首先通过@EnableFeignCleints注解开启FeignCleint

- 2、根据Feign的规则实现接口，并加@FeignCleint注解

- 3、程序启动后，会进行包扫描，扫描所有的@ FeignCleint的注解的类，并将这些信息注入到ioc容器中。

- 4、当接口的方法被调用，通过jdk的代理，来生成具体的RequesTemplate

- 5、RequesTemplate在生成Request

- 6、Request交给Client去处理，其中Client可以是HttpUrlConnection、HttpClient也可以是Okhttp

- 7、最后Client被封装到LoadBalanceClient类，这个类结合类Ribbon做到了负载均衡。


## 2、spring-cloud-openfeign

feign是一个声明Web服务客户端。这使得编写Web服务客户端更容易。





# 参考

- [feign源码地址](https://github.com/OpenFeign/feign)

- [spring-cloud-openfeign官方文档](https://cloud.spring.io/spring-cloud-openfeign/reference/html/)

- [spring-cloud官方主页](https://spring.io/projects/spring-cloud)