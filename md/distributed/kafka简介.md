
<!-- TOC -->

- [1、kafka简介](#1kafka简介)
- [2、核心技术点](#2核心技术点)
    - [1、基础结构](#1基础结构)
    - [2、Producer写](#2producer写)
    - [3、consumer读](#3consumer读)
    - [4、delivery guarantee 消息投放确认，不丢不重复](#4delivery-guarantee-消息投放确认不丢不重复)
- [3、特点](#3特点)
- [4、Kafka拓扑结构](#4kafka拓扑结构)
- [5、基本概念](#5基本概念)
    - [1、Broker](#1broker)
    - [2、消息](#2消息)
    - [3、Topic](#3topic)
    - [4、分区(partition)](#4分区partition)
    - [5、Log](#5log)
    - [6、副本](#6副本)
    - [7、生产者](#7生产者)
    - [8、消费者](#8消费者)
    - [9、ISR集合](#9isr集合)
    - [10、kafka的topic和分区](#10kafka的topic和分区)
- [6、数据模型](#6数据模型)
    - [1、网络模型](#1网络模型)
        - [1、Kafka网络通信模型的整体框架概述](#1kafka网络通信模型的整体框架概述)
    - [2、数据存储](#2数据存储)
        - [1、Kafka中几个重要概念介绍](#1kafka中几个重要概念介绍)
        - [2、Kafka的日志结构与数据存储](#2kafka的日志结构与数据存储)
            - [2.1Kafka中分区/副本的日志文件存储分析](#21kafka中分区副本的日志文件存储分析)
            - [2.2Kafka中日志索引和数据文件的存储结构](#22kafka中日志索引和数据文件的存储结构)
        - [3、总结](#3总结)
- [参考](#参考)

<!-- /TOC -->

Kafka的Producer、Broker和Consumer之间采用的是一套自行设计的基于TCP层的协议。Kafka的这套协议完全是为了Kafka自身的业务需求而定制的，而非要实现一套类似于Protocol Buffer的通用协议。

# 1、kafka简介

主要设计目标如下：

- 以时间复杂度为O(1)的方式提供消息[数据持久化]能力，即使对TB级以上数据也能保证常数时间复杂度的访问性能

- 高吞吐率。即使在非常廉价的商用机器上也能做到单机支持每秒100K条以上消息的传输

- 支持Kafka Server间的消息分区，及分布式消费，同时保证每个Partition内的消息顺序传输

- 同时支持离线数据处理和实时数据处理

- Scale out：支持在线水平扩展

# 2、核心技术点

## 1、基础结构

- 一个topic可以设置多个partition，并发读写，提高吞吐量，一个partition对应物理文件夹（包括数据文件和索引文件），一个partition只会存在一个broker物理机上[所以一般partition的数量多于broker的数量]；

- Kafka提供两种策略删除旧数据。一是基于时间，二是基于Partition文件大小。可以通过配置设置，和consumer消费与否没关；

- kafka把消息存到磁盘上，保证可靠；

## 2、Producer写

- kafka每条消息都被append到Partition中，属于顺序写磁盘，因此效率非常高（经验证，顺序写磁盘效率比随机写内存还要高，这是Kafka高吞吐率的一个很重要的保证）；

- Producer消息路由，Producer发送消息到broker时，会根据Paritition机制选择将其存储到哪一个Partition。如果Partition机制设置合理，所有消息可以均匀分布到不同的Partition里，这样就实现了负载均衡。如果一个Topic对应一个文件，那这个文件所在的机器I/O将会成为这个Topic的性能瓶颈，而有了Partition后，不同的消息可以并行写入不同broker的不同Partition里，极大的提高了吞吐率。在发送一条消息时，可以指定这条消息的key，Producer根据这个key和Partition机制来判断应该将这条消息发送到哪个Parition。

## 3、consumer读

- Kafka读取特定消息的时间复杂度为O(1)，即与文件大小无关，所以这里删除过期文件与提高Kafka性能无关。选择怎样的删除策略只与磁盘以及具体的需求有关；

- 这是Kafka用来实现一个Topic消息的广播（发给所有的Consumer）和单播（发给某一个Consumer）的手段。一个Topic可以对应多个Consumer Group。如果需要实现广播，只要每个Consumer有一个独立的Group就可以了。要实现单播只要所有的Consumer在同一个Group里；用Consumer Group还可以将Consumer进行自由的分组而不需要多次发送消息到不同的Topic；

- consumer通过pull来拉数据，自己来控制消费的速率；

## 4、delivery guarantee 消息投放确认，不丢不重复

这么几种可能的delivery guarantee：

At most once 消息可能会丢，但绝不会重复传输

At least one 消息绝不会丢，但可能会重复传输

Exactly once 每条消息肯定会被传输一次且仅传输一次，很多时候这是用户所想要的。

- Kafka默认保证At least once，并且允许通过设置Producer异步提交来实现At most once。而Exactly once要求与外部存储系统协作，幸运的是Kafka提供的offset可以非常直接非常容易得使用这种方式。



# 3、特点

Kafka是一个[分布式的]、[可分区的]、[可复制的]、[基于发布/订阅的消息系统],Kafka主要用于大数据领域,当然在分布式系统中也有应用。目前市面上流行的消息队列RocketMQ就是阿里借鉴Kafka的原理、用Java开发而得.Kafka适合离线和在线的消息消费,其消息保存在磁盘上。Kafka以Topic为单位进行消息的归纳,Producers向Topic发送(Push)消息,Consumers会消费(Pull)预订了Topic的消息。

备注：所有的消息都是落到磁盘文件的方式，消息被删除的时机不是被消费掉，Kafka提供两种策略删除旧数据。一是基于时间，二是基于Partition文件大小。


# 4、Kafka拓扑结构

![](../../pic/Kafka拓扑结构.png)


如上图所示，一个典型的Kafka集群中包含若干Producer（可以是web前端产生的Page View，或者是服务器日志，系统CPU、Memory等），若干broker（Kafka支持水平扩展，一般broker数量越多，集群吞吐率越高），若干Consumer Group，以及一个Zookeeper集群。Kafka通过Zookeeper管理集群配置，选举leader，以及在Consumer Group发生变化时进行rebalance。Producer使用push模式将消息发布到broker，Consumer使用pull模式从broker订阅并消费消息。 


# 5、基本概念

## 1、Broker

一个单独的Kafka server就是一个Broker,Broker的主要工作就是接收生产者发送来的消息,分配offset,然后将包装过的数据保存到磁盘上;此外,Broker还会接收消费者和其他Broker的请求,根据请求的类型进行相应的处理然后返回响应。多个Broker可以做成一个Cluster(集群)对外提供服务,每个Cluster当中会选出一个Broker来担任Controller,Controller是Kafka集群的指挥中心,其他的Broker则听从Controller指挥实现相应的功能。Controller负责管理分区的状态、管理每个分区的副本状态、监听zookeeper中数据的变化等。Controller也是一主多从的实现,所有的Broker都会监听Controller Leader的状态,当Leader Controller出现了故障的时候就重新选举新的Controller Leader。

## 2、消息

消息是Kafka中最基本的消息单元。消息由一串字节组成,其中主要由key和value构成,key和value都是字节数组。key的主要作用是根据一定的策略,将这个消息路由到指定的分区中,这样就可以保证包含同一个key的消息全部写入同一个分区


## 3、Topic

Topic是用于存储消息的逻辑概念,Topic可以看做是一个消息的集合。每个Topic可以有多个生产者向其中push消息,也可以有多个消费者向其中pull消息。


## 4、分区(partition)

每一个Topic都可以划分成多个分区(每一个Topic都至少有一个分区),不同的分区会分配在不同的Broker上以对Kafka进行水平扩展从而增加Kafka的并行处理能力。同一个Topic下的不同分区包含的消息是不同的。每一个消息在被添加到分区的时候,都会被分配一个offset,他是消息在此分区中的唯一编号,此外,Kafka通过offset保证消息在分区中的顺序,offset的顺序性不跨分区,也就是说在Kafka的同一个分区中的消息是有序的,不同分区的消息可能不是有序的。

![Partitions概念图](../../pic/kafka1.png)

## 5、Log

分区在逻辑上对应着一个Log,当生产者将消息写入分区的时候,实际上就是写入到了一个Log中。Log是一个逻辑概念,对应的是一个磁盘上的文件夹。Log由多个Segment组成,每一个Segment又对应着一个日志文件和一个索引文件。

## 6、副本

Kafka对消息进行了冗余备份,每一个分区都可以有多个副本,每一个副本中包含的消息是相同的(但不保证同一时刻下完全相同)。副本的类型分为Leader和Follower,当分区只有一个副本的时候,该副本属于Leader,没有Follower。Kafka的副本具有一定的同步机制,在每个副本集合中,都会选举出一个副本作为Leader副本,Kafka在不同的场景中会采用不同的选举策略。Kafka中所有的读写请求都由选举出的Leader副本处理,其他的都作为Follower副本,Follower副本仅仅是从Leader副本中把数据拉取到本地之后,同步更新到自己的Log中。

![分区副本](../../pic/kafka2.png)

## 7、生产者

生产者主要是生产消息,并将消息按照一定的规则推送到Topic的分区中

## 8、消费者

消费者主要是从Topic中拉取消息,并对消息进行消费。Consumer维护消费者消费Partition的哪一个位置(offset的值)这一信息。在Kafka中,多个Consumer可以组成一个Consumer Group,一个Consumer只能属于一个Consumer Group。Consumer Group保证其订阅的Topic中每一个分区只被分配给此Consumer Group中的一个消费者处理,所以如果需要实现消息的广播消费,则将消费者放在多个不同的Consumer Group中即可实现。通过向Consumer Group中动态的添加适量的Consumer,可以出发Kafka的Rebalance操作重新分配分区与消费者的对应关系,从而实现了水平扩展的能力。


## 9、ISR集合

ISR集合表示的是目前可用(alive)且消息量与Leader相差不多的副本集合,即整个副本集合的子集。ISR集合中副本所在的节点都与ZK保持着连接,此外,副本的最后一条消息的offset与Leader副本的最后一条消息的offset之间的差值不能超出指定的阈值。每一个分区的Leader副本都维护此分区的ISR集合。如上面所述,Leader副本进行了消息的写请求,Follower副本会从Leader上拉取写入的消息,第二个过程中会存在Follower副本中的消息数量少于Leader副本的状态,只要差值少于指定的阈值,那么此时的副本集合就是ISR集合。


## 10、kafka的topic和分区

本质上Kafka是分布式的流数据平台，因为以下特性而著名：

- 提供Pub/Sub方式的海量消息处理。

- 以高容错的方式存储海量数据流。

- 保证数据流的顺序。

Kafka提供的Pub/Sub就是典型的异步消息交换，用户可以为服务器日志或者物联网设备创建不同主题（Topic），后端数据仓库、流式分析或者全文检索等对接特定主题，服务器或者物联网设备是无需关心的。同时，Kafka可以将主题划分为多个分区（Partition），会根据分区规则选择把消息存储到哪个分区中，只要如果分区规则设置的合理，那么所有的消息将会被均匀的分布到不同的分区中，这样就实现了负载均衡和水平扩展。另外，多个订阅者可以从一个或者多个分区中同时消费数据，以支撑海量数据处理能力：

![](../../pic/kafka的topic和partition分区1.png)

Kafka的设计也是源自生活，好比是为公路运输，不同的起始点和目的地需要修不同高速公路（主题），高速公路上可以提供多条车道（分区），流量大的公路多修几条车道保证畅通，流量小的公路少修几条车道避免浪费。收费站好比消费者，车多的时候多开几个一起收费避免堵在路上，车少的时候开几个让汽车并道就好了，嗯……。顺便说一句，由于消息是以追加到分区中的，多个分区顺序写磁盘的总效率要比随机写内存还要高（引用Apache Kafka – A High Throughput Distributed Messaging System的观点），是Kafka高吞吐率的重要保证之一。为了保证数据的可靠性，Kafka会给每个分区找一个节点当带头大哥（Leader），以及若干个节点当随从（Follower）。消息写入分区时，带头大哥除了自己复制一份外还会复制到多个随从。如果随从挂了，Kafka会再找一个随从从带头大哥那里同步历史消息；如果带头大哥挂了，随从中会选举出新一任的带头大哥，继续笑傲江湖。

![](../../pic/kafka的topic和partition分区2.png)

最后，每个发布者发送到Kafka分区中的消息是确保顺序的，订阅者可以依赖这个承诺进行后续处理。


# 6、数据模型

## 1、网络模型

- [消息中间件—简谈Kafka中的NIO网络通信模型](https://www.jianshu.com/p/a6b9e5342878)

- [消息中间件—Kafka数据存储（一）](https://www.jianshu.com/p/3e54a5a39683)

### 1、Kafka网络通信模型的整体框架概述

Kafka的网络通信模型是基于NIO的Reactor多线程模型来设计的。这里先引用Kafka源码中注释的一段话：

```
An NIO socket server. The threading model is
1 Acceptor thread that handles new connections.
Acceptor has N Processor threads that each have their own selector and read requests from sockets.
M Handler threads that handle requests and produce responses back to the processor threads for writing.
```

相信大家看了上面的这段引文注释后，大致可以了解到Kafka的网络通信层模型，主要采用了**1（1个Acceptor线程）+N（N个Processor线程）+M（M个业务处理线程）**。下面的表格简要的列举了下（这里先简单的看下后面还会详细说明）：

![](../../pic/2020-03-30-20-53-41.png)

Kafka网络通信层的完整框架图如下图所示：

![](../../pic/2020-03-30-20-54-13.png)


这里可以简单总结一下其网络通信模型中的几个重要概念：

- （1），Acceptor：1个接收线程，负责监听新的连接请求，同时注册OP_ACCEPT 事件，将新的连接按照"round robin"方式交给对应的 Processor 线程处理；

- （2），Processor：N个处理器线程，其中每个 Processor 都有自己的 selector，它会向 Acceptor 分配的 SocketChannel 注册相应的 OP_READ 事件，N 的大小由“num.networker.threads”决定；

- （3），KafkaRequestHandler：M个请求处理线程，包含在线程池—KafkaRequestHandlerPool内部，从RequestChannel的全局请求队列—requestQueue中获取请求数据并交给KafkaApis处理，M的大小由“num.io.threads”决定；

- （4），RequestChannel：其为Kafka服务端的请求通道，该数据结构中包含了一个全局的请求队列 requestQueue和多个与Processor处理器相对应的响应队列responseQueue，提供给Processor与请求处理线程KafkaRequestHandler和KafkaApis交换数据的地方。

- （5），NetworkClient：其底层是对 Java NIO 进行相应的封装，位于Kafka的网络接口层。Kafka消息生产者对象—KafkaProducer的send方法主要调用NetworkClient完成消息发送；

- （6），SocketServer：其是一个NIO的服务，它同时启动一个Acceptor接收线程和多个Processor处理器线程。提供了一种典型的Reactor多线程模式，将接收客户端请求和处理请求相分离；

- （7），KafkaServer：代表了一个Kafka Broker的实例；其startup方法为实例启动的入口；

- （8），KafkaApis：Kafka的业务逻辑处理Api，负责处理不同类型的请求；比如“发送消息”、“获取消息偏移量—offset”和“处理心跳请求”等；


## 2、数据存储

Kafka这款分布式消息队列使用文件系统和操作系统的页缓存（page cache）分别存储和缓存消息，摒弃了Java的堆缓存机制，同时将随机写操作改为顺序写，再结合Zero-Copy的特性极大地改善了IO性能。而提起磁盘的文件系统，相信很多对硬盘存储了解的同学都知道：“一块SATA RAID-5阵列磁盘的线性写速度可以达到几百M/s，而随机写的速度只能是100多KB/s，线性写的速度是随机写的上千倍”，由此可以看出对磁盘写消息的速度快慢关键还是取决于我们的使用方法。鉴于此，Kafka的数据存储设计是建立在对文件进行追加的基础上实现的，因为是顺序追加，通过O(1)的磁盘数据结构即可提供消息的持久化，并且这种结构对于即使是数以TB级别的消息存储也能够保持长时间的稳定性能。在理想情况下，只要磁盘空间足够大就一直可以追加消息。此外，Kafka也能够通过配置让用户自己决定已经落盘的持久化消息保存的时间，提供消息处理更为灵活的方式。本文将主要介绍Kafka中数据的存储消息结构、存储方式以及如何通过offset来查找消息等内容。

### 1、Kafka中几个重要概念介绍

- （1）Broker：消息中间件处理节点，一个Kafka节点就是一个broker，一个或者多个Broker可以组成一个Kafka集群；

- （2）Topic：主题是对一组消息的抽象分类，比如例如page view日志、click日志等都可以以topic的形式进行抽象划分类别。在物理上，不同Topic的消息分开存储，逻辑上一个Topic的消息虽然保存于一个或多个broker上但用户只需指定消息的Topic即可使得数据的生产者或消费者不必关心数据存于何处；

- （3）Partition：每个主题又被分成一个或者若干个分区（Partition）。每个分区在本地磁盘上对应一个文件夹，分区命名规则为主题名称后接“—”连接符，之后再接分区编号，分区编号从0开始至分区总数减-1；

- （4）LogSegment：每个分区又被划分为多个日志分段（LogSegment）组成，日志段是Kafka日志对象分片的最小单位；LogSegment算是一个逻辑概念，对应一个具体的日志文件（“.log”的数据文件）和两个索引文件（“.index”和“.timeindex”，分别表示偏移量索引文件和消息时间戳索引文件）组成；

- （5）Offset：每个partition中都由一系列有序的、不可变的消息组成，这些消息被顺序地追加到partition中。每个消息都有一个连续的序列号称之为offset—偏移量，用于在partition内唯一标识消息（并不表示消息在磁盘上的物理位置）；

- （6）Message：消息是Kafka中存储的最小最基本的单位，即为一个commit log，由一个固定长度的消息头和一个可变长度的消息体组成；

### 2、Kafka的日志结构与数据存储

Kafka中的消息是以主题（Topic）为基本单位进行组织的，各个主题之间相互独立。在这里主题只是一个逻辑上的抽象概念，而在实际数据文件的存储中，Kafka中的消息存储在物理上是以一个或多个分区（Partition）构成，每个分区对应本地磁盘上的一个文件夹，每个文件夹内包含了日志索引文件（“.index”和“.timeindex”）和日志数据文件（“.log”）两部分。分区数量可以在创建主题时指定，也可以在创建Topic后进行修改。（ps：Topic的Partition数量只能增加而不能减少，这点内容超出本篇幅的减少范围，大家可以先思考下）。

在Kafka中正是因为使用了分区（Partition）的设计模型，通过将主题（Topic）的消息打散到多个分区，并分布保存在不同的Kafka Broker节点上实现了消息处理的高吞吐量。其生产者和消费者都可以多线程地并行操作，而每个线程处理的是一个分区的数据。

![](../../pic/2020-03-30-21-13-55.png)

同时，Kafka为了实现集群的高可用性，在每个Partition中可以设置有一个或者多个副本（Replica），分区的副本分布在不同的Broker节点上。同时，从副本中会选出一个副本作为Leader，Leader副本负责与客户端进行读写操作。而其他副本作为Follower会从Leader副本上进行数据同步。

#### 2.1Kafka中分区/副本的日志文件存储分析

由上面可以看出，每个分区在物理上对应一个文件夹，分区的命名规则为主题名后接“—”连接符，之后再接分区编号，分区编号从0开始，编号的最大值为分区总数减1。每个分区又有1至多个副本，分区的副本分布在集群的不同代理上，以提高可用性。从存储的角度上来说，分区的每个副本在逻辑上可以抽象为一个日志（Log）对象，即分区副本与日志对象是相对应的。下图是在三个Kafka Broker节点所组成的集群中分区的主/备份副本的物理分布情况图：

![](../../pic/2020-03-30-21-21-56.png)

#### 2.2Kafka中日志索引和数据文件的存储结构


在Kafka中，每个Log对象又可以划分为多个LogSegment文件，每个LogSegment文件包括一个日志数据文件和两个索引文件（偏移量索引文件和消息时间戳索引文件）。其中，每个LogSegment中的日志数据文件大小均相等（该日志数据文件的大小可以通过在Kafka Broker的config/server.properties配置文件的中的“log.segment.bytes”进行设置，默认为1G大小（1073741824字节），在顺序写入消息时如果超出该设定的阈值，将会创建一组新的日志数据和索引文件）。
Kafka将日志文件封装成一个FileMessageSet对象，将偏移量索引文件和消息时间戳索引文件分别封装成OffsetIndex和TimerIndex对象。Log和LogSegment均为逻辑概念，Log是对副本在Broker上存储文件的抽象，而LogSegment是对副本存储下每个日志分段的抽象，日志与索引文件才与磁盘上的物理存储相对应；下图为Kafka日志存储结构中的对象之间的对应关系图：

![](../../pic/2020-03-30-21-24-03.png)


下面的表格先列举了Kakfa消息体结构中几个主要字段的说明：

![](../../pic/2020-03-30-21-25-41.png)

> 1.日志数据文件

Kafka将生产者发送给它的消息数据内容保存至日志数据文件中，该文件以该段的基准偏移量左补齐0命名，文件后缀为“.log”。分区中的每条message由offset来表示它在这个分区中的偏移量，这个offset并不是该Message在分区中实际存储位置，而是逻辑上的一个值（Kafka中用8字节长度来记录这个偏移量），但它却唯一确定了分区中一条Message的逻辑位置，同一个分区下的消息偏移量按照顺序递增（这个可以类比下数据库的自增主键）。另外，从dump出来的日志数据文件的字符值中可以看到消息体的各个字段的内容值。

> 2.偏移量索引文件

如果消息的消费者每次fetch都需要从1G大小（默认值）的日志数据文件中来查找对应偏移量的消息，那么效率一定非常低，在定位到分段后还需要顺序比对才能找到。Kafka在设计数据存储时，为了提高查找消息的效率，故而为分段后的每个日志数据文件均使用稀疏索引的方式建立索引，这样子既节省空间又能通过索引快速定位到日志数据文件中的消息内容。偏移量索引文件和数据文件一样也同样也以该段的基准偏移量左补齐0命名，文件后缀为“.index”。

从上面dump出来的偏移量索引内容可以看出，索引条目用于将偏移量映射成为消息在日志数据文件中的实际物理位置，每个索引条目由offset和position组成，每个索引条目可以唯一确定在各个分区数据文件的一条消息。其中，Kafka采用稀疏索引存储的方式，每隔一定的字节数建立了一条索引，可以通过“index.interval.bytes”设置索引的跨度；

有了偏移量索引文件，通过它，Kafka就能够根据指定的偏移量快速定位到消息的实际物理位置。具体的做法是，根据指定的偏移量，使用二分法查询定位出该偏移量对应的消息所在的分段索引文件和日志数据文件。然后通过二分查找法，继续查找出小于等于指定偏移量的最大偏移量，同时也得出了对应的position（实际物理位置），根据该物理位置在分段的日志数据文件中顺序扫描查找偏移量与指定偏移量相等的消息。下面是Kafka中分段的日志数据文件和偏移量索引文件的对应映射关系图（其中也说明了如何按照起始偏移量来定位到日志数据文件中的具体消息）。

![](../../pic/2020-03-30-21-28-40.png)


> 3.时间戳索引文件

从上面一节的分区目录中，我们还可以看到存在一些以“.timeindex”的时间戳索引文件。这种类型的索引文件是Kafka从0.10.1.1版本开始引入的的一个基于时间戳的索引文件，它们的命名方式与对应的日志数据文件和偏移量索引文件名基本一样，唯一不同的就是后缀名。从上面dump出来的该种类型的时间戳索引文件的内容来看，每一条索引条目都对应了一个8字节长度的时间戳字段和一个4字节长度的偏移量字段，其中时间戳字段记录的是该LogSegment到目前为止的最大时间戳，后面对应的偏移量即为此时插入新消息的偏移量。

另外，时间戳索引文件的时间戳类型与日志数据文件中的时间类型是一致的，索引条目中的时间戳值及偏移量与日志数据文件中对应的字段值相同（ps：Kafka也提供了通过时间戳索引来访问消息的方法）。

### 3、总结

从全文来看，Kafka高效数据存储设计的特点在于以下几点：

- （1）、Kafka把主题中一个分区划分成多个分段的小文件段，通过多个小文件段，就容易根据偏移量查找消息、定期清除和删除已经消费完成的数据文件，减少磁盘容量的占用；

- （2）、采用稀疏索引存储的方式构建日志的偏移量索引文件，并将其映射至内存中，提高查找消息的效率，同时减少磁盘IO操作；

- （3）、Kafka将消息追加的操作逻辑变成为日志数据文件的顺序写入，极大的提高了磁盘IO的性能；

任何一位使用Kafka的同学来说，如果能够掌握其数据存储机制，对于大规模Kafka集群的性能调优和问题定位都大有裨益。














# 参考

- http://www.jasongj.com/2015/03/10/KafkaColumn1/
- http://www.jasongj.com/categories/index.html
- https://blog.csdn.net/YChenFeng/article/details/74980531
- https://zhuanlan.zhihu.com/habren-kafka
- https://zhuanlan.zhihu.com/c_1036270698523828224
- https://juejin.im/entry/59b3c1595188257e6e260252
- https://juejin.im/post/5b20e2e16fb9a01e2c698c51
- http://jm.taobao.org/2018/06/13/%E6%97%A5%E5%BF%97%E9%87%87%E9%9B%86%E4%B8%AD%E7%9A%84%E5%85%B3%E9%94%AE%E6%8A%80%E6%9C%AF%E5%88%86%E6%9E%90/
