

<!-- TOC -->

- [1、概念](#1概念)
    - [1、RowKey（行键），字典序](#1rowkey行键字典序)
    - [2、Column Family（列族），表 Schema](#2column-family列族表-schema)
    - [3、Column （列），从属于某个列族](#3column-列从属于某个列族)
    - [4、Version Number（版本号）默认时间戳降序](#4version-number版本号默认时间戳降序)
    - [5、cell由（rowkey,column,version）唯一确定，字节码（对应key）](#5cell由rowkeycolumnversion唯一确定字节码对应key)
    - [6、逻辑存储结构](#6逻辑存储结构)
- [2、HBase 集群结构](#2hbase-集群结构)
    - [1、Client](#1client)
    - [2、Master](#2master)
    - [3、RegionServer](#3regionserver)
        - [1、Region Server的结构](#1region-server的结构)
        - [2、HBase的写入流程](#2hbase的写入流程)
        - [3、HBase MemStore](#3hbase-memstore)
        - [4、HBase Region Flush](#4hbase-region-flush)
        - [5、HBase HFile and Structure](#5hbase-hfile-and-structure)
        - [6、HFile索引](#6hfile索引)
        - [7、HBase的混合读(Read Merge)](#7hbase的混合读read-merge)
        - [8、HBase Minor Compaction](#8hbase-minor-compaction)
        - [9、HBase Major Compaction](#9hbase-major-compaction)
    - [4、Zookeeper](#4zookeeper)
    - [5、HLog(WAL log)](#5hlogwal-log)
    - [6、Region](#6region)
    - [7、Memstore 与 storefile](#7memstore-与-storefile)
    - [8、HBase Meta Table](#8hbase-meta-table)
- [3、Hbase读取数据的过程](#3hbase读取数据的过程)
- [4、HBase 写入数据的过程](#4hbase-写入数据的过程)
- [5、HDFS的数据复制](#5hdfs的数据复制)
- [6、HBase的灾难恢复](#6hbase的灾难恢复)
- [7、数据恢复](#7数据恢复)
- [8、region定位](#8region定位)
- [参考](#参考)

<!-- /TOC -->



# 1、概念


按照CAP理论，HBase属于C+P类型的系统。HBase是强一致性的（仅支持单行事务）。每一行由单个区域服务器（region server）host，行锁（row locks）和多版本并发控制(multiversion concurrency control)的组合被用来保证行的一致性。



HBase 名称的由来是由于其作为 Hadoop Database 存在的，用来存储非结构化、半结构化数据。

![](../../pic/2020-05-04-23-07-20.png)

HBase 是构建在 HDFS 之上的，这是由于 HBase 内部管理的文件全部都是存储在 HDFS 当中的。

![](../../pic/2020-05-04-23-08-16.png)

基于 BigTable 这个数据模型开发的，因此也是具有 Key-Value 特征的，同时也就具有 Bigtable 稀疏的、面向列的这些特性。

也是由于 HBase 利用 HDFS 作为它的文件系统，因此它也具有 HDFS 的高可靠性和可伸缩性。和 Hadoop 一样，HBase 也是依照横向扩展，通过不断地通过添加廉价的服务器来增加计算和存储的能力。BigTable 利用 Chubby 来进行协同服务，HBase 则是利用 Zookeeper 来对整个分布式系统进行协调服务。正是因为通过HDFS 的高可靠可伸缩性，以及应用了 Bigtable 的稀疏的面向列的这些高效的数据组织形式。所以 HBase 才能如此地适合大数据随机和实时读写。


> 特点

- 强一致性读写: HBase 不是 “最终一致性(eventually consistent)” 数据存储这让它很适合高速计数聚合类任务。
- 自动分片(Automatic sharding):HBase表通过region分布在集群中。数据增长时，region会自动分割并重新分布。
- RegionServer 自动故障转移
- Hadoop/HDFS 集成: HBase 支持本机外HDFS 作为它的分布式文件系统。
- MapReduce: HBase 通过MapReduce支持大并发处理， HBase 可以同时做源和目标.
- Java 客户端 API: HBase 支持易于使用的 Java API 进行编程访问.
- Thrift/REST API:HBase 也支持Thrift和 REST 作为非Java 前端.
- Block Cache 和 Bloom Filters: 对于大容量查询优化， HBase支持 Block Cache 和 Bloom Filters。
运维管理: HBase提供内置网页用于运维视角和JMX 度量.



## 1、RowKey（行键），字典序

顾名思义也就是我们在关系型数据库中常见的主键，它是Unique 的，在 HBase 中这个主键可以是任意的字符串，其最大长度是64K，在内部存储中会被存储为字节数组，HBase 表中的数据是按照 RowKey 的字典序排列的，例如很多索引的实现，包括地理空间索引很大程度就是依赖这个特性。

不过也要注意一个点，现实当中期望排序是1、2、3、4...10，而在 HBase 中1 后面紧跟的会是10。因此，在设计行键的时候一定要充分地利用字典序这个特性，将一下经常读取的行存储到一起或者靠近，减少Scan 的耗时，提高读取的效率。这里一定要说的一点是，行键设计真的很重要，例如做组合行键时将时间排前面，导致写热点


## 2、Column Family（列族），表 Schema

它是由若干列构成，是表 Schema 的一部分，所以需要在创建表的时候就指定好。但也不是所表创建完之后就不能更改列族，只是成本会比较大，因此不建议更改。HBase 中可允许定义的列族个数最多就20多个。列族不仅仅能够帮助我们构建数据的语义边界，还能有助于我们设置某些特性，比如可以指定某个列族内数据的压缩形式。一个列族包含的所有列在物理存储上都是在同一个底层的存储文件当中。


## 3、Column （列），从属于某个列族

Column （列），一般都是从属于某个列族，跟列族不一样，列的数量一般的没有强限制的，一个列族当中可以有数百万个列，而且这些列都可以动态添加的。这也是我们常说的 HBase 面向列的优点，不像传统的关系型数据库，调整一下 Schema 都需要担心对于生产的影响。

## 4、Version Number（版本号）默认时间戳降序

Version Number（版本号），HBase 中每一列的值或者说是每个单元格的值都是具有版本号的，默认使用的系统当前的时间戳，精确到毫秒。当然也可以是用户自己显式地设置，我们是通过时间戳来识别不同的版本，因此如果要自己设置的话，也要保证版本号的唯一性。用户也可以指定保存指定单元格的最后 N 个版本，或者某个时间段的版本，这个是可以在配置中配置的。一个单元格里面是数据是按照版本号降序的。也就是说最后写入的值会被最先读取。

## 5、cell由（rowkey,column,version）唯一确定，字节码（对应key）

Cell（单元格），一个单元格就是由前面说的行键、列标示、版本号唯一确定的，这里说的列标示包括列族和列名。Cell 中的数据是没有类型的，全部都是字节码。

## 6、逻辑存储结构

![](../../pic/2020-05-04-23-52-25.png)

在本图中，列簇（Column Family）对应的值就是 info 和 area ，列（ Column 或者称为 Qualifier ）对应的就是 name 、 age 、 country 和 city ，Row key 对应的就是 Row 1 和 Row 2，Cell 对应的就是具体的值。

看完这张图，是不是有点疑惑，怎么获取其中的一条数据呢？既然 HBase 是 KV 的数据库，那么当然是以获取 KEY 的形式来获取到 Value 啦。在 HBase 中的 KEY 组成是这样的：

![](../../pic/2020-05-04-23-53-00.png)


KEY 的组成是以 Row key 、CF(Column Family) 、Column 和 TimeStamp 组成的。

TimeStamp 在 HBase 中充当的作用就是版本号，因为在 HBase 中有着数据多版本的特性，所以同一个 KEY 可以有多个版本的 Value 值（可以通过配置来设置多少个版本）。查询的话是默认取回最新版本的那条数据，但是也可以进行查询多个版本号的数据。




> 特点

![](../../pic/2020-05-04-23-19-02.png)



> 1、优点

- 强一致模型:当写返回时, 确保所有读操作读到相同的值

- 自动扩展:1、数据增长过大时, 自动分裂region；2、利用HFDS分散数据和备份数据

- 内建自动恢复：预写日志(WAL)

- 集成Hadoop生态：在HBase上运行map reduce



> Apache HBase存在的问题
- Business continuity reliability:
- 重放预写日志慢
- 故障恢复既慢又复杂
- Major compaction容易引起IO风暴(写放大)



# 2、HBase 集群结构

![](../../pic/2020-05-04-23-21-18.png)

![](../../pic/2020-05-04-23-34-26.png)


一个 HBase 集群一般由一个 Master 和多个 RegionServer ,以及zookeeper组成。

## 1、Client

客户端库：可以通过 HBase 提供的各式语言API 库访问集群。API 库也会维护一个本地缓存来加快对 HBase 对访问，比如缓存中记录着 Region 的位置信息。

## 2、Master

Maste 节点：主要为各个 RegionServer 分配 Region，负责 RegionServer 对负载均衡，管理用户对于 Table 对 CRUD 操作。

![](../../pic/2020-05-05-00-04-15.png)




## 3、RegionServer

RegionServer：维护 Region，处理对这些 Region 对IO 请求，负责切分在运行过程中变过大的 Region。

HBase的表根据Row Key的区域分成多个Region, 一个Region包含这这个区域内所有数据. 而Region server负责管理多个Region, 负责在这个Region server上的所有region的读写操作. 一个Region server最多可以管理1000个region.

![](../../pic/2020-05-05-00-03-06.png)


### 1、Region Server的结构

Region Server运行在HDFS的data node上面, 它有下面4个部分组成:

- WAL: 预写日志(Write Ahead Log)是一HDFS上的一个文件, 如果region server崩溃后, 日志文件用来恢复新写入但是还没有存储在硬盘上的数据.

- BlockCache: 读取缓存, 在内存里缓存频繁读取的数据, 如果BlockCache满了, 会根据LRU算法(Least Recently Used)选出最不活跃的数据, 然后释放掉

- MemStore: 写入缓存, 在数据真正被写入硬盘前, Memstore在内存中缓存新写入的数据. 每个region的每个列簇(column family)都有一个memstore. memstore的数据在写入硬盘前, 会先根据key排序, 然后写入硬盘.

- HFiles: HDFS上的数据文件, 里面存储KeyValue对.

![](../../pic/2020-05-05-00-17-15.png)


### 2、HBase的写入流程

当hbase客户端发起Put请求, 第一步是将数据写入预写日志(WAL):

- 1、将修改的操作记录在预写日志(WAL)的末尾
- 2、预写日志(WAL)被用来在region server崩溃时, 恢复memstore中的数据

![](../../pic/2020-05-05-00-18-53.png)

- 3、数据写入预写日志(WAL), 并存储在memstore之后, 向用户返回写成功.

![](../../pic/2020-05-05-00-19-21.png)


### 3、HBase MemStore

MemStore在内存按照Key的顺序, 存储Key-Value对, 一个Memstore对应一个列簇(column family). 同样在HFile里面, 所有的Key-Value对也是根据Key有序存储.

![](../../pic/2020-05-05-00-21-43.png)


### 4、HBase Region Flush

译注: 原文里面Flush的意识是, 把缓冲的数据从内存 转存 到硬盘里, 这就类似与冲厕所(Flush the toilet) , 把数据比作是水, 一下把积攒的水冲到下水道, 想当于把缓存的数据写入硬盘. 


当Memstore累计了足够多的数据, Region server将Memstore中的数据写入HDFS, 存储为一个HFile. 每个列簇(column family)对应多个HFile, 每个HFile里面就是实际存储的数据.

这些HFile都是当Memstore满了以后, Flush到HDFS中的文件. 注意到HBase限制了列簇(column family)的个数. 因为每个列簇(column family)都对应一个Memstore. [译注: 太多的memstore占用过多的内存].

当Memstore的数据Flush到硬盘时, 系统额外保存了最后写入操作的序列号(last written squence number), 所以HBase知道有多少数据已经成功写入硬盘. 每个HFile都记录这个序号, 表明这个HFile记录了多少数据和从哪里继续写入数据.

在region server启动后, 读取所有HFile中最高的序列号, 新的写入序列号从这个最高序列号继续向上累加.

![](../../pic/2020-05-05-00-24-03.png)


### 5、HBase HFile and Structure

HFile中存储有序的Key-Value对. 当Memstore满了之后, Memstore中的所有数据写入HDFS中,形成一个新的HFile. 这种大文件写入是顺序写, 因为避免了机械硬盘的磁头移动, 所以写入速度非常快.

![](../../pic/2020-05-05-00-24-59.png)



HFile存储了一个多级索引(multi-layered index), 查询请求不需要遍历整个HFile查询数据, 通过多级索引就可以快速得到数据(工作机制类似于b+tree)

- Key-Value按照升序排列
- Key-Value存储在以64KB为单位的Block里
- 每个Block有一个叶索引(leaf-index), 记录Block的位置
- 每个Block的最后一个Key(译注: 最后一个key也是最大的key), 放入中间索引(intermediate index)
- 根索引(root index)指向中间索引


尾部指针(trailer pointer)在HFile的最末尾, 它指向元数据块区(meta block), 布隆过滤器区域和时间范围区域. 查询布隆过滤器可以很快得确定row key是否在HFile内, 时间范围区域也可以帮助查询跳过不在时间区域的读请求.



译注: 布隆过滤器在搜索和文件存储中有广泛用途

![](../../pic/2020-05-05-00-28-00.png)

备注：这里针对每一个hfile构建了一个稀疏索引。





### 6、HFile索引

当打开HFile后, 系统自动缓存HFile的索引在Block Cache里, 这样后续查找操作只需要一次硬盘的寻道.

![](../../pic/2020-05-05-00-33-18.png)


### 7、HBase的混合读(Read Merge)

我们发现HBase中的一个row里面的数据, 分配在多个地方. 已经持久化存储的Cell在HFile, 最近写入的Cell在Memstore里, 最近读取的Cell在Block cache里. 所以当你读HBase的一行时, 混合了Block cache, memstore和Hfiles的读操作

- 1、首先, 在Block cache(读cache)里面查找cell, 因为最近的读取操作都会缓存在这里. 如果找到就返回, 没有找到就执行下一步

- 2、其次, 在memstore(写cache)里查找cell, memstore里面存储里最近的新写入, 如果找到就返回, 没有找到就执行下一步

- 3、最后, 在读写cache中都查找失败的情况下, HBase查询Block cache里面的Hfile索引和布隆过滤器, 查询有可能存在这个cell的HFile, 最后在HFile中找到数据.

![](../../pic/2020-05-05-00-35-15.png)


### 8、HBase Minor Compaction

HBase自动选择较小的HFile, 将它们合并成更大的HFile. 这个过程叫做minor compaction. Minor compaction通过合并小HFile, 减少HFile的数量.

HFile的合并采用归并排序的算法.

译注: 较少的HFile可以提高HBase的读性能

![](../../pic/2020-05-05-00-36-28.png)


### 9、HBase Major Compaction

Major compaction指一个region下的所有HFile做归并排序, 最后形成一个大的HFile. 这可以提高读性能. 但是, major compaction重写所有的Hfile, 占用大量硬盘IO和网络带宽. 这也被称为写放大现象(write amplification)

Major compaction可以被调度成自动运行的模式, 但是由于写放大的问题(write amplification), major compaction通常在一周执行一次或者只在凌晨运行. 此外, major compaction的过程中, 如果发现region server负责的数据不在本地的HDFS datanode上, major compaction除了合并文件外, 还会把其中一份数据转存到本地的data node上.

















## 4、Zookeeper

- 保证任何时候，集群中只有一个master
- 存贮所有Region的寻址入口。
- 实时监控Region server的上线和下线信息。并实时通知Master
- 存储HBase的schema和table元数据

![](../../pic/2020-05-05-00-04-54.png)


> ZooKeeper, Master和 Region server协同工作

Zookeepr负责维护集群的memberlist, 哪台服务器在线,哪台服务器宕机,都由zookeeper探测和管理. Region server, 主备Master节点主动连接Zookeeper, 维护一个Session连接,

这个session要求定时发送heartbeat, 向zookeeper说明自己在线, 并没有宕机.

![](../../pic/2020-05-05-00-07-13.png)

ZooKeeper有一个Ephemeral Node(临时节点)的概念, session连接在zookeeper中建立一个临时节点(Ephemeral Node), 如果这个session断开, 临时节点被自动删除.

所有Region server都尝试连接Zookeeper, 并在这个session中建立一个临时节点(Ephemeral node). HBase的master节点监控这些临时节点的是否存在, 可以发现新加入region server和判断已经存在的region server宕机.

为了高可用需求, HBase的master也有多个, 这些master节点也同时向Zookeeper注册临时节点(Ephemeral Node). Zookeeper把第一个成功注册的master节点设置成active状态, 而其他master node处于inactive状态.


如果zookeeper规定时间内, 没有收到active的master节点的heartbeat, 连接session超时, 对应的临时节也自动删除. 之前处于Inactive的master节点得到通知, 马上变成active状态, 立即提供服务.

同样, 如果zookeeper没有及时收到region server的heartbeat, session过期, 临时节点删除. HBase master得知region server宕机, 启动数据恢复方案.


## 5、HLog(WAL log)

HLog文件就是一个普通的Hadoop Sequence File，Sequence File 的Key是HLogKey对象，HLogKey中记录了写入数据的归属信息，除了table和 region名字外，同时还包括sequence number和timestamp，timestamp是” 写入时间”，sequence number的起始值为0，或者是最近一次存入文件系 统中sequence number。

HLog SequeceFile的Value是HBase的KeyValue对象，即对应HFile中的 KeyValue


## 6、Region

HBase自动把表水平划分成多个区域(region)，每个region会保存一个表里面某段连续的数据；每个表一开始只有一个region，随着数据不断插入表，region不断增大，当增大到一个阀值的时候，region就会等分会 两个新的region（裂变）；

当table中的行不断增多，就会有越来越多的region。这样一张完整的表 被保存在多个Regionserver上。



Region = 一组连续key
快速的复习region的概念:

- 一张表垂直分割成一个或多个region, 一个region包括一组连续并且有序的row key, 每一个row key对应一行的数据.
- 每个region最大1GB(默认)
- region由region server管理
- 一个region server可以管理多个region, 最多大约1000个region(这些region可以属于相同的表,也可以属于不同的表)

![](../../pic/2020-05-05-00-38-46.png)





## 7、Memstore 与 storefile

![](../../pic/2020-05-04-23-41-54.png)

- 一个region由多个store组成，一个store对应一个CF（列族）

- store包括位于内存中的memstore和位于磁盘的storefile。写操作先写入memstore，当memstore中的数据达到某个阈值，hregionserver会启动flashcache进程写入storefile，每次写入形成单独的一个storefile

- 当storefile文件的数量增长到一定阈值后，系统会进行合并（minor、 major compaction），在合并过程中会进行版本合并和删除工作 （majar），形成更大的storefile。

- 当一个region所有storefile的大小和超过一定阈值后，会把当前的region分割为两个，并由hmaster分配到相应的regionserver服务器，实现负载均衡。

- 客户端检索数据，先在memstore找，找不到再找storefile

- HRegion是HBase中分布式存储和负载均衡的最小单元。最小单元就表示不同的HRegion可以分布在不同的HRegion server上。

- HRegion由一个或者多个Store组成，每个store保存一个columns family。
每个Strore又由一个memStore和0至多个StoreFile组成。



## 8、HBase Meta Table

Meta table存储所有region的列表。Meta table用类似于Btree的方式存储。

Meta table的结构如下:

- Key: region的开始row key, region id

- Values: Region server

![](../../pic/2020-05-05-00-14-11.png)

译注: 在google的bigtable论文中, bigtable采用了多级meta table, Hbase的Meta table只有2级






# 3、Hbase读取数据的过程

Client 请求读取数据时，先转发到 ZK 集群，在 ZK 集群中寻找到相对应的 Region Server，再找到对应的 Region，先是查 MemStore，如果在 MemStore 中获取到数据，那么就会直接返回，否则就是再由 Region 找到对应的 Store File，从而查到具体的数据。

在 Client 端会进行 rowkey-> HRegion 映射关系的缓存，降低下次寻址的压力。


# 4、HBase 写入数据的过程

先是 Client 进行发起数据的插入请求，如果 Client 本身存储了关于 Rowkey 和 Region 的映射关系的话，那么就会先查找到具体的对应关系，如果没有的话，就会在ZK中进行查找到对应 Region server，然后再转发到具体的 Region 上。所有的数据在写入的时候先是记录在 WAL 中，同时检查关于 MemStore 是否满了，如果是满了，那么就会进行刷盘，输出到一个 Hfile 中，如果没有满的话，那么就是先写进 Memstore 中，然后再刷到 WAL 中。


# 5、HDFS的数据复制

所有读写都是操作primary node. HDFS自动复制所有WAL和HFile的数据块到其他节点. HBase依赖HDFS保证数据安全. 当在HDFS里面写入一个文件时, 一份存储在本地节点, 另两份存储到其他节点

![](../../pic/2020-05-05-00-40-37.png)

预写日志(WAL) 和 HFile都存在HDFS里面, 可以保证数据的可靠性, 但是HBase memstore里的数据都在内存中, 如果系统崩溃后重启, Hbase如何恢复Memstore里面的数据?

![](../../pic/2020-05-05-00-41-11.png)


# 6、HBase的灾难恢复


当region server宕机, 崩溃的region server管理的region不能再提供服务, HBase监测到异常后, 启动恢复程序, 恢复region.

Zookeeper发现region server的heartbeat停止, 判断region server宕机并通知master节点. Hbase master节点得知该region server停机后, 将崩溃的region server管理的region分配给其他region server. HBase从预写文件(WAL)里恢复memstore里的数据.

HBase master知道老的region被重新分配到哪些新的region server. Master把已经crash的Region server的预写日志(WAL)拆分成多个. 参与故障恢复的每个region server重放的预写日志(WAL), 重新构建出丢失Memstore.


![](../../pic/2020-05-05-00-46-07.png)





# 7、数据恢复


预写日志(WAL)记录了HBase的每个操作, 每个操作代表一个Put或者删除Delete动作. 所有的操作按照时间顺序在预写日志(WAL)排列, 文件头记录最老的操作, 最新的操作处于文件末尾.

如何恢复在memstore里, 但还没有写到HFile的数据? 重新执行预写日志(WAL)就可以. 从前到后依次执行预写日志(WAL)里的操作, 重建memstore数据. 最后, Flush memstore数据到的HFile, 完成恢复.


![](../../pic/2020-05-05-00-46-19.png)


# 8、region定位

系统如何找到某个row key (或者某个 row key range)所在的region

bigtable 使用三层类似B+树的结构来保存region位置。

0 第一层是保存zookeeper里面的文件，它持有root region的位置。

- 第二层root region是.META.表的第一个region，其中保存了.META.z表其它region的位置。通过root region，我们就可以访问.META.表的数据。

- .META.是第三层，它是一个特殊的表，保存了hbase中所有数据表的region 位置信息。

![](../../pic/2020-05-05-09-24-50.png)

说明：

- 1 root region永远不会被split，保证了最需要三次跳转，就能定位到任意region 。

- 2.META.表每行保存一个region的位置信息，row key 采用表名+表的最后一样编码而成。

- 3 为了加快访问，.META.表的全部region都保存在内存中。

假设，.META.表的一行在内存中大约占用1KB。并且每个region限制为128MB。

那么上面的三层结构可以保存的region数目为：

(128MB/1KB) * (128MB/1KB) = = 2(34)个region

- 4 client会将查询过的位置信息保存缓存起来，缓存不会主动失效，因此如果client上的缓存全部失效，则需要进行6次网络来回，才能定位到正确的region(其中三次用来发现缓存失效，另外三次用来获取位置信息)。



# 参考

- [你应该知道的 HBase 基础，都在这儿了](https://zhuanlan.zhihu.com/p/63264106)

- [Hbase系统架构及数据结构](https://www.open-open.com/lib/view/open1346821084631.html)

- [深度分析HBase架构](https://zhuanlan.zhihu.com/p/30414252)

- [Hbase架构与原理](https://zhuanlan.zhihu.com/p/29674705)

- [一文讲清HBase存储结构](https://zhuanlan.zhihu.com/p/54184168)










