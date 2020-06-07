RocketMQ简介




RocketMQ集群部署结构

![](../../pic/2020-01-11-22-47-20.png)

![](../../pic/2020-01-11-22-48-21.png)


![](../../pic/2020-01-15-20-04-51.png)



四部分组成name servers, brokers, producers and consumers

- 1、NameServer Cluster

提供轻量级的服务发现和路由。每一个NameServer记录了全部的路由信息，提供读写服务。

功能服务，包含两部分：

（1）broker的管理：NameServer 接收broker的注册，然后通过心跳机制确broker是否还活着。[注册信息如何存储的？]

（2）路由的管理：每一个NameServer保存右整个集群broker的信息和队列queue信息，供client端查询。(Producer/Consumer) 。[这里的路由信息又是如何存储的？]

有四种方式可以让client端（(Producer/Consumer) 获取NameServer的地址：

```
Programmatic Way, like producer.setNamesrvAddr("ip:port")
Java Options, use rocketmq.namesrv.addr.
Environment Variable, use NAMESRV_ADDR.
HTTP Endpoint.
```




- 2、brokers

broker负责消息的存储，并提供topic和queue机制。支持推拉数据模型、故障容忍【通过2、3备份实现】、按原始时间顺序积累数以千亿计的消息的能力。此外，提供灾难恢复、度量统计、报警机制这些都是传统消息系统所缺失的。


broker复制消息的存储、投递、消息查询、HA保证等等。

![](../../pic/2020-01-11-23-13-19.png)

如图所示，broker有以下几个重要的子模块：

（1）Remoting Module，消息进入broker的入口，处理client端的请求；

（2）Client Manager, manages the clients (Producer/Consumer) and maintains topic subscription of consumer.

（3）Store Service, provides simple APIs to store or query message in physical disk.

（4）HA Service, provides data sync feature between master broker and slave broker.

（5）Index Service, builds index for messages by specified key and provides quick message query.



- 3、producers

通过多种负责均衡模式发送消息到brokers集群。支持快速失败和低延时。

- 4、consumers

支持推拉数据模型。支持集群消费和消息广播，支持实时消息订阅机制，可以满足大部分需求场景。



> RocketMQ集群的一部分通信如下：

- 1、Broker启动后需要完成一次将自己注册至NameServer的操作；随后每隔30s时间定期向NameServer上报Topic路由信息；

- 2、消息生产者Producer作为客户端发送消息时候，需要根据Msg的Topic从本地缓存的TopicPublishInfoTable获取路由信息。如果没有则更新路由信息会从NameServer上重新拉取；

- 3、消息生产者Producer根据所获取的路由信息选择一个队列（MessageQueue）进行消息发送；Broker作为消息的接收者接收消息并落盘存储。



# 1、Remoting模块解析[实现系统各个模块之间的网络通信基于netty自定义的通信协议]

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







# 4、消息类型

## 1、集群消息与广播消息

集群消费：当使用集群消费模式时，MQ 认为任意一条消息只需要被集群内的任意一个消费者处理即可。

广播消费：当使用广播消费模式时，MQ 会将每条消息推送给集群内所有注册过的客户端，保证消息至少被每台机器消费一次。


设置集群消息：consumer.setMessageModel(MessageModel.CLUSTERING);

设置广播消息：consumer.setMessageModel(MessageModel.BROADCASTING);


> 集群消费模式:适用场景&注意事项

![](../../pic/2020-01-15-20-08-11.png)

- 消费端集群化部署，每条消息只需要被处理一次。
- 由于消费进度在服务端维护，可靠性更高。
- 集群消费模式下，每一条消息都只会被分发到一台机器上处理，如果需要被集群下的每一台机器都处理，请使用广播模式。
- 集群消费模式下，不保证消息的每一次失败重投等逻辑都能路由到同一台机器上，因此处理消息时不应该做任何确定性假设。


> 广播消费模式:适用场景&注意事项

- 顺序消息暂不支持广播消费模式。
- 每条消息都需要被相同逻辑的多台机器处理。
- 消费进度在客户端维护，出现重复的概率稍大于集群模式。
- 广播模式下，MQ 保证每条消息至少被每台客户端消费一次，但是并不会对消费失败的消息进行失败重投，因此业务方需要关注消费失败的情况。
- 广播模式下，第一次启动时默认从最新消息消费，客户端的消费进度是被持久化在客户端本地的隐藏文件中，因此不建议删除该隐藏文件，否则会丢失部分消息。
- 广播模式下，每条消息都会被大量的客户端重复处理，因此推荐尽可能使用集群模式。
- 广播模式下服务端不维护消费进度，所以 MQ 控制台不支持消息堆积查询和堆积报警功能。


## 2、普通消息、事物消息、顺序消息、延时消息

> 1、普通消息

普通消息也叫做无序消息，简单来说就是没有顺序的消息，producer 只管发送消息，consumer 只管接收消息，至于消息和消息之间的顺序并没有保证，可能先发送的消息先消费，也可能先发送的消息后消费。

举个简单例子，producer 依次发送 order id 为 1、2、3 的消息到 broker，consumer 接到的消息顺序有可能是 1、2、3，也有可能是 2、1、3 等情况，这就是普通消息。

因为不需要保证消息的顺序，所以消息可以大规模并发地发送和消费，吞吐量很高，适合大部分场景。

> 2、事物消息

MQ 的事务消息交互流程如下图所示：

![](../../pic/2020-01-15-20-12-25.png)

采用2PC提交：

第一阶段是：步骤1，2，3。

第二阶段是：步骤4，5。

![](../../pic/2020-01-15-20-12-58.png)


> 3、顺序消息

有序消息就是按照一定的先后顺序的消息类型。

举个例子来说，producer 依次发送 order id 为 1、2、3 的消息到 broker，consumer 接到的消息顺序也就是 1、2、3 ，而不会出现普通消息那样的 2、1、3 等情况。

那么有序消息是如何保证的呢？我们都知道消息首先由 producer 到 broker，再从 broker 到 consumer，分这两步走。那么要保证消息的有序，势必这两步都是要保证有序的，即要保证消息是按有序发送到 broker，broker 也是有序将消息投递给 consumer，两个条件必须同时满足，缺一不可。

进一步还可以将有序消息分成

- 全局有序消息
- 局部有序消息

实现原理：由于生产者默认是轮询获取MessageQueue队列(每个Topic默认初始化4个MessageQueue)，然后将消息轮询发送到不同的MessageQueue中，消费者从MessageQueue中获取数据时很可能是无序的。

局部有序消息：将相同顺序的消息发送到同一个MessageQueue队列，这样消费者从队列中获取数据肯定是相对有序的。

全局有序消息：将所有的消息发送到一个MessageQueue队列，消费者从单个队列中拉取消息，消息有序。

生产者：实现MessageQueueSelector接口，相同顺序的消息获取同一个MessageQueue

消费者：设置消息监听器为顺序消息监听器MessageListenerOrderly



> 4、延时消息

延时消息，简单来说就是当 producer 将消息发送到 broker 后，会延时一定时间后才投递给 consumer 进行消费。

RcoketMQ的延时等级为：1s，5s，10s，30s，1m，2m，3m，4m，5m，6m，7m，8m，9m，10m，20m，30m，1h，2h。level=0，表示不延时。level=1，表示 1 级延时，对应延时 1s。level=2 表示 2 级延时，对应5s，以此类推。

这种消息一般适用于消息生产和消费之间有时间窗口要求的场景。比如说我们网购时，下单之后是有一个支付时间，超过这个时间未支付，系统就应该自动关闭该笔订单。那么在订单创建的时候就会就需要发送一条延时消息（延时15分钟）后投递给 consumer，consumer 接收消息后再对订单的支付状态进行判断是否关闭订单。

设置延时非常简单，只需要在Message设置对应的延时级别即可


# 5、生产者普通消息发送

（1）生产者根据Topic名称从Name Server中获取相关Broker信息，根据Broker的地址信息将消息发送到对应Broker地址中

（2）对于同一个Topic，一个Broker对应多个MessageQueue(Topic可以在多个Broker中)，生产者默认通过取模轮询方式将消息发送到对应的MessageQueue中。

（3）生产者提供了接口send(Message msg, MessageQueueSelector selector, Object arg)，我们可以通过实现MessageQueueSelector接口可以实现选取MessageQueue的方法，例如实现有序消息就可以将需要有序的消息发送到同一个MessageQueue即可。

# 6、RocketMQ的存储架构


> MQ的存储模型选择

个人看来，从MQ的类型来看，存储模型分两种：

- 需要持久化（ActiveMQ,RabbitMQ,Kafka,RocketMQ）
- 不需要持久化(ZeroMQ)

几种存储方式：

- 分布式KV存储（levelDB,RocksDB,redis）
- 传统的文件系统
- 传统的关系型数据库

这几种存储方式从效率来看， 文件系统>kv存储>关系型数据库，因为直接操作文件系统肯定是最快的，而关系型数据库一般的TPS都不会很高，我印象中Mysql的写不会超过5Wtps（现在不确定最新情况）,所以如果追求效率就直接操作文件系统。

但是如果从可靠性和易实现的角度来说，则是关系型数据库>kv存储>文件系统，消息存在db里面非常可靠，但是性能会下降很多，所以具体的技术选型都是需要根据自己的业务需求去考虑。


![](../../pic/2020-01-15-20-28-27.png)



## 1、存储特点

如上图所示：
- （1）消息主体以及元数据都存储在**CommitLog**当中
- （2）Consume Queue相当于kafka中的partition，是一个逻辑队列，存储了这个Queue在CommiLog中的起始offset，log大小和MessageTag的hashCode。
- （3）每次读取消息队列先读取consumerQueue,然后再通过consumerQueue去commitLog中拿到消息主体。

## 2、为什么要这样设计？

rocketMQ的设计理念很大程度借鉴了kafka，所以有必要介绍下kafka的存储结构设计:

![](../../pic/2020-01-15-20-30-54.png)

存储特点：

和RocketMQ类似，每个Topic有多个partition(queue),kafka的每个partition都是一个独立的物理文件，消息直接从里面读写。
根据之前阿里中间件团队的测试，一旦kafka中Topic的partitoin数量过多，队列文件会过多，会给磁盘的IO读写造成很大的压力，造成tps迅速下降。所以RocketMQ进行了上述这样设计，consumerQueue中只存储很少的数据，消息主体都是通过CommitLog来进行读写。

没有一种方案是银弹，那么RocketMQ这样处理有什么优缺点？

> 优点：

队列轻量化，单个队列数据量非常少。对磁盘的访问串行化，避免磁盘竟争，不会因为队列增加导致IOWAIT增高。

> 缺点：

写虽然完全是顺序写，但是读却变成了完全的随机读。读一条消息，会先读ConsumeQueue，再读CommitLog，增加了开销。要保证CommitLog与ConsumeQueue完全的一致，增加了编程的复杂度。

> 以上缺点如何克服：

随机读，尽可能让读命中page cache，减少IO读操作，所以内存越大越好。如果系统中堆积的消息过多，读数据要访问磁盘会不会由于随机读导致系统性能急剧下降，答案是否定的。访问page cache 时，即使只访问1k的消息，系统也会提前预读出更多数据，在下次读时，就可能命中内存。

随机访问Commit Log磁盘数据，系统IO调度算法设置为NOOP方式，会在一定程度上将完全的随机读变成顺序跳跃方式，而顺序跳跃方式读较完全的随机读性能会高5倍以上。

另外4k的消息在完全随机访问情况下，仍然可以达到8K次每秒以上的读性能。

由于Consume Queue存储数据量极少，而且是顺序读，在PAGECACHE预读作用下，Consume Queue的读性能几乎与内存一致，即使堆积情况下。所以可认为Consume Queue完全不会阻碍读性能。

Commit Log中存储了所有的元信息，包含消息体，类似于Mysql、Oracle的redolog，所以只要有Commit Log在，Consume Queue即使数据丢失，仍然可以恢复出来。


## 3、存储的底层实现

RocketMQ中存储的底层实现：

1 MappedByteBuffer

RocketMQ中的文件读写主要就是通过MappedByteBuffer进行操作，来进行文件映射。利用了nio中的FileChannel模型，可以直接将物理文件映射到缓冲区，提高读写速度。


2 page cache

刚刚提到的缓冲区，也就是之前说到的page cache。通俗的说：pageCache是系统读写磁盘时为了提高性能将部分文件缓存到内存中，下面是详细解释：

page cache:这里所提及到的page cache，在我看来是linux中vfs虚拟文件系统层的cache层，一般pageCache默认是4K大小，它被操作系统的内存管理模块所管理，文件被映射到内存，一般都是被mmap()函数映射上去的。

mmap()函数会返回一个指针，指向逻辑地址空间中的逻辑地址，逻辑地址通过MMU映射到page cache上。


总结一下这里使用的存储底层（我认为的）： 通过将文件映射到内存上，直接操作文件，相比于传统的io(首先要调用系统IO，然后要将数据从内核空间传输到用户空间),避免了很多不必要的数据拷贝，所以这种技术也被称为 零拷贝

> 具体实现

消息实体存储的流程:

![](../../pic/2020-01-15-20-39-45.png)


各个关键对象的作用：

- DefaultMessageStore：这是存储模块里面最重要的一个类，包含了很多对存储文件的操作API，其他模块对消息实体的操作都是通过DefaultMessageStore进行操作。

- commitLog: commitLog是所有物理消息实体的存放文件。其中commitLog持有了MapedFileQueue。

- consumeQueue: consumeQueue就对应了相对的每个topic下的一个逻辑队列（rocketMQ中叫queque，kafka的概念里叫partition）, 它是一个逻辑队列！存储了消息在commitLog中的offSet。

- indexFile:存储具体消息索引的文件，以一个类似hash桶的数据结构进行索引维护。

- MapedFileQueue:这个对象包含一个MapedFileList,维护了多个mapedFile，升序存储。一个MapedFileQueue针对的就是一个目录下的所有二进制存储文件。理论上无线增长，定期删除过期文件。

![](../../pic/2020-01-15-20-46-27.png)

(图中左侧的目录树中，一个0目录就是一个MapedFileQueue,一个commitLog目录也是一个MapedFileQueue,右侧的000000000就是一个MapedFile。)

MapedFile: 每个MapedFile对应的就是一个物理二进制文件了，在代码中负责文件读写的就是MapedByteBuffer和fileChannel。相当于对pageCache文件的封装。


> 消息存储主流程

![](../../pic/2020-01-15-20-47-58.png)


> consumeQueue的消息处理

上述的消息存储只是把消息主体存储到了物理文件中，但是并没有把消息处理到consumeQueue文件中，那么到底是哪里存入的？

任务处理一般都分为两种：

一种是同步，把消息主体存入到commitLog的同时把消息存入consumeQueue，rocketMQ的早期版本就是这样处理的。

另一种是异步处理，起一个线程，不停的轮询，将当前的consumeQueue中的offSet和commitLog中的offSet进行对比，将多出来的offSet进行解析，然后put到consumeQueue中的MapedFile中。为什么要改同步为异步处理？应该是为了增加发送消息的吞吐量。


> 刷盘策略实现

消息在调用MapedFile的appendMessage后，也只是将消息装载到了ByteBuffer中，也就是内存中，还没有落盘。落盘需要将内存flush到磁盘上，针对commitLog，rocketMQ提供了两种落盘方式。

- 异步落盘
- 同步落盘


## 4、消息索引

> 1、消息索引的作用

这里的消息索引主要是提供根据起始时间、topic和key来查询消息的接口。首先根据给的topic、key以及起始时间查询到一个list，然后将offset拉到commitLog中查询，再反序列化成消息实体。

> 2、索引的具体实现

![](../../pic/2020-01-15-20-53-18.png)

索引的逻辑结构类似一个hashMap。

下面摘自官方文档：

- 1、根据查询的 key 的 hashcode%slotNum 得到具体的槽的位置（slotNum 是一个索引文件里面包含的最大槽的数目，
例如图中所示 slotNum=5000000） 。

- 2、根据 slotValue（slot 位置对应的值）查找到索引项列表的最后一项。

- 3、遍历索引项列表迒回查询时间范围内的结果集（默讣一次最大迒回的 32 条记彔）

- 4、Hash 冲突；寻找 key 的 slot 位置时相当亍执行了两次散列函数，一次 key 的 hash，一次 key 的 hash 值叏模，因此返里存在两次冲突的情冴；第一种，key 的 hash 值丌同但模数相同，此时查询的时候会在比较一次 key 的hash 值（每个索引项保存了 key 的 hash 值），过滤掉 hash 值丌相等的项。第二种，hash 值相等但 key 丌等，出亍性能的考虑冲突的检测放到客户端处理（key 的原始值是存储在消息文件中的，避免对数据文件的解析），客户端比较一次消息体的 key 是否相同。存储；为了节省空间索引项中存储的时间是时间差值（存储时间-开始时间，开始时间存储在索引文件头中），整个索引文件是定长的，结构也是固定的。


RocketMQ利用改了kafka的思想，针对使用文件做消息存储做了大量的实践和优化。commitLog一直顺序写，增大了写消息的吞吐量，对pageCache的利用也很好地提升了相应的效率，使文件也拥有了内存般的效率。




nameserver服务的netty-server服务接收broker和client的请求都会交给
org.apache.rocketmq.namesrv.processor.DefaultRequestProcessor#processRequest
进行处理。而处理的过程是根据请求对象RemotingCommand的code值来区分不同的请求，在这个函数中做分发处理，具体逻辑是调用org.apache.rocketmq.namesrv.routeinfo.RouteInfoManager中对应的方法。
RouteInfoManager中通过map来维护broker和topic的信息。


```
private final HashMap<String/* topic */, List<QueueData>> topicQueueTable;//topic信息。一个topic可以包含多个队列，队列可以分散在不同的broker上
private final HashMap<String/* brokerName */, BrokerData> brokerAddrTable;//单个broker信息
private final HashMap<String/* clusterName */, Set<String/* brokerName */>> clusterAddrTable;//存储broker集群的信息
private final HashMap<String/* brokerAddr */, BrokerLiveInfo> brokerLiveTable;//记录存活的broker
private final HashMap<String/* brokerAddr */, List<String>/* Filter Server */> filterServerTable;//功能？
```



# 参考

- [官网](https://rocketmq.apache.org/docs/rmq-arc/)

- [分布式开放消息系统(RocketMQ)的原理与实践](https://www.jianshu.com/p/453c6e7ff81c)

- [Rocketmq原理&最佳实践](https://www.jianshu.com/p/2838890f3284)

- [开发者如何玩转 RocketMQ？](https://www.sohu.com/a/242179383_115128)

- [源码分析blog](https://blog.csdn.net/qq924862077/article/details/84502083)

- [RocketMQ源码学习--消息存储篇](https://blog.csdn.net/mr253727942/article/details/55805876)

- [消息队列| RocketMQ 核心原理](https://cloud.tencent.com/developer/article/1446579)

- [RocketMQ原理解析-Broker](https://www.iteye.com/blog/technoboy-2368391)


- [RocketMQ(1)-架构原理](https://www.cnblogs.com/qdhxhz/p/11094624.html)

- [RocketMQ原理学习--消费者消费消息](https://blog.csdn.net/qq924862077/article/details/84797525)











这个作者感觉是rocketmq开发者

- [消息中间件—RocketMQ的RPC通信（一）](https://www.jianshu.com/p/d5da161efc33)
- [消息中间件—RocketMQ的RPC通信（二）](https://www.jianshu.com/p/8418af81a815)

- [消息中间件—RocketMQ消息发送](https://cloud.tencent.com/developer/article/1329034)

- [消息中间件—RocketMQ消息消费（一）](https://www.jianshu.com/p/f071d5069059)
- [消息中间件—RocketMQ消息消费（二）（push模式实现）](https://www.jianshu.com/p/fac642f3c1af)
- [消息中间件—RocketMQ消息消费（三）（消息消费重试）](https://www.jianshu.com/p/5843cdcd02aa)



- [消息中间件—RocketMQ消息存储（一）](https://www.jianshu.com/p/b73fdd893f98)
- [消息中间件—RocketMQ消息存储（二）](https://www.jianshu.com/p/6d0c118c17de)