# spring-boot-starter-actuator



依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

```

引入依赖启动应用，会看到控制台打印出默认是三个关于actuator的映射，所有 endpoints 默认情况下都已移至 /actuator下。

![](../../pic/2020-02-10-09-25-25.png)


访问http://localhost:8080/actuator可以看到rest风格的接口

![](../../pic/2020-02-10-09-26-48.png)

注意：在Spring Boot 2.0 中对Actuator变动很大，默认只提供这三个接口，如果想提供所有接口需要配置application.yml，配置之后重新访问/actuator就会暴露出所有接口

```yml

management:
  endpoints:
    web:
      exposure:
        include: "*"
```


```json
{
_links: {
self: {
href: "http://localhost:8092/actuator",
templated: false
},
archaius: {
href: "http://localhost:8092/actuator/archaius",
templated: false
},
auditevents: {
href: "http://localhost:8092/actuator/auditevents",
templated: false
},
beans: {
href: "http://localhost:8092/actuator/beans",
templated: false
},
health: {
href: "http://localhost:8092/actuator/health",
templated: false
},
conditions: {
href: "http://localhost:8092/actuator/conditions",
templated: false
},
configprops: {
href: "http://localhost:8092/actuator/configprops",
templated: false
},
env: {
href: "http://localhost:8092/actuator/env",
templated: false
},
env-toMatch: {
href: "http://localhost:8092/actuator/env/{toMatch}",
templated: true
},
info: {
href: "http://localhost:8092/actuator/info",
templated: false
},
loggers: {
href: "http://localhost:8092/actuator/loggers",
templated: false
},
loggers-name: {
href: "http://localhost:8092/actuator/loggers/{name}",
templated: true
},
heapdump: {
href: "http://localhost:8092/actuator/heapdump",
templated: false
},
threaddump: {
href: "http://localhost:8092/actuator/threaddump",
templated: false
},
metrics: {
href: "http://localhost:8092/actuator/metrics",
templated: false
},
metrics-requiredMetricName: {
href: "http://localhost:8092/actuator/metrics/{requiredMetricName}",
templated: true
},
scheduledtasks: {
href: "http://localhost:8092/actuator/scheduledtasks",
templated: false
},
httptrace: {
href: "http://localhost:8092/actuator/httptrace",
templated: false
},
mappings: {
href: "http://localhost:8092/actuator/mappings",
templated: false
},
refresh: {
href: "http://localhost:8092/actuator/refresh",
templated: false
},
features: {
href: "http://localhost:8092/actuator/features",
templated: false
},
service-registry: {
href: "http://localhost:8092/actuator/service-registry",
templated: false
},
jolokia: {
href: "http://localhost:8092/actuator/jolokia",
templated: false
}
}
}
```



# 参考

https://blog.csdn.net/vbirdbest/article/details/79900772

