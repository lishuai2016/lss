


JDBC 调用过程如下：APP -> ORM -> JDBC -> PROXY -> MySQL。如果要完成数据的分库分表，可以在这五层任意地方进行，Sharding-Jdbc 是在 JDBC 层进行分库分表，Sharding-Proxy 是在 PROXY 进行分库分表。




# 1、Sharding-Jdbc 包结构

Sharding-Jdbc 是一个轻量级的分库分表框架，使用时最关键的是配制分库分表策略，其余的和使用普通的 MySQL 驱动一样，几乎不用改代码。


```
sharding-jdbc  
    ├── sharding-jdbc-core      重写DataSource/Connection/Statement/ResultSet四大对象
    └── sharding-jdbc-orchestration        配置中心
sharding-core
    ├── sharding-core-api       接口和配置类	
    ├── sharding-core-common    通用分片策略实现...
    ├── sharding-core-route     SQL路由，核心类StatementRoutingEngine
    ├── sharding-core-rewrite   SQL改写，核心类ShardingSQLRewriteEngine
    ├── sharding-core-execute   SQL执行，核心类ShardingExecuteEngine
    └── sharding-core-merge     结果合并，核心类MergeEngine
shardingsphere-sql-parser 
    ├── shardingsphere-sql-parser-spi       SQLParserEntry，用于初始化SQLParser
    ├── shardingsphere-sql-parser-engine    SQL解析，核心类SQLParseEngine
    ├── shardingsphere-sql-parser-relation
    └── shardingsphere-sql-parser-mysql     MySQL解析器，核心类MySQLParserEntry和MySQLParser
shardingsphere-underlying           基础接口和api
    ├── shardingsphere-rewrite      SQLRewriteEngine接口
    ├── shardingsphere-execute      QueryResult查询结果
    └── shardingsphere-merge        MergeEngine接口
shardingsphere-spi                  SPI加载工具类
sharding-transaction
    ├── sharding-transaction-core   接口ShardingTransactionManager，SPI加载		
    ├── sharding-transaction-2pc    实现类XAShardingTransactionManager
    └── sharding-transaction-base   实现类SeataATShardingTransactionManager

```











# 参考

- [Sharding-Jdbc 源码分析](https://www.cnblogs.com/binarylei/p/12234545.html)



