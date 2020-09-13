
<!-- TOC -->

- [1、CommandLineRunner和ApplicationRunner对比](#1commandlinerunner和applicationrunner对比)
- [2、ApplicationRunner](#2applicationrunner)
- [3、CommandLineRunner](#3commandlinerunner)

<!-- /TOC -->


# 1、CommandLineRunner和ApplicationRunner对比

在开发中可能会有这样的情景。需要在容器启动的时候执行一些内容。比如读取配置文件，数据库连接之类的。SpringBoot给我们提供了两个接口来帮助我们实现这种需求。这两个接口分别为CommandLineRunner和ApplicationRunner。他们的执行时机为容器启动完成的时候。这两个接口中有一个run方法，我们只需要实现这个方法即可。不同之处在于：ApplicationRunner中run方法的参数为ApplicationArguments，而CommandLineRunner接口中run方法的参数为String数组


备注：从执行结果来看先执行ApplicationRunner接口方法，再执行CommandLineRunner接口方法。

# 2、ApplicationRunner

接口定义

```java
package org.springframework.boot;

@FunctionalInterface
public interface ApplicationRunner {
    void run(ApplicationArguments var1) throws Exception;
}
```

应用示例

```java
@Component
public class MyApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println(args);
        System.out.println("测试ApplicationRunner接口");
    }
}
```


# 3、CommandLineRunner

接口定义

```java
package org.springframework.boot;

@FunctionalInterface
public interface CommandLineRunner {
    void run(String... var1) throws Exception;
}
```

使用示例

```java
@Component
public class MyCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println(args);
        System.out.println("测试CommandLineRunner接口");
    }
}
```




