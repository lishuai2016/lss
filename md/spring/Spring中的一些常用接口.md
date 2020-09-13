
<!-- TOC -->

- [1、ApplicationContextAware接口](#1applicationcontextaware接口)
- [2、ServletContextAware 接口](#2servletcontextaware-接口)
- [3、InitializingBean 接口](#3initializingbean-接口)
- [4、ApplicationListener<ApplicationEvent> 接口](#4applicationlistenerapplicationevent-接口)

<!-- /TOC -->


Spring中的一些常用接口

# 1、ApplicationContextAware接口

```java
public interface ApplicationContextAware extends Aware {
    void setApplicationContext(ApplicationContext var1) throws BeansException;
}
```

可以在spring容器初始化的时候调用setApplicationContext方法，从而获得ApplicationContext中的所有bean。

# 2、ServletContextAware 接口

```java
public interface ServletContextAware extends Aware {
    void setServletContext(ServletContext var1);
}
```

# 3、InitializingBean 接口

这个方法将在所有的属性被初始化后调用，但是会在init前调用，在spring初始化bean的时候，如果该bean是实现了InitializingBean接口，并且同时在配置文件中指定了init-method，系统则是先调用afterPropertiesSet方法，然后在调用init-method中指定的方法。

```java
public interface InitializingBean {
    void afterPropertiesSet() throws Exception;
}
```

# 4、ApplicationListener<ApplicationEvent> 接口

```java
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {
    void onApplicationEvent(E var1);
}
```

执行顺序

```java
@Component
public class StartupListener implements ApplicationContextAware, ServletContextAware,
        InitializingBean, ApplicationListener<ContextRefreshedEvent> {
 
    protected Logger logger = LogManager.getLogger();
 
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        logger.info("1 => StartupListener.setApplicationContext");
    }
 
    @Override
    public void setServletContext(ServletContext context) {
        logger.info("2 => StartupListener.setServletContext");
    }
 
    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("3 => StartupListener.afterPropertiesSet");
    }
 
    @Override
    public void onApplicationEvent(ContextRefreshedEvent evt) {
        logger.info("4.1 => MyApplicationListener.onApplicationEvent");
        if (evt.getApplicationContext().getParent() == null) {
            logger.info("4.2 => MyApplicationListener.onApplicationEvent");
        }
    }
 
}
```

运行时，输出的顺序如下：

```
1 => StartupListener.setApplicationContext
2 => StartupListener.setServletContext
3 => StartupListener.afterPropertiesSet
4.1 => MyApplicationListener.onApplicationEvent
4.2 => MyApplicationListener.onApplicationEvent
4.1 => MyApplicationListener.onApplicationEvent
```