


有时候如果线上请求超时，应该去关注下慢查询日志，慢查询的分析很简单，先找到慢查询日志文件的位置，然后利用mysqldumpslow去分析。查询慢查询日志信息可以直接通过执行sql命令查看相关变量，常用的sql如下：




```sql
-- 查看慢查询配置
-- slow_query_log  慢查询日志是否开启
-- slow_query_log_file 的值是记录的慢查询日志到文件中
-- long_query_time 指定了慢查询的阈值
-- log_queries_not_using_indexes 是否记录所有没有利用索引的查询
SHOW VARIABLES LIKE '%quer%';

mysql> SHOW VARIABLES LIKE '%quer%';
+----------------------------------------+----------------------+
| Variable_name                          | Value                |
+----------------------------------------+----------------------+
| binlog_rows_query_log_events           | OFF                  |
| ft_query_expansion_limit               | 20                   |
| have_query_cache                       | YES                  |
| log_queries_not_using_indexes          | OFF                  |
| log_throttle_queries_not_using_indexes | 0                    |
| long_query_time                        | 10.000000            |
| query_alloc_block_size                 | 8192                 |
| query_cache_limit                      | 1048576              |
| query_cache_min_res_unit               | 4096                 |
| query_cache_size                       | 1048576              |
| query_cache_type                       | OFF                  |
| query_cache_wlock_invalidate           | OFF                  |
| query_prealloc_size                    | 8192                 |
| slow_query_log                         | ON                   |
| slow_query_log_file                    | ZB-PF115ML1-slow.log |
+----------------------------------------+----------------------+
15 rows in set, 1 warning (0.01 sec)

-- 查看慢查询是日志还是表的形式
SHOW VARIABLES LIKE 'log_output'

mysql> SHOW VARIABLES LIKE 'log_output';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| log_output    | FILE  |
+---------------+-------+
1 row in set, 1 warning (0.00 sec)

-- 查看慢查询的数量
SHOW GLOBAL STATUS LIKE 'slow_queries';

mysql> SHOW GLOBAL STATUS LIKE 'slow_queries';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| Slow_queries  | 0     |
+---------------+-------+
1 row in set (0.01 sec)
```


1、查看慢查询日志是否开启

```sql
SHOW VARIABLES LIKE '%slow_query_log%';

mysql> SHOW VARIABLES LIKE '%slow_query_log%';
+---------------------+----------------------+
| Variable_name       | Value                |
+---------------------+----------------------+
| slow_query_log      | ON                   |
| slow_query_log_file | ZB-PF115ML1-slow.log |
+---------------------+----------------------+
```


没有开启的，开启慢查询日志 SET GLOBAL slow_query_log=1;

3、查看慢查询日志阙值

```sql
SHOW  VARIABLES LIKE '%long_query_time%';

mysql> SHOW  VARIABLES LIKE '%long_query_time%';
+-----------------+-----------+
| Variable_name   | Value     |
+-----------------+-----------+
| long_query_time | 10.000000 |
+-----------------+-----------+
```

这个值表示超过多长时间的SQL语句会被记录到慢查询日志中

4、设置慢查询日志阙值

SET GLOBAL long_query_time=3;

5、查看多少SQL语句超过了阙值

```sql
SHOW GLOBAL STATUS LIKE '%Slow_queries%';

mysql> SHOW GLOBAL STATUS LIKE '%Slow_queries%';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| Slow_queries  | 1     |
+---------------+-------+
```

6、MySQL提供的日志分析工具mysqldumpslow

进入MySQL的安装目录中的bin目录下

执行 ./mysqldumpslow --help 查看帮助命令

常用参考：
- 得到返回记录集最多的10个SQL mysqldumpslow -s r -t 10 slow.log
- 得到访问次数最多的10个SQL mysqldumpslow -s c -t 10 slow.log
- 得到按照时间排序的前10条里面含有左连接的查询语句 mysqldumpslow -s t -t 10 -g "left join" slow.log

使用这些语句时结合| more使用



mysqldumpslow的工具十分简单，我主要用到的是参数如下：

- -t:限制输出的行数，我一般取前十条就够了
- -s:根据什么来排序默认是平均查询时间at，我还经常用到c查询次数，因为查询次数很频繁但是时间不高也是有必要优化的，还有t查询时间，查看那个语句特别卡。
- -v:输出详细信息

例子：mysqldumpslow -v -s t -t 10 mysql_slow.log.2018-11-20-0500




客户端可以用set设置变量的方式让慢查询开启，但是个人不推荐，因为真实操作起来会有一些问题，比如说，重启MySQL后就失效了，或者是开启了慢查询，我又去改变量值，它就不生效了。

编辑MySQL的配置文件： vim /etc/my.cnf

加入如下三行：

```sql
　　slow_query_log=ON
　　slow_query_log_file=/var/lib/mysql/localhost-centos-slow.log
　　long_query_time=3
```

我这里设置的是3秒

 重启MySQL

systemctl restart mysqld;
服务器开一个监控：

tail -f /var/lib/mysql/localhost-centos-slow.log
客户端走一条SQL：

SELECT SLEEP(11)
此时发现sql已经被记录到日志里了。（超过指定秒数才会记录，相等不记录）
```
Time                 Id Command    Argument
# Time: 2019-04-20T15:22:18.547000Z
# User@Host: root[root] @ localhost [::1]  Id:     5
# Query_time: 11.006000  Lock_time: 0.000000 Rows_sent: 1  Rows_examined: 0
use renren_security;
SET timestamp=1555773738;
SELECT SLEEP(11);
```
