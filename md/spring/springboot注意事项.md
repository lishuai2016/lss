


1、Spring Boot在本地快速编译构建

mvn clean install -DskipTests -Pfast

- [Spring和Spring Boot源码阅读环境搭建](https://www.cnblogs.com/fdzfd/p/9453021.html)


- [IDEA 编译运行 Spring Boot 2.0 源码](https://my.oschina.net/dabird/blog/1942112)


2、idea maven java.lang.outofmemoryerror gc overhead limit exceeded

参考： https://blog.csdn.net/zhongzunfa/article/details/82229948

```java
<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.1</version>
				<configuration>
					<fork>true</fork>
					<meminitial>512m</meminitial>
					<!-- 如果不够读者可以加大 -->
					<maxmem>4096m</maxmem>
				</configuration>
			</plugin>
		</plugins>
	</build>
```


3、@SpringBootApplication用在启动Springboot中，相当于@ComponentScan+@EnableAutoConfiguration+@Configuration

@ComponentScan("com.ls.controller")控制器扫包范围。


@EnableAutoConfiguration
他让 Spring Boot 根据咱应用所声明的依赖来对 Spring 框架进行自动配置。意思是，创建项目时添加的spring-boot-starter-web添加了Tomcat和Spring MVC，所以auto-configuration将假定你正在开发一个web应用并相应地对Spring进行设置


4、springboot 启动日志分析

https://www.jianshu.com/p/7992de87d177
https://blog.csdn.net/HcJsJqJSSM/article/details/78668958


5、Springboot版本+jdk 版本+Maven版本的匹配


```
Spring boot 版本	Spring Framework	jdk 版本	maven 版本
1.2.0 版本之前		6	3.0
1.2.0	4.1.3+	6	3.2+
1.2.1	4.1.3+	7	3.2+
1.2.3	4.1.5+	7	3.2+
1.3.4	4.2.6+	7	3.2+
1.3.6	4.2.7+	7	3.2+
1.3.7	4.2.7+	7	3.2+
1.3.8	4.2.8+	7	3.2+
1.4.0	4.3.2+	7	3.2+
1.4.1	4.3.3	7	3.2+
1.4.2	4.3.4	7	3.2+
1.4.3	4.3.5	7	3.2+
1.4.4	4.3.6	7	3.2+
1.4.5	4.3.7	7	3.2+
1.4.6	4.3.8	7	3.2+
1.4.7	4.3.9	7	3.2+
1.5.0	4.3.6	7	3.2+
1.5.2	4.3.7	7	3.2+
1.5.3	4.3.8	7	3.2+
1.5.4	4.3.9	7	3.2+
1.5.5	4.3.10	7	3.2+
1.5.7	4.3.11	7	3.2+
1.5.8	4.3.12	7	3.2+
1.5.9	4.3.13	7	3.2+
2.0.0	5.0.2	8	3.2+

```