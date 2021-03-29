
<!-- TOC -->

- [总结](#总结)
- [01、基本概念](#01基本概念)
- [02、Distributed分布表和ReplicatedMergeTree本地表使用](#02distributed分布表和replicatedmergetree本地表使用)
    - [1、Distributed](#1distributed)
    - [2、MergeTree](#2mergetree)
        - [1、Mergetree的存储结构](#1mergetree的存储结构)
        - [2、一级索引](#2一级索引)
            - [1、索引颗粒度](#1索引颗粒度)
            - [2、索引数据生成规则](#2索引数据生成规则)
            - [3、索引的查询过程](#3索引的查询过程)
- [03、ReplicatedMergeTree](#03replicatedmergetree)
- [04、数据存储格式](#04数据存储格式)
        - [0、primary](#0primary)
        - [1、bin文件](#1bin文件)
        - [2、mrk文件](#2mrk文件)
        - [3、检索过程](#3检索过程)
- [06、ClickHouse为什么快](#06clickhouse为什么快)
    - [1、IO层面](#1io层面)
    - [2、指令集层面](#2指令集层面)
    - [3、单机并行层面](#3单机并行层面)
    - [4、分布式层面](#4分布式层面)
- [07、ClickHouse的表引擎](#07clickhouse的表引擎)
- [08、ClickHouse的集群](#08clickhouse的集群)
    - [1、采用Distribute表引擎多写的方式实现复制](#1采用distribute表引擎多写的方式实现复制)
    - [2、采用复制表配合ZooKeeper的方式实现复制](#2采用复制表配合zookeeper的方式实现复制)
- [09、创建表](#09创建表)
    - [1、MergeTree本地表引擎，不支持副本，可以测试使用，不推荐生产使用](#1mergetree本地表引擎不支持副本可以测试使用不推荐生产使用)
    - [2、ReplicatedMergeTree复制表引擎，推荐结合Distributed表引擎一起使用](#2replicatedmergetree复制表引擎推荐结合distributed表引擎一起使用)
    - [3、Distributed分布式表引擎，结合ReplicatedMergeTree引擎表使用，是所有分片本地表数据的集合](#3distributed分布式表引擎结合replicatedmergetree引擎表使用是所有分片本地表数据的集合)
- [10、架构](#10架构)
    - [1、合并树MergeTree](#1合并树mergetree)
        - [详解](#详解)
    - [2、复制（Replication）](#2复制replication)
- [11、LSM（（ Log-Structured Merge）](#11lsm-log-structured-merge)
- [参考](#参考)

<!-- /TOC -->


> 遗留问题：

- 1、数据写入过程详细？mergetree合并过程？

- 2、如何借助zookeeper同步数据副本的详细过程？








# 总结

ClickHouse整体架构图

![](../../pic/2020-04-23-23-19-01.png)

![](../../pic/2020-04-24-08-46-06.png)

CH Cluster 无中心化，即负责数据获取和汇总计算，导致节点压力，不好负载均衡，会导致整个集群不可服务。



基本表结构都是集群中每台机器存放一个本地表，两台机器互为备份，通过zookeeper同步数据，通过分布表可以分发或者搜集整个集群上所有本地表的数据。`分布表本身是一个视图，数据存在本地表中`

- 分布表写：分布表会指定cluster（包含哪些机器）、shard key（没有指定就是random）和对应的本地表，对插入的一条数据，根据shard key判断应该属于哪个shard，然后再该shard中挑选一个可用的机器，向该机器的本地表插入数据，同一shard的机器会通过zookeeper同步数据。

- 分布表读：对一个分部表的查询，会将所有查询分发到cluster上所有的机器（同一个shard查询一台机器），每个表单独查询，查询结果再在分布表这边做汇总，所以分布表所在的机器压力会稍大于其他机器。本地表也支持单表读写，但是数据不全。


分布表写入时，会先把数据写入本地临时文件，后续在异步将数据传输到各个机器的本地表中。而对本地表一次直接插入，也会先写入本地临时文件，写入完成后，再对文件改名，变成正式的数据文件。

本地表（MergeTree Engine）会在后台对文件合并，以提高查询效率。

每个数据文件，或者叫数据块，都会计算一个78位长的一个checksum值,注册到zookeeper上，如果zookeeper上同一个shard的不同副本checksum值不一致，则触发数据同步

特殊说明：
- 从分布表插入时，写入本地临时文件完成，即返回插入完成。后续异步传输过程中如果有宕机或者数据文件损坏，则会导致插入问题，所以更加推荐直接插入本地表

- 插入本地表时，先插入临时文件，再对临时文件改名，改名完成才算插入完成，所以一次插入具有原子性。

- 每次单独的一个insert肯定会生成单独的临时文件，所以不要一行行插入，要批量插入
单次insert在默认情况大于100万行（可自定义）时，临时文件会被拆分，所以也无法保证原子性。经验值是一次插入10万行

- 数据插入的直接插入排序和后台文件合并时的归并排序都十分消耗cpu资源，所以插入不能太快。默认情况，积压150个文件未合并时，就会对后续插入延迟1秒，积压300个文件就会拒绝插入请求，该配置可以更改，但是要考虑机器性能。

- 数据同步是以checksum的更新为基准的，所以如果两次插入的数据块checksum值相同，则会被当做重复文件忽略掉。（最小checksum需要低版本进行配置）



> 1、应用场景

- 绝大多数请求都是用于读访问的
- 数据需要以大批次（大于1000行）进行更新，而不是单行更新；或者根本没有更新操作
- 数据只是添加到数据库，没有必要修改
- 读取数据时，会从数据库中提取出大量的行，但只用到一小部分列
- 表很“宽”，即表中包含大量的列
- 查询频率相对较低（通常每台服务器每秒查询数百次或更少）
- 列的值是比较小的数值和短字符串（例如，每个URL只有60个字节）
- 在处理单个查询时需要高吞吐量（每台服务器每秒高达数十亿行）
- 不需要事务
- 数据一致性要求较低
- 查询结果显著小于数据源。即数据有过滤或聚合。返回结果不超过单个服务器内存大小

> 2、缺点

- 没有完整的事物支持。
- 不支持二级索引
- 缺少高频率，低延迟的修改或删除已存在数据的能力。仅能用于批量删除或修改数据。
- 稀疏索引使得ClickHouse不适合通过其键检索单行的点查询。



# 01、基本概念

ClickHouse是一个开源的列式数据库（DBMS），主要用于在线分析处理查询（OLAP），于2016年开源，采用C++开发。

> 1、丰富的表引擎，主要用到以下表引擎

- MergeTree引擎家族：ReplicatedMergeTree
- Distributed分布式引擎

> 2、架构：采用分布式+高可用集群

Clickhouse分布式通过配置文件来实现，同一集群配置多个shard，每个shard都配置相同的配置文件；而高可用需要借助zookeeper来实现，表采用ReplicatedMergeTree引擎，共享同一个ZK路径的表，会相互同步数据；


![](../../pic/2019-10-23-19-45-18.png)

- ReplicatedMergeTree，复制引擎，基于MergeTree，实现数据复制，即高可用；
- Distributed，分布式引擎，本身不存储数据，将数据分发汇总;
- app不管连接哪台shard，通过访问Distributed引擎表，读取的数据都是一致的。


> 3、数据存储方式

记录方式:每隔8192行数据，是1个block,主键会每隔8192，取一行主键列的数据，同时记录这是第几个block 

查找过程:如果有索引，就通过索引定位到是哪个block，然后找到这个block对应的mrk文件,mrk文件里记录的是某个block的数据集，在整列bin文件的哪个物理偏移位,加载数据到内存，之后并行化过滤。

![](../../pic/2019-10-24-20-35-58.png)


这里是按照索引粒度index_granularity=3进行的。这里的块id应该就行下图中的标记号。



我们以 (CounterID, Date) 以主键。排序好的索引的图示会是下面这样

![](../../pic/2020-04-23-21-13-08.png)

如果指定查询如下：

- 1、CounterID in ('a', 'h')，服务器会读取标记号在 [0, 3) 和 [6, 8) 区间中的数据。

- 2、CounterID IN ('a', 'h') AND Date = 3，服务器会读取标记号在 [1, 3) 和 [7, 8) 区间中的数据。

- 3、Date = 3，服务器会读取标记号在 [1, 10] 区间中的数据。（不满足最左前缀，没法使用索引）

上面例子可以看出使用索引通常会比全表描述要高效。

稀疏索引会引起额外的数据读取。当读取主键单个区间范围的数据时，每个数据块中最多会多读 index_granularity * 2 行额外的数据。大部分情况下，当 index_granularity = 8192 时，ClickHouse的性能并不会降级。


主键本身也符合最左原则,下面是查找图,所以查询时最好利用好主键条件

![](../../pic/2019-10-24-20-36-23.png)


mergeTree有这么几个特点：

- 分块(part)存储(一般按日期)；

- part内按主键排序，并分成多个block;

- 插入时生成新的part;

- 异步merge


其中20171001….这个目录就是代表一个part，下面这些文件，columns.txt记录列信息；每一列有一个bin文件和mrk文件, 其中bin文件是实际数据，primary.idx存储主键信息，结构与mrk一样，类似于稀疏索引。

![](../../pic/2019-10-24-20-38-15.png)

这里展示了mrk文件和primary文件的具体结构，可以看到，数据是按照主键排序的，并且会每隔一定大小分隔出很多个block。每个block中也会抽出一个数据作为索引，放到primary.idx和各列的mrk文件中。

而利用mergetree进行查询时，最关键的步骤就是定位block，这里会根据查询的列是否在主键内有不同的方式。根据主键查询时性能会较好，但是非主键查询时，由于按列存储的关系，虽然会做一次全扫描，性能也没有那么差。所以索引在clickhouse里并不像mysql那么关键。实际使用时一般需要添加按日期的查询条件，保障非主键查询时的性能。


>> 总结

假如这里有8192+10行数据.

- primary.idx文件会存存在的数据格式：

```
key1:blockid1
key2:blockid2
```

- column1.mrk文件

blockid：偏移数据

- column1.bin文件

具体的列数据

本质每index_granularity【默认8192行】生成一个block块，然后针对这些行按列存储。查找的时候通过索引primary.idx找到块id，然后再在块id内部查找。


# 02、Distributed分布表和ReplicatedMergeTree本地表使用

- [Distributed](https://clickhouse.tech/docs/zh/engines/table_engines/special/distributed/)

## 1、Distributed

分布式引擎参数：服务器配置文件中的集群名，远程数据库名，远程表名，数据分片键（可选）,示例：

Distributed(logs, default, hits[, sharding_key]) 

```sql
CREATE TABLE db1.table1 ( 
    ...
      dt Date) 
ENGINE = Distributed(test_cluster, 'db1', 'table1_local', toRelativeDayNum(dt) % 4)

```

备注：这里按照dt对数据进行分片，这里假如有4台机器，每天机器存放的数据只是整个表数据量的1/4（假如每天的数据量一致）。一般情况会再有四台机器备份每一分片的数据，保障数据的可用性，即使一个分片的数据挂了备份分片可使用，一般在写数据的时候只会写一个分片，通过zookeeper把数据同步到备份分片上。



## 2、MergeTree

- [mergetree](https://clickhouse.tech/docs/zh/engines/table_engines/mergetree_family/mergetree/)

MergeTree在写入一批数据时，数据总会以数据片段的形式写入磁盘，且数据片段不可修改。为了避免片段过多，ClickHouse会通过后台线程定期合并这些数据片段，属于相同分区的数据片段会被合成一个新的part。这种数据片段往复合并的特点也正是合并树的名称由来。

![](../../pic/2020-04-23-23-20-40.png)

其中MergeTree作为家族中最基础的表引擎，提供了主键索引、数据分区、数据副本和数据采样等所有的基本能力，而家族中其他的表引擎则在MergeTree的基础之上各有所长。

![](../../pic/2020-04-23-23-21-35.png)

ReplicatedMergeTree基础使用方法

![](../../pic/2020-04-23-23-22-52.png)

![](../../pic/2020-04-23-23-26-00.png)

ClickHouse之Distributed表查询流程

![](../../pic/2020-04-23-23-26-52.png)

ClickHouse集群之Zookeeper及数据目录结构

![](../../pic/2020-04-23-23-27-50.png)


建表格式：

```
CREATE TABLE [IF NOT EXISTS] [db.]table_name [ON CLUSTER cluster]
(
    name1 [type1] [DEFAULT|MATERIALIZED|ALIAS expr1],
    name2 [type2] [DEFAULT|MATERIALIZED|ALIAS expr2],
    ...
    INDEX index_name1 expr1 TYPE type1(...) GRANULARITY value1,
    INDEX index_name2 expr2 TYPE type2(...) GRANULARITY value2
) ENGINE = MergeTree()
[PARTITION BY expr]
[ORDER BY expr]
[PRIMARY KEY expr]
[SAMPLE BY expr]
[SETTINGS name=value, ...]

```
- ENGINE - 引擎名和参数。
- PARTITION BY — 分区键 。
- ORDER BY — 表的排序键。
- PRIMARY KEY - 主键，如果要设成 跟排序键不相同(默认情况下主键跟排序键（由 `ORDER BY` 子句指定）相同。因此，大部分情况下不需要再专门指定一个 `PRIMARY KEY` 子句。)

- SETTINGS — 影响 MergeTree 性能的额外参数：
    - index_granularity — 索引粒度。即索引中相邻『标记』间的数据行数。默认值，8192 


### 1、Mergetree的存储结构

![](../../pic/2020-04-23-23-32-47.png)

### 2、一级索引

MergeTree的主键使用PRIMARY KEY定义，待主键定义之后，MergeTree会依据index_granularity间隔（默认8192行），为数据表生成一级索引并保存至primary.idx文件内，索引数据按照PRIMARY KEY排序，primary.idx文件内的一级索引采用稀疏索引实现。

![](../../pic/2020-04-23-23-33-21.png)


图中所见，一般索引每一行索引标记都会指向一条记录，然而稀疏索引一行索引会指向一个范围区间，如果说MergeTree是一本书的话，那么稀疏索引就相当于目录。他不会具体指向每一页。稀疏索引的优势是显而易见的，它仅需使用少量的索引标记就能够记录大量数据的区间位置信息，且数据量越大优势越为明显。以默认的索引粒度（8192）为例，MergeTree只需要12208行索引标记就能为1亿行数据记录索引。由于稀疏索引占用空间小的特性，primary.idx内的索引数据常驻内存，取用速度极快。


#### 1、索引颗粒度

index_granularity这个参数了，它表示索引的粒度。索引粒度对MergeTree而言是一个非常重要的概念，索引粒度就如同标尺一般，会丈量整个数据的长度，并依照刻度对数据进行标注，最终将数据标记成多个间隔的小段。

![](../../pic/2020-04-23-23-34-52.png)

index_granularity参数，它不单只作用于一级索引（.idx），同时它也会影响数据标记（.mrk）和数据 文件（.bin）。因为仅有一级索引自身是无法完成查询工作的，它还需要借助数据标记才能定位数据，所以一级索引和数据标记的间隔粒度相同， 同为index_granularity行），彼此对齐。而数据文件也会依照index_granularity的间隔粒度生成压缩数据块。


#### 2、索引数据生成规则

由于是稀疏索引，所以MergeTree每间隔index_granularity行数据才会生成一条索引记录，其索引值会依据声明的主键字段获取。 hits_v1使用年月分区（PARTITION BY toYYYYMM（EventDate）），所以2014年3月份的数据最终会被划分到同 一个分区目录内。如果使用CounterID作为主键（ORDER BY CounterID），则每间隔8192行取CounterID的值作为 索引值，索引数据最终会被写入primary.idx文件进行保存。



#### 3、索引的查询过程


MarkRange是ClickHouse用于定义标记区间的对象。 MergeTree按照index_granularity的间隔粒度，将一段完整的数据划分成了多个小的间隔数据段 ，一个具体的数据段，即是一个MarkRange。MarkRange与索引编号对应，使用start和end两个属性表示其区间范围。 通过start与end对应到索引编号的取值，即能够得到它所对应的数值区间。而数值区间，则表示了此MarkRange包含的数据范围。

假如现在有一份测试数据，共192行记录。其中，主键ID为String类型，ID的取值从A000、A001、A002，，按顺序增长，直至A192为止。MergeTree的索引粒度index_granularity=3，根据索引的生成规则， primary.idx文件内的索引数据会如下图所示。

![](../../pic/2020-04-23-23-45-13.png)

根据索引数据，MergeTree会将此数据片段划分成192/3=64个小的MarkRange，两个相邻MarkRange相距的步长为1。 其中，所有MarkRange（整个数据片段）的最大数值区间为[A000，+inf）。

![](../../pic/2020-04-23-23-45-45.png)


将查询条件转化为条件区间。即便是单个值的查询条件，也会被转化成区间的形式。 WHERE ID = 'A003'  ==>  ['A003', 'A003']

> 递归交集判断

以递归的形式，依次将MarkRange的数值区间与条件区间做交集判断。从最大的区间[A000，+inf）开始： 如果不存在交集，则直接枝减掉此整段MarkRange； 如果存在交集，且MarkRange步长大于8（end-start），则将此区间进一步拆分成8个子区间（由merge_tree_coarse _index_granularity指定，默认值为8）。并重复此规则，继续做递归交集判断； 如果存在交集，且MarkRange不可再分解（步长小于8），则记录返回。

将最终匹配的MarkRange聚在一起，合并它们的范围。

![](../../pic/2020-04-23-23-47-16.png)

MergeTree通过递归的形式，持续向下拆分区间，最终将MarkRange定位到最细的粒度。以帮助在后续读取数据的时候， 能够最小化扫描数据的范围。 当查询条件WHERE ID='A003'的时候，最终只需要读取[A000，A003]和[A003，A006]两个区间的数据， 它们对应MarkRange（start：0，end：2）范围，而其他无用的区间都被裁剪掉了。因为MarkRange转化的数值区间是闭区间，所以会额外匹配到临近的一个区间。


# 03、ReplicatedMergeTree

- [replication](https://clickhouse.tech/docs/zh/engines/table_engines/mergetree_family/replication/)


```sql
CREATE TABLE db1.table1_local ( 
...
 dt Date) ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard0/table1', '机器ip') PARTITION BY dt ORDER BY (column1,column2) SETTINGS index_granularity = 8192

```

```sql

CREATE TABLE table_name
(
    EventDate DateTime,
    CounterID UInt32,
    UserID UInt32
) ENGINE = ReplicatedMergeTree('/clickhouse/tables/{layer}-{shard}/table_name', '{replica}')
PARTITION BY toYYYYMM(EventDate)
ORDER BY (CounterID, EventDate, intHash32(UserID))
SAMPLE BY intHash32(UserID)


```

/clickhouse/tables/ 是公共前缀，我们推荐使用这个。

{layer}-{shard} 是分片标识部分。在此示例中，由于 Yandex.Metrica 集群使用了两级分片，所以它是由两部分组成的。但对于大多数情况来说，你只需保留 {shard} 占位符即可，它会替换展开为分片标识。

table_name 是该表在 ZooKeeper 中的名称。使其与 ClickHouse 中的表名相同比较好。

副本名称用于标识同一个表分片的不同副本。你可以使用服务器名称，如上例所示。同个分片中不同副本的副本名称要唯一。

> 总结

- 1、外部通过Distributed分布表来访问数据。
- 2、底层为通过zookeeper完成数据备份的ReplicatedMergeTree表，这个ReplicatedMergeTree表会被切分成多个shard来分片存储，实现分布式。

备注：在创建分布表Distributed时候需要知道其底层的物理表ReplicatedMergeTree，并且指定一条数据来存储在那个分片shard中，常见的是对toRelativeDayNum(dt) % 4，其中4为该表的分片shard数目。

# 04、数据存储格式

- [ClickHouse的数据存储以及检索过程](https://www.jianshu.com/p/c69b1b73b93b)


![](../../pic/2020-04-23-22-05-56.png)



其中：
- default：数据库名
- test_analysis：表名
- 20180424_20180424_1_6_1：是一个part，每次插入数据就会生成一个part，part会不定时的merge成更大的一个part，每个part里的数据都是按照主键排序存储的
- checksums.txt：校验值文件
- columns.txt：列名文件，记录了表中的所有列名
- column_name.mrk：每个列都有一个mrk文件
- column_name.bin：每个列都有一个bin文件，里边存储了压缩后的真实数据
- primary.idx：主键文件，存储了主键值

- primary.idx存储的数据结构类似于一系列marks组成的数组，这里的marks就是每隔index_granularity行取的主键值，一般默认index_granularity=8192

- column_name.mrk文件中也类似于primark.key，每隔 index_granularity行就会记录一次offset。

- primark.idx和column_name.mrk文件做了逻辑行的映射关系

当接收到查询操作时，首先在primary.idx中选出数据的大概范围，然后在column_name.mrk中得到对应数据的offset，根据offset将bin文件中的数据加载到内存，做真正的数据过滤得到查询结果。


在一个分区下（20200323_9_9_0为上面dt作为分区产生的一个分区目录）的文件类型：

- column.bin 列的数据文件（和下面的mrk文件成对出现）
- column.mrk 列的索引文件
- checksums.txt
- columns.txt 表的全部列和列的数据类型
- primary.idx
- count.txt  存放的应该是这个文件目录下的行数
- minmax_dt.idx
- partition.dat



> 数据存储特点

- 1、数据以压缩数据为单位，存储在bin文件中。

- 2、压缩数据对应的压缩数据块，严格限定按照64K~1M byte的大小来进行存储。
    - （1）如果一个block对应的大小小于64K，则需要找下一个block来拼凑，直到拼凑出来的大小大于等于64K。
    - （2）如果一个block的大小在64K到1M的范围内，则直接生成1个压缩数据块。
    - （3）如果一个block的大小大于了1M，则切割生成多个压缩数据块。

- 3、一个part下不同的列分别存储，不同的列存储的行数是一样的。







### 0、primary

存储了稀疏索引,一个part（分区）对应一个稀疏索引。

### 1、bin文件

block：一次写入生成的一个数据块。

真正存储数据的文件，由1到多个压缩数据组成。压缩数据是最小存储单位，由『头文件』和『压缩数据块』组成。头文件由压缩算法、压缩前的字节大小、压缩后的字节大小三部分组成；压缩数据块严格限定在压缩前64K~1M byte大小。（这个大小是ClickHouse认为的压缩与解压性能消耗最小的大小）。即，一个压缩数据块由N个block组成，一个bin文件又由N个压缩数据块组成。

![](../../pic/2020-04-23-21-24-26.png)


### 2、mrk文件

存储了block在bin文件中哪个压缩数据以及这个压缩数据的数据块中的起始偏移量。

![](../../pic/2020-04-23-21-27-24.png)


### 3、检索过程

以 where partition = '2019-10-23' and ID >= 10 and ID < 100 （ID是索引字段）的query描述大体检索流程:

- 1、每个索引都有对应的min/max的partition值，存储在内存中。当contition带上partition时就可以从这些block列表中找到需要检索的索引，找到对应的数据存储文件夹，命中对应的索引（primary.idx）

- 2、根据ID字段，把条件转化为[10,100)的条件区间,再把条件区间与这个partition对应的稀疏索引做交集判断。如果没有交集则不进行具体数据的检索；如果有交集，则把稀疏索引等分8份，再把条件区间与稀疏索引分片做交集判断，直到不能再拆分或者没有交集，则最后剩下的所有条件区间就是我们要检索的Mark index值。

- 3、通过步骤2我们得到了我们要检索的Mark index。通过上面我们知道存在多个block压缩在同一个压缩数据块的情况并且一个bin文件里面又存在N个压缩数据的情况，所以不能直接通过block的值直接到bin文件中搜寻数据。我们通过映射Mark index值到mrk中，通过mrk知道这个block对应到的压缩数据以及在压缩数据块里面的字节偏移量，就得到了我们最后需要读取的数据地址。

- 4、把bin文件中的数据读取到内存中，找到对应的压缩数据，直接从对应的起始偏移量开始读取数据。


示例：

> 1、构建primary索引过程

假设表的index字段为Column A，一个Column A的字符长度为30KB；还有个非index字段Column B，一个Column B的字符长度为100KB；index_granularity=2。依7次写入7行数据，数据如下

```
A	B
a	1
a	3
b	2
b	2
c	1
a	4
a	0
```
写入以后假设已经完全merge，则排序后为：

![](../../pic/2020-04-23-21-49-23.png)

> 2、bin文件

![](../../pic/2020-04-23-21-50-49.png)

![](../../pic/2020-04-23-21-54-42.png)

（3个block才大于64KB，生成一个压缩数据块）

> 3、mrk文件

![](../../pic/2020-04-23-21-51-49.png)


> 4、查询

以A='a'为例，则命中的索引mark number为0跟1。再去mrk文件中，例如对于Column A而言，mark number 0则命中[(0,0),(0,61440))对应的block，mark number 1命中了[(0,61441),(1,30720))对应的block。将这些block加载进内存，再通过偏移量计算，得到最终需要扫描的行。







# 06、ClickHouse为什么快

## 1、IO层面

列式存储，相对与行式存储，列式存储在查询时只会涉及到读取涉及到的列，显然这回大大减少查询时候的IO次数\开销。

当然，采用列式存储后在进行数据记录写入的时候会麻烦一些。


![](../../pic/2020-04-24-08-56-02.png)


## 2、指令集层面


现代CPU的扩展指令集支持SIMD。

SIMD：单指令多数据流，也就是说在同一个指令周期可以同时处理多个数据。(例如：在一个指令周期内就可以完成多个数据单元的比较)。从早期的MMX到现在的SSE。

因为ClickHouse采用了列式存储，这样就可以极大高效的利用CPU的高速缓存，也就是可以将同一列的数据高效的读取到CPU高速缓存的ScanLine中，继而加载到SIMD的寄存器中(如果采用行式存储无法做到这一点)。


## 3、单机并行层面

在传统的数据库( eg:MySQL)中，因为受到各种业务场景的限制，对一次查询请求，在服务端是由一个线程进行处理的。但在ClickHouse中，一次查询请求到了ClickHouse Server实例中是可以对表进行并行读取的，也就是一次查询到了服务端是由多个线程并行处理的。

后面我们会说到ClickHouse的表引擎，其中MergeTree系列的表引擎将数据表分割成为了多个单独的Part, 每个Part中的数据可以独立组织(每个Part有自己独立的索引)。这就会一个查询请求并行读表提供了支撑。

也正因为如此,ClickHouse处理一个查询消耗的CPU资源和内存资源较大，故不适合高并发的场景。

## 4、分布式层面

基本现在主流的号称自己分布式数据库系统都具备相同的特性，无非就是分片和多副本。分片进行性能的水平拓展，多副本可以做负载均衡和高可用

# 07、ClickHouse的表引擎

ClickHouse的表引擎，类似与MySQL的存储引擎。ClickHouse的表引擎决定了数据表在ClickHouse中的组织形式。

ClickHouse的表引擎分为Log、Memory、MergeTree三大类， 此外还有一个特殊的表引擎(例如Distribute逻辑引擎，本身不存储数据，需要结合其它的引擎进行数据分片Share, 可以将其看作某种视图)。

Log和Memory类别中的引擎适合小表操作，例如100万行以下。

Log类表引擎适合一次性写入多次读取的场景，它可以作为查询分析的一些中间表。 

Memory类引擎的数据完全存在与内存中(服务器关闭后数据就不复存在)，适合对性能要求极高的小数据表统计分析场景

MergeTree类引擎，与Merge引擎不同(Merge引擎是将多个相同结构的表合并为一个表)。而MergeTree引擎得名是因为其中的数据是按照树状的层次进行组织，同时随着数据的写入，再服务器的后台按照一定的规则进行Merge.

据说，在实际的应用中有90%以上的表引擎采用MergeTree系列进行存储，后面我们着重介绍这一类引擎


MergeTree引擎提供了根据日期进行索引和根据主键进行索引，同时提供了实时更新数据的功能(如，在写入数据的时候就可以对已写入的数据进行查询，不会阻塞。)，mergetree是clickhouse里最先进的表引擎，不要跟merge引擎混淆。

这个引擎接受参数形式如下：日期类型的列，可选的采样表达式，一个元组定义了这个表的主键，索引的间隔尺寸。

不包含采样表达式的MergerTree示例：
MergeTree(EventDate,(CounterID,EventDate),8192)

包含采样表达式的MergeTree：
MergeTree(EventDate,intHash32(UserId),(CounterID,EventDate,intHash32(UserID)),8192)

以 MergeTree 作为引擎的数据表必须含有一个独立的 Date 字段。比如说， EventDate 字段。这个日期字段必须是 Date 类型的（非 DateTime 类型）。

主键可以是任意表达式构成的元组（通常是列名称的元组），或者是单独一个字段。

采样表达式（可选的）可以是任意表达式。一旦设定了这个表达式，那么这个表达式必须在主键中。上面的例子使用了 UserID 的哈希 intHash32 作为采样表达式，旨在近乎随机地在 CounterID 和 EventDate 内打乱数据条目。换而言之，当我们在查询中使用 SAMPLE 子句时，我们就可以得到一个近乎随机分布的用户列表。

所谓的采样表达式。需要跟查询语句进行配合，目的是在全量数据集合中按照指定的规则进行采样得到采样的样本数据集合。我们在后续的介绍MergeTree的存储结构的时候先忽略这一点。

我们给一个具体的创建MergeTree表的例子

```sql
CREATE TABLE `ontime` (
  `Year` UInt16,
  `Quarter` UInt8,
  `Month` UInt8,
  `DayofMonth` UInt8,
  `DayOfWeek` UInt8,
  `FlightDate` Date,
  `UniqueCarrier` FixedString(7),
   。。。。。。
   。。。。。。
 )ENGINE = MergeTree(FlightDate, (Year, FlightDate), 8192)
```

接下来我们看看MergeTree类型的 ontime表是如何在磁盘上组织数据的。

![](../../pic/2020-04-24-09-05-28.png)


在前面创建的ontime表的数据目录下，分为多个part。每个part对应一个子目录。

目录的命名规则如下:(Start-Date)_(End-Date)_XX_XX_0

前面说过，MergeTree表必须有一个日期字段，数据表按照这个日期字段分割为多个不同的Part。每个Part内部存储的是Start Date到End Date之间的数据。 

MergeTree分割数据最大的粒度是月，也就是说不同月份的数据不可能在同一个Part中。

每个Part中的数据是独立组织的，有自己独立的索引。这就为单个查询进行并行读取表数据提供了支撑。

命名规则中最后面的那一串数字是ClickHouse用来进行多版本控制的。

![](../../pic/2020-04-24-09-07-30.png)

在ontime表的每一个part子目录中，存放如下文件下面这些文件:columns.txt记录列信息；每一列有一个bin文件和mrk文件, 其中bin文件是实际数据，primary.idx存储主键信息，结构与mrk一样，类似于稀疏索引。

每个列数据文件都是按照主键字典序的方式排序存放的。

下面介绍以下稀疏索引方式从primary.idx 到  列.mrk 在到列.bin的过程

![](../../pic/2019-10-24-20-35-58.png)

备注：这里的注解是（x,y），联合主键。

这里展示了mrk文件和primary文件的具体结构，可以看到，数据是按照主键排序的，并且会每隔一定大小分隔出很多个block。每个block中也会抽出一个数据作为索引，放到primary.idx和各列的mrk文件中。

而利用mergetree进行查询时，最关键的步骤就是定位block，这里会根据查询的列是否在主键内有不同的方式。根据主键查询时性能会较好，但是非主键查询时，由于按列存储的关系，虽然会做一次全扫描，性能也没有那么差。所以索引在clickhouse里并不像mysql那么关键。实际使用时一般需要添加按日期的查询条件，保障非主键查询时的性能。找到对应的block之后，就是在block内查找数据，获取需要的行，再拼装需要的其他列数据。

[因为bin文件内部有层级，需要定位到切分的block在哪里]


后台Merge的过程:

当插入数据的时候（通常是批量插入），会将插入的数据创建在一个新的Part 之中。 同时会在后台周期性的进行merge的过程，当merge的时候，很多个part会被选中，通常是最小的一些part，然后merge成为一个大的排好序的part。
        
换句话说，整个这个合并排序的过程是在数据插入表的时候进行的。这个merge会导致这个表总是由少量的排序好的part构成，而且这个merge本身并没有做特别多的工作。

在插入数据的过程中，属于不同的month的数据会被分割成不同的part，这些归属于不同的month的part是永远不会merge到一起的。

这些part在进行合并的时候会有一个大小的阈值，所以不会有太长的merge过程。

在MergeTree系列表引擎中还有SummingMergeTree、AggregatingMergeTree这些都是在MergeTree的基础上增加一些统计功能，具体就不说了。

另外值得一提的是，MergeTree系列里面还有一写自带复制功能的表引擎ReplicatedMergeTree、ReplicatedSummingMergeTree、ReplicatedAggregatingMergeTree这些表引擎，是在原来的基础之上又增加了多副本复制功能(但该复制功能需要结合ZooKeeper一起才能实现)。这个在我们后面介绍集群功能的时候再讲一讲。

Clickhouse支持2中复制方式，一种是Distribute表引擎提供的多写复制模式，另外就是这种复制表(ReplicateXXXXMergeTree)提供的复制模式。 (这一点一定要引起重视，这在集群搭建的时候非常重要)

# 08、ClickHouse的集群

现代数据库的集群方式无外乎: 分片+多副本。这里我们来看看ClickHouse集群具体该怎么玩

我们这里列举2个搭建集群方式，集群的模式都一样。分为3个分片(Shard),每个分片有3个副本。示例数据表采用ClickHouse官网上提供的航班信息数据(ontime表)

ClickHouse集群中的每一个实例，都知道完整的集群拓扑(通过配置文件)。所以，客户端可以连接到任意一个ClickHouse实例进行查询和数据批量写入。

这里区分的2种方式主要是副本之间复制的方式不同。

- 1、一种是利用Distribure表引擎多写的方式实现(无法保证数据一致性)
- 2、一种是利用ReplicateXXXXMergeTree这种复制表引擎配合ZooKeeper的方式实现复制

注意：虽然采用了2种复制模式，但要实现分片，都需要在XXXXMergeTree或ReplicateXXXMergeTree之上定义Distribute引擎

ClickHouse集群选择的是满足CAP原则的AP。 也就是保证可用性，牺牲一致性


## 1、采用Distribute表引擎多写的方式实现复制

![](../../pic/2020-04-24-09-18-55.png)

```
每一个ClickHouse实例的宿主机对应的域名如下:
Shard1-Replica1  Host11
Shard1-Replica2  Host12

Shard2-Replica1  Host21
Shard2-Replica2  Host22

```

这种复制模式，被戏称为“穷人的复制模式”。也就是说当你项目经费紧张的时候无法购买更多的服务器另外部署ZooKeeper集群的时候可以采用这种模式。 

建立Local表(MergeTree)和Distribute表

在4个ClickHouse实例上分别建里local表(每一个实例都要执行，而且执行的建表命令完全一样)

```sql
CREATE TABLE `ontime_local` (
  `Year` UInt16,
  `Quarter` UInt8,
  `Month` UInt8,
  `DayofMonth` UInt8,
  `DayOfWeek` UInt8,
  `FlightDate` Date,
  `UniqueCarrier` FixedString(7),
   。。。。。。
   。。。。。。
 )ENGINE = MergeTree(FlightDate, (Year, FlightDate), 8192)

```

在4个ClickHouse实例上分别建里Distribute分布表(每一个实例都要执行，而且执行的建表命令完全一样)

```sql
CREATE TABLE ontime_all AS ontime_local
ENGINE = Distributed(cluster_2hards_2replicas, default, ontime_local, rand())
```

- cluster_2shards_2replicas是集群的名称，在ClickHouse的config.xml种指定(后面会说明config.xml的配置来描述集群拓扑)。
- Default为数据库名，本例ontime_local表放在default数据库下。
- Ontime_local表示该Distribute(ontime_all)进行分片的表
- Rand() 为分片的Key,本例采用随机片

在4个ClickHouse实例上分别配置集群拓扑信息(我们前面讲过，ClickHouse的每一个实例都知道集群拓扑的完整结构，以下的配置信息在每一个实例上都一样，配置config.xml)

```xml
配置拓扑信息
<remote_servers>
 <cluster_2shards_2replicas>
  <!--分片1-->
  <shard>
   <!--分片1内部副本复制采用Distribute引擎多写模式-->
   <internal_replication>false</internal_replication>
   <!--分片1种的副本1--> 
   <replica>
    <host>Host11</host>
    <port>9000</port>                
   </replica>
   <!--分片1中的副本2-->                
   <replica>                        
    <host>Host12</host>
    <port>9000</port>                
   </replica>    
  <shard> 
 <!--分片2-->
  <shard>
   <!--分片2内部副本复制采用Distribute引擎多写模式-->
   <internal_replication>false</internal_replication>
   <!--分片2种的副本1--> 
   <replica>
    <host>Host21</host>
    <port>9000</port>                
   </replica>
   <!--分片2中的副本2-->                
   <replica>                        
    <host>Host22</host>
    <port>9000</port>                
   </replica>    
  <shard> 
 </cluster_2shards_2replicas>
</remote_servers>

```

<internal_replication>false</internal_replication>  表示分片内副本间复制模式采用Distribute表引擎多写模式 

> 在分发表(Distribute)上读取数据的过程  

![](../../pic/2020-04-24-09-24-08.png)

前面说过，ClickHouse集群的每一个ClickHouse实例都知道完整的集群拓扑结构(每一个ClickHouse实例上都有一个Distibute引擎实例)，所以客户端可以接入任何一个ClickHouse实例，进行分发表数据读取

在all表上读数据时CH数据流程如下：
- 1.分发SQL到对应多个shard上执行SQL。(Distribute引擎会选择每个分发到的Shard中的”健康的”副本执行SQL)(所谓健康的广义上来说就是存货的，当前负载小的)

- 2.执行SQL后的数据的中间结果发送到主server上(主Server就是接受到客户端查询命令的那台ClickHouse实例)

- 3.数据再次汇总过滤，然后返回给客户端


> 在分发表(Distribute)上写入数据的过程  


在all表上写数据时CH数据流程如下：
- 1.主Server的Distribute引擎选择合适的Shard写入数据。(主Server就是接受到客户端查询命令的那台ClickHouse实例)

- 2.主Server的Distribute引擎将写入的数据发给该Shard的每一个副本实例.(这就是Distribute多写复制方式，但Distribute引擎不会确保每一个副本都写成功，这就可能导致数据的不一致性)




## 2、采用复制表配合ZooKeeper的方式实现复制

![](../../pic/2020-04-24-09-27-14.png)

```
每一个ClickHouse实例的宿主机对应的域名如下:
Shard1-Replica1  Host11
Shard1-Replica2  Host12

Shard2-Replica1  Host21
Shard2-Replica2  Host22

```

这是ClickHouse推荐的一种集群搭建模式，但该模式需要依赖额外的ZooKeeper集群

在4个ClickHouse实例上分别配置集群拓扑信息.(我们前面讲过，ClickHouse的每一个实例都知道集群拓扑的完整结构，以下的配置信息在每一个实例上都一样，配置config.xml)

```xml
配置ZooKeeper集群访问方式
<!-- ZK  -->
<zookeeper-servers>  
 <node index="1">    
   <host>1.xxxx.zk.com</host>    
   <port>2181</port>  
 </node>  
 <node index="2">    
    <host>2.xxxx.zk.com</host>    
    <port>2181</port>  
 </node>  
 <node index="3">    
   <host>3.xxxx.zk.com</host>    
   <port>2181</port>  
 </node>
</zookeeper-servers>

```


```xml
配置拓扑信息
<remote_servers>
 <cluster_2shards_2replicas>
  <!--分片1-->
  <shard>
   <!--分片1内部副本复制表配合ZK的复制模式-->
   <internal_replication>true</internal_replication>
   <!--分片1种的副本1--> 
   <replica>
    <host>Host11</host>
    <port>9000</port>                
   </replica>
   <!--分片1中的副本2-->                
   <replica>                        
    <host>Host12</host>
    <port>9000</port>                
   </replica>    
  <shard> 
 <!--分片2-->
  <shard>
   <!--分片1内部副本复制表配合ZK的复制模式-->
   <internal_replication>true</internal_replication>
   <!--分片2种的副本1--> 
   <replica>
    <host>Host21</host>
    <port>9000</port>                
   </replica>
   <!--分片2中的副本2-->                
   <replica>                        
    <host>Host22</host>
    <port>9000</port>                
   </replica>    
  <shard> 
 </cluster_2shards_2replicas>
</remote_servers>


```

<internal_replication>true</internal_replication>  表示分片内副本间采用复制表配合ZooKeeper的方式实现复制 

在每个Click House实例上建立ReplicateMerge引擎的Local表在每个实例上是不一样的

```sql
Host11:
CREATE TABLE `ontime_local` (  ...) ENGINE = ReplicatedMergeTree('/clickhouse/tables/ontime/shard1', 'replica1', FlightDate, (Year, FlightDate), 8192)

Host12:
CREATE TABLE `ontime_local` (  ...) ENGINE = ReplicatedMergeTree('/clickhouse/tables/ontime/shard1', 'replica2', FlightDate, (Year, FlightDate), 8192)


Host21:
CREATE TABLE `ontime_local` (  ...) ENGINE = ReplicatedMergeTree('/clickhouse/tables/ontime/shard2', 'replica1', FlightDate, (Year, FlightDate), 8192)

Host22:
CREATE TABLE `ontime_local` (  ...) ENGINE = ReplicatedMergeTree('/clickhouse/tables/ontime/shard2', 'replica2', FlightDate, (Year, FlightDate), 8192)
```

在4个ClickHouse实例上分别建里Distribute分布表(每一个实例都要执行，而且执行的建表命令完全一样)

CREATE TABLE ontime_all AS ontime_local
ENGINE = Distributed(cluster_2hards_2replicas, default, ontime_local, rand())

- cluster_2shards_2replicas是集群的名称，在ClickHouse的config.xml种指定
- Default为数据库名，本例ontime_local表放在default数据库下。
- Ontime_local表示该Distribute(ontime_all)进行分片的表
- Rand() 为分片的Key,本例采用随机片

**数据读取过程跟前面那个复制模式是一样的**


前面说过，ClickHouse集群的每一个ClickHouse实例都知道完整的集群拓扑结构(每一个ClickHouse实例上都有一个Distibute引擎实例)，所以客户端可以接入任何一个ClickHouse实例，进行分发表数据读取

在all表上写数据时CH数据流程如下：
- 1.主Server的Distribute引擎选择合适的Shard写入数据。(主Server就是接受到客户端查询命令的那台ClickHouse实例)

- 2.主Server的Distribute引擎将选择一个该Shard中“健康的”副本实例，然后将写入数据发送给它(所谓健康的广义上来说就是存货的，当前负载小的)

- 3.然后该副本配合ZooKeeper进行后台异步的数据复制.(通过ZooKeeper可以一定程度上的保证数据的一致性)



# 09、创建表

## 1、MergeTree本地表引擎，不支持副本，可以测试使用，不推荐生产使用

```sql
CREATE TABLE test1( id UInt16,col1 String,col2 String,create_date date ) ENGINE = MergeTree(create_date, (id), 8192);

```

- ENGINE：是表的引擎类型，MergeTree：最常用的，MergeTree要求有一个日期字段，还有主键。Log引擎没有这个限制，也是比较常用。
- MergeTree 系列的引擎，数据是由多组part文件组成的。每一个part的数据，是按照主键进行字典序排列。例如，如果你有一个主键是(ID, Date)，数据行会首先按照ID排序，如果ID相同，按照Date排序。
- 主键的数据结构，看起来像是标记文件组成的矩阵，这个标记文件就是每间隔index_granularity（索引粒度）行的主键值。

- MergeTree引擎中，默认的index_granularity设置是8192。

## 2、ReplicatedMergeTree复制表引擎，推荐结合Distributed表引擎一起使用

```sql
CREATE TABLE default.test2  ( id UInt64, name String, d Date) ENGINE =ReplicatedMergeTree('/clickhouse/域名/tables/{layer}-{shard}/库名/表名', '{replica}') PARTITION BY toMonday(d) ORDER BY (id, d) SETTINGS index_granularity = 8192;
```

- 域名：申请ck资源后提供链接串
- ReplicatedMergeTree：MergeTree的分支，表复制引擎。
- d：是表的日期字段，一个表必须要有一个日期字段。
- id：是表的主键，主键可以有多个字段，每个字段用逗号分隔。
- 8192：是索引粒度，用默认值8192即可。

## 3、Distributed分布式表引擎，结合ReplicatedMergeTree引擎表使用，是所有分片本地表数据的集合

```sql
CREATE TABLE zabbix.test2_all (  id UInt64, name String, d Date) ENGINE = Distributed('cluster, 'db', 'table', rand());
```

- cluster：配置文件中的群集名称。
- db：库名。
- table：本地表名。
- rand()：分片方式：随机；intHash64()：指定字段做hash。
- Distribute引擎会选择每个分发到的Shard中的”健康的”副本执行SQL，不存储数据

# 10、架构

- [architecture](https://clickhouse.tech/docs/zh/development/architecture/)

## 1、合并树MergeTree

MergeTree 是一系列支持按主键索引的存储引擎。主键可以是一个任意的列或表达式的元组。MergeTree 表中的数据存储于«分块»中。每一个分块以主键序存储数据（数据按主键元组的字典序排序）。表的所有列都存储在这些«分块»中分离的 column.bin 文件中。column.bin 文件由压缩块组成，每一个块通常是 64 KB 到 1 MB 大小的未压缩数据，具体取决于平均值大小。这些块由一个接一个连续放置的列值组成。每一列的列值顺序相同（顺序由主键定义），因此当你按多列进行迭代时，你能够得到相应列的值。

主键本身是«稀疏»的。它并不是索引单一的行，而是索引某个范围内的数据。一个单独的 primary.idx 文件具有每个第 N 行的主键值，其中 N 称为 index_granularity（通常，N = 8192）。同时，对于每一列，都有带有标记的 column.mrk 文件，该文件记录的是每个第 N 行在数据文件中的偏移量。每个标记是一个 pair：文件中的偏移量到压缩块的起始，以及解压缩块中的偏移量到数据的起始。通常，压缩块根据标记对齐，并且解压缩块中的偏移量为 0。primary.idx 的数据始终驻留在内存，同时 column.mrk 的数据被缓存。

当我们要从 MergeTree 的一个分块中读取部分内容时，我们会查看 primary.idx 数据并查找可能包含所请求数据的范围，然后查看 column.mrk 并计算偏移量从而得知从哪里开始读取些范围的数据。由于稀疏性，可能会读取额外的数据。ClickHouse 不适用于高负载的简单点查询，因为对于每一个键，整个 index_granularity 范围的行的数据都需要读取，并且对于每一列需要解压缩整个压缩块。我们使索引稀疏，是因为每一个单一的服务器需要在索引没有明显内存消耗的情况下，维护数万亿行的数据。另外，由于主键是稀疏的，导致其不是唯一的：无法在 INSERT 时检查一个键在表中是否存在。你可以在一个表中使用同一个键创建多个行。

当你向 MergeTree 中插入一堆数据时，数据按主键排序并形成一个新的分块。为了保证分块的数量相对较少，有后台线程定期选择一些分块并将它们合并成一个有序的分块，这就是 MergeTree 的名称来源。当然，合并会导致«写入放大»。所有的分块都是不可变的：它们仅会被创建和删除，不会被修改。当运行 SELECT 查询时，MergeTree 会保存一个表的快照（分块集合）。合并之后，还会保留旧的分块一段时间，以便发生故障后更容易恢复，因此如果我们发现某些合并后的分块可能已损坏，我们可以将其替换为原分块。

MergeTree 不是 LSM 树，因为它不包含»memtable«和»log«：插入的数据直接写入文件系统。这使得它仅适用于批量插入数据，而不适用于非常频繁地一行一行插入 - 大约每秒一次是没问题的，但是每秒一千次就会有问题。我们这样做是为了简单起见，因为我们已经在我们的应用中批量插入数据。

MergeTree 表只能有一个（主）索引：没有任何辅助索引。在一个逻辑表下，允许有多个物理表示，比如，可以以多个物理顺序存储数据，或者同时表示预聚合数据和原始数据。

有些 MergeTree 引擎会在后台合并期间做一些额外工作，比如 CollapsingMergeTree 和 AggregatingMergeTree。这可以视为对更新的特殊支持。请记住这些不是真正的更新，因为用户通常无法控制后台合并将会执行的时间，并且 MergeTree 中的数据几乎总是存储在多个分块中，而不是完全合并的形式。


### 详解

ReplicatedMergeTree与普通的MergeTree又有什么区别呢?  我们接着看下面这张图:

![](../../pic/2020-04-24-09-57-20.png)

图中的虚线框部分是MergeTree的能力边界，而ReplicatedMergeTree在它的基础之上增加了分布式协同的能力。


借助ZooKeeper的消息日志广播，实现了副本实例之间的数据同步功能。

ReplicatedMergeTree系列可以用组合关系来理解，如下图所示：

![](../../pic/2020-04-24-09-58-11.png)

在没有特殊要求的场合，使用基础的MergeTree表引擎即可，它不仅拥有高效的性能，也提供了所有MergeTree共有的基础功能，包括列存、数据分区、分区索引、一级索引、二级索引、TTL、多路径存储等等。

![](../../pic/2020-04-24-10-02-58.png)










## 2、复制（Replication）

ClickHouse 中的复制是基于表实现的。你可以在同一个服务器上有一些可复制的表和不可复制的表。你也可以以不同的方式进行表的复制，比如一个表进行双因子复制，另一个进行三因子复制。

复制是在 ReplicatedMergeTree 存储引擎中实现的。ZooKeeper 中的路径被指定为存储引擎的参数。ZooKeeper 中所有具有相同路径的表互为副本：它们同步数据并保持一致性。只需创建或删除表，就可以实现动态添加或删除副本。

复制使用异步多主机方案。你可以将数据插入到与 ZooKeeper 进行会话的任意副本中，并将数据复制到所有其它副本中。由于 ClickHouse 不支持 UPDATEs，因此复制是无冲突的。由于没有对插入的仲裁确认，如果一个节点发生故障，刚刚插入的数据可能会丢失。

用于复制的元数据存储在 ZooKeeper 中。其中一个复制日志列出了要执行的操作。操作包括：获取分块、合并分块和删除分区等。每一个副本将复制日志复制到其队列中，然后执行队列中的操作。比如，在插入时，在复制日志中创建«获取分块»这一操作，然后每一个副本都会去下载该分块。所有副本之间会协调进行合并以获得相同字节的结果。所有的分块在所有的副本上以相同的方式合并。为实现该目的，其中一个副本被选为领导者，该副本首先进行合并，并把«合并分块»操作写到日志中。

复制是物理的：只有压缩的分块会在节点之间传输，查询则不会。为了降低网络成本（避免网络放大），大多数情况下，会在每一个副本上独立地处理合并。只有在存在显著的合并延迟的情况下，才会通过网络发送大块的合并分块。

另外，每一个副本将其状态作为分块和校验和组成的集合存储在 ZooKeeper 中。当本地文件系统中的状态与 ZooKeeper 中引用的状态不同时，该副本会通过从其它副本下载缺失和损坏的分块来恢复其一致性。当本地文件系统中出现一些意外或损坏的数据时，ClickHouse 不会将其删除，而是将其移动到一个单独的目录下并忘记它。

ClickHouse 集群由独立的分片组成，每一个分片由多个副本组成。集群不是弹性的，因此在添加新的分片后，数据不会自动在分片之间重新平衡。相反，集群负载将变得不均衡。该实现为你提供了更多控制，对于相对较小的集群，例如只有数十个节点的集群来说是很好的。但是对于我们在生产中使用的具有数百个节点的集群来说，这种方法成为一个重大缺陷。我们应该实现一个表引擎，使得该引擎能够跨集群扩展数据，同时具有动态复制的区域，这些区域能够在集群之间自动拆分和平衡。



# 11、LSM（（ Log-Structured Merge）

LSM就是结构化合并树。核心思想是核心思想的核心就是放弃部分读能力，换取写入的最大化能力。核心思路比较简单，就是假定内存足够大，因此不需要每次有数据更新就必须将数据写入到磁盘中，而可以先将最新的数据驻留在内存中，等到积累到最后多之后，再使用归并排序的方式将内存内的数据合并追加到磁盘队尾(因为所有待排序的树都是有序的，可以通过合并排序的方式快速合并到一起)。相比于B-tree来说能够有效减少硬盘开销。但是LSM在查询需要快速响应时性能不佳。通常LSM-tree适用于索引插入比检索更频繁的应用系统。





# 参考

- [官方文档](https://clickhouse.yandex/docs/zh)
- [列式数据库~clickhouse 底层存储原理](https://www.cnblogs.com/danhuangpai/p/9481325.html)
- [clickhouse 基础知识](https://www.jianshu.com/p/a5bf490247ea)
- [【大数据】Clickhouse基础知识](https://www.cnblogs.com/dflmg/p/11464748.html)
- [数据分析的利器-clickhouse概述](https://cloud.tencent.com/developer/news/373550)
- [ClickHouse架构概述](https://clickhouse.yandex/docs/zh/development/architecture/)

- [yandex文档](https://clickhouse.yandex/docs/zh/)
- [clickhouse 基础知识](https://www.jianshu.com/p/a5bf490247ea)



- [彪悍开源的分析数据库-ClickHouse](https://zhuanlan.zhihu.com/p/22165241)

- [ClickHouse 使用](https://www.zouyesheng.com/clickhouse.html)

- [不错的网站](http://clickhouse.com.cn/)

- [最快开源 OLAP 引擎！ClickHouse 在头条的技术演进](https://www.infoq.cn/article/NTwo*yR2ujwLMP8WCXOE)


- [ClickHouse学习笔记](https://www.cnblogs.com/grapelet520/p/11280972.html)

- [ClickHouse各种MergeTree的关系与作用](https://cloud.tencent.com/developer/article/1604965)









