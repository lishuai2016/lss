

<!-- TOC -->

- [0、总结](#0总结)
    - [1、成为leader的条件](#1成为leader的条件)
    - [2、数据一致性保障](#2数据一致性保障)
- [1、ZooKeeper是什么](#1zookeeper是什么)
- [2、ZooKeeper服务器角色](#2zookeeper服务器角色)
- [3、原子广播（ZAB）](#3原子广播zab)
    - [1、写操作](#1写操作)
    - [2、读操作](#2读操作)
    - [3、支持的领导选举算法[FastLeaderElection原理]](#3支持的领导选举算法fastleaderelection原理)
        - [01、myid](#01myid)
        - [02、zxid](#02zxid)
        - [03、服务器状态](#03服务器状态)
        - [04、选票数据结构](#04选票数据结构)
        - [05、投票流程](#05投票流程)
        - [06、集群启动领导选举](#06集群启动领导选举)
        - [07、Follower重启选举](#07follower重启选举)
        - [08、Leader重启选举](#08leader重启选举)
        - [09、Commit过的数据不丢失](#09commit过的数据不丢失)
- [4、zookeeper内部结构](#4zookeeper内部结构)
    - [1、ZooKeeper节点类型](#1zookeeper节点类型)
    - [2、ZooKeeper语义保证](#2zookeeper语义保证)
    - [3、ZooKeeper Watch机制](#3zookeeper-watch机制)
- [5、zookeeper和cap](#5zookeeper和cap)
- [9、应用](#9应用)
    - [1、分布式锁与领导选举关键点](#1分布式锁与领导选举关键点)
    - [2、非公平领导选举[Ephemeral且Non-sequence类型的节点]](#2非公平领导选举ephemeral且non-sequence类型的节点)
    - [3、公平领导选举[Ephemeral且sequence类型的节点]](#3公平领导选举ephemeral且sequence类型的节点)
    - [4、Hbase选master](#4hbase选master)
- [参考](#参考)

<!-- /TOC -->

# 0、总结

## 1、成为leader的条件

- 1、选epoch最大的
- 2、epoch相等，选 zxid 最大的
- 3、epoch和zxid都相等，选择server id最大的（就是我们配置zoo.cfg中的myid）

节点在选举开始都默认投票给自己，当接收其他节点的选票时，会根据上面的条件更改自己的选票并重新发送选票给其他节点，当有一个节点的得票超过半数，该节点会设置自己的状态为 leading，其他节点会设置自己的状态为 following。


## 2、数据一致性保障

leader挂了重新选主，选最新的leader，消息在新leader上的状态：

- 1、保证commit过的数据不丢失；

- 2、uncommit的消息在现有集群中是否存在大多数节点，是的话提交；不是的话丢弃；



# 1、ZooKeeper是什么

ZooKeeper是一个分布式协调服务，可用于服务发现、分布式锁、分布式领导选举、配置管理等。

这一切的基础，都是ZooKeeper提供了一个类似于Linux文件系统的树形结构（可认为是轻量级的内存文件系统，但只适合存少量信息，完全不适合存储大量文件或者大文件），同时提供了对于每个节点的监控与通知机制。

既然是一个文件系统，就不得不提ZooKeeper是如何保证数据的一致性的。

ZooKeeper 的设计目标是将那些复杂且容易出错的分布式一致性服务封装起来，构成一个高效可靠的原语集，并以一系列简单易用的接口提供给用户使用。

原语： 操作系统或计算机网络用语范畴。是由若干条指令组成的，用于完成一定功能的一个过程。具有不可分割性·即原语的执行必须是连续的，在执行过程中不允许被中断。

ZooKeeper 是一个典型的分布式数据一致性解决方案，分布式应用程序可以基于 ZooKeeper 实现诸如数据发布/订阅、负载均衡、命名服务、分布式协调/通知、集群管理、Master 选举、分布式锁和分布式队列等功能。


# 2、ZooKeeper服务器角色

ZooKeeper集群是一个基于主从复制的高可用集群，每个服务器承担如下三种角色中的一种：

- Leader 一个ZooKeeper集群同一时间只会有一个实际工作的Leader，它会发起并维护与各Follwer及Observer间的心跳。所有的写操作必须要通过Leader完成再由Leader将写操作广播给其它服务器。

- Follower 一个ZooKeeper集群可能同时存在多个Follower，它会响应Leader的心跳。Follower可直接处理并返回客户端的读请求，同时会将写请求转发给Leader处理，并且负责在Leader处理写请求时对请求进行投票。

- Observer 角色与Follower类似，但是无投票权。

![](../../pic/2020-03-14-16-43-13.png)

# 3、原子广播（ZAB）

为了保证写操作的一致性与可用性，ZooKeeper专门设计了一种名为原子广播（ZAB）的支持崩溃恢复的一致性协议。基于该协议，ZooKeeper实现了一种主从模式的系统架构来保持集群中各个副本之间的数据一致性。


根据ZAB协议，所有的写操作都必须通过Leader完成，Leader写入本地日志后再复制到所有的Follower节点。


一旦Leader节点无法工作，ZAB协议能够自动从Follower节点中重新选出一个合适的替代者，即新的Leader，该过程即为领导选举。该领导选举过程，是ZAB协议中最为重要和复杂的过程。


ZAB 的实现只有三个阶段:

- 1、Fast Leader Election选主阶段

- 2、Recovery Phase崩溃恢复（重新选主，即主节点挂了）

- 3、Broadcast Phase（广播新事物）



> 两个问题

>> 1、主从架构下，leader 崩溃，数据一致性怎么保证？

leader 崩溃之后，集群会选出新的 leader，然后就会进入恢复阶段，新的 leader 具有所有已经提交的提议，因此它会保证让 followers 同步已提交的提议，丢弃未提交的提议（以 leader 的记录为准），这就保证了整个集群的数据一致性。

>> 2、选举 leader 的时候，整个集群无法处理写请求的，如何快速进行 leader 选举？

这是通过 Fast Leader Election 实现的，leader 的选举只需要超过半数的节点投票即可，这样不需要等待所有节点的选票，能够尽早选出 leader。




## 1、写操作

> 写Leader

通过Leader进行写操作流程如下图所示：

![](../../pic/2020-03-14-16-48-19.png)

由上图可见，通过Leader进行写操作，主要分为五步：

- 1、客户端向Leader发起写请求

- 2、Leader将写请求以Proposal的形式发给所有Follower并等待ACK

- 3、Follower收到Leader的Proposal后返回ACK

- 4、Leader得到过半数的ACK（Leader对自己默认有一个ACK）后向所有的Follower和Observer发送Commmit

- 5、Leader将处理结果返回给客户端


这里要注意：

- Leader并不需要得到Observer的ACK，即Observer无投票权

- Leader不需要得到所有Follower的ACK，只要收到过半的ACK即可，同时Leader本身对自己有一个ACK。上图中有4个Follower，只需其中两个返回ACK即可，因为(2+1) / (4+1) > 1/2

- Observer虽然无投票权，但仍须同步Leader的数据从而在处理读请求时可以返回尽可能新的数据

> 2、写Follower/Observer

通过Follower/Observer进行写操作流程如下图所示：

![](../../pic/2020-03-14-16-50-38.png)

从上图可见：

- Follower/Observer均可接受写请求，但不能直接处理，而需要将写请求转发给Leader处理

- 除了多了一步请求转发，其它流程与直接写Leader无任何区别

## 2、读操作

Leader/Follower/Observer都可直接处理读请求，从本地内存中读取数据并返回给客户端即可。

![](../../pic/2020-03-14-16-51-40.png)

由于处理读请求不需要服务器之间的交互，Follower/Observer越多，整体可处理的读请求量越大，也即读性能越好。

## 3、支持的领导选举算法[FastLeaderElection原理]


选举的核心思想：**比较zxid谁最大选谁，一样的基于服务器编号，编号大的当选**



可通过electionAlg配置项设置ZooKeeper用于领导选举的算法。

到3.4.10版本为止，可选项有：

- 0 基于UDP的LeaderElection

- 1 基于UDP的FastLeaderElection

- 2 基于UDP和认证的FastLeaderElection

- 3 基于TCP的FastLeaderElection

在3.4.10版本中，默认值为3，也即基于TCP的FastLeaderElection。另外三种算法已经被弃用，并且有计划在之后的版本中将它们彻底删除而不再支持。





- 在 ZAB 协议的事务编号 Zxid 设计中，Zxid 是一个 64 位的数字，其中低 32 位是一个简单的单调递增的计数器，针对客户端每一个事务请求，计数器加 1；而高 32 位则代表 Leader 周期 epoch 的编号，每个当选产生一个新的 Leader 服务器，就会从这个 Leader 服务器上取出其本地日志中最大事务的ZXID，并从中读取 epoch 值，然后加 1，以此作为新的 epoch，并将低 32 位从 0 开始计数。

- epoch：可以理解为当前集群所处的年代或者周期，每个 leader 就像皇帝，都有自己的年号，所以每次改朝换代，leader 变更之后，都会在前一个年代的基础上加 1。这样就算旧的 leader 崩溃恢复之后，也没有人听他的了，因为 follower 只听从当前年代的 leader 的命令。*







### 01、myid

每个ZooKeeper服务器，都需要在数据文件夹下创建一个名为myid的文件，该文件包含整个ZooKeeper集群唯一的ID（整数）。例如，某ZooKeeper集群包含三台服务器，hostname分别为zoo1、zoo2和zoo3，其myid分别为1、2和3，则在配置文件中其ID与hostname必须一一对应，如下所示。在该配置文件中，server.后面的数据即为myid

```
server.1=zoo1:2888:3888

server.2=zoo2:2888:3888

server.3=zoo3:2888:3888
```

### 02、zxid

类似于RDBMS中的事务ID，用于标识一次更新操作的Proposal ID。为了保证顺序性，该zkid必须单调递增。因此ZooKeeper使用一个64位的数来表示，高32位是Leader的epoch，从1开始，每次选出新的Leader，epoch加一。低32位为该epoch内的序号，每次epoch变化，都将低32位的序号重置。这样保证了zkid的全局递增性。


### 03、服务器状态


- LOOKING 不确定Leader状态。该状态下的服务器认为当前集群中没有Leader，会发起Leader选举。

- FOLLOWING 跟随者状态。表明当前服务器角色是Follower，并且它知道Leader是谁。

- LEADING 领导者状态。表明当前服务器角色是Leader，它会维护与Follower间的心跳。

- OBSERVING 观察者状态。表明当前服务器角色是Observer，与Folower唯一的不同在于不参与选举，也不参与集群写操作时的投票。

 
### 04、选票数据结构

每个服务器在进行领导选举时，会发送如下关键信息：

- logicClock 每个服务器会维护一个自增的整数，名为logicClock，它表示这是该服务器发起的第多少轮投票。[这里应该理解为epoch？？？]

- state 当前服务器的状态

- self_id 当前服务器的myid

- self_zxid 当前服务器上所保存的数据的最大zxid

- vote_id 被推举的服务器的myid

- vote_zxid 被推举的服务器上所保存的数据的最大zxid

### 05、投票流程

- 1、自增选举轮次

ZooKeeper规定所有有效的投票都必须在同一轮次中。每个服务器在开始新一轮投票时，会先对自己维护的logicClock进行自增操作。

- 2、初始化选票

每个服务器在广播自己的选票前，会将自己的投票箱清空。该投票箱记录了所收到的选票。例：服务器2投票给服务器3，服务器3投票给服务器1，则服务器1的投票箱为(2, 3), (3, 1), (1, 1)。票箱中只会记录每一投票者的最后一票，如投票者更新自己的选票，则其它服务器收到该新选票后会在自己票箱中更新该服务器的选票。

- 3、发送初始化选票

每个服务器最开始都是通过广播把票投给自己。

- 4、接收外部投票

服务器会尝试从其它服务器获取投票，并记入自己的投票箱内。如果无法获取任何外部投票，则会确认自己是否与集群中其它服务器保持着有效连接。如果是，则再次发送自己的投票；如果否，则马上与之建立连接。

- 5、判断选举轮次

收到外部投票后，首先会根据投票信息中所包含的logicClock来进行不同处理：


1、外部投票的logicClock大于自己的logicClock。说明该服务器的选举轮次落后于其它服务器的选举轮次，立即清空自己的投票箱并将自己的logicClock更新为收到的logicClock，然后再对比自己之前的投票与收到的投票以确定是否需要变更自己的投票，最终再次将自己的投票广播出去。

2、外部投票的logicClock小于自己的logicClock。当前服务器直接忽略该投票，继续处理下一个投票。

3、外部投票的logickClock与自己的相等。当时进行选票PK。

 

- 6、选票PK


选票PK是基于(self_id, self_zxid)与(vote_id, vote_zxid)的对比：

1、外部投票的logicClock大于自己的logicClock，则将自己的logicClock及自己的选票的logicClock变更为收到的logicClock

2、若logicClock一致，则对比二者的vote_zxid，若外部投票的vote_zxid比较大，则将自己的票中的vote_zxid与vote_myid更新为收到的票中的vote_zxid与vote_myid并广播出去，另外将收到的票及自己更新后的票放入自己的票箱。如果票箱内已存在(self_myid, self_zxid)相同的选票，则直接覆盖

3、若二者vote_zxid一致，则比较二者的vote_myid，若外部投票的vote_myid比较大，则将自己的票中的vote_myid更新为收到的票中的vote_myid并广播出去，另外将收到的票及自己更新后的票放入自己的票箱

- 7、统计选票

如果已经确定有过半服务器认可了自己的投票（可能是更新后的投票），则终止投票。否则继续接收其它服务器的投票。

- 8、更新服务器状态

投票终止后，服务器开始更新自身状态。若过半的票投给了自己，则将自己的服务器状态更新为LEADING，否则将自己的状态更新为FOLLOWING。

### 06、集群启动领导选举

> 1、初始投票给自己

集群刚启动时，所有服务器的logicClock都为1，zxid都为0。各服务器初始化后，都投票给自己，并将自己的一票存入自己的票箱，如下图所示。

![](../../pic/2020-03-15-09-17-14.png)


在上图中，(1, 1, 0)第一位数代表投出该选票的服务器的logicClock，第二位数代表被推荐的服务器的myid，第三位代表被推荐的服务器的最大的zxid。由于该步骤中所有选票都投给自己，所以第二位的myid即是自己的myid，第三位的zxid即是自己的zxid。

此时各自的票箱中只有自己投给自己的一票。

> 2、更新选票

服务器收到外部投票后，进行选票PK，相应更新自己的选票并广播出去，并将合适的选票存入自己的票箱，如下图所示。

![](../../pic/2020-03-15-09-18-37.png)

服务器1收到服务器2的选票（1, 2, 0）和服务器3的选票（1, 3, 0）后，由于所有的logicClock都相等，所有的zxid都相等，因此根据myid判断应该将自己的选票按照服务器3的选票更新为（1, 3, 0），并将自己的票箱全部清空，再将服务器3的选票与自己的选票存入自己的票箱，接着将自己更新后的选票广播出去。此时服务器1票箱内的选票为(1, 3)，(3, 3)。

同理，服务器2收到服务器3的选票后也将自己的选票更新为（1, 3, 0）并存入票箱然后广播。此时服务器2票箱内的选票为(2, 3)，(3, ,3)。

服务器3根据上述规则，无须更新选票，自身的票箱内选票仍为（3, 3）。

服务器1与服务器2更新后的选票广播出去后，由于三个服务器最新选票都相同，最后三者的票箱内都包含三张投给服务器3的选票。

> 3、根据选票确定角色

根据上述选票，三个服务器一致认为此时服务器3应该是Leader。因此服务器1和2都进入FOLLOWING状态，而服务器3进入LEADING状态。之后Leader发起并维护与Follower间的心跳。

![](../../pic/2020-03-15-09-19-33.png)



### 07、Follower重启选举
 
> 1、Follower重启投票给自己

Follower重启，或者发生网络分区后找不到Leader，会进入LOOKING状态并发起新的一轮投票。

![](../../pic/2020-03-15-09-26-08.png)

 
> 2、发现已有Leader后成为Follower


服务器3收到服务器1的投票后，将自己的状态LEADING以及选票返回给服务器1。服务器2收到服务器1的投票后，将自己的状态FOLLOWING及选票返回给服务器1。此时服务器1知道服务器3是Leader，并且通过服务器2与服务器3的选票可以确定服务器3确实得到了超过半数的选票。因此服务器1进入FOLLOWING状态。

![](../../pic/2020-03-15-09-26-42.png)


### 08、Leader重启选举

> 1、Follower发起新投票

Leader（服务器3）宕机后，Follower（服务器1和2）发现Leader不工作了，因此进入LOOKING状态并发起新的一轮投票，并且都将票投给自己。

![](../../pic/2020-03-15-09-28-10.png)

> 2、广播更新选票


服务器1和2根据外部投票确定是否要更新自身的选票。这里有两种情况：


- 1、服务器1和2的zxid相同。例如在服务器3宕机前服务器1与2完全与之同步。此时选票的更新主要取决于myid的大小

- 2、服务器1和2的zxid不同。在旧Leader宕机之前，其所主导的写操作，只需过半服务器确认即可，而不需所有服务器确认。换句话说，服务器1和2可能一个与旧Leader同步（即zxid与之相同）另一个不同步（即zxid比之小）。此时选票的更新主要取决于谁的zxid较大

在上图中，服务器1的zxid为11，而服务器2的zxid为10，因此服务器2将自身选票更新为（3, 1, 11），如下图所示。

![](../../pic/2020-03-15-09-29-10.png) 

 

> 3、选出新Leader

经过上一步选票更新后，服务器1与服务器2均将选票投给服务器1，因此服务器2成为Follower，而服务器1成为新的Leader并维护与服务器2的心跳。

![](../../pic/2020-03-15-09-31-06.png)


> 4、旧Leader恢复后发起选举


旧的Leader恢复后，进入LOOKING状态并发起新一轮领导选举，并将选票投给自己。此时服务器1会将自己的LEADING状态及选票（3, 1, 11）返回给服务器3，而服务器2将自己的FOLLOWING状态及选票（3, 1, 11）返回给服务器3。如下图所示。

![](../../pic/2020-03-15-09-30-10.png)

> 5、旧Leader成为Follower


服务器3了解到Leader为服务器1，且根据选票了解到服务器1确实得到过半服务器的选票，因此自己进入FOLLOWING状态。

![](../../pic/2020-03-15-09-31-43.png)



### 09、Commit过的数据不丢失

> 1、Failover前状态

为更好演示Leader Failover过程，本例中共使用5个ZooKeeper服务器。A作为Leader，共收到P1、P2、P3三条消息，并且Commit了1和2，且总体顺序为P1、P2、C1、P3、C2。根据顺序性原则，其它Follower收到的消息的顺序肯定与之相同。其中B与A完全同步，C收到P1、P2、C1，D收到P1、P2，E收到P1，如下图所示。

![](../../pic/2020-03-15-09-33-12.png)


这里要注意：

- 由于A没有C3，意味着收到P3的服务器的总个数不会超过一半，也即包含A在内最多只有两台服务器收到P3。在这里A和B收到P3，其它服务器均未收到P3

- 由于A已写入C1、C2，说明它已经Commit了P1、P2，因此整个集群有超过一半的服务器，即最少三个服务器收到P1、P2。在这里所有服务器都收到了P1，除E外其它服务器也都收到了P2

问题：会不会出现B中和A不完全同步，假如只有 P1,P2,C1,P3/ P1,P2,C1/P1,P2这三种情况？

> 2、选出新Leader

旧Leader也即A宕机后，其它服务器根据上述FastLeaderElection算法选出B作为新的Leader。C、D和E成为Follower且以B为Leader后，会主动将自己最大的zxid发送给B，B会将Follower的zxid与自身zxid间的所有被Commit过的消息同步给Follower，如下图所示。

![](../../pic/2020-03-15-09-36-17.png)


在上图中：

- P1和P2都被A Commit，因此B会通过同步保证P1、P2、C1与C2都存在于C、D和E中

- P3由于未被A Commit，同时幸存的所有服务器中P3未存在于大多数据服务器中，因此它不会被同步到其它Follower

> 3、通知Follower可对外服务

同步完数据后，B会向D、C和E发送NEWLEADER命令并等待大多数服务器的ACK（下图中D和E已返回ACK，加上B自身，已经占集群的大多数），然后向所有服务器广播UPTODATE命令。收到该命令后的服务器即可对外提供服务。

![](../../pic/2020-03-15-09-39-00.png)

 ### 10、未Commit过的消息对客户端不可见

在上例中，P3未被A Commit过，同时因为没有过半的服务器收到P3，因此B也未Commit P3（如果有过半服务器收到P3，即使A未Commit P3，B会主动Commit P3，即C3），所以它不会将P3广播出去。

具体做法是，B在成为Leader后，先判断自身未Commit的消息（本例中即P3）是否存在于大多数服务器中从而决定是否要将其Commit。然后B可得出自身所包含的被Commit过的消息中的最小zxid（记为min_zxid）与最大zxid（记为max_zxid）。C、D和E向B发送自身Commit过的最大消息zxid（记为max_zxid）以及未被Commit过的所有消息（记为zxid_set）。B根据这些信息作出如下操作：

- 如果Follower的max_zxid与Leader的max_zxid相等，说明该Follower与Leader完全同步，无须同步任何数据

- 如果Follower的max_zxid在Leader的(min_zxid，max_zxid)范围内，Leader会通过TRUNC命令通知Follower将其zxid_set中大于Follower的max_zxid（如果有）的所有消息全部删除

上述操作保证了未被Commit过的消息不会被Commit从而对外不可见。

上述例子中Follower上并不存在未被Commit的消息。但可考虑这种情况，如果将上述例子中的服务器数量从五增加到七，服务器F包含P1、P2、C1、P3，服务器G包含P1、P2。此时服务器F、A和B都包含P3，但是因为票数未过半，因此B作为Leader不会Commit P3，而会通过TRUNC命令通知F删除P3。如下图所示。

![](../../pic/2020-03-15-09-43-44.png)

>  本节总结
 
- 由于使用主从复制模式，所有的写操作都要由Leader主导完成，而读操作可通过任意节点完成，因此ZooKeeper读性能远好于写性能，更适合读多写少的场景

- 虽然使用主从复制模式，同一时间只有一个Leader，但是Failover机制保证了集群不存在单点失败（SPOF）的问题

- ZAB协议保证了Failover过程中的数据一致性

- 服务器收到数据后先写本地文件再进行处理，保证了数据的持久性



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