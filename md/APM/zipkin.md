


# 1、springboot整合


## 0、启动zipkin服务

java -jar zipkin-server-2.21.5-exec.jar

访问地址：http://localhost:9411/

## 1、引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zipkin</artifactId>
</dependency>
```


## 2、配置服务器地址

```
spring.zipkin.base-url=http://localhost:9411
spring.sleuth.sampler.probability=1.0

#spring.zipkin.sender.type=RABBIT
#spring.rabbitmq.addresses=amqp://192.168.10.124:5672
#spring.rabbitmq.username=yinjihuan
#spring.rabbitmq.password=123456
```



