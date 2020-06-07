



<!-- TOC -->

- [01、server外界直接使用的是RpcServer的API](#01server外界直接使用的是rpcserver的api)
- [02、client外界直接使用的是RpcClient的API](#02client外界直接使用的是rpcclient的api)
    - [0、ConnectionManager、ConnectionFactory和Connection](#0connectionmanagerconnectionfactory和connection)
    - [1、RpcClient发送请求的流程](#1rpcclient发送请求的流程)
    - [2、几种调用方式的实现](#2几种调用方式的实现)
- [03、协议](#03协议)
        - [1、通信协议的设计](#1通信协议的设计)
        - [2、灵活的反序列化时机控制](#2灵活的反序列化时机控制)
        - [3、Server Fail-Fast 机制](#3server-fail-fast-机制)
        - [4、用户请求处理器(UserProcessor)](#4用户请求处理器userprocessor)
- [04、编解码](#04编解码)
- [05、心跳](#05心跳)
- [06、异常设计](#06异常设计)
- [07、RemotingProcessor](#07remotingprocessor)
    - [1、AbstractRemotingProcessor](#1abstractremotingprocessor)
    - [2、RpcRequestProcessor处理请求](#2rpcrequestprocessor处理请求)
    - [3、RpcResponseProcessor响应处理](#3rpcresponseprocessor响应处理)
    - [4、RpcHeartBeatProcessor心跳处理](#4rpcheartbeatprocessor心跳处理)
- [08、CommandFactory](#08commandfactory)
- [09、RemotingCommand](#09remotingcommand)
    - [1、RpcCommand](#1rpccommand)
    - [2、RpcCommandType命令的类型](#2rpccommandtype命令的类型)
    - [3、CommandCode命令码](#3commandcode命令码)
        - [1、CommonCommandCode](#1commoncommandcode)
        - [2、RpcCommandCode](#2rpccommandcode)
    - [4、RequestCommand](#4requestcommand)
        - [1、RpcRequestCommand](#1rpcrequestcommand)
        - [2、HeartbeatCommand](#2heartbeatcommand)
    - [5、ResponseCommand](#5responsecommand)
        - [1、HeartbeatAckCommand](#1heartbeatackcommand)
        - [2、RpcResponseCommand](#2rpcresponsecommand)
- [10、UserProcessor用户自己定义的业务逻辑](#10userprocessor用户自己定义的业务逻辑)
    - [11、RemotingContext这个是对netty的ChannelHandlerContext的包装类](#11remotingcontext这个是对netty的channelhandlercontext的包装类)
    - [12、BizContext](#12bizcontext)
    - [13、AsyncContext](#13asynccontext)
- [参考](#参考)

<!-- /TOC -->










> 问题：

1、client端发送rpc请求后，如何阻塞自己等待server端的结果？

2、server端如何把响应的结果写回client端？

3、RpcClientRemoting封装了原始的channel用于发送请求，而RpcServerRemoting

4、心跳机制？

> 总结：

- 1、核心处理逻辑是：通过netty框架把接收的数据交给RpcProtocol处理，协议中封装了编解码器以及业务处理器RpcCommandHandler，在它的内部初始化了请求处理器RpcRequestProcessor、响应处理器RpcResponseProcessor、心跳处理器RpcHeartBeatProcessor等；针对请求和响应消息，在经过RpcRequestProcessor\RpcResponseProcessor的处理后，交给注册的UserProcessor进行处理。

- 2、server外界直接使用的是RpcServer的API，client外界直接使用的是RpcClient的API；





# 01、server外界直接使用的是RpcServer的API

RpcServer继承图

![](../../pic/2020-01-28-17-12-06.png)


RpcRemoting类图

![](../../pic/2020-01-28-17-42-01.png)



UserProcessor接口：抽象处理让用户可以编写服务端接收客户端请求的处理接口

![](../../pic/2020-01-30-20-57-47.png)


sofa-rpc的server端netty-handler处理器为BoltServerProcessor继承自AsyncUserProcessor。这样使用者可以自定义类实现UserProcessor接口或者继承接口的实现类来完成自己的业务逻辑处理。

接口中的方法：

![](../../pic/2020-01-31-12-02-19.png)

方法String interest()指定处理那些请求类的名称。在启动server之前，把server端如何接受用户请求的处理逻辑注册到server上registerUserProcessor，在server内部使用一个map来存储<interest(),UserProcessor>，这样当请求端的请求到达后找到匹配的处理器进行处理。这样做的好处是用户可以高度自定义原始的请求传输对象，只要编写一个对应的UserProcessor注册到server上即可。client端在发送信息时会把发送对象的类名称设置到远程通信对象RpcRequestCommand中：command.setRequestClass(request.getClass().getName());


接收用户处理的核心类RpcHandler

![](../../pic/2020-01-31-10-20-11.png)


> server端接收request的处理流程

RpcHandler--->RpcProtocol[包含属性RpcCommandEncoder/RpcCommandDecoder/RpcCommandHandler]--->RpcCommandHandler--->RemotingProcessor[实现类RpcRequestProcessor/RpcResponseProcessor/RpcHeartBeatProcessor]--->UserProcessor

RpcHandler是server端和client端接收信息的处理器，共用。而且在构造函数中都是传入了userProcessor处理逻辑。

- server端使用：new RpcHandler(true, this.userProcessors);

- client端使用：new RpcHandler(userProcessors)；


rpcserver关于nettyserver启动代码：

```java
@Override
    protected void doInit() {//这里完成netty server的启动
        if (this.addressParser == null) {
            this.addressParser = new RpcAddressParser();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
            // in server side, do not care the connection service state, so use null instead of global switch
            ConnectionSelectStrategy connectionSelectStrategy = new RandomSelectStrategy(null);
            this.connectionManager = new DefaultServerConnectionManager(connectionSelectStrategy);
            this.connectionManager.startup();

            this.connectionEventHandler = new RpcConnectionEventHandler(switches());
            this.connectionEventHandler.setConnectionManager(this.connectionManager);
            this.connectionEventHandler.setConnectionEventListener(this.connectionEventListener);
        } else {
            this.connectionEventHandler = new ConnectionEventHandler(switches());
            this.connectionEventHandler.setConnectionEventListener(this.connectionEventListener);
        }
        initRpcRemoting();//
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, workerGroup)
            .channel(NettyEventLoopUtil.getServerSocketChannelClass())
            .option(ChannelOption.SO_BACKLOG, ConfigManager.tcp_so_backlog())
            .option(ChannelOption.SO_REUSEADDR, ConfigManager.tcp_so_reuseaddr())
            .childOption(ChannelOption.TCP_NODELAY, ConfigManager.tcp_nodelay())
            .childOption(ChannelOption.SO_KEEPALIVE, ConfigManager.tcp_so_keepalive());

        // set write buffer water mark
        initWriteBufferWaterMark();

        // init byte buf allocator
        if (ConfigManager.netty_buffer_pooled()) {
            this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        } else {
            this.bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        }

        // enable trigger mode for epoll if need
        NettyEventLoopUtil.enableTriggeredMode(bootstrap);

        final boolean idleSwitch = ConfigManager.tcp_idle_switch();
        final int idleTime = ConfigManager.tcp_server_idle();
        final ChannelHandler serverIdleHandler = new ServerIdleHandler();
        final RpcHandler rpcHandler = new RpcHandler(true, this.userProcessors);//封装了业务逻辑处理
        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("decoder", codec.newDecoder());//解码器
                pipeline.addLast("encoder", codec.newEncoder());//编码器
                if (idleSwitch) {
                    pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, idleTime,
                        TimeUnit.MILLISECONDS));
                    pipeline.addLast("serverIdleHandler", serverIdleHandler);
                }
                pipeline.addLast("connectionEventHandler", connectionEventHandler);//链接事件处理器
                pipeline.addLast("handler", rpcHandler);//业务处理器
                createConnection(channel);//这里维护了创建的全部socketchannel然后包装成connection
            }

            /**
             * create connection operation<br>
             * <ul>
             * <li>If flag manageConnection be true, use {@link DefaultConnectionManager} to add a new connection, meanwhile bind it with the channel.</li>
             * <li>If flag manageConnection be false, just create a new connection and bind it with the channel.</li>
             * </ul>
             */
            private void createConnection(SocketChannel channel) {
                Url url = addressParser.parse(RemotingUtil.parseRemoteAddress(channel));
                if (switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
                    connectionManager.add(new Connection(channel, url), url.getUniqueKey());
                } else {
                    new Connection(channel, url);
                }
                channel.pipeline().fireUserEventTriggered(ConnectionEventType.CONNECT);
            }
        });
    }

```



# 02、client外界直接使用的是RpcClient的API

## 0、ConnectionManager、ConnectionFactory和Connection

ConnectionFactory相关接口

![](../../pic/2020-01-28-19-35-14.png)

接口中的方法有：

![](../../pic/2020-01-31-12-50-04.png)

备注：AbstractConnectionFactory在init方法中对netty的Bootstrap进行初始化，但是并没有进行connect。

```java
public void init(final ConnectionEventHandler connectionEventHandler) {//这里初始化netty的client端
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NettyEventLoopUtil.getClientSocketChannelClass())
            .option(ChannelOption.TCP_NODELAY, ConfigManager.tcp_nodelay())
            .option(ChannelOption.SO_REUSEADDR, ConfigManager.tcp_so_reuseaddr())
            .option(ChannelOption.SO_KEEPALIVE, ConfigManager.tcp_so_keepalive());

        // init netty write buffer water mark
        initWriteBufferWaterMark();

        // init byte buf allocator
        if (ConfigManager.netty_buffer_pooled()) {
            this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        } else {
            this.bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        }

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("decoder", codec.newDecoder());
                pipeline.addLast("encoder", codec.newEncoder());

                boolean idleSwitch = ConfigManager.tcp_idle_switch();
                if (idleSwitch) {
                    pipeline.addLast("idleStateHandler",
                        new IdleStateHandler(ConfigManager.tcp_idle(), ConfigManager.tcp_idle(), 0,
                            TimeUnit.MILLISECONDS));
                    pipeline.addLast("heartbeatHandler", heartbeatHandler);//设置心跳handler
                }

                pipeline.addLast("connectionEventHandler", connectionEventHandler);
                pipeline.addLast("handler", handler);
            }
        });
    }

```



ConnectionManager接口

![](../../pic/2020-01-28-19-37-48.png)

![](../../pic/2020-01-31-20-20-24.png)

ConnectionManager中包含一个链接工厂RpcConnectionFactory。链接管理器中创建的链接都是有链接工厂创建。


Connection是一个channel的封装，代表一个和server端通信的通道。



> Connection事件处理

ConnectionEventHandler包含ConnectionEventListener，在ConnectionEventListener中注册不同链接类型处理器ConnectionEventProcessor。




## 1、RpcClient发送请求的流程

RpcClient.invokeSync--->rpcRemoting.invokeSync--->RpcClientRemoting#invokeSync--->connectionManager.getAndCreateIfAbsent(url)[把创建的链接放到ConnectionPool中，然后返回一个]--->connection--->BaseRemoting#invokeSync[发送请求到服务提供者]


RpcClient在初始化的时候创建connectionManager和rpcRemoting[内部包含connectionManager]，当client.invokeSync执行的时候，执行的是rpcRemoting.invokeSync。也就是说RpcClient执行的方法都委托给其内部的rpcRemoting。



## 2、几种调用方式的实现


![](../../pic/2020-02-01-21-12-25.png)

如图所示，我们实现了多种通信接口 oneway ，sync ，future ，callback 。图中都是ping/pong模式的通信，蓝色部分表示线程正在执行任务

- 可以看到 oneway 不关心响应，请求线程不会被阻塞，但使用时需要注意控制调用节奏，防止压垮接收方；

- sync 调用会阻塞请求线程，待响应返回后才能进行下一个请求。这是最常用的一种通信模型；

- future 调用，在调用过程不会阻塞线程，但获取结果的过程会阻塞线程；

- callback 是真正的异步调用，永远不会阻塞线程，结果处理是在异步线程里执行。

详情：

- 1、同步调用invokeSync

把请求request封装成一个InvokeFuture，内部包含一个new CountDownLatch(1)，在通过channel发送完消息后，调用this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS)进行阻塞等待。

当client接收到服务端的返回值后，在com.alipay.remoting.rpc.protocol.RpcResponseProcessor#doProcess进行处理，具体流程：channel--->connection--->[之前封装的InvokeFuture]--->future.putResponse(cmd)解除阻塞。

- 2、回调invokeWithCallback

把请求request和回调封装成一个InvokeFuture，然后设置一个超时timer，然后发生请求到server。

同上在client接到返回值时，com.alipay.remoting.rpc.protocol.RpcResponseProcessor#doProcess进行处理，调用future.executeInvokeCallback()先触发回调监听器、然后由监听器回调。

- 3、future方式

基本过程同2，去除回调和监听器部分，返回值为RpcResponseFuture，通过调用get阻塞，当client端有返回值的时候解除阻塞。

- 4、OneWay

没有返回值，发送请求直接结束。




# 03、协议

![](../../pic/2020-01-31-10-47-34.png)

备注：其实v2是因为由于涉及的缺陷，在RpcProtocol中忽略了version字段，因此设计了RpcProtocolV2添加protocol version。


```java
public class RpcProtocol implements Protocol {
    public static final byte PROTOCOL_CODE       = (byte) 1;//协议编码，一个字节
    private static final int REQUEST_HEADER_LEN  = 22;
    private static final int RESPONSE_HEADER_LEN = 20;
    private CommandEncoder   encoder;
    private CommandDecoder   decoder;
    private HeartbeatTrigger heartbeatTrigger;
    private CommandHandler   commandHandler;
    private CommandFactory   commandFactory;

    public RpcProtocol() {
        this.encoder = new RpcCommandEncoder();
        this.decoder = new RpcCommandDecoder();
        this.commandFactory = new RpcCommandFactory();//命令生成工厂
        this.heartbeatTrigger = new RpcHeartbeatTrigger(this.commandFactory);
        this.commandHandler = new RpcCommandHandler(this.commandFactory);//业务逻辑处理
    }
...
}

```

RpcCommand  所有的请求对象和响应对象都会封装成该接口的实现

![](../../pic/2020-01-30-21-48-50.png)

CommandCode

![](../../pic/2020-01-31-11-21-08.png)




CommandHandler处理器，默认唯一实现RpcCommandHandler。该对象的实例会绑定到RpcProtocol中，处理具体的业务逻辑。

![](../../pic/2020-01-31-11-26-22.png)

```java
public RpcCommandHandler(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
        this.processorManager = new ProcessorManager();//通过command code维护命令和处理器直接的映射
        //process request
        this.processorManager.registerProcessor(RpcCommandCode.RPC_REQUEST,
            new RpcRequestProcessor(this.commandFactory));
        //process response
        this.processorManager.registerProcessor(RpcCommandCode.RPC_RESPONSE,
            new RpcResponseProcessor());

        this.processorManager.registerProcessor(CommonCommandCode.HEARTBEAT,
            new RpcHeartBeatProcessor());

        this.processorManager
            .registerDefaultProcessor(new AbstractRemotingProcessor<RemotingCommand>() {//设置默认日志处理器
                @Override
                public void doProcess(RemotingContext ctx, RemotingCommand msg) throws Exception {
                    logger.error("No processor available for command code {}, msgId {}",
                        msg.getCmdCode(), msg.getId());
                }
            });
    }

```



RemotingProcessor处理器

![](../../pic/2020-01-31-11-24-14.png)

客户端的请求交给RpcRequestProcessor处理，响应交给RpcResponseProcessor进行处理。

### 1、通信协议的设计

![](../../pic/2020-02-01-21-15-17.png)


- ProtocolCode ：如果一个端口，需要处理多种协议的请求，那么这个字段是必须的。因为需要根据 ProtocolCode 来进入不同的核心编解码器。该字段可以在想换协议的时候，方便的进行更换。

- ProtocolVersion ：确定了某一种通信协议后，我们还需要考虑协议的微小调整需求，因此需要增加一个 version 的字段，方便在协议上追加新的字段

- RequestType ：请求类型， 比如request response oneway

- CommandCode ：请求命令类型，比如 request 可以分为：负载请求，或者心跳请求。oneway 之所以需要单独设置，是因为在处理响应时，需要做特殊判断，来控制响应是否回传。

- CommandVersion ：请求命令版本号。该字段用来区分请求命令的不同版本。如果修改 Command 版本，不修改协议，那么就是纯粹代码重构的需求；除此情况，Command 的版本升级，往往会同步做协议的升级。

- RequestId ：请求 ID，该字段主要用于异步请求时，保留请求存根使用，便于响应回来时触发回调。另外，在日志打印与问题调试时，也需要该字段。

- Codec ：序列化器。该字段用于保存在做业务的序列化时，使用的是哪种序列化器。通信框架不限定序列化方式，可以方便的扩展。

- Switch ：协议开关，用于一些协议级别的开关控制，比如 CRC 校验，安全校验等。

- Timeout ：超时字段，客户端发起请求时，所设置的超时时间。该字段非常有用。

- ResponseStatus ：响应码。从字段精简的角度，我们不可能每次响应都带上完整的异常栈给客户端排查问题，因此，我们会定义一些响应码，通过编号进行网络传输，方便客户端定位问题。

- ClassLen ：业务请求类名长度

- HeaderLen ：业务请求头长度

- ContentLen ：业务请求体长度

ClassName ：业务请求类名。需要注意类名传输的时候，务必指定字符集，不要依赖系统的默认字符集。曾经线上的机器，因为运维误操作，默认的字符集被修改，导致字符的传输出现编解码问题。而我们的通信框架指定了默认字符集，因此躲过一劫。

- HeaderContent ：业务请求头

- BodyContent ：业务请求体

- CRC32 ：CRC校验码，这也是通信场景里必不可少的一部分，而我们金融业务属性的特征，这个显得尤为重要。


### 2、灵活的反序列化时机控制

从上面的协议介绍，可以看到协议的基本字段所占用空间是比较小的，目前只有24个字节。协议上的主要负载就是 ClassName  ，HeaderContent ， BodyContent 这三部分。这三部分的序列化和反序列化是整个请求响应里最耗时的部分。在请求发送阶段，在调用 Netty 的写接口之前，会在业务线程先做好序列化，这里没有什么疑问。而在请求接收阶段，反序列化的时机就需要考虑一下了。结合上面提到的最佳实践的网络 IO 模型，请求接收阶段，我们有 IO 线程，业务线程两种线程池。为了最大程度的配合业务特性，保证整体吞吐我们设计了精细的开关来控制反序列化时机：


![](../../pic/2020-02-01-21-21-36.png)

![](../../pic/2020-02-01-21-21-59.png)


### 3、Server Fail-Fast 机制

![](../../pic/2020-02-01-21-24-14.png)


在协议里，留意到我们有timeout这个字段，这个是把客户端发起调用时，所设置的超时时间通过协议传到了 Server 端。有了这个，我们就可以实现 Fail-Fast 快速失败的机制。比如当客户端设置超时时间 1s，当请求到达 Server 开始计时 arriveTimeStamp ，到任务被线程调度到开始处理时，记录 startToProcessTimestamp ，二者的差值即请求反序列化与线程池排队的时延，如果这个时间间隔已经超过了 1s，那么请求就没有必要被处理了。这个机制，在服务端出现处理抖动时，对于快速恢复会很有用。

最佳实践：不要依赖跨系统的时钟，因为时钟可能会不一致，跨系统就会出现误差，因此是从请求到达 Server 的那一刻，在 Server 的进程里开始计时。


### 4、用户请求处理器(UserProcessor)

![](../../pic/2020-02-01-21-27-46.png)

除此，我们还设计了一个 RemotingContext 用于保存请求处理阶段的一些通信层的关键辅助类或者信息，方便通信框架开发者使用；同时还提供了一个 BizContext ，有选择把通信层的信息暴露给框架使用者，方便框架使用者使用。有了用户请求处理器，以及上下文的传递机制，我们就可以方便的把通信层处理逻辑与业务处理逻辑联动起来，比如一些开关的控制，字段的传递等定制功能：

- 请求超时处理开关：用于开关 Server Fail-Fast 机制。

- IO 线程业务处理开关：用户可以选择在 IO 线程处理业务请求；或者在业务线程来处理。

- 线程池选择器 ExecutorSelector ：用户可以提供多个业务线程池，使用 ExecutorSelector 来实现选择逻辑

- 泛化调用的支持：序列化请求与反序列化响应阶段，针对泛化调用，使用特殊的序列化器。而是否开启该功能，需要依赖上下文来传递一些标识。



# 04、编解码

备注：编解码接口codec，和netty进行交互的，是在协议层面，把序列化后的数据按照指定的协议格式写入channel的ByteBuf，或者从ByteBuf读出各个字节。而序列化和反序列化在这之前。




解码器的作用是把netty中的bytebuf转化为RpcCommand，编码的作用是根据RpcCommand对象按照具体传输协议的约定格式写到ByteBuf中。


定义抽象的编解码接口codec：

![](../../pic/2020-01-31-10-09-56.png)

codec接口的实现

![](../../pic/2020-01-31-10-13-44.png)

```java
public class RpcCodec implements Codec {

    @Override
    public ChannelHandler newEncoder() {
        return new ProtocolCodeBasedEncoder(ProtocolCode.fromBytes(RpcProtocolV2.PROTOCOL_CODE));
    }

    @Override
    public ChannelHandler newDecoder() {
        return new RpcProtocolDecoder(RpcProtocolManager.DEFAULT_PROTOCOL_CODE_LENGTH);
    }
}

```

解码类图

![](../../pic/2020-01-31-09-58-10.png)

编码类图

![](../../pic/2020-01-31-10-03-28.png)



在设置pipeline的时候直接通过该接口的方法设置了编解码。从上面的实现可以看出，这里把数据从netty传递到协议的编解码。具体的编解码方法是和协议进行绑定的。比如：

```java
public RpcProtocol() {
        this.encoder = new RpcCommandEncoder();
        this.decoder = new RpcCommandDecoder();
       ...
    }
```

具体的编解码实现

![](../../pic/2020-01-31-10-56-36.png)

![](../../pic/2020-01-31-10-57-05.png)




# 05、心跳


![](../../pic/2020-02-01-15-52-54.png)


1、client端[HeartbeatHandler触发发送信息]

触发心跳检测

```java
@Sharable
public class HeartbeatHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {//检测的是IdleStateEvent
            ProtocolCode protocolCode = ctx.channel().attr(Connection.PROTOCOL).get();
            Protocol protocol = ProtocolManager.getProtocol(protocolCode);
            protocol.getHeartbeatTrigger().heartbeatTriggered(ctx);//触发心跳
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
```


RpcHeartbeatTrigger封装具体的触发逻辑。

通过IdleStateHandler设置，默认没有读、写15秒触发。



2、server端[ServerIdleHandler接收client端发送的信息]

server端检测到Connection idle，会关闭它。

```java

@Sharable
public class ServerIdleHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            try {
                logger.warn("Connection idle, close it from server side: {}",
                    RemotingUtil.parseRemoteAddress(ctx.channel()));
                ctx.close();
            } catch (Exception e) {
                logger.warn("Exception caught when closing connection in ServerIdleHandler.", e);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}

```


通过IdleStateHandler设置，默认没有读和写90秒触发。



# 06、异常设计

Exception

![](../../pic/2020-02-03-09-01-09.png)

RemotingException

![](../../pic/2020-02-03-08-58-55.png)





# 07、RemotingProcessor

![](../../pic/2020-04-15-21-41-36.png)

```java
public interface RemotingProcessor<T extends RemotingCommand> {

    void process(RemotingContext ctx, T msg, ExecutorService defaultExecutor) throws Exception;

    ExecutorService getExecutor();

    void setExecutor(ExecutorService executor);

}


```



## 1、AbstractRemotingProcessor

把处理封装成任务提交到线程池去执行

## 2、RpcRequestProcessor处理请求



## 3、RpcResponseProcessor响应处理


```java
public class RpcResponseProcessor extends AbstractRemotingProcessor<RemotingCommand> {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

   
    public RpcResponseProcessor() {

    }

 
    public RpcResponseProcessor(ExecutorService executor) {
        super(executor);
    }

    
    @Override
    public void doProcess(RemotingContext ctx, RemotingCommand cmd) {
        //这样设计的好处？在client端的同步请求转化为异步，在这里接收到响应后，根据channel绑定的connection可以找到对应的请求进行处理
        Connection conn = ctx.getChannelContext().channel().attr(Connection.CONNECTION).get();//从channel中获取绑定的connection,何时绑定的？创建链接的时候设置的
        InvokeFuture future = conn.removeInvokeFuture(cmd.getId());//根据请求id获得对应的InvokeFuture
        ClassLoader oldClassLoader = null;
        try {
            if (future != null) {
                if (future.getAppClassLoader() != null) {
                    oldClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(future.getAppClassLoader());
                }
                future.putResponse(cmd);//这里解除阻塞
                future.cancelTimeout();
                try {
                    future.executeInvokeCallback();//当客户端通过回调的方式接受处理结果时在这里触发
                } catch (Exception e) {
                    logger.error("Exception caught when executing invoke callback, id={}",
                        cmd.getId(), e);
                }
            } else {
                logger
                    .warn("Cannot find InvokeFuture, maybe already timeout, id={}, from={} ",
                        cmd.getId(),
                        RemotingUtil.parseRemoteAddress(ctx.getChannelContext().channel()));
            }
        } finally {
            if (null != oldClassLoader) {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }

    }

}


```


## 4、RpcHeartBeatProcessor心跳处理




# 08、CommandFactory

![](../../pic/2020-04-15-22-23-51.png)


```java
public interface CommandFactory {
    //根据请求对象创建一个请求命令对象
    <T extends RemotingCommand> T createRequestCommand(final Object requestObject);

    //创建一个一般的响应对象
    <T extends RemotingCommand> T createResponse(final Object responseObject,
                                                 RemotingCommand requestCmd);

    <T extends RemotingCommand> T createExceptionResponse(int id, String errMsg);

    <T extends RemotingCommand> T createExceptionResponse(int id, final Throwable t, String errMsg);

    <T extends RemotingCommand> T createExceptionResponse(int id, ResponseStatus status);

    <T extends RemotingCommand> T createExceptionResponse(int id, ResponseStatus status,
                                                          final Throwable t);

    <T extends RemotingCommand> T createTimeoutResponse(final InetSocketAddress address);

    <T extends RemotingCommand> T createSendFailedResponse(final InetSocketAddress address,
                                                           Throwable throwable);

    <T extends RemotingCommand> T createConnectionClosedResponse(final InetSocketAddress address,
                                                                 String message);
}

```








# 09、RemotingCommand

![](../../pic/2020-04-15-21-56-29.png)

从接口的定义来看主要是序列化和反序列化相关的功能


```java
public interface RemotingCommand extends Serializable {
   //命令的协议码
    ProtocolCode getProtocolCode();

    //命令的命令码
    CommandCode getCmdCode();

    //命令的id
    int getId();

    InvokeContext getInvokeContext();

   //命令的序列化类型
    byte getSerializer();

    //命令的协议开关状态
    ProtocolSwitch getProtocolSwitch();

   //序列化命令
    void serialize() throws SerializationException;

    //反序列化命令
    void deserialize() throws DeserializationException;

   //序列化命令的内容
    void serializeContent(InvokeContext invokeContext) throws SerializationException;

    //反序列化命令的内容
    void deserializeContent(InvokeContext invokeContext) throws DeserializationException;
}


```

## 1、RpcCommand

A remoting command stands for a kind of transfer object in the network communication layer.

一个remoting command代表一种类型的传输对象在网络通信层。


```java
public abstract class RpcCommand implements RemotingCommand {

    /** The length of clazz */
    private short             clazzLength      = 0;
    private short             headerLength     = 0;
    private int               contentLength    = 0;
    /** The class of content */
    private byte[]            clazz;
    /** Header is used for transparent transmission. */
    private byte[]            header;
    /** The bytes format of the content of the command. */
    private byte[]            content;
    /** invoke context of each rpc command. */
    private InvokeContext     invokeContext;


    
}

```

主要包含三部分内容：类、header、content


## 2、RpcCommandType命令的类型

```java

public class RpcCommandType {
    /** rpc response */
    public static final byte RESPONSE       = (byte) 0x00;
    /** rpc request */
    public static final byte REQUEST        = (byte) 0x01;
    /** rpc oneway request */
    public static final byte REQUEST_ONEWAY = (byte) 0x02;
}
```


## 3、CommandCode命令码

![](../../pic/2020-04-15-22-12-50.png)

每一种命令都有自己的命令码

```java
public interface CommandCode {
    // value 0 is occupied by heartbeat, don't use value 0 for other commands
    short HEARTBEAT_VALUE = 0;
    short value();

}

```

### 1、CommonCommandCode


```java
public enum CommonCommandCode implements CommandCode {

    HEARTBEAT(CommandCode.HEARTBEAT_VALUE);

    private short value;

    CommonCommandCode(short value) {
        this.value = value;
    }

    @Override
    public short value() {
        return this.value;
    }

    public static CommonCommandCode valueOf(short value) {
        switch (value) {
            case CommandCode.HEARTBEAT_VALUE:
                return HEARTBEAT;
        }
        throw new IllegalArgumentException("Unknown Rpc command code value ," + value);
    }

}
```


### 2、RpcCommandCode

```java
public enum RpcCommandCode implements CommandCode {//rpc请求命令编码，1是请求；2是响应

    RPC_REQUEST((short) 1), RPC_RESPONSE((short) 2);

    private short value;

    RpcCommandCode(short value) {
        this.value = value;
    }

    @Override
    public short value() {
        return this.value;
    }

    public static RpcCommandCode valueOf(short value) {
        switch (value) {
            case 1:
                return RPC_REQUEST;
            case 2:
                return RPC_RESPONSE;
        }
        throw new IllegalArgumentException("Unknown Rpc command code value: " + value);
    }

}
```



## 4、RequestCommand

使用RpcCommandType.REQUEST标识为请求命令


### 1、RpcRequestCommand



### 2、HeartbeatCommand

```java
public class HeartbeatCommand extends RequestCommand {

    public HeartbeatCommand() {
        super(CommonCommandCode.HEARTBEAT);
        this.setId(IDGenerator.nextId());
    }

}
```
## 5、ResponseCommand

RpcCommandType.RESPONSE



### 1、HeartbeatAckCommand

```java
public class HeartbeatAckCommand extends ResponseCommand {
   
    public HeartbeatAckCommand() {
        super(CommonCommandCode.HEARTBEAT);
        this.setResponseStatus(ResponseStatus.SUCCESS);
    }
}
```

### 2、RpcResponseCommand



# 10、UserProcessor用户自己定义的业务逻辑

![](../../pic/2020-04-15-22-33-35.png)

从类的继承关联来看主要有两类：

- 1、同步异步
- 2、多兴趣单兴趣

```java
public interface UserProcessor<T> {//用户自己定义的业务逻辑（只处理一类请求）

    //预处理请求，为了避免RemotingContext直接暴露给业务处理逻辑。这里其实就是简单包裹了一下
    BizContext preHandleRequest(RemotingContext remotingCtx, T request);

   //异步处理请求
    void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request);

   //同步处理请求
    Object handleRequest(BizContext bizCtx, T request) throws Exception;

    //user请求request类的名称，用来匹配该处理器，表示我只对这个类感兴趣
    String interest();
    //用户线程池，用来处理业务逻辑
    Executor getExecutor();
    //是否在io线程中进行反序列化以及业务逻辑的处理。如果是会很影响性能
    boolean processInIOThread();

   //当业务侧处理的时候发现已经超时则不再进行处理，快速失败。
    boolean timeoutDiscard();

   //Use this method to set executor selector.
    void setExecutorSelector(ExecutorSelector executorSelector);

   //Use this method to get the executor selector.
    ExecutorSelector getExecutorSelector();

   
    interface ExecutorSelector {//功能
        Executor select(String requestClass, Object requestHeader);
    }
}

```

扩展为一个processor可以处理多个类型请求对象

```java
public interface MultiInterestUserProcessor<T> extends UserProcessor<T> {

    List<String> multiInterest();

}
```



## 11、RemotingContext这个是对netty的ChannelHandlerContext的包装类

![](../../pic/2020-04-15-22-44-50.png)

可以获得链接对象Connection、UserProcessor等


## 12、BizContext




## 13、AsyncContext


# 参考

- [蚂蚁通信框架实践](https://mp.weixin.qq.com/s/JRsbK1Un2av9GKmJ8DK7IQ?)
