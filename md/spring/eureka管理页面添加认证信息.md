

eureka管理页面添加认证信息


1、引入依赖

```xml
<!-- 开启安全认证 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```


2、eureka 服务端

```yml
server:
  port: 8760

eureka:
  instance:
    hostname: eureka2
  client:
    # 表示是否注册自身到eureka服务器
#    registerWithEureka: false
    # 是否从eureka上获取注册信息
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://user:123456@eureka1:8761/eureka/
  server:
    enableSelfPreservation: false  # 自我保护，不下线服务实例

# 安全认证的配置
spring:
  security:
    user:
      name: user
      password: 123456
```



3、服务提供者

```yml
server:
  port: 8081

spring:
  application:
    name: hello-service

eureka:
  instance:
    hostname: localhost
  client:
    serviceUrl:
      defaultZone: http://user:123456@eureka1:8761/eureka,http://user:123456@eureka2:8760/eureka
```


4、服务消费者

```yml
server:
  port: 8082

spring:
  application:
    name: api-service

eureka:
  instance:
    hostname: localhost
  client:
    serviceUrl:
      defaultZone: http://user:123456@eureka1:8761/eureka,http://user:123456@eureka2:8760/eureka
```