
<!-- TOC -->

- [0、build_options：软件信息](#0build_options软件信息)
- [1、processes：当前连接进程信息](#1processes当前连接进程信息)
- [2、clusters：集群信息(重要)](#2clusters集群信息重要)
- [3、databases 当前集群存在几个数据库](#3databases-当前集群存在几个数据库)
- [4、tables](#4tables)
- [5、dictionaries 字典表映射](#5dictionaries-字典表映射)
- [6、parts 每行描述一个数据片段，即数据库表的分区数据存储信息（重要）](#6parts-每行描述一个数据片段即数据库表的分区数据存储信息重要)
- [9、应用](#9应用)
    - [1、如果分区字段按照日期dt开头可以用来判断那一天的数据有更新了](#1如果分区字段按照日期dt开头可以用来判断那一天的数据有更新了)

<!-- /TOC -->

> system库下的系统表

```
┌─name─────────────────┐
│ asynchronous_metrics │
│ build_options        │
│ clusters             │
│ columns              │
│ databases            │
│ dictionaries         │
│ events               │
│ functions            │
│ graphite_retentions  │
│ macros               │
│ merges               │
│ metrics              │
│ models               │
│ mutations            │
│ numbers              │
│ numbers_mt           │
│ one                  │
│ parts                │
│ parts_columns        │
│ processes            │
│ replicas             │
│ replication_queue    │
│ settings             │
│ tables               │
│ zookeeper            │
└──────────────────────┘
```

- [Clickhouse System Table](https://cloud.tencent.com/developer/article/1400100)
- [ClickHouse官方文档翻译_操作-工具-系统表](https://aop.pub/docs/clickhouse/v20.4/operations/system-tables/)

# 0、build_options：软件信息

# 1、processes：当前连接进程信息


```
DESCRIBE TABLE processes

┌─name─────────────────┬─type────┬─default_type─┬─default_expression─┐
│ is_initial_query     │ UInt8   │              │                    │
│ user                 │ String  │              │                    │
│ query_id             │ String  │              │                    │
│ address              │ String  │              │                    │
│ port                 │ UInt16  │              │                    │
│ initial_user         │ String  │              │                    │
│ initial_query_id     │ String  │              │                    │
│ initial_address      │ String  │              │                    │
│ initial_port         │ UInt16  │              │                    │
│ interface            │ UInt8   │              │                    │
│ os_user              │ String  │              │                    │
│ client_hostname      │ String  │              │                    │
│ client_name          │ String  │              │                    │
│ client_version_major │ UInt64  │              │                    │
│ client_version_minor │ UInt64  │              │                    │
│ client_revision      │ UInt64  │              │                    │
│ http_method          │ UInt8   │              │                    │
│ http_user_agent      │ String  │              │                    │
│ quota_key            │ String  │              │                    │
│ elapsed              │ Float64 │              │                    │
│ is_cancelled         │ UInt8   │              │                    │
│ read_rows            │ UInt64  │              │                    │
│ read_bytes           │ UInt64  │              │                    │
│ total_rows_approx    │ UInt64  │              │                    │
│ written_rows         │ UInt64  │              │                    │
│ written_bytes        │ UInt64  │              │                    │
│ memory_usage         │ Int64   │              │                    │
│ peak_memory_usage    │ Int64   │              │                    │
│ query                │ String  │              │                    │
└──────────────────────┴─────────┴──────────────┴────────────────────┘

```



# 2、clusters：集群信息(重要)

备注：可以用集群的名称来判断当前集群有哪些集群组成，以及每台机器的分片编号

```
DESCRIBE TABLE clusters

┌─name─────────────┬─type───┬─default_type─┬─default_expression─┐
│ cluster          │ String │              │                    │
│ shard_num        │ UInt32 │              │                    │
│ shard_weight     │ UInt32 │              │                    │
│ replica_num      │ UInt32 │              │                    │
│ host_name        │ String │              │                    │
│ host_address     │ String │              │                    │
│ port             │ UInt16 │              │                    │
│ is_local         │ UInt8  │              │                    │
│ user             │ String │              │                    │
│ default_database │ String │              │                    │
└──────────────────┴────────┴──────────────┴────────────────────┘

```

cluster：群集名称

shard_num：集群中的分片编号，从1开始

shard_weight：写入数据时分片的相对权重，1

replica_num：分片中的副本号，从1开始

host_name：主机名，在config中指定

host_address：从DNS获取的主机IP地址

port：用于连接服务器的端口，9000

is_local： 是不是当前所在机器，1（备注：我这显示同一个分片的机器有两个，都显示为1）

user：用于连接服务器的用户的名称，默认default



# 3、databases 当前集群存在几个数据库


```
DESCRIBE TABLE databases

┌─name──────────┬─type───┬─default_type─┬─default_expression─┐
│ name          │ String │              │                    │
│ engine        │ String │              │                    │
│ data_path     │ String │              │                    │
│ metadata_path │ String │              │                    │
└───────────────┴────────┴──────────────┴────────────────────┘

```

name： 数据库的名称，比如test

engine：我这里显示的都是Ordinary

data_path：数据文件存储路径,比如/export/data/clickhouse/data/test/，注意最后一层目录为数据库名称；

metadata_path：数据库的元数据存储路径，比如/export/data/clickhouse/metadata/test/，注意最后一层目录为数据库名称；


# 4、tables

```
DESCRIBE TABLE tables

┌─name──────────┬─type───┬─default_type─┬─default_expression─┐
│ database      │ String │              │                    │
│ name          │ String │              │                    │
│ engine        │ String │              │                    │
│ is_temporary  │ UInt8  │              │                    │
│ data_path     │ String │              │                    │
│ metadata_path │ String │              │                    │
└───────────────┴────────┴──────────────┴────────────────────┘

```

database 表所属的数据库名称

name 表名称，如table1

engine 表引擎，分布表Distributed、local本地表ReplicatedMergeTree等


data_path：表数据文件存储路径,比如/export/data/clickhouse/data/test/table1/

metadata_path：表数据的元数据存储路径，比如/export/data/clickhouse/metadata/table1.sql;






# 5、dictionaries 字典表映射

```
DESCRIBE TABLE dictionaries

┌─name────────────┬─type──────────┬─default_type─┬─default_expression─┐
│ name            │ String        │              │                    │
│ origin          │ String        │              │                    │
│ type            │ String        │              │                    │
│ key             │ String        │              │                    │
│ attribute.names │ Array(String) │              │                    │
│ attribute.types │ Array(String) │              │                    │
│ bytes_allocated │ UInt64        │              │                    │
│ query_count     │ UInt64        │              │                    │
│ hit_rate        │ Float64       │              │                    │
│ element_count   │ UInt64        │              │                    │
│ load_factor     │ Float64       │              │                    │
│ creation_time   │ DateTime      │              │                    │
│ source          │ String        │              │                    │
│ last_exception  │ String        │              │                    │
└─────────────────┴───────────────┴──────────────┴────────────────────┘


```









# 6、parts 每行描述一个数据片段，即数据库表的分区数据存储信息（重要）

备注：这里只显示本地local表（实际存储数据的表），分布表是逻辑概念；


```
DESCRIBE TABLE parts

┌─name──────────────────────────────────┬─type─────┬─default_type─┬─default_expression─┐
│ partition                             │ String   │              │                    │
│ name                                  │ String   │              │                    │
│ active                                │ UInt8    │              │                    │
│ marks                                 │ UInt64   │              │                    │
│ rows                                  │ UInt64   │              │                    │
│ bytes_on_disk                         │ UInt64   │              │                    │
│ data_compressed_bytes                 │ UInt64   │              │                    │
│ data_uncompressed_bytes               │ UInt64   │              │                    │
│ marks_bytes                           │ UInt64   │              │                    │
│ modification_time                     │ DateTime │              │                    │
│ remove_time                           │ DateTime │              │                    │
│ refcount                              │ UInt32   │              │                    │
│ min_date                              │ Date     │              │                    │
│ max_date                              │ Date     │              │                    │
│ partition_id                          │ String   │              │                    │
│ min_block_number                      │ Int64    │              │                    │
│ max_block_number                      │ Int64    │              │                    │
│ level                                 │ UInt32   │              │                    │
│ data_version                          │ UInt64   │              │                    │
│ primary_key_bytes_in_memory           │ UInt64   │              │                    │
│ primary_key_bytes_in_memory_allocated │ UInt64   │              │                    │
│ database                              │ String   │              │                    │
│ table                                 │ String   │              │                    │
│ engine                                │ String   │              │                    │
│ path                                  │ String   │              │                    │
│ bytes                                 │ UInt64   │ ALIAS        │ bytes_on_disk      │
│ marks_size                            │ UInt64   │ ALIAS        │ marks_bytes        │
└───────────────────────────────────────┴──────────┴──────────────┴────────────────────┘

```



基于这个分区信息可以获得分区数据最后更新的时间；


partition (String) – 分区的名称，比如'2017-01-03'；

name (String) – 数据片段的名称，20170103_1_1_0、20170103_2_2_0等，一个分区会存在多个数据片段；

active (UInt8) – 指示数据片段是否处于激活状态的标志。如果数据片段是激活的，则在表中可以使用它。否则它是被删除状态。未激活的数据片段在合并后仍然存在。


marks (UInt64) – 标记的数量。要获得数据片段的大概行数，可以用索引粒度乘以标记（通常是8192）（这个提示不适用于自适应粒度）

rows (UInt64) – 行数

bytes_on_disk (UInt64) – 所有数据片段文件的总大小，单位字节

data_compressed_bytes (UInt64) – 数据片段占用压缩数据的总大小。所有的附属文件不包括在内（例如，标记文件）

data_uncompressed_bytes (UInt64) – 数据片段中未压缩数据的总大小。所有的附属文件不包括在内（例如，标记文件）

marks_bytes (UInt64) – 标记的文件的大小

modification_time (DateTime) – 修改包含数据片段的目录的时间。这通常对应于创建数据部分的时间

remove_time (DateTime) – 数据片段变为非激活状态的时间

refcount (UInt32) – 使用数据片段的位置数量。大于2的值表示数据部分用于查询或合并。


min_date (Date) – 数据片段中日期键的最小值。

max_date (Date) – 数据片段中日期键的最大值。

partition_id (String) – 分区的ID。

min_block_number (UInt64) – 合并后组成当前片段的最小数据块编号。

max_block_number (UInt64) – 合并后组成当前片段的最大数据块编号。

level (UInt32) – 合并树的深度。零意味着当前片段是通过插入创建的，而不是通过合并其他片段创建的。

data_version (UInt64) – 用于确定应用于数据片段变化的数字（变化版本高于data_version)。

primary_key_bytes_in_memory (UInt64) – 主键值使用的内存量（以字节为单位）。

primary_key_bytes_in_memory_allocated (UInt64) – 为主键值保留的内存量（以字节为单位）。


database (String) – 数据库名称

table (String) – 表名

engine (String) – 表引擎，没有参数。

path (String) – 数据片段文件的文件夹绝对路径。

disk (String) – 存储数据部分的磁盘的名称。



# 9、应用

## 1、如果分区字段按照日期dt开头可以用来判断那一天的数据有更新了

- 1、集群任一url链接解析出集群的全部IP；
    - 1、show create table获取获取集群名称clusterName；
    - 2、根据集群名称获取全部ip:select * from system.clusters where cluster=clusterName;
- 2、提取出分区字段。多个字段分区：PARTITION BY (dt, dp, batch) 或者 单字段分区PARTITION BY dt；
- 3、循环调用IP列表查询system.parts获取日期分区数据的每天的修改时间


```
┌─partition────┬─name───────────┬─active─┬─marks─┬───rows─┬─bytes_on_disk─┬─data_compressed_bytes─┬─data_uncompressed_bytes─┬─marks_bytes─┬───modification_time─┬─────────remove_time─┬─refcount─┬───min_date─┬───max_date─┬─partition_id─┬─min_block_number─┬─max_block_number─┬─level─┬─data_version─┬─primary_key_bytes_in_memory─┬─primary_key_bytes_in_memory_allocated─┬─database─┬─table──────────────────────────────────┬─engine──────────────┬─path────────────────────────────────────────────────────────────────────────────────────┐
│ '2017-01-03' │ 20170103_1_1_0 │      1 │    13 │ 100000 │       1332510 │               1327237 │                12374472 │        3328 │ 2020-06-01 18:21:19 │ 0000-00-00 00:00:00 │        1 │ 2017-01-03 │ 2017-01-03 │ 20170103     │                1 │                1 │     0 │            1 │                         357 │                                   512 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170103_1_1_0/ │
│ '2017-01-03' │ 20170103_2_2_0 │      1 │     3 │  22431 │        294238 │                291672 │                 2802873 │         768 │ 2020-06-01 18:21:20 │ 0000-00-00 00:00:00 │        1 │ 2017-01-03 │ 2017-01-03 │ 20170103     │                2 │                2 │     0 │            2 │                          76 │                                   192 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170103_2_2_0/ │
│ '2017-01-07' │ 20170107_1_1_0 │      1 │    13 │ 100000 │       1287228 │               1281957 │                12423258 │        3328 │ 2020-06-01 19:29:20 │ 0000-00-00 00:00:00 │        1 │ 2017-01-07 │ 2017-01-07 │ 20170107     │                1 │                1 │     0 │            1 │                         356 │                                   512 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170107_1_1_0/ │
│ '2017-01-07' │ 20170107_2_2_0 │      1 │     3 │  24043 │        305041 │                302472 │                 3009648 │         768 │ 2020-06-01 19:29:24 │ 0000-00-00 00:00:00 │        1 │ 2017-01-07 │ 2017-01-07 │ 20170107     │                2 │                2 │     0 │            2 │                          79 │                                   192 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170107_2_2_0/ │
│ '2017-01-11' │ 20170111_1_1_0 │      1 │    13 │ 100000 │       1267354 │               1262078 │                12406860 │        3328 │ 2020-06-01 20:29:50 │ 0000-00-00 00:00:00 │        1 │ 2017-01-11 │ 2017-01-11 │ 20170111     │                1 │                1 │     0 │            1 │                         360 │                                   512 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170111_1_1_0/ │
│ '2017-01-11' │ 20170111_2_2_0 │      1 │     3 │  21328 │        273347 │                270767 │                 2675519 │         768 │ 2020-06-01 20:29:52 │ 0000-00-00 00:00:00 │        1 │ 2017-01-11 │ 2017-01-11 │ 20170111     │                2 │                2 │     0 │            2 │                          76 │                                   192 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170111_2_2_0/ │
│ '2017-01-15' │ 20170115_1_1_0 │      1 │    13 │ 100000 │       1295391 │               1290116 │                12361739 │        3328 │ 2020-06-01 21:28:30 │ 0000-00-00 00:00:00 │        1 │ 2017-01-15 │ 2017-01-15 │ 20170115     │                1 │                1 │     0 │            1 │                         359 │                                   512 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170115_1_1_0/ │
│ '2017-01-15' │ 20170115_2_2_0 │      1 │     2 │  14746 │        181398 │                179100 │                 1867986 │         512 │ 2020-06-01 21:28:31 │ 0000-00-00 00:00:00 │        1 │ 2017-01-15 │ 2017-01-15 │ 20170115     │                2 │                2 │     0 │            2 │                          50 │                                   128 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170115_2_2_0/ │
│ '2017-01-19' │ 20170119_1_1_0 │      1 │    13 │ 100000 │       1285790 │               1280516 │                12341274 │        3328 │ 2020-06-01 22:17:42 │ 0000-00-00 00:00:00 │        1 │ 2017-01-19 │ 2017-01-19 │ 20170119     │                1 │                1 │     0 │            1 │                         358 │                                   512 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170119_1_1_0/ │
│ '2017-01-19' │ 20170119_2_2_0 │      1 │     1 │   5877 │         73331 │                 71546 │                  758195 │         256 │ 2020-06-01 22:17:42 │ 0000-00-00 00:00:00 │        1 │ 2017-01-19 │ 2017-01-19 │ 20170119     │                2 │                2 │     0 │            2 │                          23 │                                   128 │ db1      │ table1_local │ ReplicatedMergeTree │ /export/data/clickhouse/data/db1/table1_local/20170119_2_2_0/ │
└──────────────┴────────────────┴────────┴───────┴────────┴───────────────┴───────────────────────┴─────────────────────────┴─────────────┴─────────────────────┴─────────────────────┴──────────┴────────────┴────────────┴──────────────┴──────────────────┴──────────────────┴───────┴──────────────┴─────────────────────────────┴───────────────────────────────────────┴──────────┴────────────────────────────────────────┴─────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────┘


```
