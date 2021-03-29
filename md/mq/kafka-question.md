


kafka问题

> 01、为什么kafka那么快？

- 1、使用DMA zero-copy使得数据从磁盘发送到网络从之前的四次内核态和用户态切换、4次数据拷贝、2次CPU参与；优化为2次状态切换、2次数据拷贝，0CPU参与，大大提升性能；

- 2、磁盘的顺序读写替换一般的随机读写；


> 02、在Kafka中如果要实现多租户，有什么需要考虑的，以及基本设计思路是什么？

目前开源版的Kafka要实现多租户只能自己实现，有几个基本的事情要做：
- 1. 构建完备的用户认证和权限体系
- 2. 构建配额体系
- 3. 构建完善的监控体系
- 4. 开发方便的UI界面实现以上3点：）

> 怎么解决实时结果响应问题呢？比如秒杀商品，生产者产生订单，消费者处理订单结果，那这结果如何实时返回给用户呢？

这个场景使用Kafka Streams比较适合，它就是为read-process-write场景服务的


> mq和rpc调用的区别是什么呢？

mq和rpc的区别往大了说属于数据流模式（dataflow mode）的问题。我们常见的数据流有三种：1. 通过数据库；2. 通过服务调用（REST/RPC）; 3. 通过异步消息传递（消息引擎，如Kafka）

RPC和MQ是有相似之处的，毕竟我们远程调用一个服务也可以看做是一个事件，但不同之处在于：
- 1. MQ有自己的buffer，能够对抗过载（overloaded）和不可用场景
- 2. MQ支持重试
- 3. 允许发布/订阅模式

当然它们还有其他区别。应该这样说RPC是介于通过数据库和通过MQ之间的数据流模式。

> producer生产消息ack=all的时候，消息是怎么保证到follower的，因为看到follower是异步拉取数据的，难道是看leader和follower上面的offset吗？

通过HW机制。leader处的HW要等所有follower LEO都越过了才会前移

> 生产者acks=all使用异步提交, 如果ISR副本迟迟不能完成从leader的同步, 那么10s过后, 生产者会收到提交失败的回调吗? 还是一直不会有回调

会的，回调中会包含对应的错误码



> ack=all时候，生产者向leader发送完数据，而副本是异步拉取的，那生产者写入线程要一直阻塞等待吗

不会阻塞，你可以认为是不断轮询状态



> 请问ISR中的副本一定可以保证和leader副本的一致性吗？如果有一种情况是某个ISR中副本与leader副本的lag在ISR判断的边界值，这时如果leader副本挂了的话，还是会有数据丢失是吗？谢谢老师

ISR中的follower副本非常有可能与leader不一致的。如果leader挂了，其他follower又都没有保存该消息，那么该消息是可能丢失的。如果你要避免这种情况，设置producer端的acks=all吧


> 分区选举leader，是通过抢占模式来选举的。如果不开启unclean.leader.election.enable，是只能isr集合中的broker才能竞争吗？这个竞争的过程能具体说下是如何实现的吗？

目前选举leader的算法很简单，一般是选择AR中第一个处在ISR集合的副本为leader。比如AR的副本顺序是[1,2,3]，ISR是[2,3]，那么副本2就是leader


> 1.当控制器发生故障时，其他broker是如何感知到的？是ZK watch controller没有节点，然后广播通知其他的broker, 来争抢新建一个控制器节点吗？ 还是其他broker没定时收到控制器发送的元数据同步请求？2.ZK不是可以确保节点的唯一性，为什么还会出现控制器大于1的情况？

- 1. Controller所在broker发生故障，ZooKeeper上的/controller节点会自动消失。其他broker监控这个节点的存在，因此会第一时间感知到

- 2. 极少数情况下，不排除出现脑裂的情形，比如出现network partitioning，Zookeeper ensemble被分割成两个，的确有可能出现两个controller


> 各个broker之间怎样保证元数据的一致性？controller挂了后重新选举的机制是怎样的？

异步发送元数据来保持一致性。最权威的数据保存在Zk上。当controller挂掉之后，Zk上的临时节点/controller消失，所有存活broker都会感知到这一变化，于是抢注/controller，谁抢上谁就是新的controller

> 如果一台机器系统彻底坏了，不能恢复了，这时候副本肯定会丢一个，kafka 会直接把其它机器中的 一个 Follower 副本提升为 Leader 副本，对外提供服务，但是kafka会自动为其创建新的副本吗？

不会的

> 在虚拟机中搭建的kafka集群 为什么zookeeper的/controller是空的？

里面没有子节点，但是该节点本身有内容啊。

> 手动删除 /controller 节点在找到新的controller节点前，这个时间窗口期kafka集群是不是无法提供服务？比如：删除topic操作、消费消息等

嗯嗯，会有短暂的不可用


> 为何在2.2.1版本的zk的controller和consumer节点下没有任何数据和子节点呢？(单机环境)

/consumer已经被弃用了。/controller没有说明你的Kafka集群没起来。一个正常启动的Kafka集群必然有个Broker充当Controller，去抢注/controller节点


> 请问控制器也会像其他的broker一样提供消息的读写服务吗? 还是只做Broker的控制和协调工作?

也会提供正常的读写服务












# 参考

- [极客时间-Kafka核心技术与实战](https://time.geekbang.org/column/article/110388?utm_campaign=guanwang&utm_source=baidu-ad&utm_medium=ppzq-pc&utm_content=title&utm_term=baidu-ad-ppzq-title)


















