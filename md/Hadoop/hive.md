
<!-- TOC -->

- [1、hive概念](#1hive概念)
- [2、Hive架构](#2hive架构)
- [3、Hive工作原理](#3hive工作原理)
- [4、SQL](#4sql)
    - [1、窗口函数](#1窗口函数)
    - [2、内外部表](#2内外部表)
    - [3、写HQL的几个原则](#3写hql的几个原则)
- [5、参数设置](#5参数设置)
- [参考](#参考)

<!-- /TOC -->


# 1、hive概念


Hive 是基于 Hadoop 的一个数据仓库工具，可以将结构化的数据文件映射为一张表，并提供类 SQL 查询功能。

本质是：将 HiveQL 转化成 MapReduce 程序执行

- 1） Hive 处理的数据存储在 HDFS
- 2） Hive 分析数据底层的实现是 MapReduce
- 3） 执行程序运行在 Yarn 上



Hive 不是：一个关系数据库、 一个设计用于联机事务处理（OLTP）、实时查询和行级更新的语言



> Hiver特点

- 它存储架构在一个数据库中并处理数据到HDFS。

- 它是专为OLAP设计。

- 它提供SQL类型语言查询叫HiveQL或HQL。

- 它是熟知，快速，可扩展和可扩展的。


> 优势

- 1） 操作接口采用类 SQL 语法，提供快速开发的能力（简单、容易上手）
- 2） 避免了去写 MapReduce，减少开发人员的学习成本。
- 3） Hive 的执行延迟比较高， 因此 Hive 常用于数据分析，对实时性要求不高的场合；
- 4） Hive 优势在于处理大数据，对于处理小数据没有优势，因为 Hive 的执行延迟比较高。
5） Hive 支持用户自定义函数，用户可以根据自己的需求来实现自己的函数。

> 劣势

- 1） Hive 的 HQL 表达能力有限:迭代式算法无法表达、数据挖掘方面不擅长
- 2） Hive 的效率比较低：Hive 自动生成的 MapReduce 作业，通常情况下不够智能化；Hive 调优比较困难，粒度较粗





# 2、Hive架构
下面的组件图描绘了Hive的结构：

![](../../pic/2019-06-04-21-35-00.png)

![](../../pic/2019-06-04-21-39-04.png)

Hive的体系结构可以分为以下几部分：

1、用户接口主要有三个：CLI，Client 和 WUI。其中最常用的是CLI，Cli启动的时候，会同时启动一个Hive副本。Client是Hive的客户端，用户连接至Hive Server。在启动 Client模式的时候，需要指出Hive Server所在节点，并且在该节点启动Hive Server。 WUI是通过浏览器访问Hive。

2、Hive将元数据存储在数据库中，如mysql、derby。Hive中的元数据包括表的名字，表的列和分区及其属性，表的属性（是否为外部表等），表的数据所在目录等。

3、解释器、编译器、优化器完成HQL查询语句从词法分析、语法分析、编译、优化以及查询计划的生成。生成的查询计划存储在HDFS中，并在随后有MapReduce调用执行。

4、Hive的数据存储在HDFS中，大部分的查询、计算由MapReduce完成（包含*的查询，比如select * from tbl不会生成MapRedcue任务）。

![](../../pic/2020-05-02-15-20-57.png)

- （1）解析器（ SQL Parser）：将 SQL 字符串转换成抽象语法树 AST，这一步一般都用第三方工具库完成，比如 antlr；对 AST 进行语法分析，比如表是否存在、字段是否存在、 SQL 语义是否有误。
- （2）编译器（ Physical Plan）：将 AST 编译生成逻辑执行计划。
- （3）优化器（ Query Optimizer）：对逻辑执行计划进行优化。
- （4）执行器（ Execution）：把逻辑执行计划转换成可以运行的物理计划。对于 Hive 来说，就是 MR/Spark。





# 3、Hive工作原理


下图描述了Hive 和Hadoop之间的工作流程。

![](../../pic/2019-06-04-21-35-39.png)

![](../../pic/2019-06-05-09-48-48.png)

下表定义Hive和Hadoop框架的交互方式：

Step No.操作

1 Execute Query

Hive接口，如命令行或Web UI发送查询驱动程序（任何数据库驱动程序，如JDBC，ODBC等）来执行。

2 Get Plan

在驱动程序帮助下查询编译器，分析查询检查语法和查询计划或查询的要求。

3 Get Metadata

编译器发送元数据请求到Metastore（任何数据库）。

4 Send Metadata

Metastore发送元数据，以编译器的响应。

5 Send Plan

编译器检查要求，并重新发送计划给驱动程序。到此为止，查询解析和编译完成。

6 Execute Plan

驱动程序发送的执行计划到执行引擎。

7 Execute Job

在内部，执行作业的过程是一个MapReduce工作。执行引擎发送作业给JobTracker，在名称节点并把它分配作业到TaskTracker，这是在数据节点。在这里，查询执行MapReduce工作。

7.1 Metadata Ops

与此同时，在执行时，执行引擎可以通过Metastore执行元数据操作。

8 Fetch Result

执行引擎接收来自数据节点的结果。

9 Send Results

执行引擎发送这些结果值给驱动程序。

10 Send Results

驱动程序将结果发送给Hive接口。

# 4、SQL

## 1、窗口函数

over()开窗函数：开窗函数指定了分析函数工作的数据窗口大小，这个数据窗口大小可能会随着行的变化而变化。需要结合其他函数一起使用。

如常用组合：
- row_number()over() 为每条记录返回一个数字
- rank() over()   返回数据项在分组中的排名，排名相等会在名次中留下空位
- dense_rank()over()返回数据项在分组中的排名，排名相等会在名次中不会留下空位

![](../../pic/2020-05-02-15-56-41.png)












## 2、内外部表

内部表数据由Hive自身管理，外部表数据由HDFS管理；

内部表数据存储的位置是hive.metastore.warehouse.dir（默认：/user/hive/warehouse），外部表数据的存储位置由自己制定（如果没有LOCATION，Hive将在HDFS上的/user/hive/warehouse文件夹下以外部表的表名创建一个文件夹，并将属于这个表的数据存放在这里）；

删除内部表会直接删除元数据（metadata）及存储数据；删除外部表仅仅会删除元数据，HDFS上的文件并不会被删除；对内部表的修改会将修改直接同步给元数据，而对外部表的表结构和分区进行修改，则需要修复 MSCK REPAIR TABLE table_name;


## 3、写HQL的几个原则

- 1、尽早尽量过滤数据，减少每个阶段的数据量和JOB数。

```sql
例：
SELECT a.id, b.name FROM a LEFT JOIN b ON a.id=b.id WHERE a.dt=‘2017-01-01’ and b.dt=‘2017-01-01’
合理的HQL：
SELECT
        a1.id,
        b1.name 
FROM 
       (SELECT id FROM a WHERE dt = '2013-01-01')a1
LEFT JOIN 
       (SELECT id,name FROM b WHERE dt = '2013-01-01')b1  
ON a1.id = b1.id


```

- 2、列裁剪和分区裁剪，简单的理解就是只查询使用到的字段，禁止使用*；一定要卡分区。这样做节省了读取开销，中间表存储开销和数据整合开销。


- 3、如何避免数据倾斜

当在表JOIN、GROUP BY、 Count Distinct 操作时，由于关联、GROUP的key分布不均，可能导致分发到某一个或几个Reduce 上的数据远高于平均值，或者DISTINCT特殊值比较多，都会导致reduce非常耗时。

解决方案：

```
1）参数调节：

hive.map.aggr = true Map 端部分聚合，相当于Combiner。

hive.groupby.skewindata=true（万能药膏）有数据倾斜的时候进行负载均衡。

2)SQL语句调节：
 　　
a、大小表Join：小表在前大表在后，使用map join让小的维度表先进内存。在map端完成reduce.
	SELECT /*+ MAPJOIN(b) */ a.key, a.value  FROM a JOIN b ON a.key = b.key
	 set hive.auto.convert.join=true;
 	 set hive.mapjoin.smalltable.filesize=25mb; 
 	当表小于25mb的时候，小表自动注入内存
b、大表Join大表：
	把空值的key变成一个字符串加上随机数，把倾斜的数据分到不同的reduce上，由于null值关联不上，处理后并不影响最终结果。
	示例 : case when key_field is null then CONCAT("random_",cast(rand()* 1000 as int)) else key_field end key_field 
c、count distinct大量相同特殊值：
	count distinct时，将值为空的情况单独处理，如果是计算count distinct，可以不用处理，直接过滤，在最后结果中加1。如果还有其他计算需要进行group by，可以先将值为空的记录单独处理，再和其他计算结果进行union。
d、尽量避免使用Order By排序，要使用Distribute By /Sort By /Cluster By代替。
	Order by 实现全局排序，一个reduce实现，由于不能并发执行，所以效率偏低。 Sort by 实现部分有序，单个reduce输出的结果是有序的，效率高，通常和DISTRIBUTE BY关键字一起使用。CLUSTER BY col1 等价于DISTRIBUTE BY col1 SORT BY col1 但不能指定排序规则。
e、特殊情况特殊处理：
	在业务逻辑优化效果的不大情况下，有些时候是可以将倾斜的数据单独拿出来处理。最后union回去。
```



# 5、参数设置

```
常用参数设置
set hive.map.aggr=true; 开启map端聚合
set hive.groupby.skewindata=true 如果是group by过程出现倾斜应该设置为true;
set hive.groupby.mapaggr.checkintenval=1000000; 这个是group的键对应的记录条数超过这个值则会进行优化。
set hive.optimize.skewjoin=true;如果是join过程出现倾斜 应该设置为true
set hive.skewjoin.key=1000000;--这个是join的键对应的记录条数超过这个值则会进行优化
set hive.auto.convert.join=true;
set hive.mapjoin.smalltable.filesize=25mb;   当表小于25mb的时候，小表自动注入内存
set hive.exec.parallel = true;并行化执行
set hive.exec.parallel.thread.numbe=8;并行化执行最多并行运行8个job

map数据量调整：
set  mapred.max.split.size=536870912 ;
set  mapred.min.split.size=536870912 ;
set  mapred.min.split.size.per.node=536870912 ;
set  mapred.min.split.size.per.rack=536870912 ;
Hive表优化 开启动态分区
set hive.exec.dynamic.partition=true;
set hive.exec.dynamic.partition.mode=nonstrict;
set hive.exec.max.dynamic.partitions = 100000;
set hive.exec.max.dynamic.partitions.pernode = 100000;
文件合并设置
set hive.input.format = org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;
set hive.merge.mapfiles = true;
set hive.merge.mapredfiles = true;
set hive.merge.size.per.task = 256000000;
set hive.merge.smallfiles.avgsize = 256000000;

```










# 参考
- [从HiveQL到MapReduce job过程简析](https://www.cnblogs.com/harrymore/p/8950210.html)
- [如何通俗地理解Hive的工作原理？](https://www.zhihu.com/question/49969423)
- [HiveSQL解析原理：包括SQL转化为MapReduce过程及MapReduce如何实现基本SQL操作](https://blog.csdn.net/youzhouliu/article/details/70807993)
- [Hive SQL转化为MapReduce执行计划深度解析](https://blog.csdn.net/i000zheng/article/details/81082774)

- [Hive原理](https://yq.aliyun.com/articles/653935)

- [Hive架构和工作原理](https://cloud.tencent.com/developer/news/362488)







