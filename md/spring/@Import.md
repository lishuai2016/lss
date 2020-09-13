
<!-- TOC -->

- [1、@Import功能](#1import功能)
- [2、@Import的三种用法](#2import的三种用法)
    - [1、直接填class数组方式](#1直接填class数组方式)
    - [2、ImportSelector方式【重点】](#2importselector方式重点)
    - [3、ImportBeanDefinitionRegistrar方式](#3importbeandefinitionregistrar方式)
- [参考](#参考)

<!-- /TOC -->

# 1、@Import功能

- 1、@Import只能用在类上 ，@Import通过快速导入的方式实现把实例加入spring的IOC容器中；

- 2、加入IOC容器的方式有很多种，@Import注解可以用于导入第三方包 ，当然@Bean注解也可以，但是@Import注解快速导入的方式更加便捷；


# 2、@Import的三种用法

```java
public class Demo {
}
public class Demo1 {
}
```

下面提供了三种方式实现把Demo和Demo1类注入到spring容器中。

## 1、直接填class数组方式

```java
/**
1、直接填class数组方式。

对应的import的bean都将加入到spring容器中，这些在容器中bean名称是该类的全类名，如：
com.ls.testImport.Demo
com.ls.testImport.Demo1
 */
@Import({Demo.class,Demo1.class})
public class TestStyle1 {
}
```

测试程序

```java
 public static void t1() {
        //这里的参数代表要做操作的类
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(TestStyle1.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        System.out.println("容器中bean对象的个数："+beanDefinitionNames.length);
        for (String name : beanDefinitionNames){
            System.out.println(name);
        }
    }
```

备注：只需要把需要注入的类放到@Import的数组即可。类的名称是类的全限定名


## 2、ImportSelector方式【重点】

这里需要定义一个类实现接口ImportSelector，而返回值即是需要注入到spring容器中的类的全路径，而AnnotationMetadata提供了当前@Import注解添加在哪里的位置信息。

```java
public interface ImportSelector {
    String[] selectImports(AnnotationMetadata var1);
}
```

自己实现类

```java
public class Myclass implements ImportSelector {
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{"com.ls.testImport.Demo1","com.ls.testImport.Demo"};// 就是我们实际上要导入到容器中的组件全类名
    }
}

```

```java
/**
2、ImportSelector方式
 */
@Import({Myclass.class})
public class TestStyle2 {
}
```

```java
public static void t2() {
        //这里的参数代表要做操作的类
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(TestStyle2.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        System.out.println("容器中bean对象的个数："+beanDefinitionNames.length);
        for (String name : beanDefinitionNames){
            System.out.println(name);
        }
    }
```

备注：类的名称是类的全限定名

## 3、ImportBeanDefinitionRegistrar方式

自定义类需要实现接口ImportBeanDefinitionRegistrar，其中AnnotationMetadata也是通过@import注解加在哪里的位置信息，BeanDefinitionRegistry可以当成spring容器，我们需要往容器中注入什么类，自己添加就行。

```java
public interface ImportBeanDefinitionRegistrar {
    void registerBeanDefinitions(AnnotationMetadata var1, BeanDefinitionRegistry var2);
}
```

```java
public class Myclass2 implements ImportBeanDefinitionRegistrar {
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {

        //指定bean定义信息（包括bean的类型、作用域...）
        RootBeanDefinition demo1 = new RootBeanDefinition(Demo1.class);
        RootBeanDefinition demo2 = new RootBeanDefinition(Demo1.class);
        //注册一个bean指定bean名字（id）
        beanDefinitionRegistry.registerBeanDefinition("TestDemo1",demo1);
        beanDefinitionRegistry.registerBeanDefinition("TestDemo2",demo2);
    }
}
```

```java
@Import({Myclass2.class})
public class TestStyle3 {
}
```


```java
public static void t3() {
        //这里的参数代表要做操作的类
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(TestStyle3.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        System.out.println("容器中bean对象的个数："+beanDefinitionNames.length);
        for (String name : beanDefinitionNames){
            System.out.println(name);
        }
    }
```



# 参考

- [Spring @Import注解 —— 导入资源](https://blog.csdn.net/pange1991/article/details/81356594)
- [spring注解之@Import注解的三种使用方式](https://www.cnblogs.com/yichunguo/p/12122598.html)
