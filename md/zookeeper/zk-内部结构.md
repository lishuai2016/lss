

# 4、zookeeper内部结构

![](../../pic/2020-05-02-16-40-09.png)

Zookeeper提供一个多层级的节点命名空间（节点称为znode）。与文件系统不同的是，这些节点都可以设置关联的数据，而文件系统中只有文件节点可以存放数据而目录节点不行。Zookeeper为了保证高吞吐和低延迟，在内存中维护了这个树状的目录结构，这种特性使得Zookeeper不能用于存放大量的数据，每个节点的存放数据上限为1M。

> 数据模型

```
cZxid	Created ZXID表示该数据节点被创建时的事务ID
ctime	Created Time表示节点被创建的时间
mZxid	Modified ZXID 表示该节点最后一次被更新时的事务ID
mtime	Modified Time表示节点最后一次被更新的时间
pZxid	表示该节点的子节点列表最后一次被修改时的事务ID。只有子节点列表变更了才会变更，子节点内容变更不会影响。
cversion	子节点的版本号
dataVersion	数据节点版本号
aclVersion	节点的ACL版本号
ephemeralOwner	创建该临时节点的会话的SessionID。如果节点是持久节点，这个属性为0
dataLength	数据内容的长度
numChildren	当前节点的子节点个数

```


## 1、ZooKeeper节点类型

ZooKeeper 提供了一个类似于 Linux 文件系统的树形结构。该树形结构中每个节点被称为 znode ，可按如下两个维度分类：

> 1、Persist vs. Ephemeral

- Persist节点，一旦被创建，便不会意外丢失，即使服务器全部重启也依然存在。每个 Persist 节点即可包含数据，也可包含子节点

- Ephemeral节点，在创建它的客户端与服务器间的 Session 结束时自动被删除。服务器重启会导致 Session 结束，因此 Ephemeral 类型的 znode 此时也会自动删除

 

> 2、Sequence vs. Non-sequence

- Non-sequence节点，多个客户端同时创建同一 Non-sequence 节点时，只有一个可创建成功，其它匀失败。并且创建出的节点名称与创建时指定的节点名完全一样

- Sequence节点，创建出的节点名在指定的名称之后带有10位10进制数的序号。多个客户端创建同一名称的节点时，都能创建成功，只是序号不同


## 2、ZooKeeper语义保证

ZooKeeper简单高效，同时提供如下语义保证，从而使得我们可以利用这些特性提供复杂的服务。

- 顺序性：客户端发起的更新会按发送顺序被应用到 ZooKeeper 上

- 原子性：更新操作要么成功要么失败，不会出现中间状态

- 单一系统镜像：一个客户端无论连接到哪一个服务器都能看到完全一样的系统镜像（即完全一样的树形结构）。注：根据上文《ZooKeeper架构及FastLeaderElection机制》介绍的 ZAB 协议，写操作并不保证更新被所有的 Follower 立即确认，因此通过部分 Follower 读取数据并不能保证读到最新的数据，而部分 Follwer 及 Leader 可读到最新数据。如果一定要保证单一系统镜像，可在读操作前使用 sync 方法。

- 可靠性：一个更新操作一旦被接受即不会意外丢失，除非被其它更新操作覆盖

- 最终一致性：写操作最终（而非立即）会对客户端可见


## 3、ZooKeeper Watch机制

所有对 ZooKeeper 的读操作，都可附带一个 Watch 。一旦相应的数据有变化，该 Watch 即被触发。

Watch 有如下特点：

- 主动推送：Watch被触发时，由 ZooKeeper 服务器主动将更新推送给客户端，而不需要客户端轮询。

- 一次性：数据变化时，Watch 只会被触发一次。如果客户端想得到后续更新的通知，必须要在 Watch 被触发后重新注册一个 Watch。

- 可见性：如果一个客户端在读请求中附带 Watch，Watch 被触发的同时再次读取数据，客户端在得到 Watch 消息之前肯定不可能看到更新后的数据。换句话说，更新通知先于更新结果。

- 顺序性：如果多个更新触发了多个 Watch ，那 Watch 被触发的顺序与更新顺序一致。

# 5、zookeeper和cap

CAP原则又称CAP定理，指的是在一个分布式系统中，Consistency（一致性）、 Availability（可用性）、Partition tolerance（分区容错性），三者不可得兼。


ZooKeeper保证的是CP

分析：可用性（A:Available）

- （1）不能保证每次服务请求的可用性;
- （2）进行leader选举时集群都是不可用（选举期间整个zk集群都是不可用的，这就导致在选举期间注册服务瘫痪）。
