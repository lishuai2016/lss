

<!-- TOC -->

- [1、系统设计](#1系统设计)
    - [01、系统架构图](#01系统架构图)
    - [02、Message设计](#02message设计)
    - [03、Broker设计](#03broker设计)
    - [04、Registry Center设计](#04registry-center设计)
    - [05、Producer设计](#05producer设计)
    - [06、Consumer设计](#06consumer设计)
    - [07、延时消息](#07延时消息)
    - [08、事务性](#08事务性)
    - [09、失败重试](#09失败重试)
    - [10、超时控制](#10超时控制)
    - [11、海量数据堆积](#11海量数据堆积)
    - [12、数据表结构](#12数据表结构)
- [2、源码分析](#2源码分析)
    - [1、IXxlMqBroker生产者发送消息/消费者拉取消息接口](#1ixxlmqbroker生产者发送消息消费者拉取消息接口)
    - [2、客户端配置入口XxlMqClientFactory](#2客户端配置入口xxlmqclientfactory)
- [参考](#参考)

<!-- /TOC -->


>  总结

>> 1、生产者工作原理

消息中心启动一个rpc服务提供者，生产者作为消费端调用，把消息发送到消息中心一侧，消息中心接受到以后先把消息存到内存中去，由后台线程读取内存队列中的消息，然后存入数据库。


>> 2、消费者工作原理

消费者在启动的时候，根据注解消费者配置的个数，构建多个消费线程，循环通过rpc服务拉取消息中心的消息进程处理，处理完成后。再把结果通过rpc调用回传给消息中心

备注：启动的broker本身也是一格注册中心的功能，client端创建rpc代理的时候轮询选择一个进行通信。[如何实现分片存储？？？]

总结：[消息存在数据库中]。消息中心变成一个服务提供者，而生产者和消费者变成了rpc远程调用的消费端，通过代理把消息传给消息中心，或者把消息从消息中心拉取。



# 1、系统设计

## 01、系统架构图

![](../../pic/2020-04-12-17-01-12.png)

> 1、角色解释:

- Message : 消息实体;
- Broker : 消息代理中心, 负责连接Producer和Consumer;
- Topic : 消息主题, 每个消息队列的唯一性标示;
- Topic segment : 消息分段, 同一个Topic的消息队列,将会根据订阅的Consumer进行分片分组,每个Consumer拥有的消息片即一个segment;
- Producer : 消息生产者, 绑定一个消息Topic, 并向该Topic消息队列中生产消息;
- Consumer : 消息消费者, 绑定一个消息Topic, 只能消费该Topic消息队列中的消息;
- Consumer Group : 消费者分组，隔离消息；同一个Topic下一条消息消费一次；


> 2、架构图模块解读:

>> 1、Server

- Broker: 消息代理中心, 系统核心组成模块, 负责接受消息生产者Producer推送生产的消息, 同时负责提供RPC服务供消费者Consumer使用来消费消息;
- Message Queue: 消息存储模块, 目前底层使用mysql消息表;

>> 2、Registry Center

- Broker Registry Center: Broker注册中心子模块, 供Broker注册RPC服务使用;
- Consumer Registry Center: Consumer注册中心子模块, 供Consumer注册消费节点使用;

>> 3、Client

- Producer: 消息生产者模块, 负责提供API接口供开发者调用,并生成和发送队列消息;
- Consumer: 消息消费者模块, 负责订阅消息并消息;

## 02、Message设计

![](../../pic/2020-04-12-17-05-51.png)



## 03、Broker设计

Broker(消息代理中心)：系统核心组成模块, 负责接受消息生产者Producer推送生产的消息, 同时负责提供RPC服务供消费者Consumer使用来消费消息；

Broker支持集群部署, 集群节点之间地位平等, 集群部署情况下可大大提高系统的消息吞吐量。

Broker通过内置注册中心实现集群功能, 各节点在启动时会自动注册到注册中心, Producer或Consumer在生产消息或者消费消息时,将会通过内置注册中心自动感知到在线的Broker节点。

Broker在接收到Produce的生产消息的RPC调用时, 并不会立即存储该消息, 而是立即push到内存队列中, 同时立即响应RPC调用。 内存队列将会异步将队列中的消息数据存储到Mysql中。

Broker在接收到 “消息锁定” 等同步RPC调用时, 将会触发同步调用, 采用乐观锁方式锁定消息;

## 04、Registry Center设计

Registry Center(注册中心)主要分为两个子模块: Broker注册中心、Consumer注册中心;

- Broker注册中心子模块: 供Broker注册RPC服务使用;
- Consumer注册中心子模块: 供Consumer注册消费节点使用;

## 05、Producer设计

Producer(消息生产者), 兼容“异步批量多线程生产”+“同步生产”两种方式，提升消息发送性能；

底层通讯全异步化：消息新增 + 消息新增接受 + 消息回调 + 消息回调接受；仅批量PULL消息与锁消息非异步；

## 06、Consumer设计

![](../../pic/2020-04-12-17-08-24.png)

消费者通过 “多线程轮训 + 消息分片 + PULL + 消息锁定” 的方式来实现:

- 多线程轮训: 该模式下每个Consumer将会存在一个线程, 如存在多个Consumer, 多个Consumer将会并行消息同一主题下的消息, 大大提高消息的消费速度;

- 轮训延时自适应：线程轮训方式PULL消息，如若获取不到消息将会主动休眠，休眠时间依次递增10s，最长60s；即消息生产之后距离被消费存在 0~60s 的时间差，分钟范围内；

- 消息分片 : 队列中消息将会按照 “Registry Center” 中注册的Consumer列表顺序进行消息分段, 保证一条消息只会被分配给其中一个Consumer, 每个Consumer只会消费分配给自己的消息。 因此在多个Consumer并发消息时, 可以保证同一条消息不被多个Consumer竞争来重复消息。

    - 1、分片函数: MOD(“消息分片ID”, #{在线消费者总数}) = #{当前消费者排名} ,
    - 2、分片逻辑解释: 每个Consumer通过注册中心感知到在线所有的Consumer, 计算出在线Consumer总数total, 以及当前Consumer在所有Consumer中的排名rank; 把消息分片ID对在线Consumer总数total进行取模, 余数和当前Consumer排名rank一致的消息认定为分配给自己的消息;

- PULL : 每个Consumer将会轮训PULL消息分片分配给自己的消息, 顺序消费。

- 消息锁定: Consumer在消费每一条消息时,开启事务时，将会主动进行消息锁定, 通过数据库乐观锁来实现, 锁定成功后消息状态变更为执行中状态, 将不会被Consumer再次PULL到。因此, 可以更进一步保证每条消息只会被消费一次;

- 消息状态和日志: 消息执行结束后, 将会调用Broker的RPC服务修改消息状态并追加消息日志, Broker将会通过内存队列方式, 异步消息队列中变更存储到数据库中。

## 07、延时消息

支持设置消息的延迟生效时间, 到达设置的生效时间时该消息才会被消费；适用于延时消费场景，如订单超时取消等;

## 08、事务性

消费者开启事务开关后,消息事务性保证只会成功执行一次;

## 09、失败重试

支持设置消息的重试次数, 在消息执行失败后将会按照设置的值进行消息重试执行,直至重试次数耗尽或者执行成功;

## 10、超时控制

支持自定义消息超时时间，消息消费超时将会主动中断；

## 11、海量数据堆积

消息中心数据库，原生兼容支持 “MySQL、TIDB” 两种存储方式，前者支持千万级消息堆积，后者支持百亿级别消息堆积（TIDB理论上无上限）；

可视情况选择使用，当选择TIDB时，仅需要修改消息中心数据库连接jdbc地址配置即可，其他部分如SQL和驱动兼容MySQL和TIDB使用，不需修改。


## 12、数据表结构

```sql
CREATE database if NOT EXISTS `xxl-mq` default character set utf8 collate utf8_general_ci;
use `xxl-mq`;


-- 业务线表，应该是标识是那个应用程序
CREATE TABLE `xxl_mq_biz` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bizName` varchar(64) NOT NULL,
  `order` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 在那个应用下创建一个topic
CREATE TABLE `xxl_mq_topic` (
  `topic` varchar(255) NOT NULL,
  `bizId` int(11) NOT NULL DEFAULT '0',
  `author` varchar(64) DEFAULT NULL,
  `alarmEmails` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 保存消息表
CREATE TABLE `xxl_mq_message` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `topic` varchar(255) NOT NULL,
  `group` varchar(255) NOT NULL,
  `data` text NOT NULL,
  `status` varchar(32) NOT NULL,
  `retryCount` int(11) NOT NULL DEFAULT '0',
  `shardingId` bigint(11) NOT NULL DEFAULT '0',
  `timeout` int(11) NOT NULL DEFAULT '0',
  `effectTime` datetime NOT NULL,
  `addTime` datetime NOT NULL,
  `log` text NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `I_shardingId` (`shardingId`) USING BTREE,
  KEY `I_t_g_f_s` (`topic`,`group`,`status`,`effectTime`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- broker注册列表。格式：5	com.xxl.mq.client.broker.IXxlMqBroker	["192.168.31.53:7080"]

CREATE TABLE `xxl_mq_common_registry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key` varchar(255) NOT NULL COMMENT '注册Key',
  `data` text NOT NULL COMMENT '注册Value有效数据',
  PRIMARY KEY (`id`),
  UNIQUE KEY `I_k` (`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 7	com.xxl.mq.client.broker.IXxlMqBroker	192.168.31.53:7080	2020-04-12 17:53:59

CREATE TABLE `xxl_mq_common_registry_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key` varchar(255) NOT NULL COMMENT '注册Key',
  `value` varchar(255) NOT NULL COMMENT '注册Value',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `I_k_v` (`key`,`value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 注册消息
CREATE TABLE `xxl_mq_common_registry_message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `data` text NOT NULL COMMENT '消息内容',
  `addTime` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


```


# 2、源码分析

## 1、IXxlMqBroker生产者发送消息/消费者拉取消息接口

![](../../pic/2020-04-12-17-38-10.png)


```java
//
int addMessages(List<XxlMqMessage> messages);

//分片数据，批量： MOD( "分片ID", #{consumerTotal}) = #{consumerRank}, 值 consumerTotal>1 时生效
List<XxlMqMessage> pullNewMessage(String topic, String group, int consumerRank, int consumerTotal, int pagesize);

//锁定消息，单个；XxlMqMessageStatus：NEW >>> RUNNING
int lockMessage(long id, String appendLog);

//回调消息，批量；XxlMqMessageStatus：RUNNING >>> SUCCESS/FAIL
int callbackMessages(List<XxlMqMessage> messages);
```


## 2、客户端配置入口XxlMqClientFactory








































# 参考

- [xxl-mq](https://www.xuxueli.com/xxl-mq/)


