
<!-- TOC -->

- [1、Java中的元注解](#1java中的元注解)
    - [1、@Override](#1override)
    - [2、@Retention](#2retention)
    - [3、@Target](#3target)
    - [4、@Documented](#4documented)
- [2、spring的bean容器相关的注解](#2spring的bean容器相关的注解)
    - [1、@Autowired](#1autowired)
    - [2、@Qualifier](#2qualifier)
    - [3、@Resource](#3resource)
    - [4、javax.inject.*](#4javaxinject)
    - [5、@Component添加在类上的注解](#5component添加在类上的注解)
    - [6、bean的生命周期@PostConstruct 和 @PreDestroy](#6bean的生命周期postconstruct-和-predestroy)
- [3、spring中注解工作原理](#3spring中注解工作原理)
- [参考](#参考)

<!-- /TOC -->

Spring中的注解大概可以分为两大类：

1）spring的bean容器相关的注解，或者说bean工厂相关的注解:@Required， @Autowired, @PostConstruct, @PreDestory，还有Spring3.0开始支持的JSR-330标准javax.inject.*中的注解(@Inject, @Named, @Qualifier, @Provider, @Scope, @Singleton)

2）springmvc相关的注解:@Controller, @RequestMapping, @RequestParam， @ResponseBody等等。

# 1、Java中的元注解

## 1、@Override

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```

@Override的作用是，提示编译器，使用了@Override注解的方法必须override父类或者java.lang.Object中的一个同名方法。我们看到@Override的定义中使用到了 @Target, @Retention，它们就是所谓的“元注解”——就是定义注解的注解


## 2、@Retention

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {
    /**
     * Returns the retention policy.
     * @return the retention policy
     */
    RetentionPolicy value();
}
```
@Retention用于提示注解被保留多长时间，有三种取值：

- RetentionPolicy.SOURCE 保留在源码级别，被编译器抛弃(@Override就是此类)； 
- RetentionPolicy.CLASS被编译器保留在编译后的类文件级别，但是被虚拟机丢弃；
- RetentionPolicy.RUNTIME保留至运行时，可以被反射读取。



## 3、@Target

描述注解可以添加的位置

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Target {
    ElementType[] value();
}
```
```java
public enum ElementType {
    /** Class, interface (including annotation type), or enum declaration */
    TYPE,
    /** Field declaration (includes enum constants) */
    FIELD,
    /** Method declaration */
    METHOD,
    /** Formal parameter declaration */
    PARAMETER,
    /** Constructor declaration */
    CONSTRUCTOR,
    /** Local variable declaration */
    LOCAL_VARIABLE,
    /** Annotation type declaration */
    ANNOTATION_TYPE,
    /** Package declaration */
    PACKAGE,
    /**
     * Type parameter declaration
     * @since 1.8
     */
    TYPE_PARAMETER,
    /**
     * Use of a type
     * @since 1.8
     */
    TYPE_USE
}
```
分别表示该注解可以被使用的地方：1)类,接口，注解，enum; 2)属性域；3）方法；4）参数；5）构造函数；6）局部变量；7）注解类型；8）包


## 4、@Documented

```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Documented {
}
```
表示注解是否能被 javadoc 处理并保留在文档中。


`有了元注解，那么我就可以使用它来自定义我们需要的注解。`



# 2、spring的bean容器相关的注解

## 1、@Autowired 

是我们使用得最多的注解，其实就是 autowire=byType 就是根据类型的自动注入依赖（基于注解的依赖注入），可以被使用再属性域，方法，构造函数上。

## 2、@Qualifier 

就是 autowire=byName, @Autowired注解判断多个bean类型相同时，就需要使用 @Qualifier("xxBean") 来指定依赖的bean的id。

## 3、@Resource 

属于JSR250标准，用于属性域额和方法上。也是 byName 类型的依赖注入。使用方式：@Resource(name="xxBean"). 不带参数的 @Resource 默认值类名首字母小写。

## 4、javax.inject.*

JSR-330标准javax.inject.*中的注解(@Inject, @Named, @Qualifier, @Provider, @Scope, @Singleton)。@Inject就相当于@Autowired, @Named 就相当于 @Qualifier, 另外 @Named 用在类上还有 @Component的功能。

## 5、@Component添加在类上的注解

@Component， @Controller, @Service, @Repository, 这几个注解不同于上面的注解，上面的注解都是将被依赖的bean注入进入，而这几个注解的作用都是生产bean, 这些注解都是注解在类上，将类注解成spring的bean工厂中一个一个的bean。@Controller, @Service, @Repository基本就是语义更加细化的@Component。

## 6、bean的生命周期@PostConstruct 和 @PreDestroy

@PostConstruct 和 @PreDestroy 不是用于依赖注入，而是bean 的生命周期。类似于 init-method(InitializeingBean) destory-method(DisposableBean)

# 3、spring中注解工作原理

spring中注解的处理基本都是通过实现接口 BeanPostProcessor 来进行的：

```java
public interface BeanPostProcessor {
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
}
```

相关的处理类有： AutowiredAnnotationBeanPostProcessor，CommonAnnotationBeanPostProcessor，PersistenceAnnotationBeanPostProcessor，  RequiredAnnotationBeanPostProcessor

这些处理类，可以通过 <context:annotation-config/> 配置隐式的配置进spring容器。这些都是依赖注入的处理，还有生产bean的注解(@Component， @Controller, @Service, @Repository)的处理：

<context:component-scan base-package="xxx" />

这些都是通过指定扫描的基包路径来进行的，将他们扫描进spring的bean容器。注意 context:component-scan 也会默认将 AutowiredAnnotationBeanPostProcessor，CommonAnnotationBeanPostProcessor 配置进来。所以<context:annotation-config/>是可以省略的。另外context:component-scan也可以扫描@Aspect风格的AOP注解，但是需要在配置文件中加入 <aop:aspectj-autoproxy/> 进行配合。



# 参考

https://blog.csdn.net/honghailiang888/article/details/74981445
http://www.importnew.com/10294.html

- [深入理解spring中的各种注解](https://www.cnblogs.com/digdeep/p/4525567.html)