
<!-- TOC -->

- [00、MySQL 的逻辑架构图](#00mysql-的逻辑架构图)
- [08、select加锁分析(Mysql的innodb引擎)](#08select加锁分析mysql的innodb引擎)
    - [1、锁类型](#1锁类型)
    - [2、加锁算法](#2加锁算法)
    - [3、快照读和当前读](#3快照读和当前读)
    - [4、加的是表锁还是行锁呢？](#4加的是表锁还是行锁呢)
    - [5、分析说明](#5分析说明)
        - [1、隔离级别为RC/RU](#1隔离级别为rcru)
        - [2、隔离级别为RR/Serializable](#2隔离级别为rrserializable)
- [11、binlog日志](#11binlog日志)
    - [1、定义](#1定义)
    - [2、文件格式](#2文件格式)
    - [3、用途](#3用途)
- [13、mysql中set autocommit=0与start transaction区别](#13mysql中set-autocommit0与start-transaction区别)
- [15、drop、truncate和delete的区别](#15droptruncate和delete的区别)
- [98、线上配置相关问题](#98线上配置相关问题)
    - [1、根据什么来决定MySQL数据库的配置---核数、CPU、磁盘大小](#1根据什么来决定mysql数据库的配置---核数cpu磁盘大小)
    - [2、如何设计表的字段？如何评估一条记录占用的磁盘空间大小？误差会有多少？](#2如何设计表的字段如何评估一条记录占用的磁盘空间大小误差会有多少)
- [99、问题汇总](#99问题汇总)

<!-- /TOC -->


# 00、MySQL 的逻辑架构图

![MySQL 的逻辑架构图](../../pic/2020-02-29-13-20-33.png)




# 08、select加锁分析(Mysql的innodb引擎)

```sql
select * from table where id = ?
select * from table where id < ?
select * from table where id = ? lock in share mode
select * from table where id < ? lock in share mode
select * from table where id = ? for update
select * from table where id < ? for update
```
这六句sql在不同的事务隔离级别下，是否加锁，加的是共享锁还是排他锁，是否存在间隙锁?

要回答这个问题，先问自己三个问题

- 1、当前事务隔离级别是什么
- 2、id列是否存在索引
- 3、如果存在索引是聚簇索引还是非聚簇索引呢？



mysql的RR不是完全基于mvcc的Snapshot Isolation，而是mvcc+lock混合实现的，比如有范围查询，mysql会上gap lock，如果有unindexed row update，即使只有一个row行匹配，RR模式下update也会持有多个lock，直到事务结束。

## 1、锁类型

- 共享锁(S锁):假设事务T1对数据A加上共享锁，那么事务T2可以读数据A，不能修改数据A。
- 排他锁(X锁):假设事务T1对数据A加上排他锁，那么事务T2不能读数据A，不能修改数据A。

我们通过update、delete等语句加上的锁都是行级别的锁。只有LOCK TABLE … READ和LOCK TABLE … WRITE才能申请表级别的锁。

- 意向共享锁(IS锁):一个事务在获取（任何一行/或者全表）S锁之前，一定会先在所在的表上加IS锁。
- 意向排他锁(IX锁):一个事务在获取（任何一行/或者全表）X锁之前，一定会先在所在的表上加IX锁。

意向锁存在的目的?

假设事务T1，用X锁来锁住了表上的几条记录，那么此时表上存在IX锁，即意向排他锁。那么此时事务T2要进行LOCK TABLE … WRITE的表级别锁的请求，可以直接根据意向锁是否存在而判断是否有锁冲突。


## 2、加锁算法

- [官方文档](https://dev.mysql.com/doc/refman/5.7/en/innodb-locking.html)

存在以下三种锁：

- Record Locks：简单翻译为行锁吧。注意了，该锁是对索引记录进行加锁！锁是在加索引上而不是行上的。注意了，innodb一定存在聚簇索引，因此行锁最终都会落到聚簇索引上！

- Gap Locks：简单翻译为间隙锁，是对索引的间隙加锁，其目的只有一个，防止其他事物插入数据。在Read Committed隔离级别下，不会使用间隙锁。这里我对官网补充一下，隔离级别比Read Committed低的情况下，也不会使用间隙锁，如隔离级别为Read Uncommited时，也不存在间隙锁。当隔离级别为Repeatable Read和Serializable时，就会存在间隙锁。

- Next-Key Locks：这个理解为Record Lock+索引前面的Gap Lock。记住了，锁住的是索引前面的间隙！比如一个索引包含值，10，11，13和20。那么，Next-Key Locks的范围如下

```
(negative infinity, 10]
(10, 11]
(11, 13]
(13, 20]
(20, positive infinity)
```

## 3、快照读和当前读

在mysql中select分为快照读和当前读，执行下面的语句

select * from table where id = ?;

执行的是快照读，读的是数据库记录的快照版本，是不加锁的。（这种说法在隔离级别为Serializable中不成立，后面我会补充。）
那么，执行

select * from table where id = ? lock in share mode;

会对读取记录加S锁 (共享锁)，执行

select * from table where id = ? for update

会对读取记录加X锁 (排他锁)，那么

## 4、加的是表锁还是行锁呢？

针对这点，我们先回忆一下事务的四个隔离级别，他们由弱到强如下所示:

- Read Uncommited(RU)：读未提交，一个事务可以读到另一个事务未提交的数据！
- Read Committed (RC)：读已提交，一个事务可以读到另一个事务已提交的数据!
- Repeatable Read (RR):可重复读，加入间隙锁，一定程度上避免了幻读的产生！注意了，只是一定程度上，并没有完全避免!我会在下一篇文章说明!另外就是记住从该级别才开始加入间隙锁(这句话记下来，后面有用到)!
- Serializable：串行化，该级别下读写串行化，且所有的select语句后都自动加上lock in share mode，即使用了共享锁。因此在该隔离级别下，使用的是当前读，而不是快照读。

那么关于是表锁还是行锁，大家可以看到网上最流传的一个说法是这样的，

InnoDB行锁是通过给索引上的索引项加锁来实现的，这一点MySQL与Oracle不同，后者是通过在数据块中对相应数据行加锁来实现的。 InnoDB这种行锁实现特点意味着：只有通过索引条件检索数据，InnoDB才使用行级锁，否则，InnoDB将使用表锁！这句话本身有两处错误！

- 错误一:并不是用表锁来实现锁表的操作，而是利用了Next-Key Locks，也可以理解为是用了行锁+间隙锁来实现锁表的操作!

为了便于说明，我来个例子，假设有表数据如下，pId为主键索引

```
pId(int)	name(varchar)	num(int)
1	aaa	100
2	bbb	200
7	ccc	200
```

执行语句(name列无索引)

select * from table where name = `aaa` for update

那么此时在pId=1,2,7这三条记录上存在行锁(把行锁住了)。另外，在(-∞,1)(1,2)(2,7)(7,+∞)上存在间隙锁(把间隙锁住了)。因此，给人一种整个表锁住的错觉！

ps:对该结论有疑问的，可自行执行show engine innodb status;语句进行分析。

- 错误二:所有文章都不提隔离级别！

注意我上面说的，之所以能够锁表，是通过行锁+间隙锁来实现的。那么，RU和RC都不存在间隙锁，这种说法在RU和RC中还能成立么？

因此，该说法只在RR和Serializable中是成立的。如果隔离级别为RU和RC，无论条件列上是否有索引，都不会锁表，只锁行！

## 5、分析说明

前提说明：

- 快照查询：一般的select
- 前读查询[select ... lock in share mode/select ... for update]


### 1、隔离级别为RC/RU

- 1、条件列非索引

- 2、条件列是聚簇索引

- 3、条件列是非聚簇索引

为什么条件列加不加索引，加锁情况是一样的？[即1和2对应的情况]

其实是不一样的。在RC/RU隔离级别中，MySQL Server做了优化。在条件列没有索引的情况下，尽管通过聚簇索引来扫描全表，进行全表加锁。但是，MySQL Server层会进行过滤并把不符合条件的锁当即释放掉，因此你看起来最终结果是一样的。但是RC/RU+条件列非索引比直接使用聚族索引多了一个释放不符合条件的锁的过程！

针对情况3，首先会在非聚族索引B+树上锁住指定的非聚族索引，然后到聚族索引上锁住对应的聚族索引行。

> 总结

- 1、针对快照读，不加任何锁；
- 2、针对当前读查询，这两个隔离级别不存在间隙锁，不会出现锁表，只会锁住符合条件的行。

### 2、隔离级别为RR/Serializable



- 1、条件列非索引：RR级别需要多考虑的就是gap lock，他的加锁特征在于，无论你怎么查都是锁全表
    - 1、快照读：RR级别不加锁；Serializable级别，主键索引锁+间隙锁=锁表；
    - 2、当前读：全部主键索引锁+间隙锁=锁表；

- 2、条件列是聚簇索引：该情况的加锁特征在于，如果where后的条件为精确查询(=的情况)，那么只存在record lock。如果where后的条件为范围查询(>或<的情况)，那么存在的是record lock+gap lock
    - 1、快照读：RR级别不加锁；Serializable级别，等值查询主键索引锁，范围查询主键索引锁+间隙锁=锁表；
    - 2、当前读：等值查询存在加主键索引锁，不存在在符合条件的主键索引上加间隙锁；

- 3、条件列是非聚簇索引：这里非聚簇索引，需要区分是否为唯一索引。因为如果是非唯一索引，间隙锁的加锁方式是有区别的。
    - 1、先说一下，唯一索引的情况。如果是唯一索引，情况和RR/Serializable+条件列是聚簇索引类似，唯一有区别的是:这个时候有两棵索引树，加锁是加在对应的非聚簇索引树和聚簇索引树上！
    - 2、非聚簇索引是非唯一索引的情况，他和唯一索引的区别就是通过索引进行精确查询以后，不仅存在record lock，还存在gap lock。而通过唯一索引进行精确查询后，只存在record lock，不存在gap lock。

> 参考

- [select加锁分析(Mysql)](https://www.cnblogs.com/rjzheng/p/9950951.html)





# 11、binlog日志

## 1、定义

binlog是记录所有数据库表结构变更（例如CREATE、ALTER TABLE…）以及表数据修改（INSERT、UPDATE、DELETE…）的二进制日志。[如果update操作没有造成数据变化，也是会记入binlog]

binlog不会记录SELECT和SHOW这类操作，因为这类操作对数据本身并没有修改，但你可以通过查询通用日志来查看MySQL执行过的所有语句。

## 2、文件格式

> 1、这个二进制日志包括两类文件：

- 索引文件（文件名后缀为.index）用于记录哪些日志文件正在被使用

- 日志文件（文件名后缀为.00000*）记录数据库所有的DDL和DML(除了数据查询语句)语句事件。


> 2、binlog常见格式

![](../../pic/2020-03-13-09-13-10.png)

业内目前推荐使用的是row模式，准确性高，虽然说文件大，但是现在有SSD和万兆光纤网络，这些磁盘IO和网络IO都是可以接受的。

> 3、怎么查看binlog

binlog本身是一类二进制文件。二进制文件更省空间，写入速度更快，是无法直接打开来查看的。因此mysql提供了命令mysqlbinlog进行查看。

一般的statement格式的二进制文件，用下面命令就可以

mysqlbinlog mysql-bin.000001 

如果是row格式，加上-v或者-vv参数就行，如

mysqlbinlog -vv mysql-bin.000001 

> 4、怎么删binlog

删binlog的方法很多，有三种是常见的

- (1) 使用reset master,该命令将会删除所有日志，并让日志文件重新从000001开始。

- (2) 使用命令

PURGE { BINARY | MASTER } LOGS { TO 'log_name' | BEFORE datetime_expr }
例如

purge master logs to "binlog_name.00000X" 

将会清空00000X之前的所有日志文件.

- (3) 使用--expire_logs_days=N选项指定过了多少天日志自动过期清空。

> 5、binlog常见参数

![](../../pic/2020-03-13-09-17-14.png)


> 6、binlog的结构

文件头由一个四字节Magic Number构成，其值为1852400382，在内存中就是"0xfe,0x62,0x69,0x6e"。这个Magic Number就是来验证这个binlog文件是否有效 。

在文件头之后，跟随的是一个一个事件依次排列。其分为三个部分:通用事件头（common-header）、私有事件头（post-header）和事件体（event-body）。

![](../../pic/2020-03-13-09-22-52.png)


## 3、用途

分别为恢复、复制、审计。

- 1、恢复：这里网上有大把的文章指导你，如何利用binlog日志恢复数据库数据。如果你真的觉得自己很有时间，就自己去创建个库，然后删了，再去恢复一下数据，练练手吧。

- 2、复制: 如图所示。主库有一个log dump线程，将binlog传给从库。从库有两个线程，一个I/O线程，一个SQL线程，I/O线程读取主库传过来的binlog内容并写入到relay log,SQL线程从relay log里面读取内容，写入从库的数据库。

![](../../pic/2020-03-13-09-10-54.png)


- 3、审计：用户可以通过二进制日志中的信息来进行审计，判断是否有对数据库进行注入攻击。

> 参考

- [研发应该懂的binlog知识(上)](https://www.cnblogs.com/rjzheng/p/9721765.html)

- [研发应该懂的binlog知识(下)](https://www.cnblogs.com/rjzheng/p/9745551.html)

- [binlog二进制文件解析](https://mp.weixin.qq.com/s/j987HNc74gRYHl6au24XSg)






# 13、mysql中set autocommit=0与start transaction区别

- set autocommit=0,当前session禁用自动提交事物，自此句执行以后，每个SQL语句或者语句块所在的事务都需要显示"commit"才能提交事务。

- start transaction指的是启动一个新事务。

    在默认的情况下，MySQL从自动提交（autocommit）模式运行，这种模式会在每条语句执行完毕后把它作出的修改立刻提交给数据库并使之永久化。事实上，这相当于把每一条语句都隐含地当做一个事务来执行。如果你想明确地执行事务，需要禁用自动提交模式并告诉MySQL你想让它在何时提交或回滚有关的修改。

    执行事务的常用办法是发出一条START TRANSACTION（或BEGIN）语句挂起自动提交模式，然后执行构成本次事务的各条语句，最后用一条 COMMIT语句结束事务并把它们作出的修改永久性地记入数据库。万一在事务过程中发生错误，用一条ROLLBACK语句撤销事务并把数据库恢复到事务开 始之前的状态。

    START TRANSACTION语句"挂起"自动提交模式的含义是：在事务被提交或回滚之后，该模式将恢复到开始本次事务的 START TRANSACTION语句被执行之前的状态。（如果自动提交模式原来是激活的，结束事务将让你回到自动提交模式；如果它原来是禁用的，结束 当前事务将开始下一个事务。）
    
如果是autocommit模式  ，autocommit的值应该为 1 ，不autocommit 的值是 0 ；请在试验前 确定autocommit 的模式是否开启


> 参考

- [参考官方文档](http://dev.mysql.com/doc/refman/5.7/en/commit.html)












# 98、线上配置相关问题

## 1、根据什么来决定MySQL数据库的配置---核数、CPU、磁盘大小

简单来说如何根据数据量的大小来决定MySQL数据库的配置合理？

## 2、如何设计表的字段？如何评估一条记录占用的磁盘空间大小？误差会有多少？





# 99、问题汇总

- [杂谈自增主键用完了怎么办](https://www.cnblogs.com/rjzheng/p/10669043.html)

- [讲讲mysql表设计要注意啥](https://www.cnblogs.com/rjzheng/p/11174714.html)

- [为什么Mongodb索引用B树，而Mysql用B+树?](https://www.cnblogs.com/rjzheng/p/12316685.html)

- [数据库优化的几个阶段](https://www.cnblogs.com/rjzheng/p/9619855.html)

思路：优化sql和索引-->搭建缓存--->读写分离--->垂直拆分--->水平拆分

- [分库分表后如何部署上线](https://www.cnblogs.com/rjzheng/p/9597810.html)

- [数据库中为什么不推荐使用外键约束](https://www.cnblogs.com/rjzheng/p/9907304.html)

原因：性能问题；并发问题；扩展性问题；技术问题

- [Mysql中select的正确姿势](https://www.cnblogs.com/rjzheng/p/9902911.html)

- [MySQL 优化实施方案](https://www.cnblogs.com/clsn/p/8214048.html)

- [数据库介绍](http://www.cnblogs.com/clsn/p/8038964.html#_label6)

- [MySQL在并发场景下的问题及解决思路](https://www.cnblogs.com/leefreeman/p/8286550.html)

- [何登成的技术博客](http://hedengcheng.com/?cat=6)

- [使用binlog2sql做数据恢复的简单示例](https://www.cnblogs.com/leefreeman/p/7680953.html)



