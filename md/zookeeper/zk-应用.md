

# 9、应用


配置中心（xxl-conf/disconf）、分布式锁

![](../../pic/2020-05-02-16-47-01.png)

## 1、分布式锁与领导选举关键点

> 1、最多一个获取锁 / 成为Leader

对于分布式锁（这里特指排它锁）而言，任意时刻，最多只有一个进程（对于单进程内的锁而言是单线程）可以获得锁。

对于领导选举而言，任意时间，最多只有一个成功当选为Leader。否则即出现脑裂（Split brain）


> 2、锁重入 / 确认自己是Leader

对于分布式锁，需要保证获得锁的进程在释放锁之前可再次获得锁，即锁的可重入性。

对于领导选举，Leader需要能够确认自己已经获得领导权，即确认自己是Leader。


> 3、释放锁 / 放弃领导权 

锁的获得者应该能够正确释放已经获得的锁，并且当获得锁的进程宕机时，锁应该自动释放，从而使得其它竞争方可以获得该锁，从而避免出现死锁的状态。


领导应该可以主动放弃领导权，并且当领导所在进程宕机时，领导权应该自动释放，从而使得其它参与者可重新竞争领导而避免进入无主状态。


> 4、感知锁释放 / 领导权的放弃

当获得锁的一方释放锁时，其它对于锁的竞争方需要能够感知到锁的释放，并再次尝试获取锁。

原来的Leader放弃领导权时，其它参与方应该能够感知该事件，并重新发起选举流程。

从上面几个方面可见，分布式锁与领导选举的技术要点非常相似，实际上其实现机制也相近。这里以领导选举为例来说明二者的实现原理，分布式锁的实现原理也几乎一致。 

## 2、非公平领导选举[Ephemeral且Non-sequence类型的节点]

> 1、选主过程

假设有三个ZooKeeper的客户端，如下图所示，同时竞争Leader。这三个客户端同时向ZooKeeper集群注册Ephemeral且Non-sequence类型的节点，路径都为 /zkroot/leader（工程实践中，路径名可自定义）。

![](../../pic/2020-03-14-17-37-09.png)

如上图所示，由于是Non-sequence节点，这三个客户端只会有一个创建成功，其它节点均创建失败。此时，创建成功的客户端（即上图中的Client 1）即成功竞选为 Leader 。其它客户端（即上图中的Client 2和Client 3）此时匀为 Follower。

> 2、放弃领导权

如果 Leader 打算主动放弃领导权，直接删除 /zkroot/leader 节点即可。

如果 Leader 进程意外宕机，其与 ZooKeeper 间的 Session 也结束，该节点由于是Ephemeral类型的节点，因此也会自动被删除。

此时 /zkroot/leader 节点不复存在，对于其它参与竞选的客户端而言，之前的 Leader 已经放弃了领导权。

> 3、感知领导权的放弃

由上图可见，创建节点失败的节点，除了成为 Follower 以外，还会向 /zkroot/leader 注册一个 Watch ，一旦 Leader 放弃领导权，也即该节点被删除，所有的 Follower 会收到通知。

> 4、重新选举

感知到旧 Leader 放弃领导权后，所有的 Follower 可以再次发起新一轮的领导选举，如下图所示。

![](../../pic/2020-03-14-17-39-19.png)


从上图中可见：

- 新一轮的领导选举方法与最初的领导选举方法完全一样，都是发起节点创建请求，创建成功即为 Leader，否则为 Follower ，且 Follower 会 Watch 该节点

- 新一轮的选举结果，无法预测，与它们在第一轮选举中的顺序无关。这也是该方案被称为非公平模式的原因

 

> 小结  
 

- 1、非公平模式实现简单，每一轮选举方法都完全一样

- 2、竞争参与方不多的情况下，效率高。每个 Follower 通过 Watch 感知到节点被删除的时间不完全一样，只要有一个 Follower 得到通知即发起竞选，即可保证当时有新的 Leader 被选出

- 3、给ZooKeeper 集群造成的负载大，因此扩展性差。如果有上万个客户端都参与竞选，意味着同时会有上万个写请求发送给 Zookeper。如《ZooKeeper架构》一文所述，ZooKeeper 存在单点写的问题，写性能不高。同时一旦 Leader 放弃领导权，ZooKeeper 需要同时通知上万个 Follower，负载较大。



## 3、公平领导选举[Ephemeral且sequence类型的节点]

> 1、选主过程

如下图所示，公平领导选举中，各客户端均创建 /zkroot/leader 节点，且其类型为Ephemeral与Sequence。

![](../../pic/2020-03-14-17-43-30.png)


由于是Sequence类型节点，故上图中三个客户端均创建成功，只是序号不一样。此时，每个客户端都会判断自己创建成功的节点的序号是不是当前最小的。如果是，则该客户端为 Leader，否则即为 Follower。


在上图中，Client 1创建的节点序号为 1 ，Client 2创建的节点序号为 2，Client 3创建的节点序号为3。由于最小序号为 1 ，且该节点由Client 1创建，故Client 1为 Leader 。

 

> 2、放弃领导权

Leader 如果主动放弃领导权，直接删除其创建的节点即可。


如果 Leader 所在进程意外宕机，其与 ZooKeeper 间的 Session 结束，由于其创建的节点为Ephemeral类型，故该节点自动被删除。


> 3、感知领导权的放弃

与非公平模式不同，每个 Follower 并非都 Watch 由 Leader 创建出来的节点，而是 Watch 序号刚好比自己序号小的节点。

在上图中，总共有 1、2、3 共三个节点，因此Client 2 Watch /zkroot/leader1，Client 3 Watch /zkroot/leader2。（注：序号应该是10位数字，而非一位数字，这里为了方便，以一位数字代替）


一旦 Leader 宕机，/zkroot/leader1 被删除，Client 2可得到通知。此时Client 3由于 Watch 的是 /zkroot/leader2 ，故不会得到通知。


> 4、重新选举


重新选举Client 2得到 /zkroot/leader1 被删除的通知后，不会立即成为新的 Leader 。而是先判断自己的序号 2 是不是当前最小的序号。在该场景下，其序号确为最小。因此Client 2成为新的 Leader 。

![](../../pic/2020-03-14-17-42-37.png)



这里要注意，如果在Client 1放弃领导权之前，Client 2就宕机了，Client 3会收到通知。此时Client 3不会立即成为Leader，而是要先判断自己的序号 3 是否为当前最小序号。很显然，由于Client 1创建的 /zkroot/leader1 还在，因此Client 3不会成为新的 Leader ，并向Client 2序号 2 前面的序号，也即 1 创建 Watch。该过程如下图所示。

![](../../pic/2020-03-14-17-46-22.png)


> 小结  
 

- 实现相对复杂；

- 扩展性好，每个客户端都只 Watch 一个节点且每次节点被删除只须通知一个客户端；

- 旧 Leader 放弃领导权时，其它客户端根据竞选的先后顺序（也即节点序号）成为新 Leader，这也是公平模式的由来；

- 延迟相对非公平模式要高，因为它必须等待特定节点得到通知才能选出新的 Leader。


**本节总结**

基于 ZooKeeper 的领导选举或者分布式锁的实现均基于 ZooKeeper 节点的特性及通知机制。充分利用这些特性，还可以开发出适用于其它场景的分布式应用。




## 4、Hbase选master

![](../../pic/2020-05-02-17-04-48.png)



ZooKeeper作用：
- ZooKeeper 为 HBase 提供 故障转移（ Failover ）机制，选举 Master，避免单点 Master 单点故障问题
- 存储所有 Region 的寻址入口：-ROOT-表在哪台服务器上。-ROOT-这张表的位置信息
- 实时监控 RegionServer 的状态，将 RegionServer 的上线和下线信息实时通知给 Master
- 存储 HBase 的 Schema，包括有哪些 Table，每个 Table 有哪些 Column Family。












# 参考

- [实例详解ZooKeeper ZAB协议、分布式锁与领导选举](https://dbaplus.cn/news-141-1875-1.html)

- [Zookeeper ZAB 协议分析](https://blog.xiaohansong.com/zab.html)

- [浅析Zookeeper的一致性原理](https://zhuanlan.zhihu.com/p/25594630)

- [Zookeeper学习系列【三】Zookeeper 集群架构、读写机制以及一致性原理(ZAB协议)](https://segmentfault.com/a/1190000019153800)

- [CAP 一致性协议及应用解析](https://segmentfault.com/a/1190000018275818)

- [ZAB协议选主过程详解](https://zhuanlan.zhihu.com/p/27335748)