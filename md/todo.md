




[Linux IO磁盘篇整理小记](https://mp.weixin.qq.com/s/VoJcHMSQTywWUBvmPT2iKg)

[Java 和操作系统交互细节](https://mp.weixin.qq.com/s/fr1cc_s080A325EuHhym2w)

[文件句柄？文件描述符？](https://mp.weixin.qq.com/s/s8RxsKJrXuQlRsEQB2GYgw)

[简述Linux虚拟内存管理](https://mp.weixin.qq.com/s/-waQDC29PYYkx_c9x_2uHQ)

[maven 项目生命周期与构建原理](https://blog.csdn.net/luanlouis/article/details/50492163)

[系统设计](https://github.com/donnemartin/system-design-primer/blob/master/README-zh-Hans.md)

- [Github进行fork后如何与原仓库同步](https://github.com/selfteaching/the-craft-of-selfteaching/issues/67)

[disconf分布式配置中心](https://disconf.readthedocs.io/zh_CN/latest/config/src/client-config.html#disconf-client)

[TCP/IP协议详解---概述](http://www.cnblogs.com/nexiyi/p/3377908.html)
[TCP/IP协议详解](https://www.oschina.net/question/565065_86328)
[TCP和UDP报头格式各字段解释](http://www.it165.net/network/html/201306/1106.html)

[一个http请求的详细过程](http://www.cnblogs.com/yuteng/articles/1904215.html)

[四种常见的 POST 提交数据方式](https://imququ.com/post/four-ways-to-post-data-in-http.html)

[一网打尽消息队列在大型分布式系统中的实战精髓](https://dbaplus.cn/news-21-991-1.html)

网关
http://www.yunweipai.com/archives/23653.html
https://juejin.im/post/5bda67f26fb9a0223e4da611

- [平衡二叉树、B树、B+树、B*树 理解其中一种你就都明白了](https://zhuanlan.zhihu.com/p/27700617)

[什么是AES算法？（整合版）](https://mp.weixin.qq.com/s/Q99jGZOUGFiM-ZTnkWWYew)


最佳路径寻找算法[动态规划]
[什么是A*寻路算法？](https://mp.weixin.qq.com/s/FYKR_1yBKR4GJTn0fFIuAA)

[漫画：什么是Base64算法？](https://mp.weixin.qq.com/s/jZJVSt8SSZvFzSkAoTILvw)

[漫画：什么是 HTTPS 协议？](https://mp.weixin.qq.com/s/1ojSrhc9LZV8zlX6YblMtA)

- [漫画：什么是MD5算法？](https://mp.weixin.qq.com/s/k-ToL356asWtS_PN30Z17w)

[漫画：什么是SHA系列算法？](https://mp.weixin.qq.com/s/RIZuU8gmM_-HyK5DBr4pmw)
和md5类似的信息摘要生成算法


[漫画：什么是加密算法？](https://mp.weixin.qq.com/s/mszEors5SK2rThqXF79PuQ)

[字典树的原理以及Java实现](http://www.blogchong.com/?mod=pad&act=view&id=86)
[从Trie树（字典树）谈到后缀树（10.28修订）](https://blog.csdn.net/v_july_v/article/details/6897097)

[漫画：什么是布隆算法？](https://mp.weixin.qq.com/s/RmR5XmLeMvk35vgjwxANFQ)
[拜托，面试官别问我「布隆」了](https://mp.weixin.qq.com/s/QxKshmR75RBmYQkz4eU9dg)
[拜托，面试官别问我「布隆」了（修订补充版）](https://mp.weixin.qq.com/s/F021ZS4QmK0-q8hyeayiFQ)


其原理是：
通过增加多个hash函数，使得在bitmap情况下hash误判的概率降低


[蓄水池抽样算法（Reservoir Sampling）](https://www.jianshu.com/p/7a9ea6ece2af)  

[漫画：什么是计数排序？](https://mp.weixin.qq.com/s/WGqndkwLlzyVOHOdGK7X4Q)

[大数相乘、大数相加、大数相减Java版本](https://blog.csdn.net/lichong_87/article/details/6860329)


1、大数相乘，其结果的长度不超过len1+len2，第i位和第j位的成绩放在（i+j）位中，然后在考虑进位的情况
2、大数相加和大数相乘类似，考虑进位的处理
3、大数相减考虑借位的问题


[漫画：如何破解MD5算法？](https://mp.weixin.qq.com/s/fLwwu9Ol21SfMRBzA_OyQg)

[漫画：深度优先遍历 和 广度优先遍历](https://mp.weixin.qq.com/s/WA5hQXkcACIarcdVnRnuiw)

[辗转相除法是什么鬼？](https://mp.weixin.qq.com/s/7n0O86dDV7_4SpZtFa41Hw)

求最大公约数的几种求法

> 重点：

所有slf4j的实现，在提供的jar包路径下，一定是有"org/slf4j/impl/StaticLoggerBinder.class"存在的

- [Java日志框架：slf4j作用及其实现原理](https://www.cnblogs.com/xrq730/p/8619156.html)

- [Java日志框架：logback详解](https://www.cnblogs.com/xrq730/p/8628945.html)


















# 1、工作流

## 1、Azkaban

Azkaban is a batch workflow job scheduler created at LinkedIn to run Hadoop jobs. Azkaban resolves the ordering through job dependencies and provides an easy to use web user interface to maintain and track your workflows.

- [github](https://github.com/azkaban/azkaban)

- [Azkaban 简单入门](https://www.jianshu.com/p/c7d6bf6191e7)

## 2、oozie

- [oozie官网]http://oozie.apache.org/)

- [github](https://github.com/lishuai2016/oozie)

Apache Oozie Workflow Scheduler for Hadoop

Oozie is a workflow scheduler system to manage Apache Hadoop jobs.

Oozie Workflow jobs are Directed Acyclical Graphs (DAGs) of actions.

Oozie Coordinator jobs are recurrent Oozie Workflow jobs triggered by time (frequency) and data availability.

Oozie is integrated with the rest of the Hadoop stack supporting several types of Hadoop jobs out of the box (such as Java map-reduce, Streaming map-reduce, Pig, Hive, Sqoop and Distcp) as well as system specific jobs (such as Java programs and shell scripts).

Oozie is a scalable, reliable and extensible system.

## 3、airflow

Apache Airflow - A platform to programmatically author, schedule, and monitor workflows 


- [airflow](https://github.com/lishuai2016/airflow)


> 工作流相关文章

- [开源数据流管道-Luigi vs Azkaban vs Oozie vs Airflow](https://www.jianshu.com/p/4ae1faea733b)


# 2、Linkis

打通了多个计算存储引擎如：Spark、TiSpark、Hive、Python和HBase等，对外提供统一REST/WebSocket/JDBC接口，提交执行SQL、Pyspark、HiveQL、Scala等脚本的计算中间件。

类似于livy的作用。

- [Linkis](https://gitee.com/WeBank/Linkis)

- [DataSphereStudio](https://gitee.com/WeBank/DataSphereStudio)

将满足从数据交换、脱敏清洗、分析挖掘、质量检测、可视化展现、定时调度到数据输出等数据应用开发全流程场景需求。[部分依赖]

数据可视化-基于宜信开源项目Davinci二次开发的数据可视化BI工具。


# 3、radar

实时风控引擎(Risk Engine)，自定义规则引擎(Rule Script)，完美支持中文，适用于反欺诈(Anti-fraud)应用场景，开箱即用

- [radar](https://gitee.com/freshday/radar)


# 4、HeartBeat

心跳检测应用服务器(如Tomcat,Jetty)的JAVA 微服务应用程序

如何实现?

使用HttpClient对指定的服务器(application-instance) URL 按频率(10秒,20秒...) 发起请求并记录响应的信息(连接耗时,是否连接成功,是否有异常,响应数据包大小), 若检测到不正常(响应码不是200,抛出异常...)时则发送邮件给指定的地址,当检测恢复正常时也发送提醒邮件.
将来会添加更多的实时提醒方式接口,如微信,短信

- [HeartBeat](https://gitee.com/2016shuai/HeartBeat)

自己想法：

1、其实可以在应用启动的时候向一个管理中心发送一个http请求[这里相当于被监控的应用注册一个自己的IP应用等信息]，之后管理中心按照一定的频率发送请求。

2、或者应用间隔一定的频率主动向管理端发送http请求报告自己的状况，管理端在一定时间内没有收到请求可以任务这个服务挂掉了。



# 5、WePush消息推送

专注批量推送的小而美的工具。目前支持的类型：模板消息-公众号、模板消息-小程序、微信客服消息、微信企业号/企业微信消息、阿里云短信、阿里大于模板短信 、腾讯云短信、云片网短信、E-Mail、钉钉、百度云短信、华为云短信、又拍云短信、七牛云短信

- [WePush](https://gitee.com/2016shuai/WePush)


# 6、DDMQ

DDMQ 是滴滴出行架构部基于 Apache RocketMQ 构建的消息队列产品。

- [DDMQ](https://gitee.com/2016shuai/DDMQ)

# 7、TarsJava

该工程是Tars框架Java语言的源代码

Tars 是基于名字服务使用 Tars 协议的高性能 RPC 开发框架，同时配套一体化的服务治理平台，帮助个人或者企业快速的以微服务的方式构建自己稳定可靠的分布式应用

- [TarsJava](https://gitee.com/2016shuai/TarsJava)



# 8、spring-boot-mail

邮件发送服务，文本、附件、模版多种实现，队列，线程定时任务功能

- [spring-boot-mail](https://gitee.com/2016shuai/spring-boot-mail)

# 9、JustAuth

史上最全的整合第三方登录的工具,目前已支持Github、Gitee、微博、钉钉和百度、Coding、腾讯云开发者平台和OSChina登录。 Login, so easy!

- [JustAuth](https://gitee.com/2016shuai/JustAuth)



# 10、pdman

PDMan是一款开源免费的数据库模型建模工具，支持Windows,Mac,Linux等操作系统，是PowerDesigner之外，更好的免费的替代方案。他具有颜值高，使用简单的特点。包含数据库建模，灵活自动的自动生成代码模板，自动生成文档等多种开发人员实用的功能。

- [pdman](https://gitee.com/2016shuai/pdman)



# 11、tinyid

tinyid 是滴滴开发的 id 生成器 分布式id生成系统，简单易用、高性能、高可用的id生成系统

- [tinyid](https://gitee.com/2016shuai/tinyid)