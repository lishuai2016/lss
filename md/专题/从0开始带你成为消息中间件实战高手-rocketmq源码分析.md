
rocketmq源码分析

<!-- TOC -->

- [091 如何从Github拉取RocketMQ源码以及导入Intellij IDEA中？](#091-如何从github拉取rocketmq源码以及导入intellij-idea中)
- [092 如何在Intellij IDEA中启动NameServer以及本地调试源码？](#092-如何在intellij-idea中启动nameserver以及本地调试源码)
- [093 如何在Intellij IDEA中启动Broker以及本地调试源码？](#093-如何在intellij-idea中启动broker以及本地调试源码)
- [094 如何基于本地运行的RocketMQ进行消息的生产与消费？](#094-如何基于本地运行的rocketmq进行消息的生产与消费)
- [095 源码分析的起点：从NameServer的启动脚本开始讲起](#095-源码分析的起点从nameserver的启动脚本开始讲起)
    - [3、从NameServer的启动开始说起](#3从nameserver的启动开始说起)
- [096 NameServer在启动的时候都会解析哪些配置信息？](#096-nameserver在启动的时候都会解析哪些配置信息)
    - [4、非常核心的两个NameServer的配置类](#4非常核心的两个nameserver的配置类)
- [097 NameServer是如何初始化基于Netty的网络通信架构的？](#097-nameserver是如何初始化基于netty的网络通信架构的)
- [098 NameServer最终是如何启动Netty网络通信服务器的？](#098-nameserver最终是如何启动netty网络通信服务器的)
- [099 Broker启动的时候是如何初始化自己的核心配置的？](#099-broker启动的时候是如何初始化自己的核心配置的)
- [100 BrokerController是如何构建出来的，以及他包含了哪些组件？](#100-brokercontroller是如何构建出来的以及他包含了哪些组件)
- [101 在初始化BrokerController的时候，都干了哪些事情？](#101-在初始化brokercontroller的时候都干了哪些事情)
- [102 BrokerContorller在启动的时候，都干了哪些事儿？](#102-brokercontorller在启动的时候都干了哪些事儿)
- [103 第三个场景驱动：Broker是如何把自己注册到NameServer去的？](#103-第三个场景驱动broker是如何把自己注册到nameserver去的)
- [104 深入探索BrokerOuter API是如何发送注册请求的？](#104-深入探索brokerouter-api是如何发送注册请求的)
- [105 NameServer是如何处理Broker的注册请求的？](#105-nameserver是如何处理broker的注册请求的)
- [106 Broker是如何发送定时心跳的，以及如何进行故障感知？](#106-broker是如何发送定时心跳的以及如何进行故障感知)
- [107 我们系统中使用的Producer是如何创建出来的？](#107-我们系统中使用的producer是如何创建出来的)
- [108 构建好的Producer是如何启动准备好相关资源的？](#108-构建好的producer是如何启动准备好相关资源的)
- [109 当我们发送消息的时候，是如何从NameServer拉取Topic元数据的？](#109-当我们发送消息的时候是如何从nameserver拉取topic元数据的)
- [110 对于一条消息，Producer是如何选择MessageQueue去发送的？](#110-对于一条消息producer是如何选择messagequeue去发送的)
- [111 我们的系统与RocketMQ Broker之间是如何进行网络通信的？](#111-我们的系统与rocketmq-broker之间是如何进行网络通信的)
- [112 当Broker获取到一条消息之后，他是如何存储这条消息的？](#112-当broker获取到一条消息之后他是如何存储这条消息的)
- [113 一条消息写入CommitLog文件之后，如何实时更新索引文件？](#113-一条消息写入commitlog文件之后如何实时更新索引文件)
- [114 RocketMQ是如何实现同步刷盘以及异步刷盘两种策略的？](#114-rocketmq是如何实现同步刷盘以及异步刷盘两种策略的)

<!-- /TOC -->



# 091 如何从Github拉取RocketMQ源码以及导入Intellij IDEA中？

![](../../pic/2020-03-30-09-43-39.png)


# 092 如何在Intellij IDEA中启动NameServer以及本地调试源码？

todo

# 093 如何在Intellij IDEA中启动Broker以及本地调试源码？

todo

# 094 如何基于本地运行的RocketMQ进行消息的生产与消费？

todo


# 095 源码分析的起点：从NameServer的启动脚本开始讲起

我们会用场景来驱动源码的分析，也就是说，RocketMQ使用的时候，第一个步骤一定是先启动NameServer，那么我们就先来分析NameServer启动这块的源码，然后第二个步骤一定是启动Broker，那么我们再来分析Broker启动的流程。

接着Broker启动之后，必然会把自己注册到NameServer上去，那我们接着分析Broker注册到NameServer这部分源码，然后Broker必然会跟NameServer保持一个心跳，那我们继续分析Broker的心跳的源码。

所以实际上来说，我们分析源码，将会完全按照我们平时使用RocketMQ的各种场景来进行源码的分析，在一个场景中把各种源码串联起来分析，我觉得大家会觉得容易理解的多。



## 3、从NameServer的启动开始说起

那么NameServer启动的时候，是通过哪个脚本来启动的呢？

其实我们之前都给大家讲过了，就是基于rocketmq-master源码中的distribution/bin目录中的mqnamesrv这个脚本来启动的，在这个脚本中有极为关键的一行命令用于启动NameServer进程，如下。

sh ${ROCKETMQ_HOME}/bin/runserver.sh org.apache.rocketmq.namesrv.NamesrvStartup $@

大家都看到，上面那行命令中用sh命令执行了runserver.sh这个脚本，然后通过这个脚本去启动了NamesrvStartup这个Java类，那么runserver.sh这个脚本中最为关键的启动NamesrvStartup类的命令是什么呢，如下

```
set "JAVA_OPT=%JAVA_OPT% -server -Xms2g -Xmx2g -Xmn1g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m"
set "JAVA_OPT=%JAVA_OPT% -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 -XX:-UseParNewGC"
set "JAVA_OPT=%JAVA_OPT% -verbose:gc -Xloggc:"%USERPROFILE%\rmq_srv_gc.log" -XX:+PrintGCDetails"
set "JAVA_OPT=%JAVA_OPT% -XX:-OmitStackTraceInFastThrow"
set "JAVA_OPT=%JAVA_OPT% -XX:-UseLargePages"
set "JAVA_OPT=%JAVA_OPT% -Djava.ext.dirs=%BASE_DIR%lib"
set "JAVA_OPT=%JAVA_OPT% -cp "%CLASSPATH%""

"%JAVA%" %JAVA_OPT% %*
```

大家可以看到，说白了，上述命令大致简化一下就是类似这样的一行命令：

java -server -Xms4g -Xmx4g -Xmn2g org.apache.rocketmq.namesrv.NamesrvStartup


通过java命令 + 一个有main()方法的类，就是会启动一个JVM进程，通过这个JVM进程来执行NamesrvStartup类中的main()方法，这个main()方法里就完成了NameServer启动的所有流程和工作，那么既然NameServer是一个JVM进程，肯定可以设置JVM参数了，所以上面你看到的一大堆-Xms4g之类的东西，都是JVM的参数。

所以说白了，你使用mqnamesrv脚本启动NameServer的时候，本质就是基于java命令启动了一个JVM进程，执行NamesrvStartup类中的main()方法，完成NameServer启动的全部流程和逻辑，同时启动NameServer这个JVM进程的时候，有一大堆的默认JVM参数，你当然可以在这里修改这些JVM参数，甚至进行优化。

这边我也用下面的一张图来展示一下这个启动NameServer的过程，相信大家对照图来看，会理解的更加透彻一些。

![](../../pic/2020-03-30-10-11-12.png)


# 096 NameServer在启动的时候都会解析哪些配置信息？

我们现在来正式开始看NameServer的启动流程的源码，首先我们昨天已经讲到，NamesrvStartup这个类的main()方法会被执行，然后执行的时候实际上会执行一个main0()这么个方法，如下所示。

```java

public static NamesrvController main0(String[] args) {

        try {
            NamesrvController controller = createNamesrvController(args);
            start(controller);//服务启动入口
            String tip = "The Name Server boot success. serializeType=" + RemotingCommand.getSerializeTypeConfigInThisServer();
            log.info(tip);
            System.out.printf("%s%n", tip);
            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }

```

NamesrvController controller = createNamesrvController(args); 

我们可以大胆的推测一下，NameServer启动之后，是不是需要接受Broker的请求？因为Broker都要把自己注册到NameServer上去。

然后Producer这些客户端是不是也要从NameServer拉取元数据？因为他们需要知道一个Topic的MessageQueue都在哪些Broker上。

所以我们完全可以猜想一下，NamesrvController这个组件，很可能就是NameServer中专门用来接受Broker和客户端的网络请求的一个组件！因为平时我们写Java Web系统的时候，大家都喜欢用Spring MVC框架，在Spring MVC框架中，用于接受HTTP请求的，就是Controlller组件！

![](../../pic/2020-03-30-11-08-45.png)


## 4、非常核心的两个NameServer的配置类

![](../../pic/2020-03-30-11-12-45.png)

上面三行代码才是你真正要关注的，你会看到他创建了NamesrvConfig和NettyServerConfig两个关键的配置类！

从他的类名，我们就可以推测出来，NamesrvConfig包含的是NameServer自身运行的一些配置参数，NettyServerConfig包含的是用于接收网络请求的Netty服务器的配置参数。

在这里也能明确感觉到，NameServer对外接收Broker和客户端的网络请求的时候，底层应该是基于Netty实现的网络服务器！


而且我们通过nettyServerConfig.setListenPort(9876)这行代码就可以发现，NameServer他默认固定的监听请求的端口号就是9876，因为他直接在代码里写死了这个端口号了，所以NettyServer应该就是监听了9876这个端口号，来接收Broker和客户端的请求的！

![](../../pic/2020-03-30-11-15-15.png)


启动日志打印相关配置参数：

```
2020-03-30 09:54:58 INFO main - rocketmqHome=D:\opencode\rocketmq\distribution
2020-03-30 09:54:58 INFO main - kvConfigPath=C:\Users\lx\namesrv\kvConfig.json
2020-03-30 09:54:58 INFO main - configStorePath=C:\Users\lx\namesrv\namesrv.properties
2020-03-30 09:54:58 INFO main - productEnvName=center
2020-03-30 09:54:58 INFO main - clusterTest=false
2020-03-30 09:54:58 INFO main - orderMessageEnable=false
2020-03-30 09:54:58 INFO main - listenPort=9876
2020-03-30 09:54:58 INFO main - serverWorkerThreads=8
2020-03-30 09:54:58 INFO main - serverCallbackExecutorThreads=0
2020-03-30 09:54:58 INFO main - serverSelectorThreads=3
2020-03-30 09:54:58 INFO main - serverOnewaySemaphoreValue=256
2020-03-30 09:54:58 INFO main - serverAsyncSemaphoreValue=64
2020-03-30 09:54:58 INFO main - serverChannelMaxIdleTimeSeconds=120
2020-03-30 09:54:58 INFO main - serverSocketSndBufSize=65535
2020-03-30 09:54:58 INFO main - serverSocketRcvBufSize=65535
2020-03-30 09:54:58 INFO main - serverPooledByteBufAllocatorEnable=true
2020-03-30 09:54:58 INFO main - useEpollNativeSelector=false
2020-03-30 09:55:00 INFO main - Server is running in TLS permissive mode
2020-03-30 09:55:00 INFO main - Tls config file doesn't exist, skip it
2020-03-30 09:55:00 INFO main - Log the final used tls related configuration
2020-03-30 09:55:00 INFO main - tls.test.mode.enable = true
2020-03-30 09:55:00 INFO main - tls.server.need.client.auth = none
2020-03-30 09:55:00 INFO main - tls.server.keyPath = null
2020-03-30 09:55:00 INFO main - tls.server.keyPassword = null
2020-03-30 09:55:00 INFO main - tls.server.certPath = null
2020-03-30 09:55:00 INFO main - tls.server.authClient = false
2020-03-30 09:55:00 INFO main - tls.server.trustCertPath = null
2020-03-30 09:55:00 INFO main - tls.client.keyPath = null
2020-03-30 09:55:00 INFO main - tls.client.keyPassword = null
2020-03-30 09:55:00 INFO main - tls.client.certPath = null
2020-03-30 09:55:00 INFO main - tls.client.authServer = false
2020-03-30 09:55:00 INFO main - tls.client.trustCertPath = null
2020-03-30 09:55:06 INFO main - Using OpenSSL provider
2020-03-30 09:55:11 INFO main - SSLContext created for server
2020-03-30 09:55:11 INFO main - Try to start service thread:FileWatchService started:false lastThread:null
2020-03-30 09:55:11 INFO NettyEventExecutor - NettyEventExecutor service started
2020-03-30 09:55:11 INFO main - The Name Server boot success. serializeType=JSON

```

# 097 NameServer是如何初始化基于Netty的网络通信架构的？

在NamesrvController组件被构造好之后，接着进行初始化的时候，首先就是把核心的NettyRemotingServer网络服务器组件给构造了出来。

![](../../pic/2020-03-30-11-30-16.png)

讲到这里，我在图里又加入了一点东西，NettyRemotingServer是一个RocketMQ自己开发的网络服务器组件，但是其实底层就是基于Netty的原始API实现的一个ServerBootstrap，是用作真正的网络服务器的。

![](../../pic/2020-03-30-11-31-15.png)


# 098 NameServer最终是如何启动Netty网络通信服务器的？

![](../../pic/2020-03-30-11-41-53.png)

# 099 Broker启动的时候是如何初始化自己的核心配置的？

很明显，套路是一样的，broker在这里启动的时候也是先搞了几个核心的配置组件，包括了broker自己的配置、broker作为一个netty服务器的配置、broker作为一个netty客户端的配置、broker的消息存储的配置。

那么为什么broker自己又是netty服务器，又是netty客户端呢？

很简单了，当你的客户端连接到broker上发送消息的时候，那么broker就是一个netty服务器，负责监听客户端的连接请求。

但是当你的broker跟nameserver建立连接的时候，你的broker又是一个netty客户端，他要跟nameserver的netty服务器建立连接。

所以通过上述分析，我们画出了下面的图，包含了Broker的几个核心配置组件。

![](../../pic/2020-03-30-11-47-37.png)


# 100 BrokerController是如何构建出来的，以及他包含了哪些组件？

![](../../pic/2020-03-30-11-53-37.png)

上面那个图里，其实就把这几者之间的关系，说的很清晰了，Broker这个概念本身代表的不是一个代码组件，他就是你用mqbroker脚本启动的JVM进程。然后JVM进程的main class是BrokerStartup，他是一个启动组件，负责初始化核心配置组件，然后基于核心配置组件去启动BrokerControler这个管控组件。

然后在Broker这个JVM进程运行期间，都是由BrokerController这个管控组件去管理Broker的请求处理、后台线程以及磁盘数据。

BrokerController里包含了一大堆核心功能组件和后台线程池.

# 101 在初始化BrokerController的时候，都干了哪些事情？



![](../../pic/2020-03-30-12-05-24.png)

其实如果一定要我说，今天大家看完这些源码，跟着我的注释来走，一方面是对BrokerController初始化的过程有一个大致的印象，另外一方面其实最核心的，你要知道，BrokerController一旦初始化完成过后，他其实就准备好了Netty服务器，可以用于接收网络请求，然后准备好了处理各种请求的线程池，准备好了各种执行后台定时调度任务的线程池。

这些都准备好之后，明天我们就要来讲解BrokerController的启动了，他的启动，必然会正式完成Netty服务器的启动，他于是可以接收请求了，同时Broker必然会在完成启动的过程中去向NameServer进行注册以及保持心跳的。

只有这样，Producer才能从NameServer上找到你这个Broker，同时发送消息给你。


# 102 BrokerContorller在启动的时候，都干了哪些事儿？


看完上述源码，大家其实从中只要提取一些核心的东西，知道说Netty服务器启动了，可以接收网络请求了，然后还有一个BrokerOuterAPI组件是基于Netty客户端发送请求给别人的，同时还启动一个线程去向NameServer注册，知道这几点就可以了。

在这里，我在下面的图里，就给大家展示出来了，BrokerOuterAPI和向NameServer注册这两个东西。

![](../../pic/2020-03-30-12-19-42.png)


只能说在看到现在这个程度的时候，你大致脑子里有个印象，你知道Broker里有这么一些核心组件，都进行了初始化以及完成了启动，但是你应该最主要关注的事情是这么几个：

- （1）Broker启动了，必然要去注册自己到NameServer去，所以BrokerOuterAPI这个组件必须要画到自己的图里去，这是一个核心组件

- （2）Broker启动之后，必然要有一个网络服务器去接收别人的请求，此时NettyServer这个组件是必须要知道的

- （3）当你的NettyServer接收到网络请求之后，需要有线程池来处理，你需要知道这里应该有一个处理各种请求的线程池

- （4）你处理请求的线程池在处理每个请求的时候，是不是需要各种核心功能组件的协调？比如写入消息到commitlog，然后写入索引到indexfile和consumer queue文件里去，此时你是不是需要对应的一些MessageStore之类的组件来配合你？


- （5）除此之外，你是不是需要一些后台定时调度运行的线程来工作？比如定时发送心跳到NameServer去，类似这种事情。


接着再往后走，一定要从各种场景驱动，去理解RocketMQ的源码，包括Broker的注册和心跳，客户端Producer的启动和初始化，Producer从NameServer拉取路由信息，Producer根据负载均衡算法选择一个Broker机器，Producer跟Broker建立网络连接，Producer发送消息到Broker，Broker把消息存储到磁盘。


上面我说的那些东西，每一个都是RocketMQ这个中间件运行的时候一个场景，一定要从这些场景出发，一点点去理解在每一个场景下，RocketMQ的各个源码中的组件是如何配合运行的。


# 103 第三个场景驱动：Broker是如何把自己注册到NameServer去的？

当然，最为关键的一点，就是他执行了将自己注册到NameServer的一个过程，我们看一下这个注册自己到NameServer的源码入口，下面这行代码就是在BrokerController.start()方法中

BrokerController.this.registerBrokerAll(true, false, brokerConfig.isForceRegister());

因此如果我们要继续研究RocketMQ源码的话，当然应该场景驱动来研究，之前已经研究完了NameServer和Broker两个核心系统的启动场景，现在来研究第三个场景，就是Broker往NameServer进行注册的场景。

因为只有完成了注册，NameServer才能知道集群里有哪些Broker，然后Producer和Consumer才能找NameServer去拉取路由数据，他们才知道集群里有哪些Broker，才能去跟Broker进行通信！

其实大家看完上面的代码，再看一下下面的图中，我用红圈圈出来的部分，你就会发现，在这里实际上就是通过BrokerOuterAPI去发送网络请求给所有的NameServer，把这个Broker注册了上去。

![](../../pic/2020-03-30-12-28-21.png)


![](../../pic/2020-03-30-12-29-07.png)


# 104 深入探索BrokerOuter API是如何发送注册请求的？

我们看下面的图，里面的红圈就展示了通过Channel发送网络请求出去的示意。

![](../../pic/2020-03-30-12-41-50.png)

# 105 NameServer是如何处理Broker的注册请求的？

下面我们先在图里给大家体现一下RouteInfoManager这个路由数据管理组件，实际Broker注册就是通过他来做的。

![](../../pic/2020-03-30-13-07-40.png)

至于RouteInfoManager的注册Broker的方法，我们就不带着大家来看了。这里给大家留一个今天的源码分析小作业，大家可以自己到RouteInfoManager的注册Broker的方法里去看看，最终如何把一个Broker机器的数据放入RouteInfoManager中维护的路由数据表里去的。

其实我这里提示一下，核心思路非常简单，无非就是用一些Map类的数据结构，去存放你的Broker的路由数据就可以了，包括了Broker的clusterName、brokerId、brokerName这些核心数据。

而且在更新的时候，一定会基于Java并发包下的ReadWriteLock进行读写锁加锁，因为在这里更新那么多的内存Map数据结构，必须要加一个写锁，此时只能有一个线程来更新他们才行！

# 106 Broker是如何发送定时心跳的，以及如何进行故障感知？


所以其实大家看到这里就会明白，默认情况下，第一次发送注册请求就是在进行注册，就是我们上一讲讲的内容，他会把Broker路由数据放入到NameServer的RouteInfoManager的路由数据表里去。


但是后续每隔30s他都会发送一次注册请求，这些后续定时发送的注册请求，其实本质上就是Broker发送心跳给NameServer了，我们看下图示意。

![](../../pic/2020-03-30-13-12-01.png)

那么后续每隔30s，Broker就发送一次注册请求，作为心跳来发送给NameServer的时候，NameServer对后续重复发送过来的注册请求（也就是心跳），是如何进行处理的呢？


# 107 我们系统中使用的Producer是如何创建出来的？

所以今天开始我们就来讲一下Producer这个组件的底层原理，当然先是得从Producer的构造开始了

既然要说Producer的构造，那肯定是要先回顾一下Producer是如何构造出来的，其实我们可以回顾一下下面的这块使用Producer发送消息到MQ的代码，就能清晰的看到Producer是如何构造出来的。

![](../../pic/2020-03-30-13-23-01.png)

大家可以看到，其实构造Producer很简单，就是创建一个DefaultMQProducer对象实例，在其中传入你所属的Producer分组，然后设置一下NameServer的地址，最后调用他的start()方法，启动这个Producer就可以了。

其实最核心的还是调用了这个DefaultMQProducer的start()方法去启动了这个消息生产组件，那么这个start()都干了什么呢？

# 108 构建好的Producer是如何启动准备好相关资源的？

那么今天我们重点来分析一下这个Producer组件在启动的时候是如何准备好相关资源的，因为他必须内部得有独立的线程资源，还有得跟Broker建立网络连接，这样才能把我们的消息发送出去。

首先我想告诉大家的是，其实我们在构造Producer的时候，他内部构造了一个真正用于执行消息发送逻辑的组件，就是DefaultMQProducerImpl这个类的实例对象，所以其实我们要知道，真正的生产组件其实是这个组件。

主要站在Producer的核心行为的角度去看。

首先我们都知道一件事儿，假设我们后续要通过Producer发送消息，必然会指定我们要往哪个Topic里发送消息。所以我们也知道，Producer必然是知道Topic的一些路由数据的，比如Topic有哪些MessageQueue，每个MessageQueue在哪些Broker上。

![](../../pic/2020-03-30-13-26-16.png)

那么现在问题来了，到底是Producer刚启动初始化的时候，就会去拉取每个Topic的路由数据呢？还是等你第一次往一个Topic发送消息的时候再拉取路由数据呢？

其实答案是显而易见的，肯定不可能是刚初始化启动的时候就拉取Topic的路由数据，因为你刚开始启动的时候，不知道要发送消息到哪个Topic去啊！

所以这个问题，一定是在你第一次发送消息到Topic的时候，才会去拉取一个Topic的路由数据，包括这个Topic有几个MessageQueue，每个MessageQueue在哪个Broker上，然后从中选择一个MessageQueue，跟那台Broker建立网络连接，发送消息过去。

所以此时我们说第二个问题，Producer发送消息必然要跟Broker建立网络，这个是在Producer刚启动的时候就立马跟所有的Broker建立网络连接吗？

那必然也不是的，因为此时你也不知道你要跟哪个Broker进行通信。

所以其实很多核心的逻辑，包括Topic路有数据拉取，MessageQueue选择，以及跟Broker建立网络连接，通过网络连接发送消息到Broker去，这些逻辑都是在Producer发送消息的时候才会有。


# 109 当我们发送消息的时候，是如何从NameServer拉取Topic元数据的？

之前我们已经给大家讲到了发送消息到Broker的时候，使用的是Producer来发送，也大概介绍了一下Producer初始化的过程

其实初始化的过程极为的复杂，但是我们却真的不用过于的深究，因为其实比如拉取Topic的路由数据，选择MessageQueue，跟Broker构建长连接，发送消息过去，这些核心的逻辑，都是封装在发送消息的方法中的。

因此我们今天就从发送消息的方法开始讲起，实际上当你调用Producer的send()方法发送消息的时候，这个方法调用会一直到比较底层的逻辑里去，最终会调用到DefaultMQProducerImpl类的sendDefaultImpl()方法里去，在这个方法里，上来什么都没干，直接就有一行非常关键的代码，如下。

TopicPublishInfo topicPublishInfo = this.tryToFindTopicPublishInfo(msg.getTopic());

其实看到这行代码，大家就什么都明白了，每次你发送消息的时候，他都会先去检查一下，这个你要发送消息的那个Topic的路由数据是否在你客户端本地

如果不在的话，必然会发送请求到NameServer那里去拉取一下的，然后缓存在客户端本地。

所以今天我们就重点来看看，这个Producer客户端运行在你的业务系统里的时候，他如何从NameServer拉取到你的Topic的路由数据的？

其实当你进入了this.tryToFindTopicPublishInfo(msg.getTopic())这个方法逻辑之后，会发现他的逻辑非常的简单

其实简单来说，他就是先检查了一下自己本地是否有这个Topic的路由数据的缓存，如果没有的话就发送网络请求到NameServer去拉取，如果有的话，就直接返回本地Topic路由数据缓存了。

所以接着我们当然很想知道的是，Producer到底是如何发送网络请求到NameServer去拉取Topic路由数据的，其实这里就对应了tryToFindTopicPublishInfo()方法内的一行代码，我们看看。

this.mQClientFactory.updateTopicRouteInfoFromNameServer(topic);

通过这行代码，他就可以去从NameServer拉取某个Topic的路由数据，然后更新到自己本地的缓存里去了。

具体的发送请求到NameServer的拉取过程，其实之前都大致讲解到了，简单来说，就是封装一个Request请求对象，然后通过底层的Netty客户端发送请求到NameServer，接收到一个Response响应对象。


然后他就会从Response响应对象里取出来自己需要的Topic路由数据，更新到自己本地缓存里去，更新的时候会做一些判断，比如Topic路由数据是否有改变过，等等，然后把Topic路由数据放本地缓存就可以了，我们看下图演示。


![](../../pic/2020-03-30-13-30-15.png)

# 110 对于一条消息，Producer是如何选择MessageQueue去发送的？

上一次我们讲完了Producer发送消息的时候，上来不管三七二十一，其实会先检查一下要发送消息的Topic的路由数据是否在本地缓存，如果不在的话，就会通过底层的Netty网络通信模块去发送一个请求到NameServer去拉取Topic路由数据，然后缓存在Producer的本地。

那么今天我们应该继续讲解的是，当你拿到了一个Topic的路由数据之后，其实接下来就应该选择要发送消息到这个Topic的哪一个MessageQueue上去了！

因为大家都知道，Topic是一个逻辑上的概念，一个Topic的数据往往是分布式存储在多台Broker机器上的，因此Topic本质是由多个MessageQueue组成的。

每个MessageQueue都可以在不同的Broker机器上，当然也可能一个Topic的多个MessageQueue在一个Broker机器上。

所以今天我们主要就是讲解，你要发送的消息，到底应该发送到这个Topic的哪个MessageQueue上去呢？

只要你知道了要发送消息到哪个MessageQueue上去，然后就知道这个MessageQueue在哪台Broker机器上，接着就跟那台Broker机器建立连接，发送消息给他就可以了。

之前给大家说过，发送消息的核心源码是在DefaultMQProducerImpl.sendDefaultImpl()方法中的，在这个方法里，只要你获取到了Topic的路由数据，不管从本地缓存获取的，还是从NameServer拉取到的，接着就会执行下面的核心代码。

MessageQueue mqSelected = this.selectOneMessageQueue(topicPublishInfo, lastBrokerName);

这行代码其实就是在选择Topic中的一个MessageQueue，然后发送消息到这个MessageQueue去，在这行代码里面，实现了一些Broker故障自动回避机制，但是这个我们后续再讲，先看最基本的选择MessageQueue的算法

![](../../pic/2020-03-30-13-36-12.png)

上面的代码其实非常的简单，他先获取到了一个自增长的index，大家注意到没有？

接着其实他核心的就是用这个index对Topic的MessageQueue列表进行了取模操作，获取到了一个MessageQueue列表的位置，然后返回了这个位置的MessageQueue。

说实话，你只要自己去试试就知道了，这种操作就是一种简单的负载均衡的算法，比如一个Topic有8个MessageQueue，那么可能第一次发送消息到MessageQueue01，第二次就发送消息到MessageQueue02，以此类推，就是轮询把消息发送到各个MessageQueue而已！

这就是最基本的MessageQueue选择算法，但是肯定有人会说了，那万一某个Broker故障了呢？此时发送消息到哪里去呢？

所以其实这个算法里有很多别的代码，都是实现Broker规避机制的，这个后续我们再讲。

![](../../pic/2020-03-30-13-37-38.png)



# 111 我们的系统与RocketMQ Broker之间是如何进行网络通信的？

![](../../pic/2020-03-30-13-39-14.png)

所以今天我们就来看看，Producer是如何把消息发送给Broker的呢？

其实这块代码就在DefaultMQProducerImpl.sendDefaultImpl()方法中，在这个方法里，先是获取到了MessageQueue所在的broker名称，如下源码片段：

![](../../pic/2020-03-30-13-39-59.png)

上面的代码片段其实非常简单，就是通过brokerName去本地缓存找他的实际的地址，如果找不到，就去找NameServer拉取Topic的路由数据，然后再次在本地缓存获取broker的实际地址，你有这个地址了，才能给人家进行网络通信。

接下来的源码就很繁琐细节了，其实大家不用看也行，他就是用自己的方式去封装了一个Request请求出来，这里涉及到了各种信息的封装，包括了请求头，还有一大堆所有你需要的数据，都封装在Request里了。

他在这里做的事情，大体上包括了给消息分配全局唯一ID、对超过4KB的消息体进行压缩，在消息Request中包含了生产者组、Topic名称、Topic的MessageQueue数量、MessageQueue的ID、消息发送时间、消息的flag、消息扩展属性、消息重试次数、是否是批量发送的消息、如果是事务消息则带上prepared标记，等等。

总之，这里就是封装了很多很多的数据就对了，这些东西都封装到一个Request里去，然后在底层还是通过Netty把这个请求发送出去，发送到指定的Broker上去就可以了

这里Producer和Broker之间都是通过Netty建立长连接，然后基于长连接进行持续的通信的。

其实对于我们而言，如果大家想要研究更加细致的源码细节，可以找到我说的那块代码，然后自己仔细的分析里面如何封装Request请求的细节，包括底层的基于Netty发送请求出去的细节，但是如果你不想看这些细节，那么从原理层面而言，你只要知道这个过程就可以了，另外比较重要的就是知道他底层是基于Netty发送的.


# 112 当Broker获取到一条消息之后，他是如何存储这条消息的？

我们就已经给大家在原理部分讲解过一些Broker收到消息之后的处理流程，简单来说，Broker通过Netty网络服务器获取到一条消息，接着就会把这条消息写入到一个CommitLog文件里去，一个Broker机器上就只有一个CommitLog文件，所有Topic的消息都会写入到一个文件里去，如下图所示。

![](../../pic/2020-03-30-13-42-57.png)

然后同时还会以异步的方式把消息写入到ConsumeQueue文件里去，因为一个Topic有多个MessageQueue，任何一条消息都是写入一个MessageQueue的，那个MessageQueue其实就是对应了一个ConsumeQueue文件

所以一条写入MessageQueue的消息，必然会异步进入对应的ConsumeQueue文件，如下图。

同时还会异步把消息写入一个IndexFile里，在里面主要就是把每条消息的key和消息在CommitLog中的offset偏移量做一个索引，这样后续如果要根据消息key从CommitLog文件里查询消息，就可以根据IndexFile的索引来了，如下图。

![](../../pic/2020-03-30-13-43-58.png)


接着我们来一步一步分析一下他在这里写入这几个文件的一个流程

首先Broker收到一个消息之后，必然是先写入CommitLog文件的，那么这个CommitLog文件在磁盘上的目录结构大致如何呢？看下面

![](../../pic/2020-03-30-14-04-27.png)


CommitLog文件的存储目录是在${ROCKETMQ_HOME}/store/commitlog下的，里面会有很多的CommitLog文件，每个文件默认是1GB大小，一个文件写满了就创建一个新的文件，文件名的话，就是文件中的第一个偏移量，如下面所示。文件名如果不足20位的话，就用0来补齐就可以了。

00000000000000000000

000000000003052631924

在把消息写入CommitLog文件的时候，会申请一个putMessageLock锁

也就是说，在Broker上写入消息到CommitLog文件的时候，都是串行的，不会让你并发的写入，并发写入文件必然会有数据错乱的问题，下面是源码片段。

![](../../pic/2020-03-30-14-07-05.png)

接着其实会对消息做出一通处理，包括设置消息的存储时间、创建全局唯一的消息ID、计算消息的总长度，然后会走一段很关键的源码，把消息写入到MappedFile里去，这个其实我们之前还讲解过里面的黑科技，看下面的源码。

![](../../pic/2020-03-30-14-07-50.png)

上面源码片段中，其实最关键的是cb.doAppend()这行代码，这行代码其实是把消息追加到MappedFile映射的一块内存里去，并没有直接刷入磁盘中，如下图所示。

![](../../pic/2020-03-30-14-08-18.png)

至于具体什么时候才会把内存里的数据刷入磁盘，其实要看我们配置的刷盘策略，这个我们后续会讲解，另外就是不管是同步刷盘还是异步刷盘，假设你配置了主从同步，一旦你写入完消息到CommitLog之后，接下来都会进行主从同步复制的。

那今天我们内容就讲到这里，其实到这里为止，就初步的讲完了Broker收到一条消息之后的处理流程了，先写入CommitLog中去。下一次我们讲解CommitLog的刷盘策略以及主从复制机制，然后接着再讲异步把消息写入ConsumeQueue和IndexFile里去。

# 113 一条消息写入CommitLog文件之后，如何实时更新索引文件？

关于这个同步刷盘和异步刷盘的问题，我们后续再讲，今天先来说说，这个消息写入CommitLog之后，然后消息是如何进入ConsumeQueue和IndexFile的。

实际上，Broker启动的时候会开启一个线程，ReputMessageService，他会把CommitLog更新事件转发出去，然后让任务处理器去更新ConsumeQueue和IndexFile，如下图。

![](../../pic/2020-03-30-14-12-07.png)

我们看下面的源码片段，在DefaultMessageStore的start()方法里，在里面就是启动了这个ReputMessageService线程。


这个DefaultMessageStore的start()方法就是在Broker启动的时候调用的，所以相当于是Broker启动就会启动这个线程。

![](../../pic/2020-03-30-14-13-23.png)

也就是说，在这个线程里，每隔1毫秒，就会把最近写入CommitLog的消息进行一次转发，转发到ConsumeQueue和IndexFile里去，通过的是doReput()方法来实现的，我们再看doReput()方法里的实现逻辑，先看下面源码片段。

![](../../pic/2020-03-30-14-14-08.png)

这段代码意思非常的清晰明了，就是从commitLog中去获取到一个DispatchRequest，拿到了一份需要进行转发的消息，也就是从CommitLog中读取的，


![](../../pic/2020-03-30-14-11-07.png)

接着他就会通过下面的代码，调用doDispatch()方法去把消息进行转发，一个是转发到ConsumeQueue里去，一个是转发到IndexFile里去

大家看下面的源码片段，里面走了CommitLogDispatcher的循环

![](../../pic/2020-03-30-14-15-28.png)

实际上正常来说这个CommitLogDispatcher的实现类有两个，分别是CommitLogDispatcherBuildConsumeQueue和CommitLogDispatcherBuildIndex，他们俩分别会负责把消息转发到ConsumeQueue和IndexFile，我画在下图中：


![](../../pic/2020-03-30-14-11-50.png)


接着我们看一下ConsumeQueueDispatche的源码实现逻辑，其实非常的简单，就是找到当前Topic的messageQueueId对应的一个ConsumeQueue文件

一个MessageQueue会对应多个ConsumeQueue文件，找到一个即可，然后消息写入其中。

![](../../pic/2020-03-30-14-16-35.png)

再来看看IndexFile的写入逻辑，其实也很简单，无非就是在IndexFile里去构建对应的索引罢了，如下面的源码片段。

![](../../pic/2020-03-30-14-16-55.png)

因此到这里为止，我想大家基本就看明白了，当我们把消息写入到CommitLog之后，有一个后台线程每隔1毫秒就会去拉取CommitLog中最新更新的一批消息，然后分别转发到ConsumeQueue和IndexFile里去，这就是他底层的实现原理。


# 114 RocketMQ是如何实现同步刷盘以及异步刷盘两种策略的？

我们之前简单提过一次，写入CommitLog的数据进入到MappedFile映射的一块内存里之后，后续会执行刷盘策略

比如是同步刷盘还是异步刷盘，如果是同步刷盘，那么此时就会直接把内存里的数据写入磁盘文件，如果是异步刷盘，那么就是过一段时间之后，再把数据刷入磁盘文件里去。

那么今天我们来看看底层到底是如何执行不同的刷盘策略的。

大家应该还记得之前我们说过，往CommitLog里写数据的时候，是调用的CommitLog类的putMessage()这个方法吧？

没错的，其实在这个方法的末尾有两行代码，很关键的，大家看一下下面的源码片段。

![](../../pic/2020-03-30-14-19-22.png)

大家会发现在末尾有两个方法调用，一个是handleDishFlush()，一个是handleHA()

顾名思义，一个就是用于决定如何进行刷盘的，一个是用于决定如何把消息同步给Slave Broker的。

关于消息如何同步给Slave Broker，这个我们就不看了，因为涉及到Broker高可用机制，这里展开说就太多了，其实大家有兴趣可以自己慢慢去研究，我们这里主要就是讲解一些RocketMQ的核心源码原理。


所以我们重点进入到handleDiskFlush()方法里去，看看他是如何处理刷盘的。

![](../../pic/2020-03-30-14-20-12.png)

上面代码我们就看的很清晰了，其实他里面是根据你配置的两种不同的刷盘策略分别处理的，我们先看第一种，就是同步刷盘的策略是如何处理的。

![](../../pic/2020-03-30-14-21-05.png)


其实上面就是构建了一个GroupCommitRequest，然后提交给了GroupCommitService去进行处理，然后调用request.waitForFlush()方法等待同步刷盘成功

万一刷盘失败了，就打印日志。具体刷盘是由GroupCommitService执行的，他的doCommit()方法最终会执行同步刷盘的逻辑，里面有如下代码。

![](../../pic/2020-03-30-14-22-18.png)


FlushCommitLogService其实是一个线程，他是个抽象父类，他的子类是CommitRealTimeService，所以真正唤醒的是他的子类代表的线程。

![](../../pic/2020-03-30-14-22-44.png)

具体在子类线程的run()方法里就有定时刷新的逻辑，这里就不赘述了，这里留做大家的课下作业。

其实简单来说，就是每隔一定时间执行一次刷盘，最大间隔是10s，所以一旦执行异步刷盘，那么最多就是10秒就会执行一次刷盘。



