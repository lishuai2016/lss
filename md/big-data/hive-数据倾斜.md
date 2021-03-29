
<!-- TOC -->

- [1、参数调整](#1参数调整)
    - [1、Map端部分聚合，相当于Combiner](#1map端部分聚合相当于combiner)
    - [2、groupby](#2groupby)
    - [3、join](#3join)
        - [1、大小表join](#1大小表join)
        - [2、大表Join大表](#2大表join大表)
        - [1、无效值key比较多（key上加随机数）](#1无效值key比较多key上加随机数)
        - [2、当key值都是有效值时](#2当key值都是有效值时)
    - [4、不同数据类型关联产生数据倾斜](#4不同数据类型关联产生数据倾斜)
    - [5、COUNT(DISTINCT) 产生数据倾斜](#5countdistinct-产生数据倾斜)
    - [其他](#其他)
- [2、SQL语句调整](#2sql语句调整)
- [3、写hive-sql的几个原则](#3写hive-sql的几个原则)
    - [1、Hive中小表与大表关联(join)的性能分析](#1hive中小表与大表关联join的性能分析)
- [参考](#参考)

<!-- /TOC -->

解决方案：

- mapjoin方式
- count distinct的操作，先转成group，再count
- 万能膏药：hive.groupby.skewindata=true
- left semi jioin的使用
- 设置map端输出、中间结果压缩。（不完全是解决数据倾斜的问题，但是减少了IO读写和网络传输，能提高很多效率）


> mapreduce数据倾斜现象

这里如果详细的看日志或者和监控界面的话会发现：

- 有一个多几个reduce卡住
- 各种container报错OOM
- 读写的数据量极大，至少远远超过其它正常的reduce

伴随着数据倾斜，会出现任务被kill等各种诡异的表现。



> hive数据倾斜解决方案

- 1、参数调节
- 2、SQL调整

备注：有些场景需要参数和SQL同时进行调整，才能达到比较好的效果。


Hive 执行 MapReduce 过程中经常会出现数据倾斜问题，具体表现为：`作业经常在 Reduce 过程完成 99% 的时候一直停留，最后 1% 一直保持很久才完成。`这种情况是由于：在数据量很大的情况下，在 MapReduce 的 Shuffle 过程执行后，key值分布到 Reducer 节点不均匀；有的 key 少，哈希后被分在不同节点中没有问题，但是有的 key 特别的多，过于集中了，全被分配在一个 Reducer 节点，所以其他的 Reducer 都执行完了都在等这个量大的 key 值，这就导致了数据倾斜。


通俗的话来讲就是，一堆干完活的人等那个干的最慢的人，不是因为那个人能力差，大家能力水平都相同，是他真的干不完……被分配太多了，别人做完了也没法帮忙。


所以这也违背了 MapReduce 方法论产生的核心思路，不怕活儿多，活多咱们可以多分配人手；最怕的就是活分配不均匀，有人干的多，有人干的少，出现时间上的浪费。这些工作在 MapReduce 过程中，`数据倾斜可能会发生在group过程和join过程。`常见产生数据倾斜的原因大致有以下几种：

- 大量空值
- 某个 key 值大量重复
- 不同数据类型关联
- COUNT(DISTINCT)


# 1、参数调整

## 1、Map端部分聚合，相当于Combiner

hive.map.aggr = true Map 端部分聚合，相当于Combiner

## 2、groupby

使用Hive对数据做一些类型统计的时候遇到过某种类型的数据量特别多，而其他类型数据的数据量特别少。当按照类型进行group by的时候，会将相同的group by字段的reduce任务需要的数据拉取到同一个节点进行聚合，而当其中每一组的数据量过大时，会出现其他组的计算已经完成而这里还没计算完成，其他节点的一直等待这个节点的任务执行完成，所以会看到一直map 100% reduce 99%的情况。

- hive.map.aggr=true (默认true)这个配置项代表是否在map端进行聚合，相当于Combiner。

- set hive.groupby.skewindata=true 如果是group by过程出现倾斜应该设置为true;(`原理两轮MapReduce：先不按GroupBy字段分发，而是随机分发做一次聚合,额外启动一轮job，拿前面聚合过的数据按GroupBy字段分发再算结果`)

- set hive.groupby.mapaggr.checkintenval=1000000; 这个是group的键对应的记录条数超过这个值则会进行优化。

有数据倾斜的时候进行负载均衡，当选项设定为true，生成的查询计划会有两个MR Job。第一个MR Job中，Map的输出结果集合会随机分布到Reduce中，每个Reduce做部分聚合操作，并输出结果，这样处理的结果是相同的group by key有可能被分发到不同的Reduce中，从而达到负载均衡的目的；第二个MR Job再根据预处理的数据结果按照group by key分布到Reduce中(这个过程可以保证相同的group by key被分布到同一个Reduce中)，最后完成最终的聚合操作。

## 3、join

- hive.optimize.skewjoin  是否优化数据倾斜的 Join，对于倾斜的 Join 会开启新的 Map/Reduce Job 处理。 默认值 false（`原理：关联时有大量空值就给空值一个随机数，有大key时可以单独处理大key然后和别的结果union all`）

- hive.skewjoin.key 倾斜键数目阈值，超过此值则判定为一个倾斜的 Join 查询。 默认值 1000000

### 1、大小表join


1) 多表关联时，将小表(关联键记录少的表)依次放到前面，这样可以触发reduce端更少的操作次数，减少运行时间。

2) 同时可以使用Map Join让小的维度表缓存到内存。在map端完成join过程，从而省略掉reduce端的工作。但是使用这个功能，需要开启map-side join的设置属性：set hive.auto.convert.join=true(默认是false)。同时还可以设置使用这个优化的小表的大小：set hive.mapjoin.smalltable.filesize=25000000(默认值25M)


小表在前大表在后，使用map join让小的维度表先进内存。在map端完成reduce.

SELECT /*+ MAPJOIN(b) */ a.key, a.value  FROM a JOIN b ON a.key = b.key

- set hive.auto.convert.join=true;

- set hive.mapjoin.smalltable.filesize=25mb;   当表小于25mb的时候，小表自动注入内存

- set hive.mapjoin.maxsize   MapJoin 所处理的最大的行数。超过此行数，Map Join进程会异常退出。 默认值1000000

- set hive.skewjoin.mapjoin.map.tasks 处理数据倾斜的 Map Join 的 Map 数上限。 10000

- set hive.skewjoin.mapjoin.min.split 处理数据倾斜的 Map Join 的最小数据切分大小，以字节为单位，默认为32M。  33554432

### 2、大表Join大表

### 1、无效值key比较多（key上加随机数）

大表和大表关联，但是其中一张表的多是空值或者0比较多，容易shuffle给一个reduce，造成运行慢。

把空值的key变成一个字符串加上随机数，把倾斜的数据分到不同的reduce上，由于null值关联不上，处理后并不影响最终结果。

示例 : case when key_field is null then CONCAT("random_",cast(rand()* 1000 as int)) else key_field end key_field 

`注意：不要让随机数碰撞到其他值，提前要测试下是否有膨胀现象发生`;

选择以下例子是因为流量表中往往存在无”主键”的情况，c端用户不登录，就不会在流量表记录用户唯一值。例如：

优化前：

select * from click_log a left join users b on a.user_id = b.user_id;

优化方法1. 让 user_id 为空的不参与关联，

```sql
select *
  from click_log a
  join users b
    on a.user_id is not null
   and a.user_id = b.user_id
union all
select * from click_log a where a.user_id is null；
```

优化方法2. 让随机数冲散堆积在一个人 Reduce 中的很多 null 值，

```sql
select *
  from click_log a
  left join users b
    on case
         when a.user_id is null then
          concat('dp_hive', rand())
         else
          a.user_id
       end = b.user_id;
```

某 key 值大量重复产生数据倾斜解释：如果key均为空值，大量的key会分布在同一个Reduce节点上；在其他Reduce节点完成ReduceTask后，存在大量空值的Reduce还未完成Task，因此产生数据倾斜。 concat('dp_hive',rand())是为了把空值变成一个字符串加上随机数的，把 null值倾斜的数据分布在不同Reduce节点上，间接把倾斜的数据分布在不同Reduce上。

其实，当key值非空，但某个key出现大量重复的情况的解决方案和上述空值情况相同，均为引入随机数进行优化。

```sql
-- 优化前：

select a.key as key, count(b.pv) as pv
  from test_table1 a
 inner join test_table2 b
    on a.key = b.key
 group by 1;

-- 优化后：

select a.key as key, b.pv as pv
  from (select key from test_table1) a
 inner join (select key, sum(pv) as pv
               from (select key, round(rand() * 1000) as rnd, count(1) as pv
                       from test_table2
                      group by 1, 2) tmpgroup by 1) b
    on a.key = b.key;
```

解释： round(rand()*1000) as rnd –> sum(pv) 加入随机，将本来ReduceTask在一组的key，拆分成多组进行处理，增加并发度。



### 2、当key值都是有效值时

解决办法为设置以下几个参数

set hive.exec.reducers.bytes.per.reducer = 1000000000

也就是每个节点的reduce 默认是处理1G大小的数据，如果你的join操作也产生了数据倾斜，那么你可以在hive中设定：

set hive.optimize.skewjoin = true;

set hive.skewjoin.key = skew_key_threshold （default = 100000）

hive在运行的时候没有办法判断哪个key会产生多大的倾斜，所以使用这个参数控制倾斜的阀值，如果超过这个值，新的值会发送给那些还没有达到的reduce，一般可以设置成你处理的总记录数/reduce个数的2-4倍都可以接受。

倾斜是经常会存在的，一般select的层数超过2层，翻译成执行计划多余3个以上的mapreduce job都会很容易产生倾斜，建议每次运行比较复杂的sql之前都可以设一下这个参数，如果你不知道设置多少，可以就按官方默认的1个reduce只处理1G的算法，那么 skew_key_threshold  = 1G/平均行长. 或者默认直接设成250000000 (差不多算平均行长4个字节)




## 4、不同数据类型关联产生数据倾斜

用户表user_id字段为 bigint，`click_log 表中user_id字段既有string类型也有bigint类型`。当按照user_id进行两个表的join操作时，默认的hash操作会按bigint型的id来进行分配，这样会导致所有string类型id的记录都分配到一个Reduce中，例如：
优化前：

select * from click_log a left join users b on a.user_id = b.user_id;

优化后：

```sql
select *
  from users a
  left join click_log b
    on a.user_id = cast(b.user_id as string)
```


## 5、COUNT(DISTINCT) 产生数据倾斜

首先，说说 group by 和 distinct 的区别。我在工作过程中实践过很多场景，发现在处理时间上并没有什么本质区别，都是在一个 job 中，出现一个 reduce 过程。那么为什么一定要要强调用 group by 代替 distinct 呢？ 我在网上搜了很多资料，都没有说清楚这个问题的本质。接下来我来说说我的理解：

其实，大部分情况我们根本不用代替，因为 大部分的情况 数据是不倾斜的。

举个栗子，我们经常计算每个用户这一年变换手机号次数，Email次数等等，当然不会倾斜，因为谁没事儿老去更换手机号呢！

所以，数据不倾斜的时候基本上两个过程是等价的。但是，一旦出现数据倾斜，还是要代替一下，因为 group by 覆盖到了很多 hive 计算引擎参数设置，例如：

set hive.groupby.mapaggr.checkinterval = 100000; – 这个是 grby 的键对应的记录条数超过这个值则会进行优化

set hive.groupby.skewindata = true; – 如果 grby 过程中出现倾斜，设置成 true，会自动将相同的reduceKey进行 Hashing

除此之外， join 也涉及到了一些，如下：

set hive.skewjoin.key = 100000; – 这是 join 的键对应的记录条数超过这个值则会进行优化；

set hive.optimize.skewjoin = true; – 如果是 join 过程中出现数据倾斜，设置成 true

其次， count（distinct） 会压力都积压在一个 reduce 过程中，导致一个job处理太多数据，导致数据倾斜，改成 count(1) + group by 的处理方式，这样会将整个过程解耦，进而分解整体过程中的压力。

最后，有一点自己在写 Hive Sql 时候的体会，代码可读性和性能优化，一定都要考虑，过于复杂的代码逻辑一定要加注释，增强代码可读性，举个栗子：

select a, sum(b), count(distinct c),.. . from T group by a;

优化后本应该是：

```sql
select a, sum(b) as b, count(c) as c,.. .

  from (select a, b, null as c,.. .

          from T

         group by a, b

        union all

        select a, 0 as b, c,.. . from T group by a, c union all .. .) tmp1

 group by a;
```

其实，企业级的 hive 在处理 T+1 数据时，处理的速度并不会相差太远，即 优化程度不高的情况下，建议保留业务逻辑。



## 其他

- hive.join.cache.size 在做表join时缓存在内存中的行数，默认25000；

- hive.mapjoin.bucket.cache.size  mapjoin时内存cache的每个key要存储多少个value，默认100；

- hive.exec.dynamic.partition 在DML/DDL中是否支持动态分区，默认false

- mapred.min.split.size Map Reduce Job 的最小输入切分大小，与 Hadoop Client 使用相同的配置。 默认值  1

- hive.mergejob.maponly 是否启用 Map Only 的合并 Job。 默认值 true



# 2、SQL语句调整

```　
1、count distinct大量相同特殊值：
	count distinct时，将值为空的情况单独处理，如果是计算count distinct，可以不用处理，直接过滤，在最后结果中加1。如果还有其他计算需要进行group by，可以先将值为空的记录单独处理，再和其他计算结果进行union。
2、尽量避免使用Order By排序，要使用Distribute By /Sort By /Cluster By代替。
	Order by 实现全局排序，一个reduce实现，由于不能并发执行，所以效率偏低。 Sort by 实现部分有序，单个reduce输出的结果是有序的，效率高，通常和DISTRIBUTE BY关键字一起使用。CLUSTER BY col1 等价于DISTRIBUTE BY col1 SORT BY col1 但不能指定排序规则。
3、特殊情况特殊处理：
	在业务逻辑优化效果的不大情况下，有些时候是可以将倾斜的数据单独拿出来处理。最后union回去。
```



# 3、写hive-sql的几个原则

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


## 1、Hive中小表与大表关联(join)的性能分析

`事实上“把小表放在前面做关联可以提高效率”这种说法是错误的`。正确的说法应该是“把重复关联键少的表放在join前面做关联可以提高join的效率。” 

分析一下Hive对于两表关联在底层是如何实现的。因为不论多复杂的Hive查询，最终都要转化成mapreduce的JOB去执行，因此Hive对于关联的实现应该和mapreduce对于关联的实现类似。而mapreduce对于关联的实现，简单来说，是把关联键和标记是在join左边还是右边的标识位作为组合键(key)，把一条记录以及标记是在join左边还是右边的标识位组合起来作为值(value)。在reduce的shuffle阶段，按照组合键的关联键进行主排序，当关联键相同时，再按照标识位进行辅助排序。而在分区段时，只用关联键中的关联键进行分区段，这样关联键相同的记录就会放在同一个value list中，同时保证了join左边的表的记录在value list的前面，而join右边的表的记录在value list的后面。
 
例如A join B ON (A.id = b.id) ，假设A表和B表都有1条id = 3的记录，那么A表这条记录的组合键是(3,0)，B表这条记录的组合键是(3,1)。排序时可以保证A表的记录在B表的记录的前面。而在reduce做处理时，把id=3的放在同一个value list中，形成 key = 3,value list = [A表id=3的记录,B表id=3的记录] 
接下来我们再来看当两个表做关联时reduce做了什么。Reduce会一起处理id相同的所有记录。我们把value list用数组来表示。 

- 1、Reduce先读取第一条记录v[0],如果发现v[0]是B表的记录，那说明没有A表的记录，最终不会关联输出，因此不用再继续处理这个id了，读取v[0]用了1次读取操作。 

- 2、如果发现v[0]到v[length-1]全部是A表的记录，那说明没有B表的记录，同样最终不会关联输出，但是这里注意，已经对value做了length次的读取操作。 

- 3、例如A表id=3有1条记录，B表id=3有10条记录。首先读取v[0]发现是A表的记录，用了1次读取操作。然后再读取v[1]发现是B表的操作，这时v[0]和v[1]可以直接关联输出了，累计用了2次操作。这时候reduce已经知道从v[1]开始后面都是B 表的记录了，因此可以直接用v[0]依次和v[2],v[3]……v[10]做关联操作并输出，累计用了11次操作。 

- 4、换过来，假设A表id=3有10条记录，B表id=3有1条记录。首先读取v[0]发现是A表的记录，用了1次读取操作。然后再读取v[1]发现依然是A表的记录，累计用了2次读取操作。以此类推，读取v[9]时发现还是A表的记录，累计用了10次读取操作。然后读取最后1条记录v[10]发现是B表的记录，可以将v[0]和v[10]进行关联输出，累计用了11次操作。接下来可以直接把v[1]~v[9]分别与v[10]进行关联输出，累计用了20次操作。 

- 5、再复杂一点，假设A表id=3有2条记录，B表id=3有5条记录。首先读取v[0]发现是A表的记录，用了1次读取操作。然后再读取v[1]发现依然是A表的记录，累计用了2次读取操作。然后读取v[2]发现是B表的记录，此时v[0]和v[2]可以直接关联输出，累计用了3次操作。接下来v[0]可以依次和v[3]~v[6]进行关联输出，累计用了7次操作。接下来v[1]再依次和v[2]~v[6]进行关联输出，累计用了12次操作。 

- 6、把5的例子调过来，假设A表id=3有5条记录，B表id=3有2条记录。先读取v[0]发现是A表的记录，用了1次读取操作。然后再读取v[1]发现依然是A表的记录，累计用了2次读取操作。以此类推，读取到v[4]发现依然是A表的记录，累计用了5次读取操作。接下来读取v[5]，发现是B表的记录，此时v[0]和v[5]可以直接关联输出，累计用了6次操作。然后v[0]和v[6]进行关联输出，累计用了7次操作。然后v[1]分别与v[5]、v[6]关联输出，累计用了9次操作。V[2] 分别与v[5]、v[6]关联输出，累计用了11次操作。以此类推，最后v[4] 分别与v[5]、v[6]关联输出，累计用了15次操作。 

- 7、额外提一下，当reduce检测A表的记录时，还要记录A表同一个key的记录的条数，当发现同一个key的记录个数超过hive.skewjoin.key的值（默认为1000000）时，会在reduce的日志中打印出该key，并标记为倾斜的关联键。 

`最终得出的结论是：写在关联左侧的表每有1条重复的关联键时底层就会多1次运算处理。` 

假设A表有一千万个id，平均每个id有3条重复值，那么把A表放在前面做关联就会多做三千万次的运算处理，这时候谁写在前谁写在后就看出性能的差别来了。 




# 参考

- [Hive中小表与大表关联(join)的性能分析](https://blog.csdn.net/qq_26442553/article/details/80865014)

- [hive解决数据倾斜问题_如何解决Hive中经常出现的数据倾斜问题](https://blog.csdn.net/weixin_39639698/article/details/112940338)

- [Hive中常见的数据倾斜问题的处理](https://blog.csdn.net/leying521/article/details/93178185)

- [漫谈千亿级数据优化实践：数据倾斜（纯干货）](https://segmentfault.com/a/1190000009166436)

- [MapReduce、Hive、Spark中数据倾斜问题解决归纳总结](https://blog.csdn.net/lzw2016/article/details/89284124)