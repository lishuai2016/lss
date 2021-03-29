



<!-- TOC -->

- [1、集群组成](#1集群组成)
    - [1、NameServer Cluster（broker的注册中心）](#1nameserver-clusterbroker的注册中心)
    - [2、brokers](#2brokers)
    - [3、producers](#3producers)
    - [4、consumers](#4consumers)
- [2、消息类型](#2消息类型)
    - [1、集群消息与广播消息](#1集群消息与广播消息)
    - [2、普通消息、事物消息、顺序消息、延时消息](#2普通消息事物消息顺序消息延时消息)
- [3、生产者普通消息发送](#3生产者普通消息发送)
- [参考](#参考)

<!-- /TOC -->


# 1、集群组成


RocketMQ集群部署结构

![](../../pic/2020-01-11-22-47-20.png)

![](../../pic/2020-01-11-22-48-21.png)


![](../../pic/2020-01-15-20-04-51.png)



四部分组成name servers, brokers, producers and consumers

## 1、NameServer Cluster（broker的注册中心）

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




## 2、brokers

broker负责消息的存储，并提供topic和queue机制。支持推拉数据模型、故障容忍【通过2、3备份实现】、按原始时间顺序积累数以千亿计的消息的能力。此外，提供灾难恢复、度量统计、报警机制这些都是传统消息系统所缺失的。


broker复制消息的存储、投递、消息查询、HA保证等等。

![](../../pic/2020-01-11-23-13-19.png)

如图所示，broker有以下几个重要的子模块：

（1）Remoting Module，消息进入broker的入口，处理client端的请求；

（2）Client Manager, manages the clients (Producer/Consumer) and maintains topic subscription of consumer.

（3）Store Service, provides simple APIs to store or query message in physical disk.

（4）HA Service, provides data sync feature between master broker and slave broker.

（5）Index Service, builds index for messages by specified key and provides quick message query.



## 3、producers

通过多种负责均衡模式发送消息到brokers集群。支持快速失败和低延时。

## 4、consumers

支持推拉数据模型。支持集群消费和消息广播，支持实时消息订阅机制，可以满足大部分需求场景。



> RocketMQ集群的一部分通信如下

- 1、Broker启动后需要完成一次将自己注册至NameServer的操作；随后每隔30s时间定期向NameServer上报Topic路由信息；`NameServer定时（默认120s）清理没有更新心跳信息的broker，认为故障进行摘除`

- 2、消息生产者Producer作为客户端发送消息时候，需要根据Msg的Topic从本地缓存的TopicPublishInfoTable获取路由信息。如果没有则更新路由信息会从NameServer上重新拉取；

- 3、消息生产者Producer根据所获取的路由信息选择一个队列（MessageQueue）进行消息发送；Broker作为消息的接收者接收消息并落盘存储。




# 2、消息类型

## 1、集群消息与广播消息

集群消费：当使用集群消费模式时，MQ 认为任意一条消息只需要被集群内的任意一个消费者处理即可。

广播消费：当使用广播消费模式时，MQ 会将每条消息推送给集群内所有注册过的客户端，保证消息至少被每台机器消费一次。


设置集群消息：consumer.setMessageModel(MessageModel.CLUSTERING);

设置广播消息：consumer.setMessageModel(MessageModel.BROADCASTING);


> 集群消费模式:适用场景&注意事项

![](../../pic/2020-01-15-20-08-11.png)

- 消费端集群化部署，每条消息只需要被处理一次。
- 由于`消费进度在服务端维护`，可靠性更高。
- 集群消费模式下，每一条消息都只会被分发到一台机器上处理，如果需要被集群下的每一台机器都处理，请使用广播模式。
- 集群消费模式下，`不保证消息的每一次失败重投等逻辑都能路由到同一台机器上`，因此处理消息时不应该做任何确定性假设。


> 广播消费模式:适用场景&注意事项

- 顺序消息暂不支持广播消费模式。
- 每条消息都需要被相同逻辑的多台机器处理。
- `消费进度在客户端维护，出现重复的概率稍大于集群模式`。
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

实现原理：由于生产者默认是轮询获取MessageQueue队列(`每个Topic默认初始化4个MessageQueue`)，然后将消息轮询发送到不同的MessageQueue中，消费者从MessageQueue中获取数据时很可能是无序的。

局部有序消息：将相同顺序的消息发送到同一个MessageQueue队列，这样消费者从队列中获取数据肯定是相对有序的。

全局有序消息：将所有的消息发送到一个MessageQueue队列，消费者从单个队列中拉取消息，消息有序。

生产者：实现MessageQueueSelector接口，相同顺序的消息获取同一个MessageQueue

消费者：设置消息监听器为顺序消息监听器MessageListenerOrderly



> 4、延时消息

延时消息，简单来说就是当 producer 将消息发送到 broker 后，会延时一定时间后才投递给 consumer 进行消费。

RcoketMQ的延时等级为：1s，5s，10s，30s，1m，2m，3m，4m，5m，6m，7m，8m，9m，10m，20m，30m，1h，2h。level=0，表示不延时。level=1，表示 1 级延时，对应延时 1s。level=2 表示 2 级延时，对应5s，以此类推。

这种消息一般适用于消息生产和消费之间有时间窗口要求的场景。比如说我们网购时，下单之后是有一个支付时间，超过这个时间未支付，系统就应该自动关闭该笔订单。那么在订单创建的时候就会就需要发送一条延时消息（延时15分钟）后投递给 consumer，consumer 接收消息后再对订单的支付状态进行判断是否关闭订单。

设置延时非常简单，只需要在Message设置对应的延时级别即可


# 3、生产者普通消息发送

（1）生产者根据Topic名称从Name Server中获取相关Broker信息，根据Broker的地址信息将消息发送到对应Broker地址中

（2）对于同一个Topic，一个Broker对应多个MessageQueue(Topic可以在多个Broker中)，生产者默认通过取模轮询方式将消息发送到对应的MessageQueue中。

（3）生产者提供了接口send(Message msg, MessageQueueSelector selector, Object arg)，我们可以通过实现MessageQueueSelector接口可以实现选取MessageQueue的方法，例如实现有序消息就可以将需要有序的消息发送到同一个MessageQueue即可。




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