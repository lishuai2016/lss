
<!-- TOC -->

- [01、执行器](#01执行器)
    - [1、执行器配置类XxlJobExecutor](#1执行器配置类xxljobexecutor)
    - [2、job的执行线程JobThread](#2job的执行线程jobthread)
    - [3、调度中心通过接口rpc调用handler](#3调度中心通过接口rpc调用handler)
        - [1、调用run(TriggerParam triggerParam)：触发任务的执行](#1调用runtriggerparam-triggerparam触发任务的执行)
        - [2、调用log(long logDateTim, long logId, int fromLineNum)：动态输出执行日志](#2调用loglong-logdatetim-long-logid-int-fromlinenum动态输出执行日志)
        - [3、调用kill(int jobId):终止正在运行的任务](#3调用killint-jobid终止正在运行的任务)
        - [4、idleBeat(int jobId)](#4idlebeatint-jobid)
        - [5、beat()](#5beat)
- [02、调度中心](#02调度中心)
    - [1、启动初始化](#1启动初始化)
        - [1、XxlJobAdminConfig](#1xxljobadminconfig)
        - [2、XxlJobScheduler核心类](#2xxljobscheduler核心类)
            - [JobScheduleHelper包含两部分的逻辑](#jobschedulehelper包含两部分的逻辑)
                - [1、读取xxl_job_info表的下一次触发时间进行任务的调度trigger_next_time](#1读取xxl_job_info表的下一次触发时间进行任务的调度trigger_next_time)
                - [2、timewheel调度触发任务](#2timewheel调度触发任务)
        - [3、JobTriggerPoolHelper真正触发调度工具类](#3jobtriggerpoolhelper真正触发调度工具类)
        - [3、执行器进行服务注册和服务下线、回调](#3执行器进行服务注册和服务下线回调)
            - [1、服务注册和心跳](#1服务注册和心跳)
            - [2、服务型下线](#2服务型下线)
            - [3、执行结果的回调](#3执行结果的回调)
- [03、数据库表结构](#03数据库表结构)
- [04、路由策略](#04路由策略)
    - [1、第一个ExecutorRouteFirst](#1第一个executorroutefirst)
    - [2、最后一个ExecutorRouteLast](#2最后一个executorroutelast)
    - [3、轮询ExecutorRouteRound](#3轮询executorrouteround)
    - [4、随机选择ExecutorRouteRandom](#4随机选择executorrouterandom)
    - [5、一致性HASH算法ExecutorRouteConsistentHash](#5一致性hash算法executorrouteconsistenthash)
    - [6、最不经常使用ExecutorRouteLFU](#6最不经常使用executorroutelfu)
    - [7、最近最久未使用ExecutorRouteLRU](#7最近最久未使用executorroutelru)
    - [8、故障转移ExecutorRouteFailover](#8故障转移executorroutefailover)
    - [9、忙碌转移ExecutorRouteBusyover](#9忙碌转移executorroutebusyover)
- [参考](#参考)

<!-- /TOC -->


备注：源码版本为2.1.1


![](../../pic/2020-04-04-16-04-36.png)


> 思想

将调度行为抽象形成“调度中心”公共平台，而平台自身并不承担业务逻辑，“调度中心”负责发起调度请求。

将任务抽象成分散的JobHandler，交由“执行器”统一管理，“执行器”负责接收调度请求并执行对应的JobHandler中业务逻辑。

因此，“调度”和“任务”两部分可以相互解耦，提高系统整体稳定性和扩展性；


XXL-JOB中“调度模块”和“任务模块”完全解耦，调度模块进行任务调度时，将会解析不同的任务参数发起远程调用，调用各自的远程执行器服务。这种调用模型类似RPC调用，调度中心提供调用代理的功能，而执行器提供远程服务的功能。执行器”在接收到任务执行请求后，执行任务，在执行结束之后会将执行结果回调通知“调度中心”：




# 01、执行器

## 1、执行器配置类XxlJobExecutor

![](../../pic/2020-04-04-12-40-02.png)

XxlJobExecutor核心参数

```java
//需要在配置类中设置的7参数，一般通过配置文件的方式注入
private String adminAddresses;//调度中心地址
private String appName;//当前应用的名称，即执行器名称，唯一标识
private String ip;//当前机器IP
private int port;//端口
private String accessToken;//安全调用参数
private String logPath;//日志路径
private int logRetentionDays;//日志保存策略

//保存和调度中心通信的客户端AdminBiz，这里是HTTP方式。
private static List<AdminBiz> adminBizList;

//缓存handlername和对应的实现类，在执行的时候根据调度参数通过name找到实现类去执行任务
private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();

//这里缓存的是任务id和对应的调度线程之间的关系，根据调度参数jobid找对应的线程JobThread去执行，如果没有就创建一个新的线程去执行。这里会根据当前线程运行状态和不同的阻塞策略进行不同的处理。1、串行的话，当前jobid任务还在运行，如果同一个任务被频繁的调用会把调度参数添加到JobThread内部的阻塞队列中慢慢消耗。
private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<Integer, JobThread>();

```

> 在调用start方法后会启动3个后台线程：

- 1、JobLogFileCleanThread：清理日志

- 2、TriggerCallbackThread：回调执行结果

- 3、ExecutorRegistryThread：注册服务到调度中心


> 启动的容器2个容器，在关闭应用的时候需要清空销毁：

- 1、jobHandlerRepository：处理器map

- 2、jobThreadRepository：job线程map



调度参数

```
int jobId;//任务id
String executorHandler;//那个handler执行本次的调度任务
String executorParams;//执行参数
String executorBlockStrategy;//阻塞策略：串行等待、丢弃、终止上一个
int executorTimeout;//执行超时设置
long logId;//一次调度生成的日志编号唯一
long logDateTime;//触发调度的时间
String glueType;//glue方式相关参数
String glueSource;
long glueUpdatetime;
int broadcastIndex;//分片调度参数
int broadcastTotal;
```

![](../../pic/2020-04-04-12-40-57.png)


备注：xxlJobExecutor.start();执行完毕后，说明handler已经注册到调度中心并且开始监听netty server的端口，等待调度中心的调度请求。


## 2、job的执行线程JobThread

![](../../pic/2020-04-04-14-30-23.png)

public class JobThread extends Thread{}

继承自Thread类，通过在run方法内部使用一个状态变量toStop标记该线程是否停止，不关闭的话会不断从内部的阻塞队列triggerQueue中阻塞3秒拉取一次触发参数，拉取到进行处理，没有拉取到进行下一次循环阻塞，但是这里有一个空闲时长限制，超过30次没有任务就会销毁该线程，也就是线程最长空闲90秒就会被销毁。

这里会根据是否设置超时超时参数来进行不同的处理：

- 1、设置超时参数（executorTimeout>0）：会把执行任务封装成一个FutureTask对象，开启一个新的线程去执行这个FutureTask任务，然后通过这个FutureTask设置超时阻塞获取结果，当在指定的超时时间没有返回结果，就会解除阻塞，标记该任务执行超时了；

- 2、没有设置参数（默认executorTimeout=0）：直接同步执行；

- 3、如果关闭线程是发现阻塞队列中还有触发任务没有执行完，需要回调给调度中心后再进行关闭；


备注：

- 1、这里会启动设置一个日志文件的名字XxlJobFileAppender.contextHolder.set(logFileName)中，通过InheritableThreadLocal<String>来实现线程变量的私有，实现每个调度输出各自的日志到单个文件中；

- 2、中断正在执行的任务同设置toStop=true已经调用线程的interrupt实现；toStop实现不进行下一次循环，interrupt实现终止线程的阻塞状态(wait、join、sleep)；


## 3、调度中心通过接口rpc调用handler

调度中心通过rpc进行调用的接口：

![](../../pic/2020-04-04-13-03-57.png)


实现类：

public class ExecutorBizImpl implements ExecutorBiz {}

### 1、调用run(TriggerParam triggerParam)：触发任务的执行

整体先看下jobThreadRepository中是否已经存在对应的JobThread，已经根据handlername去jobHandlerRepository中去找handler的实现类。如果JobThread没有的话,根据jobid和最新的handler创建一个，然后启动线程去异步执行，这里相当于仅仅是调度，不包含执行的过程。

根据JobThread的状态会存在几种处理逻辑：

- 1、JobThread之前不存在，或者历史JobThread中的处理器handler和最新获取的handler不一致，需要销毁之前的，然后新建，然后把调度参数添加到新建线程的阻塞队列中；

- 2、之前存在jobid对应的JobThread，并且其内部的handler正常，这个时候根据阻塞策略来进行不同的处理；
    - 2.1、串行：直接把触发参数放到JobThread内部的阻塞队列即可；
    - 2.2、丢弃当前：判断JobThread正在运行中，直接返回本次调度，标记位调度失败；如果没在运行中，添加到线程的阻塞队列即可；
    - 2.3、覆盖之前：判断JobThread正在运行中，需要通过终止命令杀死之前的调度，然后新建一个JobThread，然后把调度参数；如果没在运行中，添加到线程的阻塞队列即可；



总结：整体思路是把调度和任务的具体执行分开，这里只是完成了调度任务，然后异步执行任务交给JobThread，调度成功还是失败在这里同步返回给调度中心，只有执行成功还是失败则由JobThread执行完成后回调调度中心进行。


### 2、调用log(long logDateTim, long logId, int fromLineNum)：动态输出执行日志

步骤：

- 1、根据参数logDateTim和logId构建当前日志的存储路径，格式：配置的基础路径logPath/yyyy-MM-dd以日期作为子文件建/9999.log日志id作为文件的名称；

- 2、读取文件的内容：通过 XxlJobFileAppender.readLog(logFileName, fromLineNum)从fromLineNum开始一直到当前文件的尾部然后封装成一个LogResult对象返回。每次rpc调用会传不同的fromLineNum，即从哪里开始读取



> LogResult包含以下四个字段

```java
public LogResult(int fromLineNum, int toLineNum, String logContent, boolean isEnd) {
        this.fromLineNum = fromLineNum;//返回内容的开始行
        this.toLineNum = toLineNum;//返回内容的结束行
        this.logContent = logContent;//文件的内容
        this.isEnd = isEnd;//是否到文件的尾部的标志
    }

```

> 前端的请求逻辑【在调度中心】

- 1、先请求/logDetailPage，返回一个当前调度的基本参数返回：triggerCode、handleCode、executorAddress、triggerTime和logId，

- 2、前端的js会判断是否调度失败，失败的话不会存在执行日志，显示调度失败的文案；否则，初始化请求参数请求controller中的logDetailCat(String executorAddress, long triggerTime, long logId, int fromLineNum)，controller接收到请求后通过rpc请求执行器中的日志返回给前端。

- 3、前端接收到返回值是会判断isEnd字段是否为文件的尾部，没有到尾部的话，前端有个3秒钟的定时请求logDetailCat，只不过参数fromLineNum=toLineNum+1，保证日志不重复；

- 4、isEnd字段何时标记位true：正常请求时，在执行器中是没有标记这个变量为true，而是在调度中心根据任务的handleCode状态为以及运行完毕以及执行器返回的result中fromLineNum>toLineNum，设置isEnd=true，前端接收到isEnd=true不会再发送下一次的请求；



### 3、调用kill(int jobId):终止正在运行的任务


在调度中心的运行日志中点终止任务的处理流程：

- 1、调度中心的logKill，参数为日志id，在controller中根据日志id找到对应的jobid，然后通过rpc调用执行器中的接口通过jobid杀死任务；[备注：因为现在的调度策略不支持并发调度，不会存在多个jobid对应线程同时运行的情况]

- 2、执行器中：从jobThreadRepository中找到jobid对应的线程JobThread，然后执行XxlJobExecutor.removeJobThread(jobId, "scheduling center kill job.")，包括从jobThreadRepository中移除已经终止线程的运行。



### 4、idleBeat(int jobId)

调度中心探测针对该jobId是否已经存在JobThread，并且该线程正在处理该jobid之前触发的任务。

### 5、beat()

调度中心探测执行器是否活着



# 02、调度中心

## 1、启动初始化

### 1、XxlJobAdminConfig

```java
private static XxlJobAdminConfig adminConfig = null;
    public static XxlJobAdminConfig getAdminConfig() {
        return adminConfig;
    }


    // ---------------------- XxlJobScheduler ----------------------

    private XxlJobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {//配置初步发入口
        adminConfig = this;

        xxlJobScheduler = new XxlJobScheduler();
        xxlJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        xxlJobScheduler.destroy();
    }

    //其他一些配置数据源等属性

```

从上面可以看出XxlJobAdminConfig借助于spring接口方法在对象初始化和销毁时机点进行逻辑的嵌入。这里在初始化的时候构建一个核心对象XxlJobScheduler。


### 2、XxlJobScheduler核心类

```java

public class XxlJobScheduler  {//调度入口类
    
    public void init() throws Exception {
        // init i18n 国际化
        initI18n();

        // admin registry monitor run 根据注册表xxl_job_registry更新表xxl_job_group表中执行器的在线IP列表
        JobRegistryMonitorHelper.getInstance().start();

        // admin monitor run 读取xxl_job_log中的数据进行失败重试和邮件报警。并通过字段的状态解决分布式部署并发问题
        JobFailMonitorHelper.getInstance().start();

        // admin trigger pool start 初始化两个执行调度任务线程池
        JobTriggerPoolHelper.toStart();

        // admin log report start  1、统计最近三天运行任务总数，成功、失败个数,保存更新到xxl_job_log_report中；2、清理表xxl_job_log中N天前的日志要求N >= 7
        JobLogReportHelper.getInstance().start();

        // start-schedule  触发任务的执行
        JobScheduleHelper.getInstance().start();

        logger.info(">>>>>>>>> init xxl-job admin success.");
    }


    public void destroy() throws Exception {

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

        // admin monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryMonitorHelper.getInstance().toStop();

    }


    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();
    public static ExecutorBiz getExecutorBiz(String address) throws Exception {//这里根据地址构建和执行器通信的netty客户端代理对象
       //...
        return executorBiz;
    }

}
```

这init中会启动几个后台线程：

- 1、JobRegistryMonitorHelper：根据注册表xxl_job_registry更新表xxl_job_group表中执行器的在线IP列表；【执行器注册信息保存在xxl_job_registry中】

- 2、JobFailMonitorHelper：读取xxl_job_log中的数据进行失败重试和邮件报警。并通过字段的状态解决分布式部署并发问题；

- 3、JobLogReportHelper：基于运行日志统计执行报告以及清理日志。统计最近三天运行任务总数，成功、失败个数,保存更新到xxl_job_log_report中；清理表xxl_job_log中N天前的日志要求N >= 7

- 4、JobScheduleHelper：基于timewheel算法调度定时任务；

#### JobScheduleHelper包含两部分的逻辑

##### 1、读取xxl_job_info表的下一次触发时间进行任务的调度trigger_next_time

实现分布式锁的逻辑：

```sql
CREATE TABLE `xxl_job_lock` (
  `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 只有一条数据schedule_lock



select * from xxl_job_lock where lock_name = 'schedule_lock' for update
```


这里基于数据库锁实现分布式部署访问的问题，每一次查询会读取<=now+5s时间范围的数据，然后
针对不同时间段的数据进行不同的处理：

- 1、[-无穷大,now-5) :过期超过5秒，依据当前时间计算下次触发时间更新数据库；

- 2、[now-5,now):过期5秒内，直接触发并依据当前时间计算下次触发时间，这时如果下次触发时间在[now,now+5]范围内，计算触发在第几秒，然后放入TimeRing并计算下下次触发时间，更新数据库；

- 3、[now,now+5]：这是一般情况，计算在第几秒触发，然后放入TimeRing并计算下下次触发时间，更新数据库；

备注：感觉前两种情况是在修正过期调度的数据，正常调度走的应该是第3中情况。


这里会根据一轮次读数据库并进行逻辑处理耗时，决定下次读取数据库间隔多久：

- 1、如果上面的逻辑处理耗时小于1秒，会有两种情况：
    - 1.1、预读成功，处理耗时小于1秒，然后修整为每个一秒进行预读一次数据库
    - 1.2、预读没有数据，改为5秒读取一次数据库；

- 2、处理耗时超过1秒，根据具体的执行间隔读取数据库，这里假如时间间隔比较大，比如超过5秒会使得调度不准，这个5秒的预读如何定的？？？

备注：整体来说，要是数据库读取到数据，那就是间隔1秒读一次；如果没有读到数据则每隔5秒读取一次。

##### 2、timewheel调度触发任务

private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

这里使用map存储需要在每秒调度的任务，这里key为当前时刻的秒，范围是[0,59)，value是对应这一秒要执行的任务jobid列表，这里使用ArrayList存储。


这里有一个线程在整点秒触发取map中该秒对应要触发的任务列表，然后遍历通过JobTriggerPoolHelper触发即可。




### 3、JobTriggerPoolHelper真正触发调度工具类


JobTriggerPoolHelper内部有两个线程池[fastTriggerPool默认200和slowTriggerPool默认100]来执行异步触发调度任务，使得调用触发接口的线程不会阻塞。针对1分钟执行超时10次的任务放到slowTriggerPool池中调度，一般默认放在fastTriggerPool中调度。


这里的超时定义为：一个任务500毫秒没有调度到执行器的jobthread则认为超时。


> 1、trigger触发调度方法参数说明

```java

/**
     * @param jobId 任务id
     * @param triggerType 触发类型
     * @param failRetryCount 失败重试，大于0生效
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam 分片执行参数
     * @param executorParam 一般的调度执行参数
     *          null: use job param
     *          not null: cover job param
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam) {
        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam);
    }
```
















> 2、任务的触发类型

```java

public enum TriggerTypeEnum {//任务的触发类型

    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),//手动触发
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),//cron定时触发
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),//失败重试触发
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),//父任务带起了的触发
    API(I18nUtil.getString("jobconf_trigger_type_api"));//通过api接口调用的触发
```



### 3、执行器进行服务注册和服务下线、回调


#### 1、服务注册和心跳

仅仅是把注册的参数存到数据库中xxl_job_registry，发送心跳时是更新时间戳。会有专门的一个线程把这个表的注册信息同步到表xxl_job_group中，调度是基于xxl_job_group表中的数据进行的。


#### 2、服务型下线

服务下线的时候也是删除表xxl_job_registry中的记录

备注：更新和删除是通过字段registry_group+registry_key+registry_value来实现的。其实这里这三个字段组成唯一key索引，但是作者只是设置了这三个参数的普通索引。

#### 3、执行结果的回调

这里主要做了两件事：

- 1、根据回调参数更新调度日志的状态，表xxl_job_log；

- 2、判断该任务执行成功后，如果配置了自任务，触发子任务的运行；



# 03、数据库表结构

- xxl_job_lock：任务调度锁表【保证当部署多个调度中心时，统一时刻只能由一个发起调度任务，避免任务被重复调度】；

- xxl_job_group：执行器信息表，维护任务执行器信息；

- xxl_job_info：调度扩展信息表： 用于保存XXL-JOB调度任务的扩展信息，如任务分组、任务名、机器地址、执行器、执行入参和报警邮件等等；

- xxl_job_log：调度日志表： 用于保存XXL-JOB任务调度的历史信息，如调度结果、执行结果、调度入参、调度机器和执行器等等；

- xxl_job_log_report：调度日志报表：用户存储XXL-JOB任务调度日志的报表，调度中心报表功能页面会用到【按照天统计，运行了多少次调度，成功了多少，失败了多少】；

- xxl_job_logglue：任务GLUE日志：用于保存GLUE更新历史，用于支持GLUE的版本回溯功能；

- xxl_job_registry：执行器注册表，维护在线的执行器和调度中心机器地址信息；【相当于注册中心的功能】

- xxl_job_user：系统用户表；【简单的权限管理，管理员和普通用户，普通用户可以按照执行器粒度分配权限】


# 04、路由策略

```java
public enum ExecutorRouteStrategyEnum {

    FIRST(I18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst()),//第一个
    LAST(I18nUtil.getString("jobconf_route_last"), new ExecutorRouteLast()),//最后一个
    ROUND(I18nUtil.getString("jobconf_route_round"), new ExecutorRouteRound()),//轮询
    RANDOM(I18nUtil.getString("jobconf_route_random"), new ExecutorRouteRandom()),//随机选择在线的机器
    CONSISTENT_HASH(I18nUtil.getString("jobconf_route_consistenthash"), new ExecutorRouteConsistentHash()),//一致性HASH
    LEAST_FREQUENTLY_USED(I18nUtil.getString("jobconf_route_lfu"), new ExecutorRouteLFU()),//最不经常使用
    LEAST_RECENTLY_USED(I18nUtil.getString("jobconf_route_lru"), new ExecutorRouteLRU()),//最近最久未使用
    FAILOVER(I18nUtil.getString("jobconf_route_failover"), new ExecutorRouteFailover()),//故障转移
    BUSYOVER(I18nUtil.getString("jobconf_route_busyover"), new ExecutorRouteBusyover()),//忙碌转移
    SHARDING_BROADCAST(I18nUtil.getString("jobconf_route_shard"), null);//分片广播
}

```

```java
public abstract class ExecutorRouter {//路由策略，基于触发参数和执行器地址列表选择

    /**
     * route address
     *
     * @param addressList
     * @return  ReturnT.content=address
     */
    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);

}
```

![](../../pic/2020-04-05-11-26-20.png)


## 1、第一个ExecutorRouteFirst

拿到执行器列表addressList直接取addressList.get(0)；

## 2、最后一个ExecutorRouteLast


直接取list的最后一个addressList.get(addressList.size()-1)；

## 3、轮询ExecutorRouteRound

以一天为一个单位初始化下count计数，然后%addressList.size()，每次调度后count++，实现均匀调度执行器；


## 4、随机选择ExecutorRouteRandom

通过Random实现随机

Random localRandom = new Random();

addressList.get(localRandom.nextInt(addressList.size()))


## 5、一致性HASH算法ExecutorRouteConsistentHash

一般的hash会存在分别不均匀问题，以及添加或者新增执行器，相同的key被hash后落的执行器变化比较大。

这里借助于TreeMap实现。

```java

public String hashJob(int jobId, List<String> addressList) {

    // ------A1------A2-------A3------
    // -----------J1------------------
    TreeMap<Long, String> addressRing = new TreeMap<Long, String>();
    //这里针对每个地址创建5个虚拟地址在这个一致性hash环上，实现均匀分配
    for (String address: addressList) {
        for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {//VIRTUAL_NODE_NUM=5
            long addressHash = hash("SHARD-" + address + "-NODE-" + i);
            addressRing.put(addressHash, address);
        }
    }

    long jobHash = hash(String.valueOf(jobId));
    //借助于SortedMap.tailMap函数获取这个map中大于等于这个jobHash的map集合
    SortedMap<Long, String> lastRing = addressRing.tailMap(jobHash);
    if (!lastRing.isEmpty()) {
        return lastRing.get(lastRing.firstKey());//取返回集合中最小的那个
    }
    return addressRing.firstEntry().getValue();
}
```

## 6、最不经常使用ExecutorRouteLFU

- LFU(Least Frequently Used)：最不经常使用，频率/次数
- LRU(Least Recently Used)：最近最久未使用，时间


## 7、最近最久未使用ExecutorRouteLRU

- LFU(Least Frequently Used)：最不经常使用，频率/次数
- LRU(Least Recently Used)：最近最久未使用，时间

这里通过LinkedHashMap实现没有长度现在的LRU，每次选择地址中最久没有访问的，也就是LinkedHashMap双向链表前面的数据

```java

public String route(int jobId, List<String> addressList) {

    // cache clear
    if (System.currentTimeMillis() > CACHE_VALID_TIME) {
        jobLRUMap.clear();
        CACHE_VALID_TIME = System.currentTimeMillis() + 1000*60*60*24;
    }

    // init lru
    LinkedHashMap<String, String> lruItem = jobLRUMap.get(jobId);
    if (lruItem == null) {
        /**
            * LinkedHashMap
            *      a、accessOrder：true=访问顺序排序（get/put时排序）；false=插入顺序排期；
            *      b、removeEldestEntry：新增元素时将会调用，返回true时会删除最老元素；可封装LinkedHashMap并重写该方法，比如定义最大容量，超出是返回true即可实现固定长度的LRU算法；
            */
        lruItem = new LinkedHashMap<String, String>(16, 0.75f, true);
        jobLRUMap.putIfAbsent(jobId, lruItem);
    }

    // put new
    for (String address: addressList) {
        if (!lruItem.containsKey(address)) {
            lruItem.put(address, address);
        }
    }
    // remove old
    List<String> delKeys = new ArrayList<>();
    for (String existKey: lruItem.keySet()) {
        if (!addressList.contains(existKey)) {
            delKeys.add(existKey);
        }
    }
    if (delKeys.size() > 0) {
        for (String delKey: delKeys) {
            lruItem.remove(delKey);
        }
    }

    // load
    String eldestKey = lruItem.entrySet().iterator().next().getKey();
    String eldestValue = lruItem.get(eldestKey);
    return eldestValue;
}
```




## 8、故障转移ExecutorRouteFailover

## 9、忙碌转移ExecutorRouteBusyover





# 参考

- [官方文档](https://www.xuxueli.com/xxl-job/)










