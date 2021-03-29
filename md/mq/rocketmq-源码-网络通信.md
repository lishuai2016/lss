

<!-- TOC -->

- [1、Remoting模块解析](#1remoting模块解析)
- [2、RocketMQ中RPC通信的Netty多线程模型](#2rocketmq中rpc通信的netty多线程模型)
    - [1 Netty的Reactor多线程模型设计概念与简述](#1-netty的reactor多线程模型设计概念与简述)
    - [2 RocketMQ中RPC通信的1+N+M1+M2的Reactor多线程设计与实现](#2-rocketmq中rpc通信的1nm1m2的reactor多线程设计与实现)
- [3、核心api分析](#3核心api分析)
    - [1、如何通过用户API调用netty-client的？](#1如何通过用户api调用netty-client的)

<!-- /TOC -->


# 1、Remoting模块解析

`实现系统各个模块之间的网络通信基于netty自定义的通信协议`

> 1、Remoting通信模块的类结构图

![](../../pic/2020-01-12-17-35-00.png)


> 2、消息的协议设计与编码解码

在RocketMQ中，RemotingCommand这个类在消息传输过程中对所有数据内容的封装，不但包含了所有的数据结构，还包含了编码解码操作。

RemotingCommand类的部分成员变量如下：

![](../../pic/2020-01-12-17-38-10.png)


下RocketMQ通信协议的格式：

![](../../pic/2020-01-12-17-39-35.png)

> 3、消息的通信方式和通信流程

在RocketMQ消息队列中支持通信的方式主要有同步（sync）、异步（async）和单向（oneway）这三种。

RocketMQ异步通信的整体流程图

![](../../pic/2020-01-12-17-45-03.png)



# 2、RocketMQ中RPC通信的Netty多线程模型

RocketMQ的RPC通信部分采用了"1+N+M1+M2"的Reactor多线程模式，对网络通信部分进行了一定的扩展与优化。

## 1 Netty的Reactor多线程模型设计概念与简述

Reactor多线程模型的设计思想是分而治之+事件驱动。

- 分而治之

一般来说，一个网络请求连接的完整处理过程可以分为接受（accept）、数据读取（read）、解码/编码（decode/encode）、业务处理（process）、发送响应（send）这几步骤。Reactor模型将每个步骤都映射成为一个任务，服务端线程执行的最小逻辑单元不再是一次完整的网络请求，而是这个任务，且采用以非阻塞方式执行。

- 事件驱动

每个任务对应特定网络事件。当任务准备就绪时，Reactor收到对应的网络事件通知，并将任务分发给绑定了对应网络事件的Handler执行。


## 2 RocketMQ中RPC通信的1+N+M1+M2的Reactor多线程设计与实现

> RocketMQ中RPC通信的Reactor多线程设计与流程


RocketMQ的RPC通信采用Netty组件作为底层通信库，同样也遵循了Reactor多线程模型，同时又在这之上做了一些扩展和优化。下面先给出一张RocketMQ的RPC通信层的Netty多线程模型框架图，让大家对RocketMQ的RPC通信中的多线程分离设计有一个大致的了解。

![](../../pic/2020-01-12-18-03-18.png)


从上面的框图中可以大致了解RocketMQ中NettyRemotingServer的Reactor 多线程模型。一个 Reactor 主线程（eventLoopGroupBoss，即为上面的1）负责监听 TCP网络连接请求，建立好连接后丢给Reactor 线程池（eventLoopGroupSelector，即为上面的“N”，源码中默认设置为3），它负责将建立好连接的socket 注册到 selector上去（RocketMQ的源码中会自动根据OS的类型选择NIO和Epoll，也可以通过参数配置），然后监听真正的网络数据。拿到网络数据后，再丢给Worker线程池（defaultEventExecutorGroup，即上面的“M1”，源码中默认设置为8）。

为了更为高效地处理RPC的网络请求，这里的Worker线程池是专门用于处理Netty网络通信相关的（包括编码/解码、空闲链接管理、网络连接管理以及网络请求处理）。

而处理业务操作放在业务线程池中执行，根据 RomotingCommand 的业务请求码code去processorTable这个本地缓存变量中找到对应的 processor，然后封装成task任务后，提交给对应的业务processor处理线程池来执行（sendMessageExecutor，以发送消息为例，即为上面的 “M2”）。

下面以表格的方式列举了下上面所述的“1+N+M1+M2”Reactor多线程模型：

![](../../pic/2020-01-12-18-05-21.png)


> 2、RocketMQ中RPC通信的Reactor多线程的代码具体实现

在NettyRemotingServer的实例初始化时，会初始化各个相关的变量包括serverBootstrap、nettyServerConfig参数、channelEventListener监听器并同时初始化eventLoopGroupBoss和eventLoopGroupSelector两个Netty的EventLoopGroup线程池（这里需要注意的是，如果是Linux平台，并且开启了native epoll，就用EpollEventLoopGroup，这个也就是用JNI，调的c写的epoll；否则就用Java NIO的NioEventLoopGroup）。


在NettyRemotingServer实例初始化完成后，就会将其启动。Server端在启动阶段会将之前实例化好的1个acceptor线程（eventLoopGroupBoss），N个IO线程（eventLoopGroupSelector），M1个worker 线程（defaultEventExecutorGroup）绑定上去。

这里需要说明的是，Worker线程拿到网络数据后，就交给Netty的ChannelPipeline（其采用责任链设计模式），从Head到Tail的一个个Handler执行下去，这些 Handler是在创建NettyRemotingServer实例时候指定的。NettyEncoder和NettyDecoder 负责网络传输数据和 RemotingCommand 之间的编解码。NettyServerHandler 拿到解码得到的 RemotingCommand 后，根据 RemotingCommand.type 来判断是 request 还是 response来进行相应处理，根据业务请求码封装成不同的task任务后，提交给对应的业务processor处理线程池处理。

从上面的描述中可以概括得出RocketMQ的RPC通信部分的Reactor线程池模型框图。

![](../../pic/2020-01-12-18-08-55.png)


整体可以看出RocketMQ的RPC通信借助Netty的多线程模型，其服务端监听线程和IO线程分离，同时将RPC通信层的业务逻辑与处理具体业务的线程进一步相分离。时间可控的简单业务都直接放在RPC通信部分来完成，复杂和时间不可控的业务提交至后端业务线程池中处理，这样提高了通信效率和MQ整体的性能。

其中抽象出NioEventLoop来表示一个不断循环执行处理任务的线程，每个NioEventLoop有一个selector，用于监听绑定在其上的socket链路。



# 3、核心api分析


## 1、如何通过用户API调用netty-client的？


> 1、netty封装成流程MQClientInstance

MQClientManager--->MQClientInstance--->MQClientAPIImpl--->RemotingClient[包含netty-client]

- MQClientManager是一个工厂生产MQClientInstance对象，并且MQClientManager内部有一个map缓存<clinetid,MQClientInstance>,缓存中有取缓存中的，没有的话新建；
- MQClientInstance在构造函数中初始化MQClientAPIImpl；
- MQClientAPIImpl在构造函数中初始化RemotingClient；

系统内部需要进行网络通信的全部通过MQClientManager获取。通过层层包裹，将底层通信和业务通过MQClientManager进行隔离。


> 2、使用到MQClientInstance的类有：

>> 1、生产者

![](../../pic/2020-01-15-09-54-11.png)

MQProducerInner[接口]--->DefaultMQProducerImpl唯一的实现类


>> 2、消费者


![](../../pic/2020-01-15-09-55-23.png)

```
MQConsumerInner
	DefaultMQPushConsumerImpl
	DefaultLitePullConsumerImpl
	DefaultMQPullConsumerImpl
```


>> 3、命令管理工具

![](../../pic/2020-01-15-09-56-17.png)

MQAdminExtInner--->DefaultMQAdminExtImpl


>> 4、作为类的构造函数传入

RebalanceLitePullImpl

PullAPIWrapper：消费者拉取消息的总入口，无论消费者配置的是push或者pull模式

![](../../pic/2020-01-15-11-03-07.png)


RebalanceService




> 3、直接暴露给用户使用的api

备注：下面的实现类都继承了ClientConfig对象

![](../../pic/2020-01-15-09-58-06.png)

![](../../pic/2020-01-15-10-00-33.png)


>> 1、生产者

MQAdmin[接口]--->MQProducer[接口]--->DefaultMQProducer[普通的生成者，构造函数中初始化DefaultMQProducerImpl]--->TransactionMQProducer[带事务]

所有暴露给用户的接口DefaultMQProducer或者TransactionMQProducer发送消息的时候就可以调用DefaultMQProducerImpl，借助于内部封装的netty-client发送请求到broker中；

>> 2、消费者

```
MQAdmin[接口]--->MQConsumer[接口]--->
							MQPullConsumer[接口]--->DefaultMQPullConsumer[构造函数中初始化DefaultMQPullConsumerImpl]
							MQPushConsumer[接口]--->DefaultMQPushConsumer[构造函数中初始化DefaultMQPushConsumerImpl]
```


这个自己一条处理流程

![](../../pic/2020-01-15-10-01-45.png)

LitePullConsumer--->DefaultLitePullConsumer[构造函数中初始化DefaultLitePullConsumerImpl]


>> 3、命令管理工具封装

![](../../pic/2020-01-15-20-00-09.png)

MQAdmin--->MQAdminExt--->DefaultMQAdminExt[构造函数中初始化DefaultMQAdminExtImpl]

备注：在SubCommand接口的实现类中会调用DefaultMQAdminExt中的方法。都在tools子模块中。