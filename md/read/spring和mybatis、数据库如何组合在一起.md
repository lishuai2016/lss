

# 参考
- [mybatis-3](https://mybatis.org/mybatis-3/zh/index.html)
- [MyBatis-Spring](http://mybatis.org/spring/zh/index.html)
- [Spring与Mybatis整合的MapperScannerConfigurer处理过程源码分析](https://www.2cto.com/kf/201409/331321.html)

> 方式1：直接操作sqlSessionTemplate

```xml
常见的配置格式
1、数据源
<bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
	<property name="validationQuery"><value>SELECT 1</value></property>
...
</bean>

2、sqlSessionFactory工厂
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="configLocation" value="classpath:spring/sqlmap-config.xml"></property>
        <property name="configurationProperties">
            <props>
                <prop key="cacheEnabled">true</prop>
                <prop key="lazyLoadingEnabled">true</prop>
                <prop key="aggressiveLazyLoading">false</prop>
                <prop key="multipleResultSetsEnabled">true</prop>
                <prop key="useColumnLabel">true</prop>
                <prop key="useGeneratedKeys">true</prop>
                <prop key="autoMappingBehavior">FULL</prop>
                <prop key="defaultExecutorType">BATCH</prop>
                <prop key="defaultStatementTimeout">25000</prop>
            </props>
        </property>
        <property name="plugins">
            <list>
                <!--SQL监控插件-->
                <ref bean="monitor"/>
            </list>
        </property>
</bean>

3、sqlSessionTemplate
<bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate" scope="prototype">
	<constructor-arg index="0" ref="sqlSessionFactory"/>
</bean>

4、事务
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
</bean>

<tx:annotation-driven transaction-manager="transactionManager"/>

```

> 方式2：使用.Mapper

```xml
<!-- Mapper接口所在包名，Spring会自动查找其下的类 -->
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
	<property name="basePackage" value="com.mapper"/> 写的mapper接口类所在的包路径
	<property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
</bean>
```



spring和Mybatis整合过程

1、通过SqlSessionFactoryBean，使用FactoryBean的方式生成了sqlSessionFactory。

2、使用SqlSessionTemplate方式

通过SqlSessionTemplate代理SqlSession，实现通过sqlSessionFactory生成SqlSession。spring通过SqlSessionTemplate执行SQL的过程委托给通过sqlSessionFactory生成SqlSession去执行。具体的过程是通过SqlSessionTemplate构造函数，根据入参sqlSessionFactory通过jdk的松台代理生成一个sqlSessionProxy对象.

[请求的处理流程：SqlSessionTemplate--->sqlSessionProxy--->sqlSession真正的执行SQL，由sqlSessionFactory生成]

```java
this.sqlSessionProxy = (SqlSession)Proxy.newProxyInstance(SqlSessionFactory.class.getClassLoader(), new Class[]{SqlSession.class}, new SqlSessionTemplate.SqlSessionInterceptor());
```

具体的处理逻辑由SqlSessionTemplate.SqlSessionInterceptor()进行处理。核心代码：
```java
SqlSession sqlSession = SqlSessionUtils.getSqlSession(SqlSessionTemplate.this.sqlSessionFactory, SqlSessionTemplate.this.executorType, SqlSessionTemplate.this.exceptionTranslator);
Object result = method.invoke(sqlSession, args);
```

3、使用mapper方式

这段配置会扫描org.mybatis.spring.sample.mapper下的所有接口，然后创建各自接口的动态代理类。[MapperProxy]

然后再执行自己编写的mapper的时候其实执行的是这个代理对象。





