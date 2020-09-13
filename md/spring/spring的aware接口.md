


类似的XXXAware接口就是对该类提供Spring感知,简单来说就是如果想使用Spring的XXXX就要实现XXXAware,spring会把需要的东西传送过来.


todo


# 1、Aware空接口，应该是一种标识

```java
public interface Aware {
}
```

# 2、BeanClassLoaderAware


```java
public interface BeanClassLoaderAware extends Aware {
    void setBeanClassLoader(ClassLoader var1);
}

```

# 3、BeanFactoryAware


```java
public interface BeanFactoryAware extends Aware {
    void setBeanFactory(BeanFactory var1) throws BeansException;
}
```



# 4、FactoryBean


```java
public interface FactoryBean<T> {
    T getObject() throws Exception;

    Class<?> getObjectType();

    boolean isSingleton();
}

```




# 5、ProxyFactoryBean


```java
public class ProxyFactoryBean extends ProxyCreatorSupport implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {


}

```





















