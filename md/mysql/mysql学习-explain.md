

<!-- TOC -->

- [1、开启慢查询日志，捕获慢SQL](#1开启慢查询日志捕获慢sql)
- [2、explain+慢SQL分析](#2explain慢sql分析)
    - [1、id](#1id)
    - [2、select_type](#2select_type)
    - [3、table](#3table)
    - [4、type](#4type)
    - [5、possible_keys](#5possible_keys)
    - [6、key](#6key)
    - [7、key_len](#7key_len)
    - [8、ref](#8ref)
    - [9、rows](#9rows)
    - [10、Extra](#10extra)
    - [11、filtered](#11filtered)
- [3、show profile查询SQL语句在服务器中的执行细节和生命周期](#3show-profile查询sql语句在服务器中的执行细节和生命周期)
- [参考](#参考)

<!-- /TOC -->



MySQL查询优化分析步骤：
- 1、开启慢查询日志，捕获慢SQL;
- 2、explain+慢SQL分析;
- 3、show profile查询SQL语句在服务器中的执行细节和生命周期;
- 4、SQL数据库服务器参数调优;

# 1、开启慢查询日志，捕获慢SQL

参考：mysql学习-慢查询日志

# 2、explain+慢SQL分析

`通过show warnings可以查看实际执行的语句`

explain的作用,通过explain+sql语句可以知道如下内容：

- 表的读取顺序。（对应id）：select子句或表执行顺序，id相同，从上到下执行，id不同，id值越大，执行优先级越高。

- 数据读取操作的操作类型。（对应select_type）：type主要取值及其表示sql的好坏程度（由好到差排序）：system>const>eq_ref>ref>range>index>ALL。保证range，最好到ref。

- 哪些索引可以使用。（对应possible_keys）

- 哪些索引被实际使用。（对应key）：实际被使用的索引列

- 表直接的引用。（对应ref）：关联的字段，常量等值查询，显示为const，如果为连接查询，显示关联的字段。

- 每张表有多少行被优化器查询。（对应rows）

- Extra，额外信息，使用优先级Using index>Using filesort（九死一生）>Using temporary（十死无生）。


使用EXPLAIN关键字可以模拟优化器执行SQL查询语句，从而知道MySQL是 如何处理你的SQL语句的。分析你的查询语句或是表结构的性能瓶颈。执行计划包含的信息：

```sql
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
| id | select_type | table | partitions | type  | possible_keys | key     | key_len | ref   | rows | filtered | Extra |
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
```

备注：MySQL5.7

## 1、id

SELECT查询的序列号，包含一组数字，表示查询中执行SELECT语句或操作表的顺序。包含三种情况：
- 1.id相同，执行顺序由上至下
- 2.id不同，如果是子查询，id序号会递增，id值越大优先级越高，越先被执行
- 3.id既有相同的，又有不同的。id如果相同认为是一组，执行顺序由上至下； 在所有组中，id值越大优先级越高，越先执行。

## 2、select_type

- SIMPLE:简单SELECT查询，查询中不包含子查询或者UNION
- PRIMARY:查询中包含任何复杂的子部分，最外层的查询
- SUBQUERY：SELECT或WHERE中包含的子查询部分
- DERIVED：在FROM中包含的子查询被标记为DERIVER(衍生)， MySQL会递归执行这些子查询，把结果放到临时表中
- UNION：若第二个SELECT出现UNION，则被标记为UNION, 若UNION包含在FROM子句的子查询中，外层子查询将被标记为DERIVED
- UNION RESULT：从UNION表获取结果的SELECT

## 3、table

显示这一行数据是关于哪张表的

## 4、type  

```sql
CREATE TABLE `app` (
  `Id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `AppId` varchar(500) NOT NULL DEFAULT 'default' COMMENT 'AppID',
  `Name` varchar(500) NOT NULL DEFAULT 'default' COMMENT '应用名',
  `OrgId` varchar(32) NOT NULL DEFAULT 'default' COMMENT '部门Id',
  `OrgName` varchar(64) NOT NULL DEFAULT 'default' COMMENT '部门名字',
  `OwnerName` varchar(500) NOT NULL DEFAULT 'default' COMMENT 'ownerName',
  `OwnerEmail` varchar(500) NOT NULL DEFAULT 'default' COMMENT 'ownerEmail',
  `IsDeleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '1: deleted, 0: normal',
  `DataChange_CreatedBy` varchar(32) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
  `DataChange_CreatedTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `DataChange_LastModifiedBy` varchar(32) DEFAULT '' COMMENT '最后修改人邮箱前缀',
  `DataChange_LastTime` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`Id`),
  UNIQUE KEY `AppId` (`AppId`(191)) USING BTREE,
  KEY `DataChange_LastTime` (`DataChange_LastTime`),
  KEY `IX_Name` (`Name`(191))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='应用表';

INSERT INTO `apolloconfigdb`.`app` (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`) VALUES ('1', 'SampleApp', '测试样例', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', '\0', 'apollo', '2019-07-03 10:57:47', 'apollo', '2019-07-03 10:57:47');
INSERT INTO `apolloconfigdb`.`app` (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`) VALUES ('2', 'test1', 'test1', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', '\0', 'apollo', '2019-07-03 14:48:22', 'apollo', '2019-07-03 14:48:22');
INSERT INTO `apolloconfigdb`.`app` (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`) VALUES ('3', 'test2', 'test2', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', '\0', 'apollo', '2019-07-03 14:51:52', 'apollo', '2019-07-03 14:51:52');
INSERT INTO `apolloconfigdb`.`app` (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`) VALUES ('4', '1111', '111', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', '\0', 'apollo', '2019-07-10 20:02:08', 'apollo', '2019-07-10 20:02:08');
INSERT INTO `apolloconfigdb`.`app` (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`) VALUES ('5', '666', '数据分析平台', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', '\0', 'apollo', '2019-07-11 11:27:54', 'apollo', '2019-07-11 11:27:54');
INSERT INTO `apolloconfigdb`.`app` (`Id`, `AppId`, `Name`, `OrgId`, `OrgName`, `OwnerName`, `OwnerEmail`, `IsDeleted`, `DataChange_CreatedBy`, `DataChange_CreatedTime`, `DataChange_LastModifiedBy`, `DataChange_LastTime`) VALUES ('6', 'lishuai', 'lishuai', 'TEST1', '样例部门1', 'apollo', 'apollo@acme.com', '\0', 'apollo', '2021-02-09 16:32:45', 'apollo', '2021-02-09 16:32:45');

```

type的值主要有八种，该值表示查询的sql语句好坏，从最好到最差依次为：`const>eq_ref>ref>range>index>ALL`

type显示的是访问类型，是较为重要的一个指标，结果值从最好到最坏依次是：system>const>eq_ref>ref>fulltext>ref_or_null>index_merge>unique_subquery>index_subquery>range>index>ALL。一般来说，得保证查询至少达到range级别，最好能达到ref。

- system：表只有一行记录（等于系统表），这是const类型的特例，平时不会出现。可以忽略

- const：如果通过索引依次就找到了，`const用于比较主键索引或者unique索引`。 因为只能匹配一行数据，所以很快。如果将主键置于where列表中，MySQL就能将该查询转换为一个常量

```sql
mysql> explain select * from app where id = 1;
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
| id | select_type | table | partitions | type  | possible_keys | key     | key_len | ref   | rows | filtered | Extra |
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
|  1 | SIMPLE      | app   | NULL       | const | PRIMARY       | PRIMARY | 4       | const |    1 |   100.00 | NULL  |
+----+-------------+-------+------------+-------+---------------+---------+---------+-------+------+----------+-------+
```

备注：这里通过主键id进行查询type=const


```sql
mysql> explain select * from app where appid = 'test1';
+----+-------------+-------+------------+-------+---------------+-------+---------+-------+------+----------+-------+
| id | select_type | table | partitions | type  | possible_keys | key   | key_len | ref   | rows | filtered | Extra |
+----+-------------+-------+------------+-------+---------------+-------+---------+-------+------+----------+-------+
|  1 | SIMPLE      | app   | NULL       | const | AppId         | AppId | 766     | const |    1 |   100.00 | NULL  |
+----+-------------+-------+------------+-------+---------------+-------+---------+-------+------+----------+-------+
```
备注：这里通过唯一索引进行查询type=const

- eq_ref：唯一性索引扫描，对于每个索引键，表中只有一条记录与之匹配。`简单来说，就是多表连接中使用primary key或者 unique key作为关联条件`

```sql
mysql> EXPLAIN SELECT t1.* from app t1 JOIN appnamespace t2 on t1.AppId = t2.AppId;
+----+-------------+-------+------------+--------+---------------+----------+---------+-------------------------+------+----------+-------------+
| id | select_type | table | partitions | type   | possible_keys | key      | key_len | ref                     | rows | filtered | Extra       |
+----+-------------+-------+------------+--------+---------------+----------+---------+-------------------------+------+----------+-------------+
|  1 | SIMPLE      | t2    | NULL       | index  | IX_AppId      | IX_AppId | 130     | NULL                    |    5 |   100.00 | Using index |
|  1 | SIMPLE      | t1    | NULL       | eq_ref | AppId         | AppId    | 766     | apolloconfigdb.t2.AppId |    1 |   100.00 | Using where |
+----+-------------+-------+------------+--------+---------------+----------+---------+-------------------------+------+----------+-------------+
```


- ref：非唯一性索引扫描，返回匹配某个单独值的所有行。本质上也是一种索引访问，它返回所有匹配 某个单独值的行，然而它可能会找到多个符合条件的行，所以它应该属于查找和扫描的混合体

```sql
mysql> explain select * from app where name = 'test1';
+----+-------------+-------+------------+------+---------------+---------+---------+-------+------+----------+-------------+
| id | select_type | table | partitions | type | possible_keys | key     | key_len | ref   | rows | filtered | Extra       |
+----+-------------+-------+------------+------+---------------+---------+---------+-------+------+----------+-------------+
|  1 | SIMPLE      | app   | NULL       | ref  | IX_Name       | IX_Name | 766     | const |    1 |   100.00 | Using where |
+----+-------------+-------+------------+------+---------------+---------+---------+-------+------+----------+-------------+

```
备注：普通索引type=ref


- range：只检索给定范围的行，使用一个索引来选择行。key列显示使用了哪个索引，一般就是在你的where语句中出现between、<、>、in等的查询，这种范围扫描索引比全表扫描要好，因为只需要开始于缩印的某一点，而结束于另一点，不用扫描全部索引

```sql
mysql> explain select * from app where id > 1;
+----+-------------+-------+------------+-------+---------------+---------+---------+------+------+----------+-------------+
| id | select_type | table | partitions | type  | possible_keys | key     | key_len | ref  | rows | filtered | Extra       |
+----+-------------+-------+------------+-------+---------------+---------+---------+------+------+----------+-------------+
|  1 | SIMPLE      | app   | NULL       | range | PRIMARY       | PRIMARY | 4       | NULL |    5 |   100.00 | Using where |
+----+-------------+-------+------------+-------+---------------+---------+---------+------+------+----------+-------------+
```

备注：使用主键字段id进行范围查询type=range


- index：Full Index Scan ，index与ALL的区别为index类型只遍历索引树，这通常比ALL快，因为索引文件通常比数据文件小。 （也就是说虽然ALL和index都是读全表， 但index是从索引中读取的，而ALL是从硬盘读取的）

```sql
mysql> explain select * from app where id > 1;
+----+-------------+-------+------------+-------+---------------+---------+---------+------+------+----------+-------------+
| id | select_type | table | partitions | type  | possible_keys | key     | key_len | ref  | rows | filtered | Extra       |
+----+-------------+-------+------------+-------+---------------+---------+---------+------+------+----------+-------------+
|  1 | SIMPLE      | app   | NULL       | range | PRIMARY       | PRIMARY | 4       | NULL |    5 |   100.00 | Using where |
+----+-------------+-------+------------+-------+---------------+---------+---------+------+------+----------+-------------+
1 row in set, 1 warning (0.00 sec)

mysql> explain select id from app where id > 1;
+----+-------------+-------+------------+-------+---------------+---------------------+---------+------+------+----------+--------------------------+
| id | select_type | table | partitions | type  | possible_keys | key                 | key_len | ref  | rows | filtered | Extra                    |
+----+-------------+-------+------------+-------+---------------+---------------------+---------+------+------+----------+--------------------------+
|  1 | SIMPLE      | app   | NULL       | index | PRIMARY       | DataChange_LastTime | 5       | NULL |    5 |   100.00 | Using where; Using index |
+----+-------------+-------+------------+-------+---------------+---------------------+---------+------+------+----------+--------------------------+
1 row in set, 1 warning (0.00 sec)
```

备注：范围查询索引树，索引覆盖情景`Using index`


- all：Full Table Scan，遍历全表获得匹配的行

```sql
mysql> explain select * from app ;
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+-------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows | filtered | Extra |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+-------+
|  1 | SIMPLE      | app   | NULL       | ALL  | NULL          | NULL | NULL    | NULL |    5 |   100.00 | NULL  |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+-------+
```


## 5、possible_keys

显示可能应用在这张表中的索引，一个或多个。 查询涉及到的字段上若存在索引，则该索引将被列出，但不一定被查询实际使用

## 6、key

实际使用的索引。如果为NULL，则没有使用索引。 查询中若出现了覆盖索引，则该索引仅出现在key列表中。

## 7、key_len

表示索引中使用的字节数，可通过该列计算查询中使用的索引的长度。在不损失精度的情况下，长度越短越好。key_len显示的值为索引字段的最大可能长度，并非实际使用长度，即key_len是根据表定义计算而得，不是通过表内检索出的。

## 8、ref

如果筛选列为前端传入的参数，常量显示const，否则如果是关联字段显示对应的索引字段，如果使用的组合索引，符合最左前缀原则，里面会显示多个const.

## 9、rows

根据表统计信息及索引选用情况，大致估算出找到所需记录多需要读取的行数。

## 10、Extra

包含不适合在其他列中显示但十分重要的额外信息：

- 0、Using where:不用读取表中所有信息，仅通过索引就可以获取所需数据，这发生在对表的全部的请求列都是同一个索引的部分的时候，表示mysql服务器将在存储引擎检索行后再进行过滤

- 1、Using filesort： 说明MySQL会对数据使用一个外部的索引排序，而不是按照表内的索引顺序进行读取。MySQL中`无法利用索引完成的排序操作称为“文件排序”`

```sql
mysql> EXPLAIN SELECT * from app order by OrgName;
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+----------------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows | filtered | Extra          |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+----------------+
|  1 | SIMPLE      | app   | NULL       | ALL  | NULL          | NULL | NULL    | NULL |    5 |   100.00 | Using filesort |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+----------------+
```

- 2、Using temporary：  使用了临时表保存中间结果，MySQL在对查询结果排序时使用临时表。常见于排序order by和分组查询group by

```sql
mysql> EXPLAIN SELECT * from app group by OrgName;
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+---------------------------------+
| id | select_type | table | partitions | type | possible_keys | key  | key_len | ref  | rows | filtered | Extra                           |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+---------------------------------+
|  1 | SIMPLE      | app   | NULL       | ALL  | NULL          | NULL | NULL    | NULL |    5 |   100.00 | Using temporary; Using filesort |
+----+-------------+-------+------------+------+---------------+------+---------+------+------+----------+---------------------------------+
```

- 3、Using index： 表示相应的SELECT操作中使用了覆盖索引（Covering Index），避免访问了表的数据行，效率不错。 如果同时出现using where，表明索引被用来执行索引键值的查找； 如果没有同时出现using where，表明索引用来读取数据而非执行查找动作 覆盖索引（Covering Index）： 
    - 理解方式1：SELECT的数据列只需要从索引中就能读取到，不需要读取数据行，MySQL可以利用索引返回SELECT列表中 的字段，而不必根据索引再次读取数据文件，换句话说查询列要被所建的索引覆盖 
    - 理解方式2：索引是高效找到行的一个方法，但是一般数据库也能使用索引找到一个列的数据，因此他不必读取整个行。 毕竟索引叶子节点存储了他们索引的数据；当能通过读取索引就可以得到想要的数据，那就不需要读取行了，一个索引 包含了（覆盖）满足查询结果的数据就叫做覆盖索引 
    - 注意： 如果要使用覆盖索引，一定要注意SELECT列表中只取出需要的列，不可SELECT *, 因为如果所有字段一起做索引会导致索引文件过大查询性能下降

- 4、impossible where： WHERE子句的值总是false，不能用来获取任何元组

- 5、select tables optimized away： 在没有GROUP BY子句的情况下基于索引优化MIN/MAX操作或者对于MyISAM存储引擎优化COUNT(*)操作， 不必等到执行阶段再进行计算，查询执行计划生成的阶段即完成优化

- 6、distinct： 优化distinct操作，在找到第一匹配的元祖后即停止找同样值的操作

- Using join buffer：改值强调了在获取连接条件时没有使用索引，并且需要连接缓冲区来存储中间结果。如果出现了这个值，那应该注意，根据查询的具体情况可能需要添加索引来改进能。

- ICP特性(Index Condition Pushdown)：本来index仅仅是data access的一种访问模式，存数引擎通过索引回表获取的数据会传递到MySQL server层进行where条件过滤,5.6版本开始当ICP打开时，如果部分where条件能使用索引的字段，MySQL server会把这部分下推到引擎层，可以利用index过滤的where条件在存储引擎层进行数据过滤。EXTRA显示`using index condition`。

- 索引合并(index merge)：对多个索引分别进行条件扫描，然后将它们各自的结果进行合并(intersect/union)。一般用OR会用到，如果是AND条件，考虑建立复合索引。EXPLAIN显示的索引类型会显示`index_merge`，EXTRA会显示具体的合并算法和用到的索引



## 11、filtered

百分比值，表示存储引擎返回的数据经过滤后，剩下多少满足查询条件记录数量的比例。





# 3、show profile查询SQL语句在服务器中的执行细节和生命周期

Show Profile是MySQL提供可以用来分析当前会话中语句执行的资源消耗情况，可以用于SQL的调优测量。默认关闭，并保存最近15次的运行结果。分析步骤
- 1、查看状态：SHOW VARIABLES LIKE 'profiling';
- 2、开启：set profiling=on;
- 3、查看结果：show profiles;
- 4、诊断SQL：show profile cpu,block io for query 上一步SQL数字号码;参数含义
    - ALL：显示所有开销信息
    - BLOCK IO：显示IO相关开销
    - CONTEXT SWITCHES：显示上下文切换相关开销
    - CPU：显示CPU相关开销
    - IPC：显示发送接收相关开销
    - MEMORY：显示内存相关开销
    - PAGE FAULTS：显示页面错误相关开销
    - SOURCE：显示和Source_function，Source_file，Source_line相关开销
    - SWAPS：显示交换次数相关开销

注意（遇到这几种情况要优化）
- converting HEAP to MyISAM： 查询结果太大，内存不够用往磁盘上搬
- Creating tmp table：创建临时表
- Copying to tmp table on disk：把内存中的临时表复制到磁盘
- locked

另外当order by 和 group by无法使用索引时，增大max_length_for_sort_data参数设置和增大sort_buffer_size参数的设置


```sql
mysql> explain select id from app where id > 1;
+----+-------------+-------+------------+-------+---------------+---------------------+---------+------+------+----------+--------------------------+
| id | select_type | table | partitions | type  | possible_keys | key                 | key_len | ref  | rows | filtered | Extra                    |
+----+-------------+-------+------------+-------+---------------+---------------------+---------+------+------+----------+--------------------------+
|  1 | SIMPLE      | app   | NULL       | index | PRIMARY       | DataChange_LastTime | 5       | NULL |    5 |   100.00 | Using where; Using index |
+----+-------------+-------+------------+-------+---------------+---------------------+---------+------+------+----------+--------------------------+
1 row in set, 1 warning (0.00 sec)

mysql> show profiles;
+----------+------------+-----------------------------------------+
| Query_ID | Duration   | Query                                   |
+----------+------------+-----------------------------------------+
|        1 | 0.00132800 | explain select id from app where id > 1 |
+----------+------------+-----------------------------------------+
1 row in set, 1 warning (0.00 sec)

mysql> show profile cpu,block io for query 1;
+----------------------+----------+----------+------------+--------------+---------------+
| Status               | Duration | CPU_user | CPU_system | Block_ops_in | Block_ops_out |
+----------------------+----------+----------+------------+--------------+---------------+
| starting             | 0.000110 | 0.000000 |   0.000000 |         NULL |          NULL |
| checking permissions | 0.000012 | 0.000000 |   0.000000 |         NULL |          NULL |
| Opening tables       | 0.000028 | 0.000000 |   0.000000 |         NULL |          NULL |
| init                 | 0.000035 | 0.000000 |   0.000000 |         NULL |          NULL |
| System lock          | 0.000034 | 0.000000 |   0.000000 |         NULL |          NULL |
| optimizing           | 0.000018 | 0.000000 |   0.000000 |         NULL |          NULL |
| statistics           | 0.000090 | 0.000000 |   0.000000 |         NULL |          NULL |
| preparing            | 0.000048 | 0.000000 |   0.000000 |         NULL |          NULL |
| explaining           | 0.000070 | 0.000000 |   0.000000 |         NULL |          NULL |
| end                  | 0.000008 | 0.000000 |   0.000000 |         NULL |          NULL |
| query end            | 0.000235 | 0.015600 |   0.000000 |         NULL |          NULL |
| closing tables       | 0.000112 | 0.000000 |   0.000000 |         NULL |          NULL |
| freeing items        | 0.000503 | 0.000000 |   0.000000 |         NULL |          NULL |
| cleaning up          | 0.000029 | 0.000000 |   0.000000 |         NULL |          NULL |
+----------------------+----------+----------+------------+--------------+---------------+
14 rows in set, 1 warning (0.00 sec)


mysql> show profile all for query 1;
+----------------------+----------+----------+------------+-------------------+---------------------+--------------+---------------+---------------+-------------------+-------------------+-------------------+-------+-----------------------------+----------------------+-------------+
| Status               | Duration | CPU_user | CPU_system | Context_voluntary | Context_involuntary | Block_ops_in | Block_ops_out | Messages_sent | Messages_received | Page_faults_major | Page_faults_minor | Swaps | Source_function             | Source_file          | Source_line |
+----------------------+----------+----------+------------+-------------------+---------------------+--------------+---------------+---------------+-------------------+-------------------+-------------------+-------+-----------------------------+----------------------+-------------+
| starting             | 0.000110 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | NULL                        | NULL                 |        NULL |
| checking permissions | 0.000012 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | check_access                | sql_authorization.cc |         802 |
| Opening tables       | 0.000028 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | open_tables                 | sql_base.cc          |        5709 |
| init                 | 0.000035 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | handle_query                | sql_select.cc        |         121 |
| System lock          | 0.000034 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | mysql_lock_tables           | lock.cc              |         323 |
| optimizing           | 0.000018 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | JOIN::optimize              | sql_optimizer.cc     |         151 |
| statistics           | 0.000090 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | JOIN::optimize              | sql_optimizer.cc     |         367 |
| preparing            | 0.000048 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | JOIN::optimize              | sql_optimizer.cc     |         475 |
| explaining           | 0.000070 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | explain_query_specification | opt_explain.cc       |        2038 |
| end                  | 0.000008 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | handle_query                | sql_select.cc        |         199 |
| query end            | 0.000235 | 0.015600 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | mysql_execute_command       | sql_parse.cc         |        4946 |
| closing tables       | 0.000112 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | mysql_execute_command       | sql_parse.cc         |        4998 |
| freeing items        | 0.000503 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | mysql_parse                 | sql_parse.cc         |        5610 |
| cleaning up          | 0.000029 | 0.000000 |   0.000000 |              NULL |                NULL |         NULL |          NULL |          NULL |              NULL |              NULL |              NULL |  NULL | dispatch_command            | sql_parse.cc         |        1924 |
+----------------------+----------+----------+------------+-------------------+---------------------+--------------+---------------+---------------+-------------------+-------------------+-------------------+-------+-----------------------------+----------------------+-------------+
```






# 参考

- [MySQL之SQL语句优化步骤](https://blog.csdn.net/DrDanger/article/details/79092808)
- [mysql 用法 Explain](https://blog.csdn.net/lvhaizhen/article/details/90763799)
- [一张图彻底搞懂MySQL的 explain](https://segmentfault.com/a/1190000021458117?utm_source=tag-newest)