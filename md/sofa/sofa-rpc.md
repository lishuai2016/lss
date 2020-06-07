
<!-- TOC -->

- [sofa-rpc源码分析](#sofa-rpc源码分析)
    - [1、配置相关类config](#1配置相关类config)
        - [1、服务提供者相关ProviderConfig和serverconfig](#1服务提供者相关providerconfig和serverconfig)
        - [2、服务消费者相关ConsumerConfig](#2服务消费者相关consumerconfig)
        - [3、服务注册相关RegistryConfig](#3服务注册相关registryconfig)
    - [2、server](#2server)
        - [1、http服务](#1http服务)
            - [1、ServerTransport接口](#1servertransport接口)
    - [3、client](#3client)
        - [ClientTransport](#clienttransport)
    - [4、注册中心](#4注册中心)
- [参考](#参考)

<!-- /TOC -->



# sofa-rpc源码分析



## 1、配置相关类config

### 1、服务提供者相关ProviderConfig和serverconfig

![](../../pic/2020-01-29-11-43-43.png)

其中ProviderConfig内部包含一个List<ServerConfig>配置的协议列表，在暴露服务的时候把这个协议列表包含的协议全部暴露一遍。

备注：每个ProviderConfig和每个ConsumerConfig表示一个接口的封装，其中ProviderConfig封装接口具体的业务实现，而ConsumerConfig根据接口生成一个代理对象。消费者ConsumerConfig根据直连地址或者注册中心找到服务提供者的IP地址，然后发起网络请求。



针对服务接口的具体实现和代理封装接口invoker类图：

![](../../pic/2020-01-29-20-20-47.png)

在暴露服务的过程中，创建一个接口具体实现的包装对象代理ProviderProxyInvoker[包含ProviderInvoker和一些系统过滤器]传递给具体处理server，这样在client端的请求到来的时候，就调用ProviderProxyInvoker进行处理。其中ProviderProxyInvoker内部有个FilterChain，这个FilterChain实例构造参数包含ProviderInvoker。在ProviderInvoker的invoke方法中通过反射完成client端的请求。


当多个ProviderConfig暴露服务，共用一个ServerConfig时[即多个接口通过一个ip:port暴露出现]，这个时候server只会start()启动一次。

那server端如何区分一个端口对应多个服务？以boltserver为例，其内部包含一个Map<String, Invoker>，在这里缓存多个接口以及对应的实现，这样就可以根据请求找到对应接口的实现。





### 2、服务消费者相关ConsumerConfig



### 3、服务注册相关RegistryConfig





## 2、server

服务提供者启动ProviderBootstrap

![](../../pic/2020-01-29-11-09-34.png)



启动过程：

```
providerConfig.export()发布服务
    通过配置信息选择一个通信协议等，封装成一个providerBootstrap对象：providerBootstrap = Bootstraps.from(this);然后开始暴露服务，providerBootstrap.export();具体流程就是先根据配置暴露服务，然后再根据注册的配置信息觉得是否需要进行注册。
```



server端暴露服务：核心接口[Server]

![](../../pic/2020-01-29-13-48-46.png)

从类图中可以看到，支持多种协议暴露服务。

接口中的方法：

![](../../pic/2020-02-03-14-21-59.png)

- init 初始化一些配置信息；

- start 启动接口暴露服务；

- registerProcessor 注册服务提供者信息，invoker封装了接口的具体实现；


接收用户请求的处理过程：

针对boltserver，借助于sofa-bolt底层通信，把BoltServerProcessor当成UserProcessor注册到BoltServer中。以boltserver为例，其内部包含一个Map<String, Invoker>，在这里缓存多个接口以及对应的实现，这样就可以根据请求找到对应接口的实现。这个BoltServerProcessor相当于分发器。

public class BoltServerProcessor extends AsyncUserProcessor<SofaRequest> {}

其中SofaRequest为client传递过来的请求对象。




### 1、http服务

AbstractHttpServer

![](../../pic/2020-02-03-14-14-56.png)


具体的通信委托给内部的对象ServerTransport。

备注：Http1Server、Http2WithSSLServer和Http2ClearTextServer只有container字段不一样。

#### 1、ServerTransport接口


![](../../pic/2020-02-03-11-43-50.png)

对应于三种不同的server这里有三个不同的ServerTransport，在server.start的时候根据spi机制加载实例化。


核心逻辑实现在AbstractHttp2ServerTransport，基于netty的http服务。其中初始化pipeline使用Http2ServerChannelInitializer实现。同样ClientTransport的实现AbstractHttp2ClientTransport封装了netty提供client端的http实现。


serverdaunt请求任务的封装AbstractTask

![](../../pic/2020-02-03-11-57-32.png)












## 3、client

消费者启动类：ConsumerBootstrap

![](../../pic/2020-01-29-11-12-24.png)


cluster类

![](../../pic/2020-01-29-19-47-50.png)

备注：cluster内部包含connectionHolder[保存了和服务提供者的链接]。



client端的代理生成类proxy

![](../../pic/2020-01-29-19-56-22.png)


启动过程：

```

```

调用过程：

consumerConfig.refer()返回一个动态代理对象，交给DefaultClientProxyInvoker.invoke[ClientProxyInvoker]，然后传递到cluster.invoke(request)[FailFastCluster#doInvoke],最后到达ConsumerInvoker#invoke。然后调用consumerBootstrap.getCluster().sendMsg(providerInfo, sofaRequest);发送信息到服务提供者。



![](../../pic/2020-01-29-23-20-29.png)

![](../../pic/2020-01-29-23-26-49.png)


备注：com.alipay.sofa.rpc.client.AbstractCluster#init初始化filterChain，并构造一个ConsumerInvoker放到链的尾部，封装client端的请求到server端。




ClientProxyInvoker:客户端引用代理Invoker，一个引用一个。线程安全

![](../../pic/2020-01-29-22-38-41.png)

其中ClientProxyInvoker包含一个Cluster对象。




### ClientTransport


client端封装远程调用:核心类[ClientTransport]

![](../../pic/2020-01-29-13-46-58.png)

接口中定义的方法：

![](../../pic/2020-01-31-19-25-53.png)

接口的一个实现实例：[接口的实现BoltClientTransport通过sofa-bolt的远程通信RpcClient完成通信]。

public abstract AbstractChannel getChannel(); //得到长连接

AbstractChannel的唯一实现为NettyChannel，返回的是一个netty的channel。



ClientTransportHolder：在这里根据配置创建ClientTransport

![](../../pic/2020-01-31-19-42-46.png)

```java
public interface ClientTransportHolder extends Destroyable {

ClientTransport getClientTransport(ClientTransportConfig config);//通过配置获取长连接

boolean removeClientTransport(ClientTransport clientTransport);//销毁长连接

int size();//长连接数量

}

```


唯一实现NotReusableClientTransportHolder通过一个map维护，ConcurrentMap<ClientTransportConfig, ClientTransport> allTransports；

ClientTransportFactory通过NotReusableClientTransportHolder对象来维护ClientTransport的创建和释放。静态工厂模式。

![](../../pic/2020-01-31-19-51-39.png)


AllConnectConnectionHolder和ElasticConnectionHolder通过ClientTransportFactory来创建ClientTransport。





ConnectionHolder接口

![](../../pic/2020-01-29-21-25-49.png)


在Cluster中包含一个ConnectionHolder接口的实现。



client通信端初始化流程：

ConsumerBootstrap--->Cluster--->ConnectionHolder--->ClientTransportFactory--->ClientTransport[bolt协议封装RpcClient]

比如BoltClientTransport包含BoltClientConnectionManager用来创建通信connection。


BoltClientConnectionManager

![](../../pic/2020-02-03-11-13-15.png)

方法

![](../../pic/2020-02-03-11-16-32.png)

可以看出，这个BoltClientConnectionManager是维护RpcClient和connection的关系。而一个RpcClient对应一个IP：port，可以包含多个connection。






## 4、注册中心

registry注册中心接口

![](../../pic/2020-01-29-17-40-40.png)



以zookeeper注册实例：

启动服务提供者和消费者的时候

![](../../pic/2020-01-29-19-39-57.png)

关闭服务提供者和消费者的时候

![](../../pic/2020-01-29-19-44-54.png)


这个也是基于URL的注册：

sofa-rpc/com.alipay.sofa.rpc.test.HelloService/providers/bolt://192.168.60.1:22101?version=1.0&accepts=100000&weight=100&language=java&pid=14240&interface=com.alipay.sofa.rpc.test.HelloService&timeout=0&serialization=hessian2&protocol=bolt&delay=-1&dynamic=true&startTime=1580291962700&id=rpc-cfg-0&uniqueId=&rpcVer=50604

上面是注册服务com.alipay.sofa.rpc.test.HelloService的实现者，按照/分割在zookeeper上创建目录。子目录providers下面为该服务的所有服务提供者节点。每个服务提供者节点存的值是0下线\1在线。

- 服务提供者目录：sofa-rpc/xxxService/providers

- 服务消费者目录：sofa-rpc/xxxService/consumers

- 具体服务信息

bolt://192.168.60.1:22101?version=1.0&accepts=100000&weight=100&language=java&pid=14240&interface=com.alipay.sofa.rpc.test.HelloService&timeout=0&serialization=hessian2&protocol=bolt&delay=-1&dynamic=true&startTime=1580291962700&id=rpc-cfg-0&uniqueId=&rpcVer=50604

可以看出bolt为协议，192.168.60.1:22101为服务的IP和端口，？后面的参数为具体的属性信息

其中configs是服务提供者创建，overrides是消费者创建，具体用途？？？

备注：
经过测试发现如果知道了服务提供者的协议、IP：port可以通过直接链接的方式直接调用服务，可以绕过注册中心。












# 参考

- [sofa-rpc](https://www.sofastack.tech/projects/sofa-rpc/overview/)
