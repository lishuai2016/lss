
ClickHouse函数整理（详细） https://blog.csdn.net/u012111465/article/details/85250030

ck系列：  https://cloud.tencent.com/developer/column/84496



ClickHouse快速上手 https://zhuanlan.zhihu.com/p/34669883

clickhouse单机安装及配置 http://www.wuzhq.com/2019/12/12/install-clickhouse/

- [ClickHouse Distribute 引擎深度解读](http://www.clickhouse.com.cn/topic/5a3e768d2141c2917483557e)

- [ClickHouse存储结构及索引详解](http://www.clickhouse.com.cn/topic/5ffec51eba8f16b55dd0ffe4)

```sql
CREATE TABLE test_merge_tree
(
    `Id` UInt64,
    `Birthday` Date,
    `Name` String
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(Birthday)
ORDER BY (Id, Name)
SETTINGS index_granularity = 2
```


ORDER BY排序键。用于指定数据以何种方式排序，默认情况下排序键和主键相同。

如何提取出其他字段？（不在排序键中的字段）



- [Clickhouse_Table_Engine_总结](http://www.clickhouse.com.cn/topic/5c74dc8169c415035e68d4e1)



- [clickhouse数据目录详解](http://www.clickhouse.com.cn/topic/5b2ccbb49d28dfde2ddc6193)




- [ClickHouse 基础介绍](http://www.clickhouse.com.cn/topic/5a3bb3e12141c29174835568)





# 参考

- [列式数据库 ClickHouse v20.8 使用教程](https://www.bookstack.cn/read/clickhouse-20.8-zh/6bf5dde66788dc6f.md)

- [实时大数据分析引擎 ClickHouse 介绍](https://toutiao.io/posts/q7phx6/preview)


- [ClickHouse 使用指南](https://blog.alexanderliu.top/posts/clickhouse-user-guide.html)

- [clickHouse的文档中文翻译(几年前的)](https://github.com/sparkthu/clickhouse-doc-cn)


- [官方文档](https://clickhouse.tech/docs/en/operations/system-tables/parts/)




https://clickhouse.tech/docs/zh/getting-started/install/

https://www.cnblogs.com/freeweb/p/9343011.html

https://www.howtoing.com/how-to-install-and-use-clickhouse-on-ubuntu-18-04


ClickHouse常见问题及其解决方案 https://blog.csdn.net/weixin_43786255/article/details/106417641

ClickHouse 用户名密码设置 https://www.jianshu.com/p/e339336e7bb9

Clickhouse 访问控制和账号管理 https://blog.csdn.net/vkingnew/article/details/107308936

ClickHouse学习系列之二【用户权限管理】 https://www.cnblogs.com/zhoujinyi/p/12613026.html

centos7下使用rpm包安装clickhouse https://www.cnblogs.com/freeweb/p/9343011.html

clickhouse的安装和使用（单机+集群） https://blog.csdn.net/wyee000/article/details/90027301



> 安装后主要的目录分布如下：

/etc/clickhouse-server   clickhouse服务的配置文件目录，包括：config.xml和users.xml

/etc/clickhouse-client    clickhouse客户端的配置文件目录，里面只有一个config.xml并且默认为空

/var/lib/clickhouse     clickhouse默认数据目录

/var/log/clickhouse-server    clickhouse默认日志目录

/etc/init.d/clickhouse-server   clickhouse启动shell脚本，用来方便启动服务的.

/etc/security/limits.d/clickhouse.conf   最大文件打开数的配置，这个在config.xml也可以配置

/etc/cron.d/clickhouse-server    clickhouse定时任务配置，默认没有任务，但是如果文件不存在启动会报错.

/usr/bin    clickhouse编译好的可执行文件目录，主要有下面几个：

clickhouse     clickhouse主程序可执行文件

clickhouse-compressor

clickhouse-client      是一个软链指向clickhouse，主要是客户端连接操作使用

clickhouse-server     是一个软链接指向clickhouse，主要是服务操作使用

注意：虽然clickhouse-client是一个软链，但是执行这个软链是进入默认客户端，但是执行clickhouse却不行，需要加--client参数，这个需要注意，还是客户端和服务命令分开使用比较好.

根据上面目录我们可以将这些主要的文件收集下来，打成安装包，那么其他机器安装就完全不需要重新安装了，直接执行编译好的二进制即可，并且这个二进制不依赖其他的系统库，这里使用tree打包后的目录结构如下：
