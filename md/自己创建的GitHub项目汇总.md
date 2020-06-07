# 自己创建的GitHub项目汇总

<!-- TOC -->

- [自己创建的GitHub项目汇总](#自己创建的github项目汇总)
- [didi](#didi)
- [ls](#ls)
- [ls-bigdata](#ls-bigdata)
- [ls-elasticsearch](#ls-elasticsearch)
- [ls-es-hbase](#ls-es-hbase)
- [ls-py](#ls-py)
- [ls-spark](#ls-spark)
- [ls-springboot-learn](#ls-springboot-learn)
- [ls-springcloud-learn](#ls-springcloud-learn)
- [ls-springconfig-repo](#ls-springconfig-repo)
- [WebRTC01](#webrtc01)
- [源码学习项目](#源码学习项目)
    - [0、mybatis](#0mybatis)
    - [1、netty网络通信相关](#1netty网络通信相关)
    - [2、dubbo[采用netty网络通信]](#2dubbo采用netty网络通信)
    - [3、xxl[采用netty网络通信]](#3xxl采用netty网络通信)
    - [4、druid数据库连接池](#4druid数据库连接池)
    - [5、java工具包](#5java工具包)
    - [6、springboot后台项目](#6springboot后台项目)
    - [7、rocketmq消息队列[采用netty网络通信]](#7rocketmq消息队列采用netty网络通信)
    - [8、sofa-bolt和SOFARPC[采用netty网络通信]](#8sofa-bolt和sofarpc采用netty网络通信)
    - [9、alibaba](#9alibaba)
    - [9、okhttp发送http请求的封装](#9okhttp发送http请求的封装)
    - [hdfs往clickhouse同步数据](#hdfs往clickhouse同步数据)
    - [apollo分布式配置中心](#apollo分布式配置中心)
    - [eureka分布式注册中心](#eureka分布式注册中心)
- [spring-cloud-netflix](#spring-cloud-netflix)
    - [网关](#网关)
    - [spring](#spring)
    - [SQL解析器](#sql解析器)
    - [rpc](#rpc)
    - [Hadoop的RPC框架avro使用Netty作为通信框架](#hadoop的rpc框架avro使用netty作为通信框架)

<!-- /TOC -->

# didi
一个阿里校招的面试题

题目：模拟用户滴滴打车场景，根据用户的经纬度信息查找距离用户最近的车辆。
接受一个带有经纬度信息的http请求，查找距离该经度最近的车辆。


方案：
根据我查得的信息，滴滴司机端那边会定时刷新自己的地理，然后上传到服务器；
由于没有数据，我这里通过以用户的位置坐标为基准，在其附近随机生成30个点代表附近的车辆，
每次刷新由于是随机生成的点，动态模拟用户身边的车辆；
我对这30个点，根据其与用户的距离进行排序，并在地图上标注出来1~30，点击红色的标注点还可以看到距离用户的具体距离数据。


注释：要是考虑真实司机经纬度数据从数据库取的话，我的思路是根据客户传回来的经纬度，
以该客户的经纬度点为圆心，以R为半径，取出数据库中的附近所有车辆经纬度信息，然后根据距离的远近进行地图上的标注。


地址：
https://github.com/lishuai2016/didi.git

# ls
自己的课设demo

https://github.com/lishuai2016/ls


# ls-bigdata
大数据各个组件的开发学习,其中包括：
- zookeeper
- Hadoop
- MapReduce
- flume
- kafka
- HBASE
- storm
- spark


https://github.com/lishuai2016/ls-bigdata

升级版
https://github.com/lishuai2016/ls-bigdata-learn

# ls-elasticsearch
elasticsearch实践demo
这只是一个dubbo的服务提供端
https://github.com/lishuai2016/ls-elasticsearch

服务的消费端
ls-dubbo-consumer
https://github.com/lishuai2016/ls-dubbo-consumer

# ls-es-hbase
一个类似百度的搜索引擎，通过es和hbase实现
https://github.com/lishuai2016/ls-es-hbase


# ls-py
自己的Python学习分支
https://github.com/lishuai2016/ls-py

# ls-spark
https://github.com/lishuai2016/ls-spark
spark学习

# ls-springboot-learn
项目地址：
https://github.com/lishuai2016/ls-springboot-learn

# ls-springcloud-learn
项目地址：
https://github.com/lishuai2016/ls-springcloud-learn

# ls-springconfig-repo
地址：https://gitee.com/2016shuai/ls-springconfig-repo
springcloud的远程仓库


# WebRTC01
一个简单的WebRTC实例
https://github.com/lishuai2016/WebRTC01




# 源码学习项目

## 0、mybatis

- [mybatis-3](https://github.com/lishuai2016/mybatis-3/tree/ls_b_f_master_20190414)

分支：ls_b_f_master_20190414


- [Mybatis-PageHelper](https://github.com/lishuai2016/Mybatis-PageHelper/tree/b_f_5.1.6_20191024)

- [pagehelper-spring-boot](https://github.com/lishuai2016/pagehelper-spring-boot)




mybatis分页插件

- [mybatis-plus](https://github.com/lishuai2016/mybatis-plus/tree/b_f_3.2.0_ls_20200112)



## 1、netty网络通信相关

- [ls-netty源码学习项目-废弃](https://github.com/lishuai2016/ls-netty)

- [ls-netty源码学习项目](https://github.com/lishuai2016/netty-jike-learn)


- [自己创建的依托于netty开发的脚手架](https://github.com/lishuai2016/lss-netty)


- [极客时间-Netty源码剖析与实战-源码](https://github.com/lishuai2016/netty-jike-learn)


netty应用实例项目

- [cim](https://github.com/lishuai2016/cim)

备注：dubbo以及xxl相关的项目也有用到。


- [koalas-rpc](https://gitee.com/2016shuai/koalas-rpc)

企业生产级百亿日PV高可用可拓展的RPC框架。理论上并发数量接近服务器带宽，客户端采用thrift协议，服务端支持netty和thrift的TThreadedSelectorServer半同步半异步线程模型，支持动态扩容，服务上下线，权重动态，可用性配置，页面流量统计，支持trace跟踪等，天然接入cat支持数据大盘展示等，持续为个人以及中小型公司提供可靠的RPC框架技术方案


其他

- [netty-socketio](https://github.com/lishuai2016/netty-socketio)

- [netty-in-action](https://github.com/lishuai2016/netty-in-action)

- [ 一个基于Netty的轻量级Web容器](https://github.com/lishuai2016/redant/tree/b_f_master_ls_20200112)


- [java AIO通信框架](https://gitee.com/2016shuai/smart-socket)

- [Netty权威指南(第2版)李林锋/著 源码](https://gitee.com/2016shuai/phei-netty)

- [使用spring websocket实现将tomcat日志实时输出到web页面](https://github.com/lishuai2016/springWebsockeForTomcatLog)

- [qiqiim-server](https://gitee.com/2016shuai/qiqiim-server)




## 2、dubbo[采用netty网络通信]

- [incubator-dubbo](https://github.com/lishuai2016/incubator-dubbo)

本地地址：D:\opencode\incubator-dubbo



## 3、xxl[采用netty网络通信]

- [gitee地址](https://gitee.com/xuxueli0323/projects)

- [xxl开源社区文档](https://www.xuxueli.com/page/projects.html)

- [xxl-job_2.1.1源码学习](https://gitee.com/2016shuai/xxl-job/tree/b_f_2.1.1_20191218/)

- [xxl-rpc_1.5.0源码学习](https://gitee.com/2016shuai/xxl-rpc/tree/b_f_1.5.0_20191220/)

- [xxl-registry_1.1源码学习](https://gitee.com/2016shuai/xxl-registry/tree/b_f_1.1_20191220/)

- [xxl-conf-tag1.6.1源码学习](https://gitee.com/2016shuai/xxl-conf/tree/b_f_tag1.6.1_20191221/)

- [xxl-sso_1.1源码学习](https://gitee.com/2016shuai/xxl-sso/tree/b_f_1.1_20191222/)





## 4、druid数据库连接池

- [druid](https://github.com/lishuai2016/druid/tree/b_f_1.1.10_20191228)



## 5、java工具包

- [hutool](https://gitee.com/2016shuai/hutool/tree/b_f_v5_master_20190102/)




## 6、springboot后台项目

- [renren-security](https://gitee.com/2016shuai/renren-security/tree/20181124_ls/)

- [RuoYi](https://gitee.com/2016shuai/RuoYi/tree/b_f_master_20181124_lishuai/)


- [spring-boot-task](https://gitee.com/2016shuai/spring-boot-task)

基于spring-boot 2.x + quartz 的CRUD任务管理系统


其他

- [后台管理](https://gitee.com/2016shuai/springboot-plus)

- [open-cloud](https://gitee.com/2016shuai/open-cloud)



- [zheng](https://gitee.com/2016shuai/zheng/tree/b_from_master_20181124_lishuai/)


- [spring-boot-quartz](https://gitee.com/2016shuai/spring-boot-quartz)

基于spring-boot+quartz的CRUD动态任务管理系统，适用于中小项目。





## 7、rocketmq消息队列[采用netty网络通信]

- [rocketmq](https://github.com/lishuai2016/rocketmq/tree/b_f_4.6_20200111)


- [rocketmq-externals](https://github.com/lishuai2016/rocketmq-externals/tree/b_f_master_ls_20200112)

外部扩展项目


## 8、sofa-bolt和SOFARPC[采用netty网络通信]

- [sofa-bolt](https://github.com/lishuai2016/sofa-bolt)

SOFABolt 是蚂蚁金融服务集团开发的一套基于 Netty 实现的网络通信框架。

- [sofa-rpc](https://github.com/lishuai2016/sofa-rpc/tree/b_f_master_ls_200112)

基于sofa-bolt的rpc通信框架

- [sofa-registry](https://github.com/lishuai2016/sofa-registry/tree/b_f_5.3_ls_20200112)

基于sofa-bolt的注册中心



seata分布式事务[采用netty网络通信]。阿里开源的。自定义rpc通信

- [seata](https://github.com/lishuai2016/seata/tree/b_f_1.0.0_ls_2020112)

rpc通信协议代码在seata-core包中。



其他

- [sofa-common-tools](https://github.com/lishuai2016/sofa-common-tools)

- [sofa-lookout](https://github.com/lishuai2016/sofa-lookout)

- [sofa-rpc-boot-projects](https://github.com/lishuai2016/sofa-rpc-boot-projects)

- [sofa-tracer](https://github.com/lishuai2016/sofa-tracer/tree/b_f_3.0.8_ls_20200112)

sofa-tracer分布式链路追踪


## 9、alibaba

- [nacos](https://github.com/lishuai2016/nacos/tree/b_f_1.1.4_ls_20200112)

dynamic service discovery and configuration and service management

服务发现、配置和管理。


- [DataX](https://github.com/lishuai2016/DataX/tree/b_f_master_ls_20200112)

DataX 是阿里巴巴集团内被广泛使用的离线数据同步工具/平台，实现包括 MySQL、Oracle、SqlServer、Postgre、HDFS、Hive、ADS、HBase、TableStore(OTS)、MaxCompute(ODPS)、DRDS 等各种异构数据源之间高效的数据同步功能。


- [Sentinel](https://github.com/lishuai2016/Sentinel/tree/b_f_1.7.1_ls_20200112)

轻量级的流量控制、熔断降级 Java 库


- [canal](https://github.com/lishuai2016/canal/tree/b_f_mster_ls_200112)

阿里巴巴 MySQL binlog 增量订阅&消费组件 






其他

- [jetcache](https://github.com/lishuai2016/jetcache)

缓存

- [easyexcel](https://github.com/lishuai2016/easyexcel)

快速、简单避免OOM的java处理Excel工具


- [dubbo-spring-boot-starter](https://github.com/lishuai2016/dubbo-spring-boot-starter)

- [arthas](https://github.com/lishuai2016/arthas)

性能诊断

- [metrics](https://github.com/lishuai2016/metrics)

度量


## 9、okhttp发送http请求的封装

- [okhttp](https://github.com/lishuai2016/okhttp/tree/b_f_master_20200112)




## hdfs往clickhouse同步数据

- [clickhouse-hdfs-loader](https://github.com/lishuai2016/clickhouse-hdfs-loader/tree/b_f_master_20191030)


## apollo分布式配置中心

- [apollo](https://github.com/lishuai2016/apollo/tree/b_learn_ls_20190702)


## eureka分布式注册中心

- [eureka](https://github.com/lishuai2016/eureka/tree/b_f_master_20190713)


# spring-cloud-netflix

- [spring-cloud-netflix](https://github.com/lishuai2016/spring-cloud-netflix/tree/b_from_2.1.0_20190830)



包含eureka和下面子组件

```
spring-cloud-netflix-archaius

spring-cloud-netflix-hystrix-contract

spring-cloud-netflix-hystrix-dashboard

spring-cloud-netflix-hystrix-stream

spring-cloud-netflix-hystrix

spring-cloud-netflix-ribbon

spring-cloud-netflix-turbine-stream

spring-cloud-netflix-turbine

spring-cloud-netflix-zuul
```


- [spring-boot-admin](https://github.com/lishuai2016/spring-boot-admin/tree/b_f_2.2.1_ls_20200112)


springboot 监控


## 网关

- [soul](https://github.com/lishuai2016/soul/tree/b_f_master_20190713)

这是一个高性能，异步的反应式的gateway

- [zuul](https://github.com/lishuai2016/zuul/tree/b_f_master_ls_20200112)


- [spring-cloud-gateway](https://github.com/lishuai2016/spring-cloud-gateway/tree/b_f_2.2.1_ls_20200112)



## spring

- [spring-framework源码路径](https://github.com/lishuai2016/spring-framework/tree/b_f_5.0.x_20190714)



- [ls-mini-spring spring的ioc和aop的简单实现](https://github.com/lishuai2016/ls-mini-spring)

两个仿造spring项目

- [tiny-spring](https://github.com/lishuai2016/tiny-spring)

- [toy-spring](https://github.com/lishuai2016/toy-spring)




## SQL解析器

- [JSqlParser](https://github.com/lishuai2016/JSqlParser)




## rpc


- [mini-rpc](https://github.com/lishuai2016/mini-rpc)

Spring + Netty + Protostuff + ZooKeeper 实现了一个轻量级 RPC 框架，使用 Spring 提供依赖注入与参数配置，使用 Netty 实现 NIO 方式的数据传输，使用 Protostuff 实现对象序列化，使用 ZooKeeper 实现服务注册与发现。使用该框架，可将服务部署到分布式环境中的任意节点上，客户端通过远程接口来调用服务端的具体实现，让服务端与客户端的开发完全分离，为实现大规模分布式应用提供了基础支持




## Hadoop的RPC框架avro使用Netty作为通信框架


- [avro](https://github.com/lishuai2016/avro/tree/b_f_1.9.1_ls_20200112)