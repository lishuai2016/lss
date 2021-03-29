备注：创建一个分布表以及它对于的本地local表，假设集群由八台机器组成，IP最后一位从190~197，其中一半机器作为分区的备份


> 构建一个分布表

```sql
CREATE TABLE db1.table1 (
	money Float64,
	username String,
	id String,
	dt Date
) ENGINE = Distributed (
	default_clickhouse_cluster,
	'db1',
	'table1_local',
	(toRelativeDayNum(dt) + 3) % 4
)

```

备注：这里使用dt作为路由分片字段，把数据按照dt划分为四个分区即分片进行存储，toRelativeDayNum函数把当前日期转化为绝对日期数字，然后进行取模运算。结果就是比如20200101取模为0，对应的分区1，则20200102对应分区2，依次类推20200103分区3,20200104分区4，20200105对应分区1，其他类似。


> 本地local表

```sql

CREATE TABLE db1.table1_local (
	money Float64,
	username String,
	id String,
	dt Date
) ENGINE = ReplicatedMergeTree (
	'/clickhouse/tables/db1/shard0/table1',
	'190'
) PARTITION BY dt
  ORDER BY(id) SETTINGS index_granularity = 8192

ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard0/table1', '190') 
ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard1/table1', '191')
ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard2/table1', '192')
ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard3/table1', '193')

ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard0/table1', '194')
ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard1/table1', '195')
ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard2/table1', '196')
ENGINE = ReplicatedMergeTree('/clickhouse/tables/db1/shard3/table1', '197')

```

备注：如上，190~193存储四个分片。194~197存储四个分片的备份数据，做容灾。


备注：
- 1、版本信息ClickHouse client version 20.1.8.41
- 2、ck的表名称大小写敏感；
- 3、分布表的引擎参数，数据分片键sharding_key可能和具体本地表partition by的字段不一致。（数据分片逻辑概念和数据分区物理概念，把指定分区的数据物理空间在一起）


- [自定义分区键](https://clickhouse.tech/docs/zh/engines/table-engines/mergetree-family/custom-partitioning-key/)