
<!-- TOC -->

- [0、架构](#0架构)
    - [1、客户端调用流程](#1客户端调用流程)
- [0、暴露给用户使用API](#0暴露给用户使用api)
    - [1、ServerConfig和ProviderConfig](#1serverconfig和providerconfig)
        - [1、ServerConfig](#1serverconfig)
        - [2、ProviderConfig](#2providerconfig)
    - [2、ConsumerConfig](#2consumerconfig)
    - [3、RegistryConfig](#3registryconfig)
- [1、自定义SPI](#1自定义spi)
    - [1、ExtensionLoaderFactory扩展类加载器缓存工厂](#1extensionloaderfactory扩展类加载器缓存工厂)
    - [2、ExtensionLoader(只读取被注解Extensible标记抽象类或者接口)](#2extensionloader只读取被注解extensible标记抽象类或者接口)
    - [3、ExtensionClass](#3extensionclass)
    - [4、Extensible](#4extensible)
    - [5、Extension](#5extension)
- [2、ProviderBootstrap发布服务的包装类，包括具体的启动后的对象](#2providerbootstrap发布服务的包装类包括具体的启动后的对象)
    - [1、DefaultProviderBootstrap（sofa）默认实现](#1defaultproviderbootstrapsofa默认实现)
- [3、服务提供者server](#3服务提供者server)
    - [1、BoltServer](#1boltserver)
    - [2、BoltServerProcessor业务处理client端发送的请求](#2boltserverprocessor业务处理client端发送的请求)
- [4、ConsumerBootstrap服务消费者启动类](#4consumerbootstrap服务消费者启动类)
    - [01、DefaultConsumerBootstrap（sofa）默认实现](#01defaultconsumerbootstrapsofa默认实现)
    - [02、Cluster](#02cluster)
        - [1、AbstractCluster主要逻辑在这里实现](#1abstractcluster主要逻辑在这里实现)
            - [1、建立client连接过程](#1建立client连接过程)
        - [2、FailoverCluster故障转移，支持重试和指定地址调用](#2failovercluster故障转移支持重试和指定地址调用)
        - [3、FailFastCluster快速失败](#3failfastcluster快速失败)
    - [03、Proxy（SPI）生成消费端的代理对象](#03proxyspi生成消费端的代理对象)
    - [04、LoadBalancer负载均衡器：从一堆Provider列表里选出一个](#04loadbalancer负载均衡器从一堆provider列表里选出一个)
        - [1、AbstractLoadBalancer提供如何选择服务的模板](#1abstractloadbalancer提供如何选择服务的模板)
        - [2、RandomLoadBalancer负载均衡随机算法:全部列表按权重随机选择](#2randomloadbalancer负载均衡随机算法全部列表按权重随机选择)
        - [3、LocalPreferenceLoadBalancer本机优先的随机算法（看服务提供者的IP是否和本机IP一样）](#3localpreferenceloadbalancer本机优先的随机算法看服务提供者的ip是否和本机ip一样)
        - [4、RoundRobinLoadBalancer负载均衡轮询算法，按方法级进行轮询，互不影响](#4roundrobinloadbalancer负载均衡轮询算法按方法级进行轮询互不影响)
        - [5、ConsistentHashLoadBalancer一致性hash算法，同样的请求（第一参数）会打到同样的节点](#5consistenthashloadbalancer一致性hash算法同样的请求第一参数会打到同样的节点)
    - [05、ConnectionHolder（SPI）](#05connectionholderspi)
        - [1、AllConnectConnectionHolder全部建立长连接，自动维护长连接](#1allconnectconnectionholder全部建立长连接自动维护长连接)
        - [2、ElasticConnectionHolder弹性长连接，可按百分比配置以及按个数配置](#2elasticconnectionholder弹性长连接可按百分比配置以及按个数配置)
    - [06、ClientTransport网络通信层](#06clienttransport网络通信层)
        - [1、BoltClientTransport](#1boltclienttransport)
    - [07、Router路由器：从一堆Provider中筛选出一堆Provider](#07router路由器从一堆provider中筛选出一堆provider)
        - [1、RegistryRouter从注册中心获取地址进行路由](#1registryrouter从注册中心获取地址进行路由)
    - [08、ProviderInfoListener服务提供者信息监听器](#08providerinfolistener服务提供者信息监听器)
    - [09、AddressHolder(SPI)](#09addressholderspi)
        - [1、SingleGroupAddressHolder只是在内部通过变量保存ProviderGroup（外部传入的）](#1singlegroupaddressholder只是在内部通过变量保存providergroup外部传入的)
    - [10、ProviderGroup一个分组服务提供者](#10providergroup一个分组服务提供者)
- [5、Invoker调用器](#5invoker调用器)
    - [1、ProviderProxyInvoker服务提供者实现代理类](#1providerproxyinvoker服务提供者实现代理类)
    - [2、ProviderInvoker服务提供者接口实现类封装[具体的业务逻辑]](#2providerinvoker服务提供者接口实现类封装具体的业务逻辑)
    - [3、FilterChain[这里应该是封装一些过滤器，最后把请求交给接口的实现类]](#3filterchain这里应该是封装一些过滤器最后把请求交给接口的实现类)
        - [1、Filter](#1filter)
- [6、RegistryConfig注册中心配置](#6registryconfig注册中心配置)
    - [1、Registry（SPI）](#1registryspi)
    - [2、ZookeeperRegistry基于zookeeper的注册中心](#2zookeeperregistry基于zookeeper的注册中心)
- [参考](#参考)

<!-- /TOC -->

# 0、架构

## 1、客户端调用流程

![](../../pic/2020-04-15-09-09-00.png)





# 0、暴露给用户使用API


## 1、ServerConfig和ProviderConfig

### 1、ServerConfig

设置底层网络通信协议，默认bolt

### 2、ProviderConfig

设置暴露的接口和对应接口的实现类，如果有多个接口和实现类需要暴露，这里就会构建多个ProviderConfig，然后暴露时把调用server.registerProcessor(providerConfig, providerProxyInvoker);把当前配置类以及对应接口实现传递给server。

## 2、ConsumerConfig

ConsumerConfig封装一个接口以及通信协议（默认bolt），然后调用consumerConfig.refer()即可生成一个动态代理对象。client通过这个代理即可发送请求到服务提供者哪里。

## 3、RegistryConfig

设置一个注册协议以及设置注册中心的地址。


# 1、自定义SPI


## 1、ExtensionLoaderFactory扩展类加载器缓存工厂

![](../../pic/2020-04-12-19-48-58.png)


```java
public class ExtensionLoaderFactory {//扩展类加载器缓存工厂
    private ExtensionLoaderFactory() {
    }

    //All extension loader {Class : ExtensionLoader} key可以是抽象类或者接口
    private static final ConcurrentMap<Class, ExtensionLoader> LOADER_MAP = new ConcurrentHashMap<Class, ExtensionLoader>();

   //Get extension loader by extensible class with listener   没有新建有的话直接从缓存中返回
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> clazz, ExtensionLoaderListener<T> listener) {
        ExtensionLoader<T> loader = LOADER_MAP.get(clazz);
        if (loader == null) {
            synchronized (ExtensionLoaderFactory.class) {
                loader = LOADER_MAP.get(clazz);
                if (loader == null) {
                    loader = new ExtensionLoader<T>(clazz, listener);
                    LOADER_MAP.put(clazz, loader);
                }
            }
        }
        return loader;
    }

    //Get extension loader by extensible class without listener
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> clazz) {
        return getExtensionLoader(clazz, null);
    }
}

```



## 2、ExtensionLoader(只读取被注解Extensible标记抽象类或者接口)


![](../../pic/2020-04-12-19-52-06.png)


以服务提供者启动类为例：

ProviderBootstrap providerBootstrap = ExtensionLoaderFactory.getExtensionLoader(ProviderBootstrap.class)
            .getExtension(bootstrap, new Class[] { ProviderConfig.class }, new Object[] { providerConfig });



![](../../pic/2020-04-12-19-46-01.png)


核心逻辑是读取每个jar包下指定目录：META-INF/services/sofa-rpc/或者META-INF/services/下以抽象类或者接口文件名，内容为扩展类别名=具体类实现（如：sofa=com.alipay.sofa.rpc.bootstrap.DefaultProviderBootstrap），把读取到扩展类的实现缓存到内存中，格式：ConcurrentMap<String, ExtensionClass<T>> all;其中key为别名，value是使用ExtensionClass类包裹具体的实现类。[ExtensionClass(Class<? extends T> clazz, String alias)]


## 3、ExtensionClass

扩展类包裹类

![](../../pic/2020-04-12-20-04-47.png)


## 4、Extensible

```java
//代表这个类或者接口是可扩展的，默认单例、不需要编码
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Extensible {

   //指定自定义扩展文件名称，默认就是全类名
    String file() default "";

   //扩展类是否使用单例，默认使用
    boolean singleton() default true;

   //扩展类是否需要编码，默认不需要
    boolean coded() default false;
}

```


## 5、Extension

```java
//扩展点
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Extension {
   //扩展点名字
    String value();

   // 扩展点编码，默认不需要，当接口需要编码的时候需要
    byte code() default -1;

   //优先级排序，默认不需要
    int order() default 0;

   //是否覆盖其它低{@link #order()}的同名扩展
    boolean override() default false;

   //排斥其它扩展，可以排斥掉其它低{@link #order()}的扩展
    String[] rejection() default {};
}

```


# 2、ProviderBootstrap发布服务的包装类，包括具体的启动后的对象


```java
@Extensible(singleton = false)//非单例扩展点
public abstract class ProviderBootstrap<T> {

    //服务发布者配置
    protected final ProviderConfig<T> providerConfig;

    //构造函数,服务发布者配置
    protected ProviderBootstrap(ProviderConfig<T> providerConfig) {
        this.providerConfig = providerConfig;
    }

   //得到服务发布者配置
    public ProviderConfig<T> getProviderConfig() {
        return providerConfig;
    }

    //发布一个服务
    public abstract void export();

    // 取消发布一个服务
    public abstract void unExport();
}

```

该抽象类的全部实现：

![](../../pic/2020-04-12-20-15-27.png)


## 1、DefaultProviderBootstrap（sofa）默认实现

```java

@Extension("sofa")
public class DefaultProviderBootstrap<T> extends ProviderBootstrap<T> {
}
```






# 3、服务提供者server



```java
//Server SPI
@Extensible(singleton = false)
public interface Server extends Destroyable {
   //启动server端
    void init(ServerConfig serverConfig);

   //启动
    void start();

   //是否已经启动
    boolean isStarted();

  //是否还绑定了服务（没有可以销毁）
    boolean hasNoEntry();

   //停止
    void stop();

    /**
     * 注册服务，针对多个接口这里就是注册多个即可
     *
     * @param providerConfig 服务提供者配置
     * @param instance       服务提供者实例
     */
    void registerProcessor(ProviderConfig providerConfig, Invoker instance);

    /**
     * 取消注册服务
     *
     * @param providerConfig 服务提供者配置
     * @param closeIfNoEntry 如果没有注册服务，最后一个关闭Server
     */
    void unRegisterProcessor(ProviderConfig providerConfig, boolean closeIfNoEntry);
}

```

![](../../pic/2020-04-12-20-23-53.png)


有ServerConfig.buildIfAbsent()---> ServerFactory.getServer(this)触发SPI加载机制，基于协议来构建server，默认的协议是bolt。在构建完之后会触发init方法。


## 1、BoltServer

```java
@Extension("bolt")
public class BoltServer implements Server {
// Invoker列表，接口--> Invoker   在这里缓存多个接口以及对应的实现。
    protected Map<String, Invoker> invokerMap = new ConcurrentHashMap<String, Invoker>();

}
```

核心是start()方法，会构建一个RemotingServer接口的实现类RpcServer=new RpcServer（ip,port）指定了IP和端口，然后设置一个业务处理器BoltServerProcessor(构造函数以BoltServer作为参数，就可以使用BoltServer中注册的接口和实现类处理client端的请求)，用来处理业务请求，然后调用RpcServer.start()启动，暴露服务。


备注：可以看到，这里通过server层来把底层的具体通信协议和rpc上层解耦，这样底层可以使用不同的通信协议。这里采用的是默认的bolt通信协议。


## 2、BoltServerProcessor业务处理client端发送的请求

```java
//处理当前client请求
public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, SofaRequest request) {
    // 从boltServer的invokerMap通过接口全路径查找服务
    Invoker invoker = boltServer.findInvoker(serviceName);

    // 真正调用
    response = doInvoke(serviceName, invoker, request);

    //返回数据，即把请求写会到网络通道中
    asyncCtx.sendResponse(response);

}

//执行调用服务提供者接口实现类
 private SofaResponse doInvoke(String serviceName, Invoker invoker, SofaRequest request) throws SofaRpcException {
        // 开始调用，先记下当前的ClassLoader
        ClassLoader rpcCl = Thread.currentThread().getContextClassLoader();
        try {
            // 切换线程的ClassLoader到 服务 自己的ClassLoader
            ClassLoader serviceCl = ReflectCache.getServiceClassLoader(serviceName);
            Thread.currentThread().setContextClassLoader(serviceCl);
            return invoker.invoke(request);//跳转到FilterChain去执行
        } finally {
            Thread.currentThread().setContextClassLoader(rpcCl);
        }
    }
```




# 4、ConsumerBootstrap服务消费者启动类

```java
//引用服务的包装类，包括具体的启动后的对象
@Extensible(singleton = false)
public abstract class ConsumerBootstrap<T> {
//服务消费者配置
    protected final ConsumerConfig<T> consumerConfig;

   //构造函数
    protected ConsumerBootstrap(ConsumerConfig<T> consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    //得到服务消费者配置
    public ConsumerConfig<T> getConsumerConfig() {
        return consumerConfig;
    }

   //调用一个服务
    public abstract T refer();

    //取消调用一个服务
    public abstract void unRefer();

   //拿到代理类
    public abstract T getProxyIns();

   //得到调用集群
    public abstract Cluster getCluster();

    //订阅服务列表
    public abstract List<ProviderGroup> subscribe();

   //是否已经订阅完毕
    public abstract boolean isSubscribed();
}


```

![](../../pic/2020-04-12-21-04-16.png)

## 01、DefaultConsumerBootstrap（sofa）默认实现

备注：调用流程，cluster相关实现类中封装了链接注册中心查找服务提供者，然后选择服务提供者建立链接发送请求；

- 1、com.alipay.sofa.rpc.config.ConsumerConfig#refer
- 2、com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap#refer
- 3、生成的动态代理调用DefaultClientProxyInvoker.invoke(sofaRequest)；
- 4、调用父类中的方法com.alipay.sofa.rpc.client.ClientProxyInvoker#invoke(sofaRequest)
- 5、调用集群cluster.invoke(request)，cluster也实现了invoke接口；
- 6、各个子类可实现各自的调用方法com.alipay.sofa.rpc.client.AbstractCluster#doInvoke，com.alipay.sofa.rpc.client.AbstractCluster#filterChain，
com.alipay.sofa.rpc.filter.FilterInvoker#invoke，
com.alipay.sofa.rpc.filter.ConsumerInvoker#invoke，
com.alipay.sofa.rpc.client.AbstractCluster#sendMsg，
com.alipay.sofa.rpc.client.AbstractCluster#doSendMsg
- 7、com.alipay.sofa.rpc.transport.ClientTransport#syncSend调用ClientTransport发送多种方式的请求
- 8、com.alipay.remoting.rpc.RpcClient#invokeSync(com.alipay.remoting.Url, java.lang.Object, com.alipay.remoting.InvokeContext, int)这里调用bolt通信协议的rpc通信



```java
@Extension("sofa")
public class DefaultConsumerBootstrap<T> extends ConsumerBootstrap<T> {

}
```



## 02、Cluster

基于SPI加载实现类

![](../../pic/2020-04-14-21-33-03.png)

![](../../pic/2020-04-14-21-33-52.png)



### 1、AbstractCluster主要逻辑在这里实现

重点：

- 1、调用拦截链filterChain中封装真正发起client端调用的ConsumerInvoker；


#### 1、建立client连接过程

- 1、在init函数中调用consumerBootstrap.subscribe()通过注册中心获取服务提供者列表；
- 2、updateAllProviders(all)初始化服务端连接（建立长连接) 
- 3、connectionHolder.updateAllProviders(providerGroups);//这里创建长连接
- 4、com.alipay.sofa.rpc.client.AllConnectConnectionHolder#updateProviders
- 5、com.alipay.sofa.rpc.client.AllConnectConnectionHolder#addNode
- 6、com.alipay.sofa.rpc.client.AllConnectConnectionHolder#initClientRunnable
- 7、com.alipay.sofa.rpc.client.AllConnectConnectionHolder#initClientTransport


```java
@Override
    public synchronized void init() {
        if (initialized) { // 已初始化
            return;
        }
        // 构造Router链
        routerChain = RouterChain.buildConsumerChain(consumerBootstrap);
        // 负载均衡策略 考虑是否可动态替换？
        loadBalancer = LoadBalancerFactory.getLoadBalancer(consumerBootstrap);
        // 地址管理器
        addressHolder = AddressHolderFactory.getAddressHolder(consumerBootstrap);
        // 连接管理器
        connectionHolder = ConnectionHolderFactory.getConnectionHolder(consumerBootstrap);//包含远程通信client
        // 构造Filter链,最底层是调用过滤器
        this.filterChain = FilterChain.buildConsumerChain(this.consumerConfig,
            new ConsumerInvoker(consumerBootstrap));//ConsumerInvoker封装client端的请求到server端

        if (consumerConfig.isLazy()) { // 延迟连接
            if (LOGGER.isInfoEnabled(consumerConfig.getAppName())) {
                LOGGER.infoWithApp(consumerConfig.getAppName(), "Connection will be initialized when first invoke.");
            }
        }

        // 启动重连线程
        connectionHolder.init();
        try {
            // 得到服务端列表
            List<ProviderGroup> all = consumerBootstrap.subscribe();
            if (CommonUtils.isNotEmpty(all)) {
                // 初始化服务端连接（建立长连接)    初始化了rpc的client【重要】
                updateAllProviders(all);
            }
        } catch (SofaRpcRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SofaRpcRuntimeException("Init provider's transport error!", e);
        }

        // 启动成功
        initialized = true;

        // 如果check=true表示强依赖
        if (consumerConfig.isCheck() && !isAvailable()) {
            throw new SofaRpcRuntimeException("The consumer is depend on alive provider " +
                "and there is no alive provider, you can ignore it " +
                "by ConsumerConfig.setCheck(boolean) (default is false)");
        }
    }
```


### 2、FailoverCluster故障转移，支持重试和指定地址调用

这里会根据配置的方法失败重试次数从服务提供者列表进行选择重试，直到调用成功或者重试次数到达最大。


### 3、FailFastCluster快速失败

选择一个服务提供者进行调用，失败就返回调用失败了

```java

@Extension("failfast")
public class FailFastCluster extends AbstractCluster {
//构造函数
    public FailFastCluster(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public SofaResponse doInvoke(SofaRequest request) throws SofaRpcException {
        ProviderInfo providerInfo = select(request);//[选择一个服务提供者信息]
        try {
            SofaResponse response = filterChain(providerInfo, request);//进行调用
            if (response != null) {
                return response;
            } else {
                throw new SofaRpcException();
            }
        } catch (Exception e) {
            throw new SofaRpcException();
        }
    }
}
```


## 03、Proxy（SPI）生成消费端的代理对象

```java
@Extensible
public interface Proxy {

    //生成代理对象
    <T> T getProxy(Class<T> interfaceClass, Invoker proxyInvoker);

   //从代理对象里解析Invoker
    Invoker getInvoker(Object proxyObject);
}

```

![](../../pic/2020-04-14-22-01-45.png)




## 04、LoadBalancer负载均衡器：从一堆Provider列表里选出一个

![](../../pic/2020-04-14-22-06-23.png)

核心逻辑是给一个服务提供者列表list，通过负载均衡算法选择出来一个

```java

@Extensible(singleton = false)
@ThreadSafe
public abstract class LoadBalancer {

    /**
     * 核心方法：选择服务
     *
     * @param request       本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providerInfos <b>当前可用</b>的服务Provider列表
     * @return 选择其中一个Provider
     * @throws SofaRpcException rpc异常
     */
    public abstract ProviderInfo select(SofaRequest request, List<ProviderInfo> providerInfos)
        throws SofaRpcException;
}

```


![](../../pic/2020-04-14-22-08-05.png)


### 1、AbstractLoadBalancer提供如何选择服务的模板

```java
public abstract class AbstractLoadBalancer extends LoadBalancer {

  //构造函数
    public AbstractLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public ProviderInfo select(SofaRequest request, List<ProviderInfo> providerInfos) throws SofaRpcException {
        if (providerInfos.size() == 0) {
            throw noAvailableProviderException(request.getTargetServiceUniqueName());
        }
        if (providerInfos.size() == 1) {
            return providerInfos.get(0);
        } else {
            return doSelect(request, providerInfos);//模板方法，让子类去实现
        }
    }

   //找不到可用的服务列表的异常
    protected SofaRouteException noAvailableProviderException(String serviceKey) {
        return new SofaRouteException(LogCodes.getLog(LogCodes.ERROR_NO_AVAILBLE_PROVIDER, serviceKey));
    }

    //根据负载均衡筛选
    protected abstract ProviderInfo doSelect(SofaRequest invocation, List<ProviderInfo> providerInfos);

   
    protected int getWeight(ProviderInfo providerInfo) {
        // 从provider中或得到相关权重,默认值100
        return providerInfo.getWeight() < 0 ? 0 : providerInfo.getWeight();
    }
}

```

### 2、RandomLoadBalancer负载均衡随机算法:全部列表按权重随机选择

```java
@Extension("random")
public class RandomLoadBalancer extends AbstractLoadBalancer {
//随机
    private final Random random = new Random();

    @Override
    public ProviderInfo doSelect(SofaRequest invocation, List<ProviderInfo> providerInfos) {
        ProviderInfo providerInfo = null;
        int size = providerInfos.size(); // 总个数
        int totalWeight = 0; // 总权重
        boolean isWeightSame = true; // 权重是否都一样
        for (int i = 0; i < size; i++) {
            int weight = getWeight(providerInfos.get(i));
            totalWeight += weight; // 累计总权重
            if (isWeightSame && i > 0 && weight != getWeight(providerInfos.get(i - 1))) {//从i=1开始计算和前一个的权重是否一致
                isWeightSame = false; // 计算所有权重是否一样
            }
        }
        if (totalWeight > 0 && !isWeightSame) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offset = random.nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (int i = 0; i < size; i++) {
                offset -= getWeight(providerInfos.get(i));
                if (offset < 0) {
                    providerInfo = providerInfos.get(i);
                    break;
                }
            }
        } else {
            // 如果权重相同或权重为0则均等随机
            providerInfo = providerInfos.get(random.nextInt(size));
        }
        return providerInfo;
    }
}

```
### 3、LocalPreferenceLoadBalancer本机优先的随机算法（看服务提供者的IP是否和本机IP一样）

```java
@Extension("localPref")
public class LocalPreferenceLoadBalancer extends RandomLoadBalancer {

    @Override
    public ProviderInfo doSelect(SofaRequest invocation, List<ProviderInfo> providerInfos) {
        String localhost = SystemInfo.getLocalHost();
        if (StringUtils.isEmpty(localhost)) {
            return super.doSelect(invocation, providerInfos);
        }
        List<ProviderInfo> localProviderInfo = new ArrayList<ProviderInfo>();
        for (ProviderInfo providerInfo : providerInfos) { // 解析IP，看是否和本地一致
            if (localhost.equals(providerInfo.getHost())) {
                localProviderInfo.add(providerInfo);
            }
        }
        if (CommonUtils.isNotEmpty(localProviderInfo)) { // 命中本机的服务端
            return super.doSelect(invocation, localProviderInfo);
        } else { // 没有命中本机上的服务端
            return super.doSelect(invocation, providerInfos);
        }
    }
}

```

### 4、RoundRobinLoadBalancer负载均衡轮询算法，按方法级进行轮询，互不影响

```java
@Extension("roundRobin")
public class RoundRobinLoadBalancer extends AbstractLoadBalancer {

    private final ConcurrentMap<String, PositiveAtomicCounter> sequences = new ConcurrentHashMap<String, PositiveAtomicCounter>();

   

    @Override
    public ProviderInfo doSelect(SofaRequest request, List<ProviderInfo> providerInfos) {
        String key = getServiceKey(request); // 每个方法级自己轮询，互不影响
        int length = providerInfos.size(); // 总个数
        PositiveAtomicCounter sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new PositiveAtomicCounter());
            sequence = sequences.get(key);
        }
        return providerInfos.get(sequence.getAndIncrement() % length);
    }

    private String getServiceKey(SofaRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getTargetAppName()).append("#")
            .append(request.getMethodName());
        return builder.toString();
    }

}


```

### 5、ConsistentHashLoadBalancer一致性hash算法，同样的请求（第一参数）会打到同样的节点

```java

@Extension("consistentHash")
public class ConsistentHashLoadBalancer extends AbstractLoadBalancer {

    /**
     * {interface#method : selector}
     */
    private ConcurrentMap<String, Selector> selectorCache = new ConcurrentHashMap<String, Selector>();

    /**
     * 构造函数
     *
     * @param consumerBootstrap 服务消费者配置
     */
    public ConsistentHashLoadBalancer(ConsumerBootstrap consumerBootstrap) {
        super(consumerBootstrap);
    }

    @Override
    public ProviderInfo doSelect(SofaRequest request, List<ProviderInfo> providerInfos) {
        String interfaceId = request.getInterfaceName();
        String method = request.getMethodName();
        String key = interfaceId + "#" + method;
        int hashcode = providerInfos.hashCode(); // 判断是否同样的服务列表
        Selector selector = selectorCache.get(key);
        if (selector == null // 原来没有
            ||
            selector.getHashCode() != hashcode) { // 或者服务列表已经变化
            selector = new Selector(interfaceId, method, providerInfos, hashcode);
            selectorCache.put(key, selector);
        }
        return selector.select(request);
    }

    /**
     * 选择器
     */
    private static class Selector {

        /**
         * The Hashcode.
         */
        private final int                         hashcode;

        /**
         * The Interface id.
         */
        private final String                      interfaceId;

        /**
         * The Method name.
         */
        private final String                      method;

        /**
         * 虚拟节点
         */
        private final TreeMap<Long, ProviderInfo> virtualNodes;

        /**
         * Instantiates a new Selector.
         *
         * @param interfaceId the interface id
         * @param method      the method
         * @param actualNodes the actual nodes
         */
        public Selector(String interfaceId, String method, List<ProviderInfo> actualNodes) {
            this(interfaceId, method, actualNodes, actualNodes.hashCode());
        }

        /**
         * Instantiates a new Selector.
         *
         * @param interfaceId the interface id
         * @param method      the method
         * @param actualNodes the actual nodes
         * @param hashcode    the hashcode
         */
        public Selector(String interfaceId, String method, List<ProviderInfo> actualNodes, int hashcode) {
            this.interfaceId = interfaceId;
            this.method = method;
            this.hashcode = hashcode;
            // 创建虚拟节点环 （默认一个provider共创建128个虚拟节点，较多比较均匀）
            this.virtualNodes = new TreeMap<Long, ProviderInfo>();
            int num = 128;
            for (ProviderInfo providerInfo : actualNodes) {
                for (int i = 0; i < num / 4; i++) {
                    byte[] digest = HashUtils.messageDigest(providerInfo.getHost() + providerInfo.getPort() + i);
                    for (int h = 0; h < 4; h++) {
                        long m = HashUtils.hash(digest, h);
                        virtualNodes.put(m, providerInfo);
                    }
                }
            }
        }

        /**
         * Select provider.
         *
         * @param request the request
         * @return the provider
         */
        public ProviderInfo select(SofaRequest request) {
            String key = buildKeyOfHash(request.getMethodArgs());
            byte[] digest = HashUtils.messageDigest(key);
            return selectForKey(HashUtils.hash(digest, 0));
        }

        /**
         * 获取第一参数作为hash的key
         *
         * @param args the args
         * @return the string
         */
        private String buildKeyOfHash(Object[] args) {
            if (CommonUtils.isEmpty(args)) {
                return StringUtils.EMPTY;
            } else {
                return StringUtils.toString(args[0]);
            }
        }

        /**
         * Select for key.
         *
         * @param hash the hash
         * @return the provider
         */
        private ProviderInfo selectForKey(long hash) {
            Map.Entry<Long, ProviderInfo> entry = virtualNodes.ceilingEntry(hash);
            if (entry == null) {
                entry = virtualNodes.firstEntry();
            }
            return entry.getValue();
        }

        /**
         * Gets hash code.
         *
         * @return the hash code
         */
        public int getHashCode() {
            return hashcode;
        }
    }
}

```

## 05、ConnectionHolder（SPI）

![](../../pic/2020-04-14-23-04-22.png)

![](../../pic/2020-04-14-23-01-47.png)

```java
@Extensible(singleton = false)
@ThreadSafe
public abstract class ConnectionHolder implements Initializable, Destroyable, ProviderInfoListener {

   //服务消费者配置
    protected ConsumerBootstrap consumerBootstrap;

    protected ConnectionHolder(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
    }

   //关闭所有长连接
    public abstract void closeAllClientTransports(DestroyHook destroyHook);

    //根据provider查找存活的ClientTransport
    public abstract ClientTransport getAvailableClientTransport(ProviderInfo providerInfo);

   //是否没有存活的的provider
    public abstract boolean isAvailableEmpty();

   // 获取当前的Provider列表（包括连上和没连上的）
    @Deprecated
    public abstract Collection<ProviderInfo> currentProviderList();

   //设置为不可用
    public abstract void setUnavailable(ProviderInfo providerInfo, ClientTransport transport);

}


```

### 1、AllConnectConnectionHolder全部建立长连接，自动维护长连接



### 2、ElasticConnectionHolder弹性长连接，可按百分比配置以及按个数配置



## 06、ClientTransport网络通信层

![](../../pic/2020-04-14-23-20-03.png)

![](../../pic/2020-04-14-23-21-07.png)


```java
@Extensible(singleton = false)
public abstract class ClientTransport {

   //客户端配置
    protected ClientTransportConfig transportConfig;

    protected ClientTransport(ClientTransportConfig transportConfig) {
        this.transportConfig = transportConfig;
    }

    public ClientTransportConfig getConfig() {
        return transportConfig;
    }
    //建立长连接
    public abstract void connect();

   //断开连接
    public abstract void disconnect();

    //销毁（最好是通过工厂模式销毁，这样可以清理缓存）
    public abstract void destroy();

   //是否可用（有可用的长连接）
    public abstract boolean isAvailable();

   //设置长连接
    public abstract void setChannel(AbstractChannel channel);

   //得到长连接
    public abstract AbstractChannel getChannel();

  //当前请求数
    public abstract int currentRequests();

   //异步调用
    public abstract ResponseFuture asyncSend(SofaRequest message, int timeout) throws SofaRpcException;

  //同步调用
    public abstract SofaResponse syncSend(SofaRequest message, int timeout) throws SofaRpcException;

    //单向调用
    public abstract void oneWaySend(SofaRequest message, int timeout) throws SofaRpcException;

   //客户端收到异步响应
    public abstract void receiveRpcResponse(SofaResponse response);

   //客户端收到服务端的请求，可能是服务端Callback
    public abstract void handleRpcRequest(SofaRequest request);

   //远程地址，一般是服务端地址
    public abstract InetSocketAddress remoteAddress();

   //本地地址，一般是客户端地址
    public abstract InetSocketAddress localAddress();

}

```

### 1、BoltClientTransport


```java
@Extension("bolt")
public class BoltClientTransport extends ClientTransport {//基于sofa-bolt进行发送请求



}

```



## 07、Router路由器：从一堆Provider中筛选出一堆Provider

![](../../pic/2020-04-14-23-44-25.png)

![](../../pic/2020-04-14-23-43-02.png)

```java
@Extensible(singleton = false)
@ThreadSafe
public abstract class Router {

    // 初始化
    public void init(ConsumerBootstrap consumerBootstrap) {
    }

   //是否自动加载
    public boolean needToLoad(ConsumerBootstrap consumerBootstrap) {
        return true;
    }

    /**
     * 筛选Provider
     *
     * @param request       本次调用（可以得到类名，方法名，方法参数，参数值等）
     * @param providerInfos providers（<b>当前可用</b>的服务Provider列表）
     * @return 路由匹配的服务Provider列表
     */
    public abstract List<ProviderInfo> route(SofaRequest request, List<ProviderInfo> providerInfos);

    /**
     * 记录路由路径记录
     *
     * @param routerName 路由名字
     * @since 5.2.0
     */
    protected void recordRouterWay(String routerName) {
        if (RpcInternalContext.isAttachmentEnable()) {
            RpcInternalContext context = RpcInternalContext.getContext();
            String record = (String) context.getAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD);
            record = record == null ? routerName : record + ">" + routerName;
            context.setAttachment(RpcConstants.INTERNAL_KEY_ROUTER_RECORD, record);
        }
    }
}

```

### 1、RegistryRouter从注册中心获取地址进行路由


## 08、ProviderInfoListener服务提供者信息监听器

![](../../pic/2020-04-15-09-16-36.png)

```java
public interface ProviderInfoListener {

   //增加某标签的服务端列表 （增量）
    void addProvider(ProviderGroup providerGroup);

   // 删除某标签的服务端列表（增量）
    void removeProvider(ProviderGroup providerGroup);

   //更新某标签的服务端列表（全量）
    void updateProviders(ProviderGroup providerGroup);

   //全部服务端列表，为空代表清空已有列表
    void updateAllProviders(List<ProviderGroup> providerGroups);
}

```

## 09、AddressHolder(SPI)

![](../../pic/2020-04-15-09-41-50.png)

```java
@Extensible(singleton = false)
public abstract class AddressHolder implements ProviderInfoListener {
    //服务消费者配置
    protected ConsumerBootstrap consumerBootstrap;

    protected AddressHolder(ConsumerBootstrap consumerBootstrap) {
        this.consumerBootstrap = consumerBootstrap;
    }

   //得到某分组的服务列表，注意获取的地址列表最好是只读，不要随便修改
    public abstract List<ProviderInfo> getProviderInfos(String groupName);

    //得到某服务分组
    public abstract ProviderGroup getProviderGroup(String groupName);

   //得到全部服务端列表分组
    public abstract List<ProviderGroup> getProviderGroups();

    //得到全部服务端大小
    public abstract int getAllProviderSize();
}

```

### 1、SingleGroupAddressHolder只是在内部通过变量保存ProviderGroup（外部传入的）

![](../../pic/2020-04-15-09-46-38.png)



## 10、ProviderGroup一个分组服务提供者




# 5、Invoker调用器

```java
public interface Invoker {

   // 执行调用
    SofaResponse invoke(SofaRequest request) throws SofaRpcException;
}
```
![](../../pic/2020-04-12-22-11-32.png)


## 1、ProviderProxyInvoker服务提供者实现代理类

```java
//服务端调用链入口
public class ProviderProxyInvoker implements Invoker {

   //对应的客户端信息
    private final ProviderConfig providerConfig;

   //过滤器执行链
    private final FilterChain    filterChain;

    //根据ProviderConfig构造执行链
    public ProviderProxyInvoker(ProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
        // 最底层是调用过滤器
        this.filterChain = FilterChain.buildProviderChain(providerConfig,
            new ProviderInvoker(providerConfig));
    }

    // proxy拦截的调用
    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {
        return filterChain.invoke(request);
    }
    //...
}
```

## 2、ProviderInvoker服务提供者接口实现类封装[具体的业务逻辑]

```java

//服务端调用业务实现类
public class ProviderInvoker<T> extends FilterInvoker {//处理client端的请求

   
    private final ProviderConfig<T> providerConfig;

    
    public ProviderInvoker(ProviderConfig<T> providerConfig) {
        super(providerConfig);
        this.providerConfig = providerConfig;
    }

    //通过反射执行具体的接口实现类中的方法
    @Override
    public SofaResponse invoke(SofaRequest request) throws SofaRpcException {

        SofaResponse sofaResponse = new SofaResponse();
        long startTime = RpcRuntimeContext.now();
        try {
            // 反射 真正调用业务代码
            Method method = request.getMethod();
            if (method == null) {
                throw new SofaRpcException(RpcErrorType.SERVER_FILTER, "Need decode method first!");
            }
            Object result = method.invoke(providerConfig.getRef(), request.getMethodArgs());

            sofaResponse.setAppResponse(result);
        } catch (IllegalArgumentException e) { // 非法参数，可能是实现类和接口类不对应)
            sofaResponse.setErrorMsg(e.getMessage());
        } catch (IllegalAccessException e) { // 如果此 Method 对象强制执行 Java 语言访问控制，并且底层方法是不可访问的
            sofaResponse.setErrorMsg(e.getMessage());
            //        } catch (NoSuchMethodException e) { // 如果找不到匹配的方法
            //            sofaResponse.setErrorMsg(e.getMessage());
            //        } catch (ClassNotFoundException e) { // 如果指定的类加载器无法定位该类
            //            sofaResponse.setErrorMsg(e.getMessage());
        } catch (InvocationTargetException e) { // 业务代码抛出异常
            cutCause(e.getCause());
            sofaResponse.setAppResponse(e.getCause());
        } finally {
            if (RpcInternalContext.isAttachmentEnable()) {
                long endTime = RpcRuntimeContext.now();
                RpcInternalContext.getContext().setAttachment(RpcConstants.INTERNAL_KEY_IMPL_ELAPSE,
                    endTime - startTime);
            }
        }

        return sofaResponse;
    }

//...
}

```

## 3、FilterChain[这里应该是封装一些过滤器，最后把请求交给接口的实现类]

![](../../pic/2020-04-12-23-11-06.png)


这里会根据spi机制加载filter接口的实现类，然后根据AutoActive注解判断是服务提供者端的过滤是还是消费端的过滤器，分别缓存到不同的map中，然后包裹当前invoker。




### 1、Filter

```java
//Filter SPI
@Extensible(singleton = false)
public abstract class Filter {

   //Is this filter need load in this invoker
    public boolean needToLoad(FilterInvoker invoker) {
        return true;
    }

   
    public abstract SofaResponse invoke(FilterInvoker invoker, SofaRequest request) throws SofaRpcException;

  
    public void onAsyncResponse(ConsumerConfig config, SofaRequest request, SofaResponse response, Throwable exception)
        throws SofaRpcException {
    }
}

```



# 6、RegistryConfig注册中心配置

## 1、Registry（SPI）

![](../../pic/2020-04-15-09-27-20.png)

```java
@Extensible(singleton = false)
public abstract class Registry implements Initializable, Destroyable {
    //注册中心服务配置
    protected RegistryConfig registryConfig;

    protected Registry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    //启动
    public abstract boolean start();

   //注册服务提供者
    public abstract void register(ProviderConfig config);

   //反注册服务提供者
    public abstract void unRegister(ProviderConfig config);
    //批量反注册服务提供者
    public abstract void batchUnRegister(List<ProviderConfig> configs);

    //订阅服务列表
    public abstract List<ProviderGroup> subscribe(ConsumerConfig config);

    //反订阅服务调用者相关配置
    public abstract void unSubscribe(ConsumerConfig config);

   //批量，反订阅服务调用者相关配置
    public abstract void batchUnSubscribe(List<ConsumerConfig> configs);

    @Override
    public void destroy(DestroyHook hook) {
        if (hook != null) {
            hook.preDestroy();
        }
        destroy();
        if (hook != null) {
            hook.postDestroy();
        }
    }

}

```

可以看出主要是根据服务提供者配置注册服务以及消费者配置订阅服务。


## 2、ZookeeperRegistry基于zookeeper的注册中心








# 参考

- [sofa-rpc](https://www.sofastack.tech/projects/sofa-rpc)






