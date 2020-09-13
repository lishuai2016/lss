

<!-- TOC -->

- [1、DataSourceAutoConfiguration](#1datasourceautoconfiguration)
- [2、DataSourceProperties](#2datasourceproperties)
- [3、ConfigurationProperties注解](#3configurationproperties注解)
- [9、如何自定义数据源](#9如何自定义数据源)
- [问题](#问题)
- [参考](#参考)

<!-- /TOC -->

spring boot 启动时会默认加载org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration这个类，

DataSourceAutoConfiguration类使用了@Configuration注解向spring注入了dataSource bean。因为工程中没有关于dataSource相关的配置信息，当spring创建dataSource bean因缺少相关的信息就会报错。


默认会加载 spring.datasource开头的配置来构建数据源

```yml
spring.datasource.url=jdbc:mysql://127.0.0.1:3306
spring.datasource.username=root
spring.datasource.password=111
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource    # 来指定数据源的类型
spring.datasource.initialSize=5
spring.datasource.minIdle=10
spring.datasource.maxActive=20

```



# 1、DataSourceAutoConfiguration


```java
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@EnableConfigurationProperties({DataSourceProperties.class})//从配置文件中映射 DataSource 的值
@Import({DataSourcePoolMetadataProvidersConfiguration.class, DataSourceInitializationConfiguration.class})
public class DataSourceAutoConfiguration {
    public DataSourceAutoConfiguration() {
    }

    static class EmbeddedDatabaseCondition extends SpringBootCondition {
        private final SpringBootCondition pooledCondition = new DataSourceAutoConfiguration.PooledDataSourceCondition();

        EmbeddedDatabaseCondition() {
        }

        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Builder message = ConditionMessage.forCondition("EmbeddedDataSource", new Object[0]);
            if (this.anyMatches(context, metadata, new Condition[]{this.pooledCondition})) {
                return ConditionOutcome.noMatch(message.foundExactly("supported pooled data source"));
            } else {
                EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(context.getClassLoader()).getType();
                return type == null ? ConditionOutcome.noMatch(message.didNotFind("embedded database").atAll()) : ConditionOutcome.match(message.found("embedded database").items(new Object[]{type}));
            }
        }
    }

    static class PooledDataSourceAvailableCondition extends SpringBootCondition {
        PooledDataSourceAvailableCondition() {
        }

        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Builder message = ConditionMessage.forCondition("PooledDataSource", new Object[0]);
            return this.getDataSourceClassLoader(context) != null ? ConditionOutcome.match(message.foundExactly("supported DataSource")) : ConditionOutcome.noMatch(message.didNotFind("supported DataSource").atAll());
        }

        private ClassLoader getDataSourceClassLoader(ConditionContext context) {
            Class<?> dataSourceClass = DataSourceBuilder.findType(context.getClassLoader());
            return dataSourceClass == null ? null : dataSourceClass.getClassLoader();
        }
    }

    static class PooledDataSourceCondition extends AnyNestedCondition {
        PooledDataSourceCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional({DataSourceAutoConfiguration.PooledDataSourceAvailableCondition.class})
        static class PooledDataSourceAvailable {
            PooledDataSourceAvailable() {
            }
        }

        @ConditionalOnProperty(
            prefix = "spring.datasource",
            name = {"type"}
        )
        static class ExplicitType {
            ExplicitType() {
            }
        }
    }

    @Configuration
    @Conditional({DataSourceAutoConfiguration.PooledDataSourceCondition.class})
    @ConditionalOnMissingBean({DataSource.class, XADataSource.class})
    @Import({Hikari.class, Tomcat.class, Dbcp2.class, Generic.class, DataSourceJmxConfiguration.class})
    protected static class PooledDataSourceConfiguration {
        protected PooledDataSourceConfiguration() {
        }
    }

    @Configuration
    @Conditional({DataSourceAutoConfiguration.EmbeddedDatabaseCondition.class})
    @ConditionalOnMissingBean({DataSource.class, XADataSource.class})
    @Import({EmbeddedDataSourceConfiguration.class})
    protected static class EmbeddedDatabaseConfiguration {
        protected EmbeddedDatabaseConfiguration() {
        }
    }
}

```

# 2、DataSourceProperties


```java
@ConfigurationProperties(
    prefix = "spring.datasource"
)
public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {
    private ClassLoader classLoader;
    private String name;
    private boolean generateUniqueName;
    private Class<? extends DataSource> type;
    private String driverClassName;
    private String url;
    private String username;
    private String password;
    private String jndiName;
    private DataSourceInitializationMode initializationMode;
    private String platform;
    private List<String> schema;
    private String schemaUsername;
    private String schemaPassword;
    private List<String> data;
    private String dataUsername;
    private String dataPassword;
    private boolean continueOnError;
    private String separator;
    private Charset sqlScriptEncoding;
    private EmbeddedDatabaseConnection embeddedDatabaseConnection;
    private DataSourceProperties.Xa xa;
    private String uniqueName;
```


# 3、ConfigurationProperties注解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConfigurationProperties {
    @AliasFor("prefix")
    String value() default "";

    @AliasFor("value")
    String prefix() default "";

    boolean ignoreInvalidFields() default false;

    boolean ignoreUnknownFields() default true;
}
```






# 9、如何自定义数据源
- 1、每个数据源都有自己的属性，
- 2、通过 DataSourceBuilder 类来创建数据源实例
- 3、通过 @ConfigurationProperties() 注解，将配置文件中的属性，映射到相应的数据源实例中
- 4、DataSourcePoolMetadataProvidersConfiguration 中只配置了默认的3个数据源，如果需要，需要自己定义它
- 5、当配置多个数据源的时候添加注解@Primary的那个被当做默认的数据源。

```java
@Bean(name = "druidDatasource")
@ConfigurationProperties("ls.druid.datasource")
public DataSource druidDatasource() {
    return DataSourceBuilder.create().type(DruidDataSource.class).build();
}
```

```java
@Bean(name = "primaryDataSource")
@Primary
@ConfigurationProperties(prefix = "spring.datasource.primary")
public DataSource costDataSource() {
    return DruidDataSourceBuilder.create().build();
}
```


# 问题

2、spring boot集成mybatis，启动报无法创建dataSource问题
分析原因：
这是因为spring boot 会默认加载org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration这个类，
DataSourceAutoConfiguration类使用了@Configuration注解向spring注入了dataSource bean。因为工程中没有关于dataSource相关的配置信息，当spring创建dataSource bean因缺少相关的信息就会报错。

@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})


3、Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.
这是因为添加了数据库组件，所以autoconfig会去读取数据源配置，而新建的项目还没有配置数据源，所以会导致异常出现。
解决办法：
去掉数据库依赖在启动类的@EnableAutoConfiguration或@SpringBootApplication中添加exclude = {DataSourceAutoConfiguration.class}，排除此类的autoconfig。启动以后就可以正常运行


# 参考


- [Springboot 模块分析 —— DataSourceAutoConfiguration 解析](https://blog.csdn.net/kangsa998/article/details/90231518)

