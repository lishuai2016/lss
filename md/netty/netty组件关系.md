


# 0、组件之间的关系


核心组件

- Bootstrap or ServerBootstrap

- EventLoop

- EventLoopGroup

- ChannelPipeline

- Channel

- Future or ChannelFuture

- ChannelInitializer

- ChannelHandler

Bootstrap，一个Netty应用通常由一个Bootstrap开始，它主要作用是配置整个Netty程序，串联起各个组件。

Handler，为了支持各种协议和处理数据的方式，便诞生了Handler组件。Handler主要用来处理各种事件，这里的事件很广泛，比如可以是连接、数据接收、异常、数据转换等。

ChannelInboundHandler，一个最常用的Handler。这个Handler的作用就是处理接收到数据时的事件，也就是说，我们的业务逻辑一般就是写在这个Handler里面的，ChannelInboundHandler就是用来处理我们的核心业务逻辑。

ChannelInitializer，当一个链接建立时，我们需要知道怎么来接收或者发送数据，当然，我们有各种各样的Handler实现来处理它，那么ChannelInitializer便是用来配置这些Handler，它会提供一个ChannelPipeline，并把Handler加入到ChannelPipeline。

ChannelPipeline，一个Netty应用基于ChannelPipeline机制，这种机制需要依赖于EventLoop和EventLoopGroup，因为它们三个都和事件或者事件处理相关。

EventLoops的目的是为Channel处理IO操作，一个EventLoop可以为多个Channel服务。

EventLoopGroup会包含多个EventLoop。

Channel代表了一个Socket链接，或者其它和IO操作相关的组件，它和EventLoop一起用来参与IO处理。

Future，在Netty中所有的IO操作都是异步的，因此，你不能立刻得知消息是否被正确处理，但是我们可以过一会等它执行完成或者直接注册一个监听，具体的实现就是通过Future和ChannelFutures,他们可以注册一个监听，当操作执行成功或失败时监听会自动触发。总之，所有的操作都会返回一个ChannelFuture。
 



> Channel\ChannelPipeline\ChannelHandlerContext\ChannelHandler之间关系

![](../../pic/2020-01-05-17-54-04.png)

- 1、每个Channel会绑定一个ChannelPipeline，ChannelPipeline中也会持有Channel的引用

- 2、ChannelPipeline持有ChannelHandlerContext链路，保留ChannelHandlerContext的头尾节点指针

- 3、每个ChannelHandlerContext会对应一个ChannelHandler，也就相当于ChannelPipeline持有ChannelHandler链路

- 4、ChannelHandlerContext同时也会持有ChannelPipeline引用，也就相当于持有Channel引用

- 5、ChannelHandler链路会根据Handler的类型，分为InBound和OutBound两条链路



> Channel生命周期

![](../../pic/2020-01-05-18-09-26.png)

> ChannelHandler

![](../../pic/2020-01-05-18-15-20.png)

ChannelHandler用于处理Channel对应的事件ChannelHandler接口里面只定义了三个生命周期方法，我们主要实现它的子接口ChannelInboundHandler和ChannelOutboundHandler，为了便利，框架提供了ChannelInboundHandlerAdapter，ChannelOutboundHandlerAdapter和ChannelDuplexHandler这三个适配类，在使用的时候只需要实现你关注的方法即可

>> ChannelInboundHandler

![](../../pic/2020-01-05-18-15-45.png)

![](../../pic/2020-01-05-18-16-02.png)

可以注意到每个方法都带了ChannelHandlerContext作为参数，具体作用是，在每个回调事件里面，处理完成之后，使用ChannelHandlerContext的fireChannelXXX方法来传递给下个ChannelHandler，netty的codec模块和业务处理代码分离就用到了这个链路处理

>> ChannelOutboundHandler

![](../../pic/2020-01-05-18-17-13.png)

![](../../pic/2020-01-05-18-17-28.png)

注意到一些回调方法有ChannelPromise这个参数，我们可以调用它的addListener注册监听，当回调方法所对应的操作完成后，会触发这个监听



>> ChannelHandlerContext

每个ChannelHandler通过add方法加入到ChannelPipeline中去的时候，会创建一个对应的ChannelHandlerContext，并且绑定，ChannelPipeline实际维护的是ChannelHandlerContext 的关系在DefaultChannelPipeline源码中可以看到会保存第一个ChannelHandlerContext以及最后一个ChannelHandlerContext的引用

![](../../pic/2020-01-05-18-21-50.png)


上面的整条链式的调用是通过Channel接口的方法直接触发的，如果使用ChannelContextHandler的接口方法间接触发，链路会从ChannelContextHandler对应的ChannelHandler开始，而不是从头或尾开始











> 自己初步理解

- 1、一个channel生成的时候关联产生一个ChannelPipline[DefaultChannelPipeline只有一个参数channel的构造函数]

- 2、在ChannelPipline中添加ChannelHandler时，会把ChannelHandler包装成DefaultChannelHandlerContext(this,ChannelHandler),ChannelHandlerContext的构造函数包含ChannelPipline和ChannelHandler，然后添加到双向链表中去。

- 3、整个处理的过程中ChannelPipline是唯一的，各个channelhandler通过这个DefaultChannelPipeline来传递channel的信息？？？


问题：

1、一个ChannelHandler处理完是如何传递到下一个ChannelHandler中去的？

2、一个ChannelHandler关联一个ChannelHandlerContext？

3、整个接收数据的入口在哪里？出口在哪里？整个处理ChannelHandler链条在哪里定义的处理完一个后接着处理下一个？


# 1、核心接口以及实现类

## 1.1、NioEventLoopGroup

![](../../pic/2020-01-05-18-07-06.png)

EventLoopGroup 说白了，就是一个死循环，不停地检测IO事件，处理IO事件，执行任务


## 1.2、NioEventLoop

![](../../pic/2020-01-09-10-24-16.png)

干的三件事

- 1.首先轮询注册到reactor线程对用的selector上的所有的channel的IO事件；

- 2.处理产生网络IO事件的channel

- 3.处理任务队列

![](../../pic/2020-01-09-20-45-39.png)


## DefaultChannelPipeline

![](../../pic/2020-01-09-13-06-30.png)


> ChannelInboundInvoker

![](../../pic/2020-01-09-13-07-42.png)


> ChannelOutboundInvoker

![](../../pic/2020-01-09-13-08-43.png)



![](../../pic/2020-01-09-21-00-21.png)

整个pipeline结构

![](../../pic/2020-01-09-21-00-41.png)

pipeline中两种不同类型的节点，一个是 ChannelInboundHandler，处理inBound事件，最典型的就是读取数据流，加工处理；还有一种类型的Handler是 ChannelOutboundHandler, 处理outBound事件，比如当调用writeAndFlush()类方法时，就会经过该种类型的handler


pipeline添加节点

![](../../pic/2020-01-09-21-02-28.png)

添加节点后

![](../../pic/2020-01-09-21-02-43.png)

删除节点

![](../../pic/2020-01-09-21-03-12.png)

删除后

![](../../pic/2020-01-09-21-03-32.png)




## channel

![](../../pic/2020-01-09-20-58-55.png)

![](../../pic/2020-01-09-20-59-45.png)


![](../../pic/2020-01-09-21-00-52.png)


channel的生命周期。

- 1、channelRegistered channel以及被注册到了eventloop
- 2、channelActive channel处于活动状态（已经连接到它的远程节点）。可以接收和发送数据了
- 3、channelInactive 没有链接到远程节点
- 4、channelUnregistered channel已经被创建出来了还没有注册到eventloop

备注：channel的生命周期会触发ChannelOutboundHandler接口定义的对应方法



## ChannelHandler

![](../../pic/2020-01-09-21-01-54.png)

> ChannelHandler接口定义

![](../../pic/2020-01-11-10-25-43.png)

ChannelHandler的生命周期

- 1、handlerAdded   当ChannelHandler添加到pipeline中时被调用

- 2、handlerRemoved 对应的被移除时调用

- 3、exceptionCaught 处理过程在pipeline中产生错误时被调用






> ChannelInboundHandler 处理入站IO事件

![](../../pic/2020-01-11-10-27-08.png)

这些方法将在数据被接收的时候或者与其对应的channel状态发生变化时被调用。



> ChannelOutboundHandler 处理出站IO操作

![](../../pic/2020-01-11-10-28-14.png)

> ChannelHandlerAdapter实现接口的基本骨架

![](../../pic/2020-01-11-10-30-53.png)

> ChannelInboundHandlerAdapter

![](../../pic/2020-01-11-10-32-46.png)

把对应方法的实现委托给ChannelHandlerContext处理，如ctx.fireChannelRegistered();这里的实现是把消息传递给pipeline链中的next进行处理。


> ChannelOutboundHandlerAdapter

![](../../pic/2020-01-11-10-35-08.png)

同理也是把方法的实现委托给ChannelHandlerContext处理，如 ctx.bind(localAddress, promise);



> SimpleChannelInboundHandler

![](../../pic/2020-01-11-10-56-19.png)

核心点就是释放读取到消息的内存。bytebuf中占用的内存。



## DefaultChannelHandlerContext

![](../../pic/2020-01-10-10-14-43.png)



## Unsafe

![](../../pic/2020-01-09-21-04-28.png)


## channelFuture接口

![](../../pic/2020-01-11-11-43-05.png)

![](../../pic/2020-01-11-11-45-11.png)

可以看出channelFuture接口重写了自定义Future部分方法

![](../../pic/2020-01-11-11-47-13.png)




1、io.netty.channel.Channel.Unsafe 接口

2.1、io.netty.channel.nio.AbstractNioChannel.NioUnsafe 接口[增加了可以访问底层jdk的SelectableChannel的功能，定义了从SelectableChannel读取数据的read方法]

2.2、io.netty.channel.AbstractChannel.AbstractUnsafe[实现大部分功能]

3、io.netty.channel.nio.AbstractNioChannel.AbstractNioUnsafe

主要是通过代理到其外部类AbstractNioChannel拿到了与jdk nio相关的一些信息，比如SelectableChannel，SelectionKey等等

4.1、io.netty.channel.nio.AbstractNioMessageChannel.NioMessageUnsafe

4.2、io.netty.channel.nio.AbstractNioByteChannel.NioByteUnsafe

4.2.1、io.netty.channel.socket.nio.NioSocketChannel.NioSocketChannelUnsafe


NioSocketChannelUnsafe和NioByteUnsafe放到一起讲，其实现了IO的基本操作，读，和写，这些操作都与jdk底层相关

NioMessageUnsafe和 NioByteUnsafe 是处在同一层次的抽象，netty将一个新连接的建立也当作一个io操作来处理，这里的Message的含义我们可以当作是一个SelectableChannel，读的意思就是accept一个SelectableChannel，写的意思是针对一些无连接的协议，比如UDP来操作的




总结出两种类型的Unsafe分类，一个是与连接的字节数据读写相关的NioByteUnsafe[NioSocketChannel用到]，一个是与新连接建立操作相关的NioMessageUnsafe。[NioServerSocketChannel用的]



##  ByteBufAllocator buf分配器

![](../../pic/2020-01-11-12-26-08.png)

定义三种buf。1、堆内存buf;2、直接内存buf;3、复合buf

关联类：io.netty.buffer.ByteBufUtil


















# 参考

- [Netty学习笔记之ChannelHandler](https://www.jianshu.com/p/96a50869b527)

- [Netty源码分析](https://www.jianshu.com/nb/7269354)

- [netty-study](https://gitee.com/chenfanglin/netty-study)


- [Netty源码分析之服务端启动全解析](https://mp.weixin.qq.com/s/3mteIL4qR8fXGvzzQxBZwQ)

- [Netty4详解三：Netty架构设计](https://www.cnblogs.com/DaTouDaddy/p/6801906.html)


- [Netty 启动过程源码分析 （本文超长慎读）(基于4.1.23)](https://www.jianshu.com/p/46861a05ce1e)

- [闪电侠netty](https://www.jianshu.com/u/4fdc8c2315e8)






- https://www.jianshu.com/p/46861a05ce1e
- https://segmentfault.com/a/1190000007282789
- https://www.jianshu.com/p/f16698aa8be2?utm_source=oschina-app
- http://www.52im.net/thread-1935-1-1.html



- https://blog.csdn.net/u013857458/article/details/82527722
- https://blog.csdn.net/u013857458/article/category/7514839
- https://www.jianshu.com/p/f16698aa8be2?utm_source=oschina-app