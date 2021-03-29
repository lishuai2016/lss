


<!-- TOC -->

- [1、数据倾斜](#1数据倾斜)
    - [1、现象](#1现象)
    - [2、原因（shuffle）](#2原因shuffle)
    - [3、解决思路](#3解决思路)
    - [9、table join关联key存在大量null值的情况](#9table-join关联key存在大量null值的情况)
- [9、spark shuffle 优化过程](#9spark-shuffle-优化过程)
            - [1、Hash based shuffle v1](#1hash-based-shuffle-v1)
            - [2、Hash based shuffle v2（consolidation Shuffle）](#2hash-based-shuffle-v2consolidation-shuffle)
            - [3、Sort based shuffle](#3sort-based-shuffle)
            - [4、BypassMergeSortShuffleWriter](#4bypassmergesortshufflewriter)
            - [5、Spark shuffle 调优](#5spark-shuffle-调优)
- [参考](#参考)

<!-- /TOC -->



# 1、数据倾斜

## 1、现象

park中的数据倾斜也很常见，这里包括Spark Streaming和Spark Sql，表现主要有下面几种：

- Executor lost，OOM，Shuffle过程出错
- Driver OOM
- 单个Executor执行时间特别久，整体任务卡在某个阶段不能结束
- 正常运行的任务突然失败

补充一下，在Spark streaming程序中，数据倾斜更容易出现，特别是在程序中包含一些类似sql的join、group这种操作的时候。 因为Spark Streaming程序在运行的时候，我们一般不会分配特别多的内存，因此一旦在这个过程中出现一些数据倾斜，就十分容易造成OOM。

## 2、原因（shuffle）

我们以Spark和Hive的使用场景为例。他们在做数据运算的时候会设计到，countdistinct、group by、join等操作，这些都会触发Shuffle动作，一旦触发，所有相同key的值就会拉到一个或几个节点上，就容易发生单点问题。

Shuffle是一个能产生奇迹的地方，不管是在Spark还是Hadoop中，它们的作用都是至关重要的。这里主要针对，在Shuffle如何产生了数据倾斜。

Hadoop和Spark在Shuffle过程中产生数据倾斜的原理基本类似。因为数据分布不均匀，导致大量的数据分配到了一个节点。

## 3、解决思路

- mapjoin方式
- 设置rdd压缩
- 合理设置driver的内存
- Spark Sql中的优化和Hive类似，可以参考Hive




## 9、table join关联key存在大量null值的情况

备注：table1大小1.4亿，table2大小400万

结论：数据倾斜：首先想能否改join方式，然后再考虑开启spark自适应框架，最后再考虑优化sql。



1、开启sparksql的数据倾斜时的自适应关联优化

spark.shuffle.statistics.verbose=true   --打开后MapStatus会采集每个partition条数的信息，用于倾斜处理

spark.sql.adaptive.skewedJoin.enabled=true      --倾斜处理开关


开启自适应倾斜参数后，SparkSQL自适应框架可以根据预先的配置在作业运行过程中自动检测是否出现倾斜，并对检测到的倾斜进行优化处理。

优化的主要逻辑是对倾斜的partition进行拆分由多个task来进行处理，最后通过union进行结果合并

细节分析：把key=null的数据分到多个task去处理，最后union起来，提高了并行度。


2、优化SQL

将关联key=null的单独处理，分别处理后，将2分数据union。


3、改变join方式？

Sortmergejoin 改成 BroadcastHashJoin。调大BroadcastHashJoin的阈值，在某些场景下可以把SortMergeJoin转化成BroadcastHashJoin而避免shuffle产生的数据倾斜

增加 参数：spark.sql.autoBroadcastJoinThreshold=524288000  --将BHJ的阈值提高到500M。

把join方式从SMJ转变成BMJ方式，从根本上避免了shuffle，可以极大的提升性能。但是只适合大小表join的情况。








# 9、spark shuffle 优化过程

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


# 参考


- [漫谈千亿级数据优化实践：数据倾斜（纯干货）](https://segmentfault.com/a/1190000009166436)













