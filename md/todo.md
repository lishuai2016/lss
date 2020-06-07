
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