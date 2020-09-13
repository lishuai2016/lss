


<!-- TOC -->

- [1、spring的数据源连接池路由抽象类AbstractRoutingDataSource](#1spring的数据源连接池路由抽象类abstractroutingdatasource)
- [2、DynamicDataSourceContextHolder当前请求线程使用的数据源holder](#2dynamicdatasourcecontextholder当前请求线程使用的数据源holder)
- [3、继承spring-jdbc的AbstractRoutingDataSource](#3继承spring-jdbc的abstractroutingdatasource)
- [4、把自己创建的DynamicDataSource放入spring容器](#4把自己创建的dynamicdatasource放入spring容器)
- [5、如何动态切换查询的数据源](#5如何动态切换查询的数据源)
    - [1：通过自定义注解来标识当前执行方法走哪个数据库查询。](#1通过自定义注解来标识当前执行方法走哪个数据库查询)
        - [1、定义注解](#1定义注解)
        - [2、定义注解拦截处理切面](#2定义注解拦截处理切面)
    - [2、不使用切面](#2不使用切面)

<!-- /TOC -->


spring-jdbc动态数据源



> 原理总结：

在配置中spring容器中识别DynamicDataSource对象是当前服务的数据库连接池，而在DynamicDataSource继承spring-jdbc中的AbstractRoutingDataSource，可以在其内部通过一个Map<Object, DataSource>维护多个自定义的数据库链接池。当程序调用DynamicDataSource.getConnection()获取链接的时候，通过扩展接口函数determineCurrentLookupKey()，让用户来决定当前查询使用那个数据库连接池，这样就实现了动态替换查询不同的数据库。

//一种思路是在线程的环境变量中获取该选择那个数据源执行SQL查询

protected Object determineCurrentLookupKey() { return DynamicDataSourceContextHolder.getDataSourceType(); }


# 1、spring的数据源连接池路由抽象类AbstractRoutingDataSource

```java
public abstract class AbstractRoutingDataSource extends AbstractDataSource implements InitializingBean {
    @Nullable
    private Map<Object, Object> targetDataSources;;//用户传入，数据源识别key和对应的数据源map
    @Nullable
    private Object defaultTargetDataSource;//用户传入，默认是用的数据源识别key
    private boolean lenientFallback = true;
    private DataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
    @Nullable
    private Map<Object, DataSource> resolvedDataSources;//解析后的数据源识别key和数据源映射map，程序运行时查询该map，对应targetDataSources
    @Nullable
    private DataSource resolvedDefaultDataSource;//解析后的默认数据源，对应defaultTargetDataSource

    public AbstractRoutingDataSource() {
    }

    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources = targetDataSources;
    }

    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        this.defaultTargetDataSource = defaultTargetDataSource;
    }

    public void setLenientFallback(boolean lenientFallback) {
        this.lenientFallback = lenientFallback;
    }

    public void setDataSourceLookup(@Nullable DataSourceLookup dataSourceLookup) {
        this.dataSourceLookup = (DataSourceLookup)(dataSourceLookup != null ? dataSourceLookup : new JndiDataSourceLookup());
    }

    public void afterPropertiesSet() {
        if (this.targetDataSources == null) {
            throw new IllegalArgumentException("Property 'targetDataSources' is required");
        } else {
            this.resolvedDataSources = new HashMap(this.targetDataSources.size());
            this.targetDataSources.forEach((key, value) -> {
                Object lookupKey = this.resolveSpecifiedLookupKey(key);
                DataSource dataSource = this.resolveSpecifiedDataSource(value);
                this.resolvedDataSources.put(lookupKey, dataSource);
            });
            if (this.defaultTargetDataSource != null) {
                this.resolvedDefaultDataSource = this.resolveSpecifiedDataSource(this.defaultTargetDataSource);
            }

        }
    }

    protected Object resolveSpecifiedLookupKey(Object lookupKey) {
        return lookupKey;
    }

    protected DataSource resolveSpecifiedDataSource(Object dataSource) throws IllegalArgumentException {
        if (dataSource instanceof DataSource) {
            return (DataSource)dataSource;
        } else if (dataSource instanceof String) {
            return this.dataSourceLookup.getDataSource((String)dataSource);
        } else {
            throw new IllegalArgumentException("Illegal data source value - only [javax.sql.DataSource] and String supported: " + dataSource);
        }
    }

    public Connection getConnection() throws SQLException {//关键，获取数据源链接
        return this.determineTargetDataSource().getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return this.determineTargetDataSource().getConnection(username, password);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return iface.isInstance(this) ? this : this.determineTargetDataSource().unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || this.determineTargetDataSource().isWrapperFor(iface);
    }

    protected DataSource determineTargetDataSource() {//核心：实现数据源替换
        Assert.notNull(this.resolvedDataSources, "DataSource router not initialized");
        Object lookupKey = this.determineCurrentLookupKey();//扩展抽象方法，让用户根据参数来决定使用那个数据源
        DataSource dataSource = (DataSource)this.resolvedDataSources.get(lookupKey);
        if (dataSource == null && (this.lenientFallback || lookupKey == null)) {
            dataSource = this.resolvedDefaultDataSource;
        }

        if (dataSource == null) {
            throw new IllegalStateException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
        } else {
            return dataSource;
        }
    }

    @Nullable
    protected abstract Object determineCurrentLookupKey();//核心。一般的实现，可以使用当前线程的变量来保存数据源切换识别key
}
```

# 2、DynamicDataSourceContextHolder当前请求线程使用的数据源holder

```java
public class DynamicDataSourceContextHolder {
    public static final Logger log = LoggerFactory.getLogger(DynamicDataSourceContextHolder.class);

    /**
     * 使用ThreadLocal维护变量，ThreadLocal为每个使用该变量的线程提供独立的变量副本，
     *  所以每一个线程都可以独立地改变自己的副本，而不会影响其它线程所对应的副本。
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置数据源的变量
     */
    public static void setDataSourceType(String dsType)
    {
        log.info("切换到{}数据源", dsType);
        CONTEXT_HOLDER.set(dsType);
    }

    /**
     * 获得数据源的变量
     */
    public static String getDataSourceType()
    {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清空数据源变量
     */
    public static void clearDataSourceType()
    {
        CONTEXT_HOLDER.remove();
    }
}
```



# 3、继承spring-jdbc的AbstractRoutingDataSource

整合自己的各个自定义的数据源封装进spring-jdbc的入口

```java
//自定义的动态数据源类
public class DynamicDataSource extends AbstractRoutingDataSource {
    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }
}
```


# 4、把自己创建的DynamicDataSource放入spring容器

```java
//配置类，把动态数据源实例添加到spring容器中去，通过@Primary注解，spring会把所有的查询SQL操作都交给该实例，然后再由该实例分发不同的数据库进行查询
@Configuration
public class DruidConfig{
 	@Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource(DataSource firstDataSource, DataSource secondDataSource) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceNames.FIRST, firstDataSource);
        targetDataSources.put(DataSourceNames.SECOND, secondDataSource);
        return new DynamicDataSource(firstDataSource, targetDataSources);
    }
}
```




# 5、如何动态切换查询的数据源



那如何知道那个请求走哪个数据源？


## 1：通过自定义注解来标识当前执行方法走哪个数据库查询。

### 1、定义注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataSource{
    String name() default ""; //指定数据源（多数据源配置）
}
```



### 2、定义注解拦截处理切面

```java
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class DataSourceAspect {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Pointcut("@annotation(com.jd.framework.aspectj.lang.annotation.DataSource)")
    public void dsPointCut(){}

    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        MethodSignature signature = (MethodSignature) point.getSignature();

        Method method = signature.getMethod();

        DataSource dataSource = method.getAnnotation(DataSource.class);

        //默认的为第一个数据源
        if (dataSource == null) {
            DynamicDataSourceContextHolder.setDataSourceType(DataSourceNames.FIRST);
            logger.debug("不带注解的数据源设置set datasource is " + DataSourceNames.FIRST);
        } else {
            DynamicDataSourceContextHolder.setDataSourceType(dataSource.name()); //根据注解的名称来设置指定的数据源
            logger.debug("带注解数据源设置set datasource is " + dataSource.name());
        }

        System.out.println("DataSource name is : "+ dataSource.name());
        try {
            return point.proceed();
        } finally {
            // 销毁数据源 在执行方法之后
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }
}
```


切面的处理逻辑是根据注解参数解析到当前方法使用那个数据库标识码，然后把识别码放到线程环境变量中，在方法处理完毕，清空当前线程的数据源识别码。



## 2、不使用切面

通过变量识别到当前查询要走哪一个数据源，然后把当前查询要使用的数据源识别码放到线程环境变量中，如:DynamicDataSourceContextHolder,然后剩下的切换原理和上面一样。











































