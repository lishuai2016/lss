


<!-- TOC -->

- [1、概念](#1概念)
- [2、架构](#2架构)
- [3、 HDFS 文件存储](#3-hdfs-文件存储)
    - [1、写数据的过程](#1写数据的过程)
- [4、MapReduce](#4mapreduce)
    - [1、MapReduce流程](#1mapreduce流程)
    - [2、shuffle](#2shuffle)
        - [1、Map端shuffle](#1map端shuffle)
        - [2、Reduce端shuffle](#2reduce端shuffle)
        - [3、spark shuffle 优化过程](#3spark-shuffle-优化过程)
            - [1、Hash based shuffle v1](#1hash-based-shuffle-v1)
            - [2、Hash based shuffle v2（consolidation Shuffle）](#2hash-based-shuffle-v2consolidation-shuffle)
            - [3、Sort based shuffle](#3sort-based-shuffle)
            - [4、BypassMergeSortShuffleWriter](#4bypassmergesortshufflewriter)
            - [5、Spark shuffle 调优](#5spark-shuffle-调优)
- [5、yarn](#5yarn)
    - [1、资源模型](#1资源模型)
    - [2、Scheduler](#2scheduler)
        - [1、Capacity Scheduler](#1capacity-scheduler)
        - [2、Fair Scheduler](#2fair-scheduler)
- [6、Hadoop RPC](#6hadoop-rpc)
- [问题](#问题)
    - [1、HDFS中块（block）的大小为什么设置为128M？](#1hdfs中块block的大小为什么设置为128m)

<!-- /TOC -->



# 1、概念

- Google发表的3篇论文:分布式文件系统 GFS、分布式计算框架 MapReduce、NoSQL数据库系统BigTable，俗称“三驾马车”

- Lucene开源项目的创始人 Doug Cutting基于Google论文实现了类似GFS和MapReduce的功能：HDFS和MapReduce。随后YARN发布，HDFS、MapRedue、YARN被视为Hadoop生态的基石：存储、计算、资源管理

- Facebook发布Hive，Hive 支持使用 SQL 语法来进行大数据计算，将SQL转换成MapReduce计算任务，大大降低了分布式计算系统的使用门槛

- NoSQL数据库：Hbase、Cassandra

- 数据适配&传输：Sqoop、Flume、Kafka

- 计算框架：Spark、Storm、Flink

- 分布式搜索：ElasticSearch

![](../../pic/2020-05-02-17-50-55.png)


![大数据技术生态体系](../../pic/2020-05-02-15-12-16.png)

![](../../pic/2020-05-02-17-51-32.png)











Hadoop组成
- 1）Hadoop HDFS：一个高可靠、高吞吐量的分布式文件系统。
- 2）HadoopMapReduce：一个分布式的离线并行计算框架。
- 3）HadoopYARN：作业调度与集群资源管理的框架。
- 4）Hadoop Common：支持其他模块的工具模块。

Hadoop1.0版本两个核心：HDFS+MapReduce

Hadoop2.0版本，引入了Yarn,增加了HA。核心：HDFS+Yarn+Mapreduce

- Yarn是资源调度框架。能够细粒度的管理和调度任务。此外，还能够支持其他的计算框架，比如spark等。

- HA主要指的是可以同时启动2个NameNode。其中一个处于工作（Active）状态，另一个处于随时待命（Standby）状态。这样，当一个NameNode所在的服务器宕机时，可以在数据不丢失的情况下，手工或者自动切换到另一个NameNode提供服务。

2019年6月hadoop已经推出3.x版本


hdfs部署：client+NameNode+DateNode+SecondaryNameNode

![](../../pic/2020-05-02-15-08-16.png)


# 2、架构

HDFS集群以Master-Slave模式运行，主要有两类节点：一个Namenode(即Master)和多个Datanode(即Slave)。　

Namenode 管理者文件系统的Namespace。它维护着文件系统树(filesystem tree)以及文件树中所有的文件和文件夹的元数据(metadata)。管理这些信息的文件有两个，分别是Namespace 镜像文件(Namespace image)和操作日志文件(edit log)，这些信息被Cache在RAM中，当然，这两个文件也会被持久化存储在本地硬盘。Namenode记录着每个文件中各个块所在的数据节点的位置信息，但是他并不持久化存储这些信息，因为这些信息会在系统启动时从数据节点重建。

客户端(client)代表用户与namenode和datanode交互来访问整个文件系统。客户端提供了一些列的文件系统接口，因此在编程时，几乎无须知道datanode和namenode，即可完成我们所需要的功能。

- NameNode：是Master节点，管理数据块映射；处理客户端的读写请求；配置副本策略；管理HDFS的名称空间；用户通过namenode来实现对其他数据的访问和操作，类似于root根目录的感觉；目录与数据块之间的关系（靠fsimage和edits来实现），数据块和节点之间的关系

- SecondaryNameNode：保存着NameNode的部分信息（不是全部信息NameNode宕掉之后恢复数据用），是NameNode的冷备份；合并fsimage和edits然后再发给namenode。（防止edits过大的一种解决方案）

- DataNode：负责存储client发来的数据块block；执行数据块的读写操作。是NameNode的小弟。

- fsimage:元数据镜像文件（HDFS的目录树。）

- edits：元数据的操作日志（针对文文件系统件系统做的修改操作记录）

namenode内存中存储的是=fsimage+edits。

定时合并edits和fsimage也是一个很耗费资源的操作，且合并的同时，查询操作就无法保证效率和正确性，从而引入了secondNameNode，将耗费资源的操作放到另一台节点中。

fsimage是HDFS文件系统存于硬盘中的元数据检查点，里面记录了自最后一次检查点之前HDFS文件系统中所有目录和文件的序列化信息；而edits保存了自最后一次检查点之后所有针对HDFS文件系统的操作

在NameNode启动时候，会先将fsimage中的文件系统元数据信息加载到内存，然后根据eidts中的记录将内存中的元数据同步至最新状态；所以，这两个文件一旦损坏或丢失，将导致整个HDFS文件系统不可用。

Namenode包含： 

fsimage文件与edits文件是Namenode结点上的核心文件
。
Namenode中仅仅存储目录树信息，而关于BLOCK的位置信息则是从各个Datanode上传到Namenode上的。

Namenode的目录树信息就是物理的存储在fsimage这个文件中的，当Namenode启动的时候会首先读取fsimage这个文件，将目录树信息装载到内存中。

而edits存储的是日志信息，在Namenode启动后所有对目录结构的增加，删除，修改等操作都会记录到edits文件中，并不会同步的记录在fsimage中。

而当Namenode结点关闭的时候，也不会将fsimage与edits文件进行合并，这个合并的过程实际上是发生在Namenode启动的过程中。

也就是说，当Namenode启动的时候，首先装载fsimage文件，然后在应用edits文件，最后还会将最新的目录树信息更新到新的fsimage文件中，然后启用新的edits文件。

整个流程是没有问题的，但是有个小瑕疵，就是如果Namenode在启动后发生的改变过多，会导致edits文件变得非常大，大得程度与Namenode的更新频率有关系。
那么在下一次Namenode启动的过程中，读取了fsimage文件后，会应用这个无比大的edits文件，导致启动时间变长，并且不可控，可能需要启动几个小时也说不定。
Namenode的edits文件过大的问题，也就是SecondeNamenode要解决的主要问题。
SecondNamenode会按照一定规则被唤醒，然后进行fsimage文件与edits文件的合并，防止edits文件过大，导致Namenode启动时间过长。

Namenode容错机制

没有Namenode，HDFS就不能工作。事实上，如果运行namenode的机器坏掉的话，系统中的文件将会完全丢失，因为没有其他方法能够将位于不同datanode上的文件块(blocks)重建文件。因此，namenode的容错机制非常重要，Hadoop提供了两种机制。

第一种方式是将持久化存储在本地硬盘的文件系统元数据备份。Hadoop可以通过配置来让Namenode将他的持久化状态文件写到不同的文件系统中。这种写操作是同步并且是原子化的。比较常见的配置是在将持久化状态写到本地硬盘的。同时，也写入到一个远程挂载的网络文件系统。

第二种方式是运行一个辅助的Namenode(Secondary Namenode)。 事实上Secondary Namenode并不能被用作Namenode它的主要作用是定期的将Namespace镜像与操作日志文件(edit log)合并，以防止操作日志文件(edit log)变得过大。通常，Secondary Namenode 运行在一个单独的物理机上，因为合并操作需要占用大量的CPU时间以及和Namenode相当的内存。辅助Namenode保存着合并后的Namespace镜像的一个备份，万一哪天Namenode宕机了，这个备份就可以用上了。但是辅助Namenode总是落后于主Namenode，所以在Namenode宕机时，数据丢失是不可避免的。在这种情况下，一般的，要结合第一种方式中提到的远程挂载的网络文件系统(NFS)中的Namenode的元数据文件来使用，把NFS中的Namenode元数据文件，拷贝到辅助Namenode，并把辅助Namenode作为主Namenode来运行。


Hdfs 和 RAID 在思想上是有一些相似之处的。都是通过水平拓展，比如 RAID 水平拓展磁盘，Hdfs 则是水平拓展机器。

RAID （Redundant Array of Independent Disks）即独立磁盘冗余阵列，简称为磁盘阵列，是用多个独立的磁盘组成在一起形成一个大的磁盘系统，从而实现比单块磁盘更好的存储性能和更高的可靠性。

传统的文件系统是单机的，不能横跨不同的机器。HDFS(Hadoop Distributed FileSystem)的设计本质上是为了大量的数据能横跨成百上千台机器，但是你看到的是一个文件系统而不是很多文件系统。比如你说我要获取/hdfs/tmp/file1的数据，你引用的是一个文件路径，但是实际的数据存放在很多不同的机器上。你作为用户，不需要知道这些，就好比在单机上你不关心文件分散在什么磁道什么扇区一样。HDFS为你管理这些数据。

# 3、 HDFS 文件存储

## 1、写数据的过程

![](../../pic/2020-05-02-16-12-10.png)

- 1>将64M的block1按64k的package划分;

- 2>然后将第一个package发送给host2;

- 3>host2接收完后，将第一个package发送给host1，同时client向host2发送第二个package；

- 4>host1接收完第一个package后，发送给host3，同时接收host2发来的第二个package。

- 5>以此类推，如图红线实线所示，直到将block1发送完毕。

- 6>host2,host1,host3向NameNode，host2向Client发送通知，说“消息发送完了”。如图粉红颜色实线所示。

- 7>client收到host2发来的消息后，向namenode发送消息，说我写完了。这样就真完成了。如图黄色粗实线

- 8>发送完block1后，再向host7，host8，host4发送block2，如图蓝色实线所示。

- 9>发送完block2后，host7,host8,host4向NameNode，host7向Client发送通知，如图浅绿色实线所示。

- 10>client向NameNode发送消息，说我写完了，如图黄色粗实线，这样就完毕了。


![](../../pic/2020-05-02-16-19-08.png)




# 4、MapReduce

## 1、MapReduce流程

![](../../pic/2020-05-02-17-14-05.png)

- 1、一个Map/Reduce作业（job）通常会把指定要处理数据集切分为若干独立的数据块，map调用InputFormat()方法来生成可处理的<key,value>对.

- 2、Map：根据用户提供的函数，处理上面输入的键值对数据，生成新的键值对数据，提供给reduce处理；

- 3、Shuffle：把从map任务输出到reducer任务输入之间的map/reduce框架所做的工作

- 4、Reduce：以拉取到的数据作为输入，并依次为每个键值对执行reduce函数。并将结果写入HDFS中。


## 2、shuffle

![](../../pic/2020-05-02-17-16-25.png)

Shuffle横跨Map端和Reduce端，在Map端包括Spill过程，在Reduce端包括copy和sort过程。

由于shuffle涉及到了磁盘的读写和网络的传输，因此shuffle性能的高低直接影响到了整个程序的运行效率

### 1、Map端shuffle

每个map task都有一个内存缓冲区，存储map的输出结果，当缓冲区快满的时候需要将缓冲区的数据以一个临时文件的方式存放到磁盘，当整个map task结束后再对磁盘中这个map task产生的所有临时文件做合并，生成最终的正式输出文件，然后等待reduce task来拉数据。

![](../../pic/2020-05-02-17-18-35.png)

每个Map任务以<key, value>对的形式把数据输出到在内存中构造的一个环形数据结构中。该数据结构是个字节数组，称为KVbuffer。Kvbuffer中不仅存放<key, value>数据，还放置了一些索引数据。索引数据称为kvmeta。

数据在写入缓冲区之前，会通过Partitioner进行分区，指定那些数据交给哪个reduce task处理，对key进行hash后，再以reducetask数量取模
kvmeta，包括：value的起始位置、key的起始位置、partition值（指定哪个reduce处理）、value的长度。
用一个分界点来划分两者，初始的分界点是0，<key, value>数据的存储方向是向上增长，索引数据的存储方向是向下增长。


当缓冲区的空间不够时，需要将数据刷到硬盘上，这个过程称为spill。如果把Kvbuffer用完时候再开始Spill，那Map任务就需要等Spill完成腾出空间之后才能继续写数据；如果Kvbuffer只是满到一定程度，比如80%的时候就开始Spill，那在Spill的同时，Map任务还能继续写数据。

一开始bufstart=bufend，如果达到溢写条件，令bufend=bufindex，并将[bufstart,bufend]之间的数据写到磁盘上；溢写完成之后，令bufstart=butend=newEquator，完成分界点的转移。

在spill之前，会先把数据按照partition值和key两个关键字升序排序，移动的只是索引数据，排序结果是Kvmeta中数据按照partition为单位聚集在一起，同一partition内的按照key有序。

如果用户设置了combiner，还会对数据进行合并操作，计算规则与reduce一致。
Spill线程根据排过序的Kvmeta挨个partition的把<key, value>数据写入磁盘文件中，一个partition对应的数据写完之后顺序地写下个partition，直到把所有的partition遍历完。每次spill都会产生一个溢写文件，而最终每个map task只会有一个输出文件，因此会对这些中间结果进行归并，称为merge。

merge时，会进行一次排序，排序算法是多路归并排序；如果设置了combier，也会进行合并。最终生成的文件格式与单个溢出文件一致，也是按分区顺序存储，分区内按照key排序。

![](../../pic/2020-05-02-17-23-36.png)


### 2、Reduce端shuffle

![](../../pic/2020-05-02-17-24-43.png)

Copy：Reduce进程启动一些数据copy线程(Fetcher)，通过HTTP方式请求map task所在的TaskTracker获取map task的输出文件.Copy过来的数据会先放入内存缓冲区中，这里的缓冲区大小要比 map 端的更为灵活，它基于 JVM 的heap size设置，reduce会使用其heapsize的70%来在内存中缓存数据。当内存被使用到了一定的限度，就会开始往磁盘刷（刷磁盘前会先做sort）。这个限度阈值也是可以通过参数 mapred.job.shuffle.merge.percent（default 0.66）来设定。与map 端类似，这也是溢写的过程，这个过程中如果你设置有Combiner，也是会启用的。在远程copy数据的同时，Reduce Task在后台启动了两个后台线程对内存和磁盘上的数据文件做合并操作，以防止内存使用过多或磁盘生的文件过多。这种merge方式一直在运行，直到没有map端的数据时才结束，然后启动磁盘到磁盘的merge方式生成最终的那个文件。



### 3、spark shuffle 优化过程

#### 1、Hash based shuffle v1

Spark中将shuffle分为shuffle write 和shuffle read两个阶段，Maptask执行write，reduce task执行read。

Map任务会为每个Reduce创建对应的bucket，每个bucket对应一个内存文件，根据设置的partitioner得到对应的bucketId，将结果写到相应的bucket中去。每个Map的输出结果可能包含所有的Reduce所需要的数据，所以每个Map会创建R个bucket（R是reduce的个数），M个Map总共会创建M*R个bucket。

Reduce拖过来的数据会放在一个HashMap中，HashMap中存储的也是<key, value>对，Shuffle过来的数据先放在内存中，当内存中存储的<key, value>对超过1000并且内存使用超过70%时，判断节点上可用内存如果还足够，则把内存缓冲区大小翻倍，如果可用内存不再够了，则把内存中的<key, value>对排序然后写到磁盘文件中。

问题：map端会同时申请M*R个文件描述符，同时生成M*R个write handler，会带来大量内存的消耗。在reduce阶段(shuffle read)，每个reduce task都会拉取所有map对应的那部分partition数据，那么executor会打开所有临时文件准备网络传输，很容易引发OOM操作。

![](../../pic/2020-05-02-17-33-23.png)


#### 2、Hash based shuffle v2（consolidation Shuffle）

在consolidation Shuffle中每个bucket并非对应一个文件，而是对应文件中的一个segment部分。map在某个节点上第一次执行，为每个reduce创建bucket对应的输出文件，把这些文件组织成ShuffleFileGroup；当又有map在这个节点上执行时，不需要创建新的bucket文件，而是在上次的ShuffleFileGroup中取得已经创建的文件继续追加写一个segment。让一个core上map共用文件，减少文件数目。
但是假如下游stage的分区数N很大，还是会在每个executor上生成N个文件，同样，如果一个executor上有K个core，还是会开K*N个writer handler，总体上来说基本没太解决问题。对于shuffle read阶段跟v1版一样没改进，仍然容易导致OOM。

![](../../pic/2020-05-02-17-35-53.png)

#### 3、Sort based shuffle

在map阶段(shuffle write)，会按照partition id以及key对记录进行排序，将所有partition的数据写在同一个文件中，该文件中的记录首先是按照partition id分区排列，每个partition内部按照key进行排序存放。

另外会产生一个索引文件记录每个partition的大小和偏移量。这样一来，每个map task一次只开两个文件描述符，一个写数据，一个写索引，大大减轻了Hash Shuffle大量文件描述符的问题，即使一个executor有K个core，那么最多一次性开K*2个文件描述符。

shuffle read的拉取过程是一边拉取一边进行聚合的。每个shuffle read task都会有一个自己的buffer缓冲，每次都只能拉取与buffer缓冲相同大小的数据，然后通过内存中的一个Map进行聚合等操作。聚合完一批数据后，再拉取下一批数据。以此类推，直到最后将所有数据到拉取完，并得到最终的结果。

SortShuffleManager的运行机制主要分成两种，一种是普通运行机制，另一种是bypass运行机制。当shuffle read task的数量小于等于spark.shuffle.sort.bypassMergeThreshold参数的值时（默认为200），就会启用bypass机制。

![](../../pic/2020-05-02-17-36-34.png)

#### 4、BypassMergeSortShuffleWriter

BypassMergeSortShuffleWriter,会根据reduce的个数n（reduceByKey中指定的参数，有partitioner决定）创建n个临时文件，然后计算每一个key的hash，放到对应的临时文件中，最后合并这些临时文件成一个文件，同时创建一个索引文件来记录每一个临时文件在合并后的文件中偏移。当reducer取数据时根据reducer partitionid就能以及索引文件就能找到对应的数据块。

![](../../pic/2020-05-02-17-38-19.png)


![](../../pic/2020-05-02-17-39-39.png)

每写一条数据进入内存数据结构之后，就会判断一下，是否达到了某个临界阈值。如果达到临界阈值的话，那么就会尝试将内存数据结构中的数据溢写到磁盘，然后清空内存数据结构。

在溢写到磁盘文件之前，会先根据key对内存数据结构中已有的数据进行排序。排序过后，会以每批1万条数据的形式分批写入磁盘文件。一个task将所有数据写入内存的过程中，会发生多次磁盘溢写操作，也就会产生多个临时文件。最后会将之前所有的临时磁盘文件都进行合并，也就是merge，此时会将之前所有临时磁盘文件中的数据读取出来，然后依次写入最终的磁盘文件之中。


#### 5、Spark shuffle 调优

```
spark.shuffle.file.buffer（default 32K）
spark.reducer.maxSizeInFlight（default 48M）
spark.shuffle.memoryFraction（default 0.2）
spark.shuffle.sort.bypassMergeThreshold（default 200）


```

- 1.参数说明：该参数用于设置shuffle write task的buffer缓冲大小。将数据写到磁盘文件之前，会先写入buffer缓冲中，待缓冲写满之后，才会溢写到磁盘。
调优建议：如果作业可用的内存资源较为充足的话，可以适当增加这个参数的大小，从而减少shuffle write过程中溢写磁盘文件的次数，也就可以减少磁盘IO次数，进而提升性能。

- 2.参数说明：该参数用于设置shuffle read task的buffer缓冲大小，而这个buffer缓冲决定了每次能够拉取多少数据。
调优建议：如果作业可用的内存资源较为充足的话，可以适当增加这个参数的大小，从而减少拉取数据的次数，也就可以减少网络传输的次数，进而提升性能。

- 3.参数说明：该参数代表了Executor内存中，分配给shuffle read task进行聚合操作的内存比例，默认是20%。
调优建议：如果内存充足，而且很少使用持久化操作，建议调高这个比例，给shuffle read的聚合操作更多内存，以避免由于内存不足导致聚合过程中频繁读写磁盘。

- 4.参数说明：当ShuffleManager为SortShuffleManager时，如果shuffle read task的数量小于这个阈值（默认是200），就采用bypass方式。
调优建议：当你使用SortShuffleManager时，如果的确不需要排序操作，那么建议将这个参数调大一些，大于shuffle read task的数量。那么此时就会自动启用bypass机制，map-side就不会进行排序了，减少了排序的性能开销。但是这种方式下，依然会产生大量的磁盘文件，因此shuffle write性能有待提高。







# 5、yarn

![](../../pic/2020-05-02-16-20-13.png)


定位：Yarn核心组件，插拔式框架，利用事件来驱动资源各种分配管理。

默认集成的调度器：FIFO,FAIR,CAPACITY，配置参数：yarn.resourcemanager.scheduler.class

资源调度模型：
- 1、双层资源调度模型，RM中的资源调度器将资源分配给AM，AM在APP内部做二次分配；
- 2、资源分配过程为异步，并且AM获取资源的方式是通过心跳主动拉取；
- 3、计算节点NM也是通过心跳的方式来汇报当前的资源信息；
- 4、调度器均才用的是层级队列管理机制，fair/capacity都是支持子队列模式。


## 1、资源模型

```
Hadoop-2.6.1版本资源表示模型只支持cpu和memory，资源表述模型为：Container

NodeManager：
yarn.nodemanager.resource.memory-mb：NM可供分配的总内存大小，单位MB
yarn.nodemanager.resource.cpu-vcores ： NM可供分配的cpu核数

Container：
yarn.scheduler.minimum-allocation-mb
yarn.scheduler.maximum-allocation-mb
yarn.scheduler.minimum-allocation-vcores
yarn.scheduler.maximum-allocation-vcores
以上四个参数分别定义container的内存和cpu的上下限。


```

## 2、Scheduler

### 1、Capacity Scheduler

Capacity Scheduler：容量(能力)调度器，通过配置队列的资源百分比来分配资源，每个队列可以设定一定比例的资源最低保证和使用上限，也可以对每个用户设定一定的资源使用上限以防止资源滥用。

特点：较为灵活的容量保证。


### 2、Fair Scheduler

Fair Scheduler： 公平调度器，可配置队列最大和最小的资源值 (cpu&memory的绝对值)，也可以定义可运行的App数据量，job权重等。

特点：相对公平，不会使作业一直饥饿。







# 6、Hadoop RPC

![](../../pic/2020-05-02-16-23-00.png)

![](../../pic/2020-05-02-16-23-49.png)

![](../../pic/2020-05-02-16-24-19.png)


# 问题
 
## 1、HDFS中块（block）的大小为什么设置为128M？

- 1.HDFS中平均寻址时间大概为10ms；
- 2.经过前人的大量测试发现，寻址时间为传输时间的1%时，为最佳状态；所以最佳传输时间为10ms/0.01=1000ms=1s
- 3.目前磁盘的传输速率普遍为100MB/s；计算出最佳block大小：100MB/s x 1s = 100MB，所以我们设定block大小为128MB。









