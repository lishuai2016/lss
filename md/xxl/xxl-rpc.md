
<!-- TOC -->

- [0 系统结构](#0-系统结构)
    - [1、系统架构图](#1系统架构图)
    - [2、RPC工作原理剖析](#2rpc工作原理剖析)
    - [3、TCP通讯模型](#3tcp通讯模型)
    - [4、sync-over-async](#4sync-over-async)
- [1、服务提供者](#1服务提供者)
    - [1、Server服务暴露，内部封装netty的server](#1server服务暴露内部封装netty的server)
        - [1、NettyServer](#1nettyserver)
            - [业务处理器NettyServerHandler](#业务处理器nettyserverhandler)
        - [2、NettyHttpServer](#2nettyhttpserver)
            - [NettyHttpServerHandler](#nettyhttpserverhandler)
    - [2、XxlRpcProviderFactory服务提供者配置类，启动服务的入口](#2xxlrpcproviderfactory服务提供者配置类启动服务的入口)
    - [3、XxlRpcSpringProviderFactory](#3xxlrpcspringproviderfactory)
    - [4、服务提供者启动配置类](#4服务提供者启动配置类)
- [2、服务消费者](#2服务消费者)
    - [0、client发送请求](#0client发送请求)
        - [1、NettyClient委托给NettyConnectClient](#1nettyclient委托给nettyconnectclient)
        - [2、NettyHttpClient委托给NettyHttpClient](#2nettyhttpclient委托给nettyhttpclient)
    - [1、XxlRpcInvokerFactory](#1xxlrpcinvokerfactory)
        - [NettyClientHandler/NettyHttpClientHandler把接收到的server数据返回调用端](#nettyclienthandlernettyhttpclienthandler把接收到的server数据返回调用端)
    - [2、XxlRpcSpringInvokerFactory](#2xxlrpcspringinvokerfactory)
    - [3、使用者引用注解XxlRpcReference](#3使用者引用注解xxlrpcreference)
    - [4、XxlRpcReferenceBean生成接口的远程服务的本地代理](#4xxlrpcreferencebean生成接口的远程服务的本地代理)
        - [1、同步调用：XxlRpcFutureResponse同步调用转化为异步](#1同步调用xxlrpcfutureresponse同步调用转化为异步)
        - [2、异步调用：XxlRpcInvokeFuture支持异步调用，返回一个future对象](#2异步调用xxlrpcinvokefuture支持异步调用返回一个future对象)
        - [3、回调：XxlRpcInvokeCallback](#3回调xxlrpcinvokecallback)
        - [4、oneway不关心执行结果：直接调用即可](#4oneway不关心执行结果直接调用即可)
    - [5、LoadBalance](#5loadbalance)
    - [9、消费者使用配置类](#9消费者使用配置类)
- [3、ServiceRegistry服务注册和发现接口](#3serviceregistry服务注册和发现接口)
    - [1、LocalServiceRegistry本地注册](#1localserviceregistry本地注册)
    - [2、XxlRegistryServiceRegistry](#2xxlregistryserviceregistry)
    - [3、ZkServiceRegistry](#3zkserviceregistry)
- [4、NettyDecoder和NettyEncoder](#4nettydecoder和nettyencoder)
    - [1、NettyDecoder](#1nettydecoder)
    - [2、NettyEncoder](#2nettyencoder)
- [5、XxlRpcRequest和XxlRpcResponse](#5xxlrpcrequest和xxlrpcresponse)
    - [1、XxlRpcRequest](#1xxlrpcrequest)
    - [2、XxlRpcResponse](#2xxlrpcresponse)
- [6、远程通信客户端抽象ConnectClient](#6远程通信客户端抽象connectclient)
    - [1、NettyConnectClient](#1nettyconnectclient)
        - [NettyClientHandler业务处理器读出服务端的响应结果](#nettyclienthandler业务处理器读出服务端的响应结果)
    - [2、NettyHttpConnectClient](#2nettyhttpconnectclient)
        - [NettyHttpClientHandler读取响应结果](#nettyhttpclienthandler读取响应结果)
- [参考](#参考)

<!-- /TOC -->

# 0 系统结构

## 1、系统架构图

![](../../pic/2020-04-11-17-38-42.png)

- 1、provider：服务提供方；
- 2、invoker：服务消费方；
- 3、serializer: 序列化模块；
- 4、remoting：服务通讯模块；
- 5、registry：服务注册中心；


## 2、RPC工作原理剖析

![](../../pic/2020-04-11-17-43-01.png)

概念：

- 1、serialization：序列化，通讯数据需要经过序列化，从而支持在网络中传输；
- 2、deserialization：反序列化，服务接受到序列化的请求数据，需要序列化为底层原始数据；
- 3、stub：体现在XXL-RPC为服务的api接口；
- 4、skeleton：体现在XXL-RPC为服务的实现api接口的具体服务；
- 5、proxy：根据远程服务的stub生成的代理服务，对开发人员透明；
- 6、provider：远程服务的提供方；
- 7、consumer：远程服务的消费方；

RPC通讯，可大致划分为四个步骤，可参考上图进行理解：（XXL-RPC提供了多种调用方案，此处以 “SYNC” 方案为例讲解；）

- 1、consumer发起请求：consumer会根据远程服务的stub实例化远程服务的代理服务，在发起请求时，代理服务会封装本次请求相关底层数据，如服务iface、methos、params等等，然后将数据经过serialization之后发送给provider；

- 2、provider接收请求：provider接收到请求数据，首先会deserialization获取原始请求数据，然后根据stub匹配目标服务并调用；

- 3、provider响应请求：provider在调用目标服务后，封装服务返回数据并进行serialization，然后把数据传输给consumer；

- 4、consumer接收响应：consumer接受到相应数据后，首先会deserialization获取原始数据，然后根据stub生成调用返回结果，返回给请求调用处。结束。


## 3、TCP通讯模型

![](../../pic/2020-04-11-17-46-06.png)

consumer和provider采用NIO方式通讯，其中TCP通讯方案可选NETTY具体选型，高吞吐高并发；但是仅仅依靠单个TCP连接进行数据传输存在瓶颈和风险，因此XXL-RPC在consumer端自身实现了内部连接池，consumer和provider之间为了一个连接池，当尽情底层通讯是会取出一条TCP连接进行通讯（可参考上图）。


## 4、sync-over-async

![](../../pic/2020-04-11-17-47-56.png)

XXL-RPC采用NIO进行底层通讯，但是NIO是异步通讯模型，调用线程并不会阻塞获取调用结果，因此，XXL-RPC实现了在异步通讯模型上的同步调用，即“sync-over-async”，实现原理如下，可参考上图进行理解：

- 1、每次请求会生成一个唯一的RequestId和一个RpcResponse，托管到请求池中。
- 2、调度线程，执行RpcResponse的get方法阻塞获取本次请求结果；
- 3、然后，底层通过NIO方式发起调用，provider异步响应请求结果，然后根据RequestId寻找到本次调用的RpcResponse，设置响应结果后唤醒调度线程。
- 4、调度线程被唤醒，返回异步响应的请求数据。


# 1、服务提供者

- 1、XxlRpcProviderFactory

服务启动时的配置入口，在spring环境下XxlRpcSpringProviderFactory借助setApplicationContext方法扫描注解XxlRpcService实例对象完成服务注册，
否则需要XxlRpcProviderFactory.addService(iface, version, serviceBean)完成服务实现者的手动注册。

com.xxl.rpc.remoting.provider.XxlRpcProviderFactory.start开启了服务服务暴露的入口

在netty服务启动中以及停止的过程中，分别调用了服务的注册和移除。

com.xxl.rpc.remoting.net.Server.onStarted

com.xxl.rpc.remoting.net.Server.onStoped


调用的的请求会交给com.xxl.rpc.remoting.net.impl.netty.server.NettyServerHandler.channelRead0来处理，内部调用
com.xxl.rpc.remoting.provider.XxlRpcProviderFactory.invokeService进行处理。具体就是根据xxlRpcRequest封装的参数，通过反射进行方法的调用。


## 1、Server服务暴露，内部封装netty的server

![](../../pic/2020-04-11-10-13-18.png)

这里的核心是定义抽象的启动和销毁方法，让子类去实现，而且定义了启动和销毁可以执行的回调，这样就可以在服务的启动时以及销毁的时候执行自定义的逻辑，比如服务注册到注册中心和摘除注册的服务。


```java
public abstract class BaseCallback {//在注册的时候用到了
    public abstract void run() throws Exception;
}
```

NettyServer和NettyHttpServer,分别提供tcp和http通信服务，对应netty中的两种通信server

![](../../pic/2020-04-11-10-14-07.png)


### 1、NettyServer

![](../../pic/2020-04-11-10-16-01.png)

这里使用一个后台线程去启动server。

```java
public class NettyServer extends Server {

    private Thread thread;

    @Override
    public void start(final XxlRpcProviderFactory xxlRpcProviderFactory) throws Exception {

        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                // param 设置处理业务线程池
                final ThreadPoolExecutor serverHandlerPool = ThreadPoolUtil.makeServerThreadPool(
                        NettyServer.class.getSimpleName(),
                        xxlRpcProviderFactory.getCorePoolSize(),
                        xxlRpcProviderFactory.getMaxPoolSize());
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();

                try {
                    // start server
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel channel) throws Exception {
                                    channel.pipeline()
                                            .addLast(new IdleStateHandler(0,0, Beat.BEAT_INTERVAL*3, TimeUnit.SECONDS))     // beat 3N, close if idle
                                            .addLast(new NettyDecoder(XxlRpcRequest.class, xxlRpcProviderFactory.getSerializerInstance()))//解码
                                            .addLast(new NettyEncoder(XxlRpcResponse.class, xxlRpcProviderFactory.getSerializerInstance()))//编码
                                            .addLast(new NettyServerHandler(xxlRpcProviderFactory, serverHandlerPool));//业务处理handler
                                }
                            })
                            .childOption(ChannelOption.TCP_NODELAY, true)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    // bind
                    ChannelFuture future = bootstrap.bind(xxlRpcProviderFactory.getPort()).sync();

                    logger.info(">>>>>>>>>>> xxl-rpc remoting server start success, nettype = {}, port = {}", NettyServer.class.getName(), xxlRpcProviderFactory.getPort());

                    onStarted();//埋点进行服务的注册

                    // wait util stop
                    future.channel().closeFuture().sync();

                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.info(">>>>>>>>>>> xxl-rpc remoting server stop.");
                    } else {
                        logger.error(">>>>>>>>>>> xxl-rpc remoting server error.", e);
                    }
                } finally {

                    // stop
                    try {
                        serverHandlerPool.shutdown();    // shutdownNow
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    try {
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }

                }
            }
        });
        thread.setDaemon(true);
        thread.start();

    }

    @Override
    public void stop() throws Exception {

        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        // on stop
        onStoped();//移除服务

        logger.info(">>>>>>>>>>> xxl-rpc remoting server destroy success.");
    }

}

```

#### 业务处理器NettyServerHandler


```java
public class NettyServerHandler extends SimpleChannelInboundHandler<XxlRpcRequest> {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    private XxlRpcProviderFactory xxlRpcProviderFactory;
    private ThreadPoolExecutor serverHandlerPool;

    public NettyServerHandler(final XxlRpcProviderFactory xxlRpcProviderFactory, final ThreadPoolExecutor serverHandlerPool) {
        this.xxlRpcProviderFactory = xxlRpcProviderFactory;
        this.serverHandlerPool = serverHandlerPool;//业务线程池
    }


    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final XxlRpcRequest xxlRpcRequest) throws Exception {

        // filter beat
        if (Beat.BEAT_ID.equalsIgnoreCase(xxlRpcRequest.getRequestId())){
            logger.debug(">>>>>>>>>>> xxl-rpc provider netty server read beat-ping.");
            return;
        }

        // do invoke
        try {
            serverHandlerPool.execute(new Runnable() {//接收客户端的请求并处理返回
                @Override
                public void run() {
                    // invoke + response
                    XxlRpcResponse xxlRpcResponse = xxlRpcProviderFactory.invokeService(xxlRpcRequest);

                    ctx.writeAndFlush(xxlRpcResponse);//写会响应结果
                }
            });
        } catch (Exception e) {
            // catch error
            XxlRpcResponse xxlRpcResponse = new XxlRpcResponse();
            xxlRpcResponse.setRequestId(xxlRpcRequest.getRequestId());
            xxlRpcResponse.setErrorMsg(ThrowableUtil.toString(e));

            ctx.writeAndFlush(xxlRpcResponse);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	logger.error(">>>>>>>>>>> xxl-rpc provider netty server caught exception", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            ctx.channel().close();      // beat 3N, close if idle
            logger.debug(">>>>>>>>>>> xxl-rpc provider netty server close an idle channel.");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}


```



### 2、NettyHttpServer

![](../../pic/2020-04-11-10-18-04.png)

代码格式同上面的，只不过这里换成了

```java
public class NettyHttpServer extends Server  {

    private Thread thread;

    @Override
    public void start(final XxlRpcProviderFactory xxlRpcProviderFactory) throws Exception {

        thread = new Thread(new Runnable() {

            @Override
            public void run() {

                // param
                final ThreadPoolExecutor serverHandlerPool = ThreadPoolUtil.makeServerThreadPool(
                        NettyHttpServer.class.getSimpleName(),
                        xxlRpcProviderFactory.getCorePoolSize(),
                        xxlRpcProviderFactory.getMaxPoolSize());
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();

                try {
                    // start server
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel channel) throws Exception {
                                    channel.pipeline()
                                            .addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL * 3, TimeUnit.SECONDS))  // beat 3N, close if idle
                                            .addLast(new HttpServerCodec())
                                            .addLast(new HttpObjectAggregator(5 * 1024 * 1024))  // merge request & reponse to FULL
                                            .addLast(new NettyHttpServerHandler(xxlRpcProviderFactory, serverHandlerPool));
                                }
                            })
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    // bind
                    ChannelFuture future = bootstrap.bind(xxlRpcProviderFactory.getPort()).sync();

                    logger.info(">>>>>>>>>>> xxl-rpc remoting server start success, nettype = {}, port = {}", NettyHttpServer.class.getName(), xxlRpcProviderFactory.getPort());
                    onStarted();

                    // wait util stop
                    future.channel().closeFuture().sync();

                } catch (InterruptedException e) {
                    if (e instanceof InterruptedException) {
                        logger.info(">>>>>>>>>>> xxl-rpc remoting server stop.");
                    } else {
                        logger.error(">>>>>>>>>>> xxl-rpc remoting server error.", e);
                    }
                } finally {

                    // stop
                    try {
                        serverHandlerPool.shutdown();	// shutdownNow
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    try {
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }

            }

        });
        thread.setDaemon(true);	// daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        // destroy server thread
        if (thread!=null && thread.isAlive()) {
            thread.interrupt();
        }

        // on stop
        onStoped();
        logger.info(">>>>>>>>>>> xxl-rpc remoting server destroy success.");
    }

}
```


#### NettyHttpServerHandler

```java
public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);


    private XxlRpcProviderFactory xxlRpcProviderFactory;
    private ThreadPoolExecutor serverHandlerPool;

    public NettyHttpServerHandler(final XxlRpcProviderFactory xxlRpcProviderFactory, final ThreadPoolExecutor serverHandlerPool) {
        this.xxlRpcProviderFactory = xxlRpcProviderFactory;
        this.serverHandlerPool = serverHandlerPool;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

        // request parse
        final byte[] requestBytes = ByteBufUtil.getBytes(msg.content());    // byteBuf.toString(io.netty.util.CharsetUtil.UTF_8);
        final String uri = msg.uri();
        final boolean keepAlive = HttpUtil.isKeepAlive(msg);

        // do invoke
        serverHandlerPool.execute(new Runnable() {
            @Override
            public void run() {
                process(ctx, uri, requestBytes, keepAlive);
            }
        });
    }

    private void process(ChannelHandlerContext ctx, String uri, byte[] requestBytes, boolean keepAlive){
        String requestId = null;
        try {
            if ("/services".equals(uri)) {	// services mapping

                // request
                StringBuffer stringBuffer = new StringBuffer("<ui>");
                for (String serviceKey: xxlRpcProviderFactory.getServiceData().keySet()) {
                    stringBuffer.append("<li>").append(serviceKey).append(": ").append(xxlRpcProviderFactory.getServiceData().get(serviceKey)).append("</li>");
                }
                stringBuffer.append("</ui>");

                // response serialize
                byte[] responseBytes = stringBuffer.toString().getBytes("UTF-8");

                // response-write
                writeResponse(ctx, keepAlive, responseBytes);

            } else {

                // valid
                if (requestBytes.length == 0) {
                    throw new XxlRpcException("xxl-rpc request data empty.");
                }

                // request deserialize
                XxlRpcRequest xxlRpcRequest = (XxlRpcRequest) xxlRpcProviderFactory.getSerializerInstance().deserialize(requestBytes, XxlRpcRequest.class);
                requestId = xxlRpcRequest.getRequestId();

                // filter beat
                if (Beat.BEAT_ID.equalsIgnoreCase(xxlRpcRequest.getRequestId())){
                    logger.debug(">>>>>>>>>>> xxl-rpc provider netty_http server read beat-ping.");
                    return;
                }

                // invoke + response
                XxlRpcResponse xxlRpcResponse = xxlRpcProviderFactory.invokeService(xxlRpcRequest);

                // response serialize
                byte[] responseBytes = xxlRpcProviderFactory.getSerializerInstance().serialize(xxlRpcResponse);

                // response-write
                writeResponse(ctx, keepAlive, responseBytes);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            // response error
            XxlRpcResponse xxlRpcResponse = new XxlRpcResponse();
            xxlRpcResponse.setRequestId(requestId);
            xxlRpcResponse.setErrorMsg(ThrowableUtil.toString(e));

            // response serialize
            byte[] responseBytes = xxlRpcProviderFactory.getSerializerInstance().serialize(xxlRpcResponse);

            // response-write
            writeResponse(ctx, keepAlive, responseBytes);
        }

    }

    /**
     * write response
     */
    private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, byte[] responseBytes){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(responseBytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");       // HttpHeaderValues.TEXT_PLAIN.toString()
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(">>>>>>>>>>> xxl-rpc provider netty_http server caught exception", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            ctx.channel().close();      // beat 3N, close if idle
            logger.debug(">>>>>>>>>>> xxl-rpc provider netty_http server close an idle channel.");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}
```



## 2、XxlRpcProviderFactory服务提供者配置类，启动服务的入口

核心方法：

```java

public void start() throws Exception {}//启动server暴露服务，并且会在这里完成服务的注册

public void  stop() throws Exception {}//关闭服务，这里会去注册中心移除服务

//提供该server可以针对那些接口提供服务，这里用一个hashmap存储接口和对应的实现类，用iface+version作为key，value作为实现类，然后再进行服务注册的时候会把keyset注册到注册中心
public void addService(String iface, String version, Object serviceBean){}

//这里解析请求XxlRpcRequest，看客户端请求的是那个类的那个方法，然后基于反射进行调用，封装返回结果
public XxlRpcResponse invokeService(XxlRpcRequest xxlRpcRequest) {

		//  make response
		XxlRpcResponse xxlRpcResponse = new XxlRpcResponse();
		xxlRpcResponse.setRequestId(xxlRpcRequest.getRequestId());//请求和响应id一致

		// match service bean
		String serviceKey = makeServiceKey(xxlRpcRequest.getClassName(), xxlRpcRequest.getVersion());
		Object serviceBean = serviceData.get(serviceKey);//查找服务

		// valid
		if (serviceBean == null) {
			xxlRpcResponse.setErrorMsg("The serviceKey["+ serviceKey +"] not found.");
			return xxlRpcResponse;
		}

		if (System.currentTimeMillis() - xxlRpcRequest.getCreateMillisTime() > 3*60*1000) {//3分钟
			xxlRpcResponse.setErrorMsg("The timestamp difference between admin and executor exceeds the limit.");
			return xxlRpcResponse;
		}
		if (accessToken!=null && accessToken.trim().length()>0 && !accessToken.trim().equals(xxlRpcRequest.getAccessToken())) {//token安全认证
			xxlRpcResponse.setErrorMsg("The access token[" + xxlRpcRequest.getAccessToken() + "] is wrong.");
			return xxlRpcResponse;
		}

		try {
			// invoke 根据参数通过反射机制调用本地提供的服务
			Class<?> serviceClass = serviceBean.getClass();
			String methodName = xxlRpcRequest.getMethodName();
			Class<?>[] parameterTypes = xxlRpcRequest.getParameterTypes();
			Object[] parameters = xxlRpcRequest.getParameters();

            Method method = serviceClass.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
			Object result = method.invoke(serviceBean, parameters);
			xxlRpcResponse.setResult(result);
		} catch (Throwable t) {
			// catch error
			logger.error("xxl-rpc provider invokeService error.", t);
			xxlRpcResponse.setErrorMsg(ThrowableUtil.toString(t));
		}

		return xxlRpcResponse;
	}

```

## 3、XxlRpcSpringProviderFactory

过注解XxlRpcService获取全部提供的服务实例，后面通过接口进行服务的注册，暴露服务

```java
public class XxlRpcSpringProviderFactory extends XxlRpcProviderFactory implements ApplicationContextAware, InitializingBean,DisposableBean {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //这里通过注解XxlRpcService获取全部提供的服务实例，后面通过接口进行服务的注册
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(XxlRpcService.class);
        if (serviceBeanMap!=null && serviceBeanMap.size()>0) {
            for (Object serviceBean : serviceBeanMap.values()) {
                // valid
                if (serviceBean.getClass().getInterfaces().length ==0) {
                    throw new XxlRpcException("xxl-rpc, service(XxlRpcService) must inherit interface.");
                }
                // add service
                XxlRpcService xxlRpcService = serviceBean.getClass().getAnnotation(XxlRpcService.class);

                String iface = serviceBean.getClass().getInterfaces()[0].getName();
                String version = xxlRpcService.version();

                super.addService(iface, version, serviceBean);//注册服务
            }
        }

        // TODO，addServices by api + prop

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();//这里是配置好后启动服务的入口
    }

    @Override
    public void destroy() throws Exception {
        super.stop();
    }

}

```


## 4、服务提供者启动配置类

```java
@Configuration
public class XxlRpcProviderConfig {
    private Logger logger = LoggerFactory.getLogger(XxlRpcProviderConfig.class);

    @Value("${xxl-rpc.remoting.port}")
    private int port;

    @Value("${xxl-rpc.registry.xxlregistry.address}")
    private String address;

    @Value("${xxl-rpc.registry.xxlregistry.env}")
    private String env;

    @Bean
    public XxlRpcSpringProviderFactory xxlRpcSpringProviderFactory() {

        XxlRpcSpringProviderFactory providerFactory = new XxlRpcSpringProviderFactory();
        providerFactory.setServer(NettyServer.class);//指定使用哪种通信方式
        providerFactory.setSerializer(HessianSerializer.class);//指定序列化方式
        providerFactory.setCorePoolSize(-1);//会被设置为默认值
        providerFactory.setMaxPoolSize(-1);
        providerFactory.setIp(null);
        providerFactory.setPort(port);
        providerFactory.setAccessToken(null);
        providerFactory.setServiceRegistry(XxlRegistryServiceRegistry.class);//指定使用哪种服务进行注册
        providerFactory.setServiceRegistryParam(new HashMap<String, String>() {{//注册服务参数
            put(XxlRegistryServiceRegistry.XXL_REGISTRY_ADDRESS, address);
            put(XxlRegistryServiceRegistry.ENV, env);
        }});

        logger.info(">>>>>>>>>>> xxl-rpc provider config init finish.");
        return providerFactory;
    }

}

```








# 2、服务消费者

- 1、XxlRpcInvokerFactory和XxlRpcSpringInvokerFactory

XxlRpcSpringInvokerFactory内部包含了XxlRpcInvokerFactory，只不过前者借助于spring，在实例化对象的过程中，会扫描对象的字段是否使用了XxlRpcReference注解。
然后把这些字段替换com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean.getObject生成的动态代理对象。

在服务调用端使用的时候，会调用XxlRpcReferenceBean动态代理的handler处理，这里会把请求接口、方法、参数类型、参数封装成一个xxlRpcRequest对象，
然后netty的client端发起rpc通信到server端。

这里有一个netty的异步调用转化为同步调用的操作。借助于XxlRpcFutureResponse阻塞来实现，当server端返回结果后，解除阻塞。
在这里com.xxl.rpc.remoting.net.impl.netty.client.NettyClientHandler.channelRead0调用xxlRpcInvokerFactory.notifyInvokerFuture(xxlRpcResponse.getRequestId(), xxlRpcResponse);
notify阻塞线程。


## 0、client发送请求

![](../../pic/2020-04-11-16-57-41.png)



![](../../pic/2020-04-11-16-40-27.png)

```java
public abstract class Client {//抽象的服务调用端，用来和服务暴露端通信
	protected static final Logger logger = LoggerFactory.getLogger(Client.class);


	// ---------------------- init ----------------------

	protected volatile XxlRpcReferenceBean xxlRpcReferenceBean;

	public void init(XxlRpcReferenceBean xxlRpcReferenceBean) {
		this.xxlRpcReferenceBean = xxlRpcReferenceBean;
	}


    // ---------------------- send ----------------------

	/**
	 * async send, bind requestId and future-response
	 *
	 * @param address
	 * @param xxlRpcRequest
	 * @return
	 * @throws Exception
	 */
	public abstract void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception;

}

```


### 1、NettyClient委托给NettyConnectClient

```java
public class NettyClient extends Client {

	private Class<? extends ConnectClient> connectClientImpl = NettyConnectClient.class;//链接客户端的默认实现，封装了原生的netty

	@Override
	public void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception {
		ConnectClient.asyncSend(xxlRpcRequest, address, connectClientImpl, xxlRpcReferenceBean);
	}

}
```



### 2、NettyHttpClient委托给NettyHttpClient

```java
public class NettyHttpClient extends Client {

    private Class<? extends ConnectClient> connectClientImpl = NettyHttpConnectClient.class;

    @Override
    public void asyncSend(String address, XxlRpcRequest xxlRpcRequest) throws Exception {
        ConnectClient.asyncSend(xxlRpcRequest, address, connectClientImpl, xxlRpcReferenceBean);
    }

}


```


## 1、XxlRpcInvokerFactory

![](../../pic/2020-04-11-11-31-41.png)

- start()：实例化注册中心client，后面可以进行服务发现

### NettyClientHandler/NettyHttpClientHandler把接收到的server数据返回调用端

>  netty的处理器handler在接收到server端的返回结果时，如何通知服务调用方

```java

 public void notifyInvokerFuture(String requestId, final XxlRpcResponse xxlRpcResponse){//netty处理完之后的回调

        // get 从相应池ConcurrentHashMap中根据请求id封装的响应对象
        final XxlRpcFutureResponse futureResponse = futureResponsePool.get(requestId);
        if (futureResponse == null) {
            return;
        }

        // notify
        if (futureResponse.getInvokeCallback()!=null) {

            // callback type
            try {
                executeResponseCallback(new Runnable() {
                    @Override
                    public void run() {
                        if (xxlRpcResponse.getErrorMsg() != null) {
                            futureResponse.getInvokeCallback().onFailure(new XxlRpcException(xxlRpcResponse.getErrorMsg()));
                        } else {
                            futureResponse.getInvokeCallback().onSuccess(xxlRpcResponse.getResult());
                        }
                    }
                });
            }catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {

            // other nomal type 设置返回结果触发调用端的阻塞线程【】
            futureResponse.setResponse(xxlRpcResponse);
        }

        // do remove 从响应池中移除该对象
        futureResponsePool.remove(requestId);

    }
```




## 2、XxlRpcSpringInvokerFactory

![](../../pic/2020-04-11-16-30-54.png)

核心是借助spring的afterPropertiesSet()启动xxlRpcInvokerFactory.start()，初始化连上注册中心获取服务的client。

并且扫描那个对象使用了XxlRpcReference注解注入属性，需要把添加注解的属性使用动态代理对象替换。

```java
public class XxlRpcSpringInvokerFactory extends InstantiationAwareBeanPostProcessorAdapter implements InitializingBean,DisposableBean, BeanFactoryAware {
    private Logger logger = LoggerFactory.getLogger(XxlRpcSpringInvokerFactory.class);

    // ---------------------- config ----------------------

    private Class<? extends ServiceRegistry> serviceRegistryClass;          // class.forname
    private Map<String, String> serviceRegistryParam;


    public void setServiceRegistryClass(Class<? extends ServiceRegistry> serviceRegistryClass) {
        this.serviceRegistryClass = serviceRegistryClass;
    }

    public void setServiceRegistryParam(Map<String, String> serviceRegistryParam) {
        this.serviceRegistryParam = serviceRegistryParam;
    }


    // ---------------------- util ----------------------

    private XxlRpcInvokerFactory xxlRpcInvokerFactory;//【核心的逻辑封装在这里】

    @Override
    public void afterPropertiesSet() throws Exception {
        // start invoker factory
        xxlRpcInvokerFactory = new XxlRpcInvokerFactory(serviceRegistryClass, serviceRegistryParam);//初始化
        xxlRpcInvokerFactory.start();
    }

    @Override //在spring启动的时候扫描添加XxlRpcReference注解的属性字段使用动态代理对象进行替换
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {

        // collection
        final Set<String> serviceKeyList = new HashSet<>();

        // parse XxlRpcReferenceBean
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                if (field.isAnnotationPresent(XxlRpcReference.class)) {//查找属性字段是否加了rpc服务引用注解
                    // valid
                    Class iface = field.getType();//获取该字段的类型，不是接口报错
                    if (!iface.isInterface()) {
                        throw new XxlRpcException("xxl-rpc, reference(XxlRpcReference) must be interface.");
                    }

                    XxlRpcReference rpcReference = field.getAnnotation(XxlRpcReference.class);

                    // init reference bean
                    XxlRpcReferenceBean referenceBean = new XxlRpcReferenceBean();
                    referenceBean.setClient(rpcReference.client());
                    referenceBean.setSerializer(rpcReference.serializer());
                    referenceBean.setCallType(rpcReference.callType());
                    referenceBean.setLoadBalance(rpcReference.loadBalance());
                    referenceBean.setIface(iface);
                    referenceBean.setVersion(rpcReference.version());
                    referenceBean.setTimeout(rpcReference.timeout());
                    referenceBean.setAddress(rpcReference.address());
                    referenceBean.setAccessToken(rpcReference.accessToken());
                    referenceBean.setInvokeCallback(null);
                    referenceBean.setInvokerFactory(xxlRpcInvokerFactory);


                    // get proxyObj
                    Object serviceProxy = null;
                    try {
                        serviceProxy = referenceBean.getObject();//这里生成rpc服务的本地代理对象
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    // set bean
                    field.setAccessible(true);
                    field.set(bean, serviceProxy);//对字段的属性使用代理对象进行替换

                    logger.info(">>>>>>>>>>> xxl-rpc, invoker factory init reference bean success. serviceKey = {}, bean.field = {}.{}",
                            XxlRpcProviderFactory.makeServiceKey(iface.getName(), rpcReference.version()), beanName, field.getName());

                    // collection
                    String serviceKey = XxlRpcProviderFactory.makeServiceKey(iface.getName(), rpcReference.version());
                    serviceKeyList.add(serviceKey);

                }
            }
        });

        // mult discovery
        if (xxlRpcInvokerFactory.getServiceRegistry() != null) {
            try {
                xxlRpcInvokerFactory.getServiceRegistry().discovery(serviceKeyList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return super.postProcessAfterInstantiation(bean, beanName);
    }


    @Override
    public void destroy() throws Exception {

        // stop invoker factory
        xxlRpcInvokerFactory.stop();
    }

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
```

## 3、使用者引用注解XxlRpcReference

![](../../pic/2020-04-11-17-36-45.png)


```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlRpcReference {

    Class<? extends Client> client() default NettyClient.class;//默认的通信客户端
    Class<? extends Serializer> serializer() default HessianSerializer.class;//默认的序列化方式
    CallType callType() default CallType.SYNC;//默认同步调用
    LoadBalance loadBalance() default LoadBalance.ROUND;//默认的负载策略轮询

    //Class<?> iface;
    String version() default "";

    long timeout() default 1000;

    String address() default "";
    String accessToken() default "";

    //XxlRpcInvokeCallback invokeCallback() ;

}


```


## 4、XxlRpcReferenceBean生成接口的远程服务的本地代理

生成动态代理的逻辑：

简单来说就是根据接口首先查询注册中心对应的服务提供者列表，如果有多个服务提供者则根据负载均衡策略选择一个地址address，然后构建请求对象，调用client把构建的请求对象发送到address地址上的服务提供者上，这里会根据是同步调用、异步、回调执行不同的调用。

发送调用的方法均是：clientInstance.asyncSend(finalAddress, xxlRpcRequest);

```java
public Object getObject() throws Exception {

		// initClient 初始化和服务提供者进行通信的client端
		initClient();

		// newProxyInstance 通过动态代理生成本地服务=代理
		return Proxy.newProxyInstance(Thread.currentThread()
				.getContextClassLoader(), new Class[] { iface },
				new InvocationHandler() {//动态代理具体的业务逻辑处理
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

						// method param
						String className = method.getDeclaringClass().getName();	// iface.getName()
						String varsion_ = version;
						String methodName = method.getName();
						Class<?>[] parameterTypes = method.getParameterTypes();
						Object[] parameters = args;

						// filter for generic 泛型调用？？？工作原理
						if (className.equals(XxlRpcGenericService.class.getName()) && methodName.equals("invoke")) {

							Class<?>[] paramTypes = null;
							if (args[3]!=null) {
								String[] paramTypes_str = (String[]) args[3];
								if (paramTypes_str.length > 0) {
									paramTypes = new Class[paramTypes_str.length];
									for (int i = 0; i < paramTypes_str.length; i++) {
										paramTypes[i] = ClassUtil.resolveClass(paramTypes_str[i]);
									}
								}
							}

							className = (String) args[0];
							varsion_ = (String) args[1];
							methodName = (String) args[2];
							parameterTypes = paramTypes;
							parameters = (Object[]) args[4];
						}

						// filter method like "Object.toString()"
						if (className.equals(Object.class.getName())) {
							logger.info(">>>>>>>>>>> xxl-rpc proxy class-method not support [{}#{}]", className, methodName);
							throw new XxlRpcException("xxl-rpc proxy class-method not support");
						}

						// address
						String finalAddress = address;//默认应该是null
						if (finalAddress==null || finalAddress.trim().length()==0) {
							if (invokerFactory!=null && invokerFactory.getServiceRegistry()!=null) {
								// discovery 根据接口的名称和版本生成标识key去注册中心查询这个接口的服务提供者列表
								String serviceKey = XxlRpcProviderFactory.makeServiceKey(className, varsion_);
								TreeSet<String> addressSet = invokerFactory.getServiceRegistry().discovery(serviceKey);//去注册中心拉取
								// load balance
								if (addressSet==null || addressSet.size()==0) {
									// pass
								} else if (addressSet.size()==1) {
									finalAddress = addressSet.first();
								} else {//假如返回的服务提供者有多个，根据路由策略进行选择
									finalAddress = loadBalance.xxlRpcInvokerRouter.route(serviceKey, addressSet);//更具路由策略选择服务
								}

							}
						}
						if (finalAddress==null || finalAddress.trim().length()==0) {
							throw new XxlRpcException("xxl-rpc reference bean["+ className +"] address empty");
						}

						// request 封装rpc请求
						XxlRpcRequest xxlRpcRequest = new XxlRpcRequest();
	                    xxlRpcRequest.setRequestId(UUID.randomUUID().toString());
	                    xxlRpcRequest.setCreateMillisTime(System.currentTimeMillis());
	                    xxlRpcRequest.setAccessToken(accessToken);
	                    xxlRpcRequest.setClassName(className);
	                    xxlRpcRequest.setMethodName(methodName);
	                    xxlRpcRequest.setParameterTypes(parameterTypes);
	                    xxlRpcRequest.setParameters(parameters);
	                    xxlRpcRequest.setVersion(version);

	                    // send
						if (CallType.SYNC == callType) {//同步调用
							// future-response set  在这里设置异步回调阻塞，当结果返回的时候再触发，解除阻塞【异步调用转为同步的关键】
							XxlRpcFutureResponse futureResponse = new XxlRpcFutureResponse(invokerFactory, xxlRpcRequest, null);
							try {
								// do invoke 进行远程服务调用【重要】
								clientInstance.asyncSend(finalAddress, xxlRpcRequest);

								// future get
								XxlRpcResponse xxlRpcResponse = futureResponse.get(timeout, TimeUnit.MILLISECONDS);
								if (xxlRpcResponse.getErrorMsg() != null) {
									throw new XxlRpcException(xxlRpcResponse.getErrorMsg());
								}
								return xxlRpcResponse.getResult();
							} catch (Exception e) {
								logger.info(">>>>>>>>>>> xxl-rpc, invoke error, address:{}, XxlRpcRequest{}", finalAddress, xxlRpcRequest);

								throw (e instanceof XxlRpcException)?e:new XxlRpcException(e);
							} finally{
								// future-response remove
								futureResponse.removeInvokerFuture();
							}
						} else if (CallType.FUTURE == callType) {//异步调用
							// future-response set
							XxlRpcFutureResponse futureResponse = new XxlRpcFutureResponse(invokerFactory, xxlRpcRequest, null);
                            try {
								// invoke future set
								XxlRpcInvokeFuture invokeFuture = new XxlRpcInvokeFuture(futureResponse);
								XxlRpcInvokeFuture.setFuture(invokeFuture);

                                // do invoke
								clientInstance.asyncSend(finalAddress, xxlRpcRequest);

                                return null;
                            } catch (Exception e) {
								logger.info(">>>>>>>>>>> xxl-rpc, invoke error, address:{}, XxlRpcRequest{}", finalAddress, xxlRpcRequest);

								// future-response remove
								futureResponse.removeInvokerFuture();

								throw (e instanceof XxlRpcException)?e:new XxlRpcException(e);
                            }

						} else if (CallType.CALLBACK == callType) {

							// get callback
							XxlRpcInvokeCallback finalInvokeCallback = invokeCallback;
							XxlRpcInvokeCallback threadInvokeCallback = XxlRpcInvokeCallback.getCallback();
							if (threadInvokeCallback != null) {
								finalInvokeCallback = threadInvokeCallback;
							}
							if (finalInvokeCallback == null) {
								throw new XxlRpcException("xxl-rpc XxlRpcInvokeCallback（CallType="+ CallType.CALLBACK.name() +"） cannot be null.");
							}

							// future-response set
							XxlRpcFutureResponse futureResponse = new XxlRpcFutureResponse(invokerFactory, xxlRpcRequest, finalInvokeCallback);
							try {
								clientInstance.asyncSend(finalAddress, xxlRpcRequest);
							} catch (Exception e) {
								logger.info(">>>>>>>>>>> xxl-rpc, invoke error, address:{}, XxlRpcRequest{}", finalAddress, xxlRpcRequest);

								// future-response remove
								futureResponse.removeInvokerFuture();

								throw (e instanceof XxlRpcException)?e:new XxlRpcException(e);
							}

							return null;
						} else if (CallType.ONEWAY == callType) {
							clientInstance.asyncSend(finalAddress, xxlRpcRequest);
                            return null;
                        } else {
							throw new XxlRpcException("xxl-rpc callType["+ callType +"] invalid");
						}

					}
				});
	}
```


### 1、同步调用：XxlRpcFutureResponse同步调用转化为异步

当client底层通信接收到server端的响应会调用setResponse解除阻塞

```java
public class XxlRpcFutureResponse implements Future<XxlRpcResponse> {//rpc nio异步调用转为同步调用的关键

	private XxlRpcInvokerFactory invokerFactory;

	// net data
	private XxlRpcRequest request;
	private XxlRpcResponse response;

	// future lock
	private boolean done = false;
	private Object lock = new Object();

	// callback, can be null
	private XxlRpcInvokeCallback invokeCallback;


	public XxlRpcFutureResponse(final XxlRpcInvokerFactory invokerFactory, XxlRpcRequest request, XxlRpcInvokeCallback invokeCallback) {
		this.invokerFactory = invokerFactory;
		this.request = request;
		this.invokeCallback = invokeCallback;

		// set-InvokerFuture  设置异步回调【重要】
		setInvokerFuture();
	}


	// ---------------------- response pool ----------------------

	public void setInvokerFuture(){
		this.invokerFactory.setInvokerFuture(request.getRequestId(), this);
	}
	public void removeInvokerFuture(){
		this.invokerFactory.removeInvokerFuture(request.getRequestId());
	}


	// ---------------------- get ----------------------

	public XxlRpcRequest getRequest() {
		return request;
	}
	public XxlRpcInvokeCallback getInvokeCallback() {
		return invokeCallback;
	}


	// ---------------------- for invoke back ----------------------

	public void setResponse(XxlRpcResponse response) {//设置结果唤醒阻塞的线程
		this.response = response;
		synchronized (lock) {
			done = true;
			lock.notifyAll();
		}
	}


	// ---------------------- for invoke ----------------------

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO
		return false;
	}

	@Override
	public boolean isCancelled() {
		// TODO
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public XxlRpcResponse get() throws InterruptedException, ExecutionException {//自己编写了调用future.get阻塞的逻辑，基于wait和notify
		try {
			return get(-1, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new XxlRpcException(e);
		}
	}

	@Override
	public XxlRpcResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!done) {
			synchronized (lock) {
				try {
					if (timeout < 0) {
						lock.wait();
					} else {
						long timeoutMillis = (TimeUnit.MILLISECONDS==unit)?timeout:TimeUnit.MILLISECONDS.convert(timeout , unit);
						lock.wait(timeoutMillis);
					}
				} catch (InterruptedException e) {
					throw e;
				}
			}
		}

		if (!done) {
			throw new XxlRpcException("xxl-rpc, request timeout at:"+ System.currentTimeMillis() +", request:" + request.toString());
		}
		return response;
	}


}

```

### 2、异步调用：XxlRpcInvokeFuture支持异步调用，返回一个future对象

本质还是XxlRpcFutureResponse对象封装一下，然后借助ThreadLocal把这个异步请求的future绑定到当前线程上，当调用Future<UserDTO> userDTOFuture = XxlRpcInvokeFuture.getFuture(UserDTO.class);获得future，然后调用get阻塞获取结果。

```java
public class XxlRpcInvokeFuture implements Future {


    private XxlRpcFutureResponse futureResponse;

    public XxlRpcInvokeFuture(XxlRpcFutureResponse futureResponse) {
        this.futureResponse = futureResponse;
    }
    public void stop(){
        // remove-InvokerFuture
        futureResponse.removeInvokerFuture();
    }



    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureResponse.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureResponse.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureResponse.isDone();
    }

    @Override
    public Object get() throws ExecutionException, InterruptedException {
        try {
            return get(-1, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new XxlRpcException(e);
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            // future get
            XxlRpcResponse xxlRpcResponse = futureResponse.get(timeout, unit);
            if (xxlRpcResponse.getErrorMsg() != null) {
                throw new XxlRpcException(xxlRpcResponse.getErrorMsg());
            }
            return xxlRpcResponse.getResult();
        } finally {
            stop();
        }
    }


    // ---------------------- thread invoke future ---------------------- 绑定当前线程返回一个future对象

    private static ThreadLocal<XxlRpcInvokeFuture> threadInvokerFuture = new ThreadLocal<XxlRpcInvokeFuture>();

    /**
     * get future
     *
     * @param type
     * @param <T>
     * @return
     */
    public static <T> Future<T> getFuture(Class<T> type) {
        Future<T> future = (Future<T>) threadInvokerFuture.get();
        threadInvokerFuture.remove();
        return future;
    }

    /**
     * set future
     *
     * @param future
     */
    public static void setFuture(XxlRpcInvokeFuture future) {
        threadInvokerFuture.set(future);
    }

    /**
     * remove future
     */
    public static void removeFuture() {
        threadInvokerFuture.remove();
    }

}


```




### 3、回调：XxlRpcInvokeCallback

把XxlRpcInvokeCallback逻辑借助ThreadLocal绑定到当前线程。？？？


```java
ublic abstract class XxlRpcInvokeCallback<T> {

    public abstract void onSuccess(T result);

    public abstract void onFailure(Throwable exception);


    // ---------------------- thread invoke callback ----------------------

    private static ThreadLocal<XxlRpcInvokeCallback> threadInvokerFuture = new ThreadLocal<XxlRpcInvokeCallback>();

    /**
     * get callback
     *
     * @return
     */
    public static XxlRpcInvokeCallback getCallback() {
        XxlRpcInvokeCallback invokeCallback = threadInvokerFuture.get();
        threadInvokerFuture.remove();
        return invokeCallback;
    }

    /**
     * set future
     *
     * @param invokeCallback
     */
    public static void setCallback(XxlRpcInvokeCallback invokeCallback) {
        threadInvokerFuture.set(invokeCallback);
    }

    /**
     * remove future
     */
    public static void removeCallback() {
        threadInvokerFuture.remove();
    }


}

```



### 4、oneway不关心执行结果：直接调用即可


## 5、LoadBalance

在XxlRpcReferenceBean通过注册中心发现有多个服务提供者时，这里服务消费者可指定负载策略，默认是轮询

```java
public enum LoadBalance {

    RANDOM(new XxlRpcLoadBalanceRandomStrategy()),
    ROUND(new XxlRpcLoadBalanceRoundStrategy()),
    LRU(new XxlRpcLoadBalanceLRUStrategy()),
    LFU(new XxlRpcLoadBalanceLFUStrategy()),
    CONSISTENT_HASH(new XxlRpcLoadBalanceConsistentHashStrategy());
}
```


## 9、消费者使用配置类

```java

Configuration
public class XxlRpcInvokerConfig {
    private Logger logger = LoggerFactory.getLogger(XxlRpcInvokerConfig.class);


    @Value("${xxl-rpc.registry.xxlregistry.address}")
    private String address;

    @Value("${xxl-rpc.registry.xxlregistry.env}")
    private String env;


    @Bean
    public XxlRpcSpringInvokerFactory xxlJobExecutor() {

        XxlRpcSpringInvokerFactory invokerFactory = new XxlRpcSpringInvokerFactory();
        invokerFactory.setServiceRegistryClass(XxlRegistryServiceRegistry.class);//指定注册中心
        invokerFactory.setServiceRegistryParam(new HashMap<String, String>(){{
            put(XxlRegistryServiceRegistry.XXL_REGISTRY_ADDRESS, address);
            put(XxlRegistryServiceRegistry.ENV, env);
        }});

        logger.info(">>>>>>>>>>> xxl-rpc invoker config init finish.");
        return invokerFactory;
    }

}
```




# 3、ServiceRegistry服务注册和发现接口

![](../../pic/2020-04-11-10-36-10.png)

```java
public abstract class ServiceRegistry {//服务注册抽象类

    //服务启动，主要是进行服务注册进行初始化，比如，远程通信的client端
    public abstract void start(Map<String, String> param);

    //关闭服务
    public abstract void stop();

    //注册服务，value是Ip+port
    public abstract boolean registry(Set<String> keys, String value);

    //摘除服务
    public abstract boolean remove(Set<String> keys, String value);

    //服务发现
    public abstract Map<String, TreeSet<String>> discovery(Set<String> keys);

    //服务发现
    public abstract TreeSet<String> discovery(String key);

}


```

![](../../pic/2020-04-11-10-42-17.png)


## 1、LocalServiceRegistry本地注册

```java
public class LocalServiceRegistry extends ServiceRegistry {//本地注册
    //使用map存储
    private Map<String, TreeSet<String>> registryData;

    //初始化map
    @Override
    public void start(Map<String, String> param) {
        registryData = new HashMap<String, TreeSet<String>>();
    }

    //清空map
    @Override
    public void stop() {
        registryData.clear();
    }

    //注册服务
    @Override
    public boolean registry(Set<String> keys, String value) {
        if (keys==null || keys.size()==0 || value==null || value.trim().length()==0) {
            return false;
        }
        for (String key : keys) {//遍历key，借助set去重
            TreeSet<String> values = registryData.get(key);
            if (values == null) {
                values = new TreeSet<>();
                registryData.put(key, values);
            }
            values.add(value);
        }
        return true;
    }

    //移除服务
    @Override
    public boolean remove(Set<String> keys, String value) {
        if (keys==null || keys.size()==0 || value==null || value.trim().length()==0) {
            return false;
        }
        for (String key : keys) {
            TreeSet<String> values = registryData.get(key);
            if (values != null) {
                values.remove(value);
            }
        }
        return true;
    }
    //查找这些服务IP和端口
    @Override
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        if (keys==null || keys.size()==0) {
            return null;
        }
        Map<String, TreeSet<String>> registryDataTmp = new HashMap<String, TreeSet<String>>();
        for (String key : keys) {
            TreeSet<String> valueSetTmp = discovery(key);
            if (valueSetTmp != null) {
                registryDataTmp.put(key, valueSetTmp);
            }
        }
        return registryDataTmp;
    }
    //返回一个key对应的IP和端口
    @Override
    public TreeSet<String> discovery(String key) {
        return registryData.get(key);
    }

}

```


## 2、XxlRegistryServiceRegistry

```java
public class XxlRegistryServiceRegistry extends ServiceRegistry {//把服务注册到注册中心去

    public static final String XXL_REGISTRY_ADDRESS = "XXL_REGISTRY_ADDRESS";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String BIZ = "BIZ";
    public static final String ENV = "ENV";

    private XxlRegistryClient xxlRegistryClient;//进行服务注册的client
    public XxlRegistryClient getXxlRegistryClient() {
        return xxlRegistryClient;
    }

    @Override
    public void start(Map<String, String> param) {
        String xxlRegistryAddress = param.get(XXL_REGISTRY_ADDRESS);//注册中心的地址
        String accessToken = param.get(ACCESS_TOKEN);//安全认证token
        String biz = param.get(BIZ);
        String env = param.get(ENV);

        // fill
        biz = (biz!=null&&biz.trim().length()>0)?biz:"default";
        env = (env!=null&&env.trim().length()>0)?env:"default";
        //创建一个client
        xxlRegistryClient = new XxlRegistryClient(xxlRegistryAddress, accessToken, biz, env);
    }

    @Override
    public void stop() {
        if (xxlRegistryClient != null) {//销毁client
            xxlRegistryClient.stop();
        }
    }

    @Override
    public boolean registry(Set<String> keys, String value) {
        if (keys==null || keys.size() == 0 || value == null) {
            return false;
        }

        // init
        List<XxlRegistryDataParamVO> registryDataList = new ArrayList<>();
        for (String key:keys) {
            registryDataList.add(new XxlRegistryDataParamVO(key, value));
        }
        //封装服务参数进行注册
        return xxlRegistryClient.registry(registryDataList);
    }

    @Override
    public boolean remove(Set<String> keys, String value) {
        if (keys==null || keys.size() == 0 || value == null) {
            return false;
        }

        // init
        List<XxlRegistryDataParamVO> registryDataList = new ArrayList<>();
        for (String key:keys) {
            registryDataList.add(new XxlRegistryDataParamVO(key, value));
        }
        //从注册中心移除那些服务
        return xxlRegistryClient.remove(registryDataList);
    }

    @Override
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        return xxlRegistryClient.discovery(keys);
    }

    @Override
    public TreeSet<String> discovery(String key) {
        return xxlRegistryClient.discovery(key);
    }

}

```


进行服务注册的参数

```java
public class XxlRegistryDataParamVO {
    private String key;
    private String value;
}
```


## 3、ZkServiceRegistry

```java

public class ZkServiceRegistry extends ServiceRegistry {
    private static Logger logger = LoggerFactory.getLogger(ZkServiceRegistry.class);

    // param
    public static final String ENV = "env";                       // zk env
    public static final String ZK_ADDRESS = "zkaddress";        // zk registry address, like "ip1:port,ip2:port,ip3:port"
    public static final String ZK_DIGEST = "zkdigest";          // zk registry digest


    // ------------------------------ zk conf ------------------------------

    // config
    private static final String zkBasePath = "/xxl-rpc";
    private String zkEnvPath;
    private XxlZkClient xxlZkClient = null;

    private Thread refreshThread;
    private volatile boolean refreshThreadStop = false;
    //服务注册数据
    private volatile ConcurrentMap<String, TreeSet<String>> registryData = new ConcurrentHashMap<String, TreeSet<String>>();
    //服务发现数据
    private volatile ConcurrentMap<String, TreeSet<String>> discoveryData = new ConcurrentHashMap<String, TreeSet<String>>();


    /**
     * key 2 path
     * @param   nodeKey
     * @return  znodePath
     */
    public String keyToPath(String nodeKey){
        return zkEnvPath + "/" + nodeKey;
    }

    /**
     * path 2 key
     * @param   nodePath
     * @return  nodeKey
     */
    public String pathToKey(String nodePath){
        if (nodePath==null || nodePath.length() <= zkEnvPath.length() || !nodePath.startsWith(zkEnvPath)) {
            return null;
        }
        return nodePath.substring(zkEnvPath.length()+1, nodePath.length());
    }

    // ------------------------------ util ------------------------------

    /**
     * @param param
     *      Environment.ZK_ADDRESS  ：zk address
     *      Environment.ZK_DIGEST   ：zk didest
     *      Environment.ENV         ：env
     */
    @Override
    public void start(Map<String, String> param) {
        String zkaddress = param.get(ZK_ADDRESS);
        String zkdigest = param.get(ZK_DIGEST);
        String env = param.get(ENV);

        // valid
        if (zkaddress==null || zkaddress.trim().length()==0) {
            throw new XxlRpcException("xxl-rpc zkaddress can not be empty");
        }

        // init zkpath
        if (env==null || env.trim().length()==0) {
            throw new XxlRpcException("xxl-rpc env can not be empty");
        }

        zkEnvPath = zkBasePath.concat("/").concat(env);

        // init
        xxlZkClient = new XxlZkClient(zkaddress, zkEnvPath, zkdigest, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                try {
                    logger.debug(">>>>>>>>>>> xxl-rpc: watcher:{}", watchedEvent);

                    // session expire, close old and create new
                    if (watchedEvent.getState() == Event.KeeperState.Expired) {
                        xxlZkClient.destroy();
                        xxlZkClient.getClient();

                        // refreshDiscoveryData (all)：expire retry
                        refreshDiscoveryData(null);

                        logger.info(">>>>>>>>>>> xxl-rpc, zk re-connect reloadAll success.");
                    }

                    // watch + refresh
                    String path = watchedEvent.getPath();
                    String key = pathToKey(path);
                    if (key != null) {
                        // keep watch conf key：add One-time trigger
                        xxlZkClient.getClient().exists(path, true);

                        // refresh
                        if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                            // refreshDiscoveryData (one)：one change
                            refreshDiscoveryData(key);
                        } else if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                            logger.info("reload all 111");
                        }
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });

        // init client      // TODO, support init without conn, and can use mirror data
        xxlZkClient.getClient();


        // refresh thread
        refreshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!refreshThreadStop) {
                    try {
                        TimeUnit.SECONDS.sleep(60);

                        // refreshDiscoveryData (all)：cycle check
                        refreshDiscoveryData(null);

                        // refresh RegistryData
                        refreshRegistryData();
                    } catch (Exception e) {
                        if (!refreshThreadStop) {
                            logger.error(">>>>>>>>>>> xxl-rpc, refresh thread error.", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-rpc, refresh thread stoped.");
            }
        });
        refreshThread.setName("xxl-rpc, ZkServiceRegistry refresh thread.");
        refreshThread.setDaemon(true);
        refreshThread.start();

        logger.info(">>>>>>>>>>> xxl-rpc, ZkServiceRegistry init success. [env={}]", env);
    }

    @Override
    public void stop() {
        if (xxlZkClient!=null) {
            xxlZkClient.destroy();
        }
        if (refreshThread != null) {
            refreshThreadStop = true;
            refreshThread.interrupt();
        }
    }

    /**
     * refresh discovery data, and cache
     *
     * @param key
     */
    private void refreshDiscoveryData(String key){

        Set<String> keys = new HashSet<String>();
        if (key!=null && key.trim().length()>0) {
            keys.add(key);
        } else {
            if (discoveryData.size() > 0) {
                keys.addAll(discoveryData.keySet());
            }
        }

        if (keys.size() > 0) {
            for (String keyItem: keys) {

                // add-values
                String path = keyToPath(keyItem);
                Map<String, String> childPathData = xxlZkClient.getChildPathData(path);

                // exist-values
                TreeSet<String> existValues = discoveryData.get(keyItem);
                if (existValues == null) {
                    existValues = new TreeSet<String>();
                    discoveryData.put(keyItem, existValues);
                }

                if (childPathData.size() > 0) {
                    existValues.clear();
                    existValues.addAll(childPathData.keySet());
                }
            }
            logger.info(">>>>>>>>>>> xxl-rpc, refresh discovery data success, discoveryData = {}", discoveryData);
        }
    }

    /**
     * refresh registry data
     */
    private void refreshRegistryData(){
        if (registryData.size() > 0) {
            for (Map.Entry<String, TreeSet<String>> item: registryData.entrySet()) {
                String key = item.getKey();
                for (String value:item.getValue()) {
                    // make path, child path
                    String path = keyToPath(key);
                    xxlZkClient.setChildPathData(path, value, "");
                }
            }
            logger.info(">>>>>>>>>>> xxl-rpc, refresh registry data success, registryData = {}", registryData);
        }
    }

    @Override
    public boolean registry(Set<String> keys, String value) {
        for (String key : keys) {
            // local cache
            TreeSet<String> values = registryData.get(key);
            if (values == null) {
                values = new TreeSet<>();
                registryData.put(key, values);
            }
            values.add(value);

            // make path, child path
            String path = keyToPath(key);
            xxlZkClient.setChildPathData(path, value, "");
        }
        logger.info(">>>>>>>>>>> xxl-rpc, registry success, keys = {}, value = {}", keys, value);
        return true;
    }

    @Override
    public boolean remove(Set<String> keys, String value) {
        for (String key : keys) {
            TreeSet<String> values = discoveryData.get(key);
            if (values != null) {
                values.remove(value);
            }
            String path = keyToPath(key);
            xxlZkClient.deleteChildPath(path, value);
        }
        logger.info(">>>>>>>>>>> xxl-rpc, remove success, keys = {}, value = {}", keys, value);
        return true;
    }

    @Override
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        if (keys==null || keys.size()==0) {
            return null;
        }
        Map<String, TreeSet<String>> registryDataTmp = new HashMap<String, TreeSet<String>>();
        for (String key : keys) {
            TreeSet<String> valueSetTmp = discovery(key);
            if (valueSetTmp != null) {
                registryDataTmp.put(key, valueSetTmp);
            }
        }
        return registryDataTmp;
    }

    @Override
    public TreeSet<String> discovery(String key) {

        // local cache
        TreeSet<String> values = discoveryData.get(key);
        if (values == null) {

            // refreshDiscoveryData (one)：first use
            refreshDiscoveryData(key);

            values = discoveryData.get(key);
        }

        return values;
    }

}
```


# 4、NettyDecoder和NettyEncoder

## 1、NettyDecoder

这里是把netty中的字节通过序列化器Serializer变成指定的对象XxlRpcRequest或者XxlRpcResponse

```java

public class NettyDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;
    private Serializer serializer;

    public NettyDecoder(Class<?> genericClass, final Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    //这里先通过字节的长度进行有效性判断
    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        int dataLength = in.readInt();//传输数据的长度
        if (dataLength < 0) {
            ctx.close();
        }
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;	// fix 1024k buffer splice limix
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        Object obj = serializer.deserialize(data, genericClass);
        out.add(obj);
    }
}

```

## 2、NettyEncoder


```java
public class NettyEncoder extends MessageToByteEncoder<Object> {

    private Class<?> genericClass;
    private Serializer serializer;

    public NettyEncoder(Class<?> genericClass, final Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            byte[] data = serializer.serialize(in);
            out.writeInt(data.length);//先写长度
            out.writeBytes(data);//再写数据
        }
    }
}

```



# 5、XxlRpcRequest和XxlRpcResponse


## 1、XxlRpcRequest

```java
public class XxlRpcRequest implements Serializable{//封装了rpc请求信息
	private static final long serialVersionUID = 42L;

	private String requestId;//请求编号
	private long createMillisTime;//请求创建时间戳
	private String accessToken;

    private String className;//类名称
    private String methodName;//方法名称
    private Class<?>[] parameterTypes;//方法参数类型
    private Object[] parameters;//方法参数

	private String version;//版本
}
```


## 2、XxlRpcResponse

```java
public class XxlRpcResponse implements Serializable{//封装请求结果
	private static final long serialVersionUID = 42L;


	private String requestId;//请求序号
    private String errorMsg;//错误信息
    private Object result;//返回的业务数据对象
}
```

# 6、远程通信客户端抽象ConnectClient

因为netty本身基于nio就是异步通信，这里定义的消息发送不论是那种都会转化为netty的异步通信，即this.channel.writeAndFlush(xxlRpcRequest).sync();这里定义的同步还是异步只是调用方的视角。

![](../../pic/2020-04-11-16-42-48.png)

[针对每一个定义的被XxlRpcReference修饰的接口创建一个通信client]


```java
public abstract class ConnectClient {
    protected static transient Logger logger = LoggerFactory.getLogger(ConnectClient.class);

    // ---------------------- iface ----------------------

    public abstract void init(String address, final Serializer serializer, final XxlRpcInvokerFactory xxlRpcInvokerFactory) throws Exception;//初始化通信的通道

    public abstract void close();

    public abstract boolean isValidate();

    public abstract void send(XxlRpcRequest xxlRpcRequest) throws Exception ;//发送消息，模板方法接口。留给子类自己去实现，真正发送消息的方法


    // ---------------------- client pool map ----------------------

    /**
     * async send 从客户端池中获取一个客户端进行进行通信
     */
    public static void asyncSend(XxlRpcRequest xxlRpcRequest, String address,
                                 Class<? extends ConnectClient> connectClientImpl,
                                 final XxlRpcReferenceBean xxlRpcReferenceBean) throws Exception {

        // client pool	[tips03 : may save 35ms/100invoke if move it to constructor, but it is necessary. cause by ConcurrentHashMap.get]
        ////根据地址address、通信客户端类型以及创建通信client指定调用接口引用，去链接池中获取一个通信client对象
        ConnectClient clientPool = ConnectClient.getPool(address, connectClientImpl, xxlRpcReferenceBean);

        try {
            // do invoke
            clientPool.send(xxlRpcRequest);
        } catch (Exception e) {
            throw e;
        }

    }

    private static volatile ConcurrentMap<String, ConnectClient> connectClientMap;        // (static) alread addStopCallBack
    private static volatile ConcurrentMap<String, Object> connectClientLockMap = new ConcurrentHashMap<>();
    private static ConnectClient getPool(String address, Class<? extends ConnectClient> connectClientImpl,
                                         final XxlRpcReferenceBean xxlRpcReferenceBean) throws Exception {

        // init base compont, avoid repeat init
        if (connectClientMap == null) {
            synchronized (ConnectClient.class) {
                if (connectClientMap == null) {//双重检测进行初始化
                    // init
                    connectClientMap = new ConcurrentHashMap<String, ConnectClient>();
                    // stop callback
                    xxlRpcReferenceBean.getInvokerFactory().addStopCallBack(new BaseCallback() {//销毁XxlRpcInvokerFactory的时候对这里client进行销毁处理
                        @Override
                        public void run() throws Exception {
                            if (connectClientMap.size() > 0) {
                                for (String key: connectClientMap.keySet()) {//遍历关系client
                                    ConnectClient clientPool = connectClientMap.get(key);
                                    clientPool.close();
                                }
                                connectClientMap.clear();//清空缓存池
                            }
                        }
                    });
                }
            }
        }

        // get-valid client校验这个地址上的通信client是否正常，正常的话直接返回
        ConnectClient connectClient = connectClientMap.get(address);
        if (connectClient!=null && connectClient.isValidate()) {
            return connectClient;
        }

        // lock 每一个地址分配一个创建锁
        Object clientLock = connectClientLockMap.get(address);
        if (clientLock == null) {
            connectClientLockMap.putIfAbsent(address, new Object());
            clientLock = connectClientLockMap.get(address);
        }

        // remove-create new client
        synchronized (clientLock) {//避免针对同一个地址同时创建连接client，通过加锁实现

            // get-valid client, avlid repeat
            connectClient = connectClientMap.get(address);
            if (connectClient!=null && connectClient.isValidate()) {
                return connectClient;
            }

            // remove old client存在但是已经失效了，需要移除新建
            if (connectClient != null) {
                connectClient.close();
                connectClientMap.remove(address);
            }

            // set pool 新建一个连接client
            ConnectClient connectClient_new = connectClientImpl.newInstance();
            try {
                connectClient_new.init(address, xxlRpcReferenceBean.getSerializerInstance(), xxlRpcReferenceBean.getInvokerFactory());//新建完成进行初始化
                connectClientMap.put(address, connectClient_new);//放入缓存中
            } catch (Exception e) {
                connectClient_new.close();
                throw e;
            }

            return connectClient_new;
        }

    }

}


```


## 1、NettyConnectClient

![](../../pic/2020-04-11-16-49-45.png)

```java
public class NettyConnectClient extends ConnectClient {


    private EventLoopGroup group;
    private Channel channel;//通信通道


    @Override
    public void init(String address, final Serializer serializer, final XxlRpcInvokerFactory xxlRpcInvokerFactory) throws Exception {
        final NettyConnectClient thisClient = this;

        Object[] array = IpUtil.parseIpPort(address);
        String host = (String) array[0];
        int port = (int) array[1];


        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(0,0,Beat.BEAT_INTERVAL, TimeUnit.SECONDS))    // beat N, close if fail
                                .addLast(new NettyEncoder(XxlRpcRequest.class, serializer))
                                .addLast(new NettyDecoder(XxlRpcResponse.class, serializer))
                                .addLast(new NettyClientHandler(xxlRpcInvokerFactory, thisClient));
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        this.channel = bootstrap.connect(host, port).sync().channel();

        // valid
        if (!isValidate()) {
            close();
            return;
        }

        logger.debug(">>>>>>>>>>> xxl-rpc netty client proxy, connect to server success at host:{}, port:{}", host, port);
    }


    @Override
    public boolean isValidate() {
        if (this.channel != null) {
            return this.channel.isActive();
        }
        return false;
    }

    @Override
    public void close() {
        if (this.channel != null && this.channel.isActive()) {
            this.channel.close();        // if this.channel.isOpen()
        }
        if (this.group != null && !this.group.isShutdown()) {
            this.group.shutdownGracefully();
        }
        logger.debug(">>>>>>>>>>> xxl-rpc netty client close.");
    }


    @Override
    public void send(XxlRpcRequest xxlRpcRequest) throws Exception {//发送请求
        this.channel.writeAndFlush(xxlRpcRequest).sync();
    }
}

```


### NettyClientHandler业务处理器读出服务端的响应结果

```java
public class NettyClientHandler extends SimpleChannelInboundHandler<XxlRpcResponse> {
	private static final Logger logger = LoggerFactory.getLogger(NettyClientHandler.class);


	private XxlRpcInvokerFactory xxlRpcInvokerFactory;
	private NettyConnectClient nettyConnectClient;
	public NettyClientHandler(final XxlRpcInvokerFactory xxlRpcInvokerFactory, NettyConnectClient nettyConnectClient) {
		this.xxlRpcInvokerFactory = xxlRpcInvokerFactory;
		this.nettyConnectClient = nettyConnectClient;
	}


	@Override
	protected void channelRead0(ChannelHandlerContext ctx, XxlRpcResponse xxlRpcResponse) throws Exception {

		// notify response  读取服务端返回的响应
		xxlRpcInvokerFactory.notifyInvokerFuture(xxlRpcResponse.getRequestId(), xxlRpcResponse);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(">>>>>>>>>>> xxl-rpc netty client caught exception", cause);
		ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent){
			/*ctx.channel().close();      // close idle channel
			logger.debug(">>>>>>>>>>> xxl-rpc netty client close an idle channel.");*/

			nettyConnectClient.send(Beat.BEAT_PING);	// beat N, close if fail(may throw error)
			logger.debug(">>>>>>>>>>> xxl-rpc netty client send beat-ping.");

		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

}

```



## 2、NettyHttpConnectClient

```java
public class NettyHttpConnectClient extends ConnectClient {

    private EventLoopGroup group;
    private Channel channel;

    private Serializer serializer;
    private String address;
    private String host;
    private DefaultFullHttpRequest beatRequest;

    @Override
    public void init(String address, final Serializer serializer, final XxlRpcInvokerFactory xxlRpcInvokerFactory) throws Exception {
        final NettyHttpConnectClient thisClient = this;

        if (!address.toLowerCase().startsWith("http")) {
            address = "http://" + address;	// IP:PORT, need parse to url
        }

        this.address = address;
        URL url = new URL(address);
        this.host = url.getHost();
        int port = url.getPort()>-1?url.getPort():80;


        this.group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(0,0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS))   // beat N, close if fail
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(5*1024*1024))
                                .addLast(new NettyHttpClientHandler(xxlRpcInvokerFactory, serializer, thisClient));
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        this.channel = bootstrap.connect(host, port).sync().channel();

        this.serializer = serializer;

        // valid
        if (!isValidate()) {
            close();
            return;
        }

        logger.debug(">>>>>>>>>>> xxl-rpc netty client proxy, connect to server success at host:{}, port:{}", host, port);
    }

    @Override
    public boolean isValidate() {
        if (this.channel != null) {
            return this.channel.isActive();
        }
        return false;
    }


    @Override
    public void close() {
        if (this.channel!=null && this.channel.isActive()) {
            this.channel.close();		// if this.channel.isOpen()
        }
        if (this.group!=null && !this.group.isShutdown()) {
            this.group.shutdownGracefully();
        }
        logger.debug(">>>>>>>>>>> xxl-rpc netty client close.");
    }


    @Override
    public void send(XxlRpcRequest xxlRpcRequest) throws Exception {
        byte[] requestBytes = serializer.serialize(xxlRpcRequest);

        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, new URI(address).getRawPath(), Unpooled.wrappedBuffer(requestBytes));
        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());

        this.channel.writeAndFlush(request).sync();
    }

}

```

### NettyHttpClientHandler读取响应结果

```java
public class NettyHttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private static final Logger logger = LoggerFactory.getLogger(NettyHttpClientHandler.class);


    private XxlRpcInvokerFactory xxlRpcInvokerFactory;
    private Serializer serializer;
    private NettyHttpConnectClient nettyHttpConnectClient;
    public NettyHttpClientHandler(final XxlRpcInvokerFactory xxlRpcInvokerFactory, Serializer serializer, final NettyHttpConnectClient nettyHttpConnectClient) {
        this.xxlRpcInvokerFactory = xxlRpcInvokerFactory;
        this.serializer = serializer;
        this.nettyHttpConnectClient = nettyHttpConnectClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {

        // valid status
        if (!HttpResponseStatus.OK.equals(msg.status())) {
            throw new XxlRpcException("xxl-rpc response status invalid.");
        }

        // response parse
        byte[] responseBytes = ByteBufUtil.getBytes(msg.content());

        // valid length
        if (responseBytes.length == 0) {
            throw new XxlRpcException("xxl-rpc response data empty.");
        }

        // response deserialize
        XxlRpcResponse xxlRpcResponse = (XxlRpcResponse) serializer.deserialize(responseBytes, XxlRpcResponse.class);

        // notify response
        xxlRpcInvokerFactory.notifyInvokerFuture(xxlRpcResponse.getRequestId(), xxlRpcResponse);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //super.exceptionCaught(ctx, cause);
        logger.error(">>>>>>>>>>> xxl-rpc netty_http client caught exception", cause);
        ctx.close();
    }

    /*@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // retry
        super.channelInactive(ctx);
    }*/

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            /*ctx.channel().close();      // close idle channel
            logger.debug(">>>>>>>>>>> xxl-rpc netty_http client close an idle channel.");*/

            nettyHttpConnectClient.send(Beat.BEAT_PING);    // beat N, close if fail(may throw error)
            logger.debug(">>>>>>>>>>> xxl-rpc netty_http client send beat-ping.");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

}

```






# 参考

- [xxl-rpc](https://www.xuxueli.com/xxl-rpc/)