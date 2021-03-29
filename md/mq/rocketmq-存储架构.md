<!-- TOC -->

- [1、RocketMQ的存储架构](#1rocketmq的存储架构)
    - [1、存储特点](#1存储特点)
    - [2、为什么要这样设计？](#2为什么要这样设计)
    - [3、存储的底层实现](#3存储的底层实现)
    - [4、消息索引](#4消息索引)

<!-- /TOC -->



# 1、RocketMQ的存储架构


> MQ的存储模型选择

个人看来，从MQ的类型来看，存储模型分两种：

- 需要持久化（ActiveMQ,RabbitMQ`,Kafka,RocketMQ`）
- 不需要持久化(ZeroMQ)

几种存储方式：

- 分布式KV存储（levelDB,RocksDB,redis）
- 传统的文件系统
- 传统的关系型数据库

`这几种存储方式从效率来看， 文件系统>kv存储>关系型数据库`，因为直接操作文件系统肯定是最快的，而关系型数据库一般的TPS都不会很高，我印象中Mysql的写不会超过5Wtps（现在不确定最新情况）,所以如果追求效率就直接操作文件系统。

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

根据之前阿里中间件团队的测试，一旦kafka中Topic的partitoin数量过多，`队列文件会过多，会给磁盘的IO读写造成很大的压力，造成tps迅速下降。`所以RocketMQ进行了上述这样设计，consumerQueue中只存储很少的数据，消息主体都是通过CommitLog来进行读写。

没有一种方案是银弹，那么RocketMQ这样处理有什么优缺点？

> 优点：

队列轻量化，单个队列数据量非常少。对磁盘的访问串行化，避免磁盘竟争，不会因为队列增加导致IOWAIT增高。

> 缺点：

`写虽然完全是顺序写，但是读却变成了完全的随机读。`读一条消息，会先读ConsumeQueue，再读CommitLog，增加了开销。要保证CommitLog与ConsumeQueue完全的一致，增加了编程的复杂度。

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

- consumeQueue: consumeQueue就对应了相对的每个topic下的一个逻辑队列（`rocketMQ中叫queque，kafka的概念里叫partition`）, 它是一个逻辑队列！存储了消息在commitLog中的offSet。

- indexFile:`存储具体消息索引的文件`，以一个类似hash桶的数据结构进行索引维护。

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
