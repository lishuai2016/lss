
<!-- TOC -->

- [1、基本参数](#1基本参数)
- [2、关于文件个数和合并的参数](#2关于文件个数和合并的参数)
- [3、SQL优化的参数](#3sql优化的参数)
- [4、压缩参数](#4压缩参数)
- [参考](#参考)

<!-- /TOC -->


> 常用参数设置

```shell
set hive.exec.parallel = true;并行化执行
set hive.exec.parallel.thread.numbe=8;并行化执行最多并行运行8个job

map数据量调整：
set  mapred.max.split.size=536870912 ;
set  mapred.min.split.size=536870912 ;
set  mapred.min.split.size.per.node=536870912 ;
set  mapred.min.split.size.per.rack=536870912 ;

Hive表优化 开启动态分区
set hive.exec.dynamic.partition=true;
set hive.exec.dynamic.partition.mode=nonstrict;
set hive.exec.max.dynamic.partitions = 100000;
set hive.exec.max.dynamic.partitions.pernode = 100000;


文件合并设置
set hive.input.format = org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;
set hive.merge.mapfiles = true;
set hive.merge.mapredfiles = true;
set hive.merge.size.per.task = 256000000;
set hive.merge.smallfiles.avgsize = 256000000;

```



MapReduce作业配置参数,可在客户端的mapred-site.xml中配置，作为MapReduce作业的缺省配置参数。也可以在作业提交时，个性化指定这些参数。

> 推测执行(Speculative Execution)

1.为什么需要推测执行？

MapReduce将作业分解成多个任务并行运行的机制，决定了作业运行的总体时间对运行缓慢的任务比较敏感。为了尽量避免运行缓慢的任务对作业运行时间“托后腿”的情况，需要启动作业的推测执行。
 

2.什么是推测执行？

当Hadoop检测到一个任务运行比预期慢时，它会启动一个相同的任务进行备份。这就是任务的推测执行。  当原任务与推测任务其中之一完成后，便立即停止另一个任务。

3.推测执行的属性设置

推测执行可以基于集群，也可以基于某个作业。

- mapred.map.tasks.speculative.execution  如果任务运行较慢，决定是否运行相应Map任务的推测执行，默认true；
- mapred.reduce.tasks.speculative.execution 如果任务运行较慢，决定是否运行相应Reduce任务的推测执行，。默认true；


备注：
- 任务运行缓慢的原因有多种，可能是硬件老化，也可能是软件问题。若是软件问题，需优化程序，推测执行不能从根本上解决问题。
- 推测执行是利用资源来优化时间的一种策略。若资源本来就紧张的情况下，是无法通过推测执行来优化系统性能的。
- 推测执行会降低集群容量。



# 1、基本参数

- hive.cli.print.current.db=true; 让提示符显示当前库
- hive.cli.print.header=true; 显示查询结果时显示字段名称


- mapreduce.job.user.name=ls        # 队列对应的账户
- mapreduce.job.queuename=query_queue    # 提交任务到那个队列执行

- mapreduce.job.running.reduce.limit=600;
- mapreduce.job.running.map.limit=1000;
- mapreduce.job.reduce.slowstart.completedmaps=1.0;

- mapreduce.map.cpu.vcores=4; # 每个Map task可使用的最多cpu core数目, 默认值: 1
- mapreduce.map.memory.mb=4096;# 一个Map Task可使用的资源上限（单位:MB），默认为1024。如果Map Task实际使用的资源量超过该值，则会被强制杀死，并且其要大于等于mapreduce.map.java.opts设置的大小
- mapreduce.map.java.opts=-Xmx3276M; # Map Task的JVM参数，你可以在此配置默认的java heap size等参数
- mapreduce.map.maxattempts  Map Task最大失败尝试次数，默认4
- mapreduce.map.speculative  是否对Map Task启用推测执行机制，默认false

- mapreduce.reduce.cpu.vcores=4; # 每个Reduce task可使用的最多cpu core数目, 默认值: 1
- mapreduce.reduce.memory.mb=8192;# 一个Reduce Task可使用的资源上限（单位:MB），默认为1024。如果Reduce Task实际使用的资源量超过该值，则会被强制杀死。，并且其要大于等于mapreduce.map.java.opts设置的大小
- mapreduce.reduce.java.opts=-Xmx6553M; # Reduce Task的JVM参数，你可以在此配置默认的java heap size等参数
- mapreduce.reduce.maxattempts Reduce Task最大失败尝试次数，默认4
- mapreduce.reduce.speculative 是否对Reduce Task启用推测执行机制，默认false


- mapreduce.task.io.sort.mb=512;  # Map端数据排序可用内存，如果内存充裕建议调大至256M或512M，可提升排序性能。任务内部排序缓冲区大小
- mapreduce.map.sort.spill.percent Map阶段溢写文件的阈值（排序缓冲区大小的百分比），默认0.8

- mapreduce.am.max-attempts    MR ApplicationMaster最大失败尝试次数，默认2



- yarn.app.mapreduce.am.resource.cpu-vcores = 3; MR ApplicationMaster占用的虚拟CPU个数,默认1
- yarn.app.mapreduce.am.resource.mb = 4096; MR ApplicationMaster占用的内存量，默认1536
- yarn.app.mapreduce.am.command-opts = -Xmx3276m;




# 2、关于文件个数和合并的参数


- hive.merge.smallfiles.avgsize=134217728; 在作业输出文件小于该值时，起一个额外的map/reduce作业将小文件合并为大文件，小文件的基本阈值，设置大点可以减少小文件个数，注意：需要mapfiles和mapredfiles为true， 默认值是16MB；
- hive.merge.mapfiles=true;  在只有map的作业结束时合并小文件，默认开启true
- hive.merge.mapredfiles=true; 在一个map/reduce作业结束后合并小文件，默认不开启false
- hive.merge.orcfile.stripe.level=false;
- hive.merge.size.per.task 作业结束时合并文件的大小，默认256MB；


- mapred.output.compress=true;
- mapred.output.compression.codec=com.hadoop.compression.lzo.LzopCodec;
- mapred.reduce.tasks = 50; 每个作业的reduce任务数，默认是hadoop client的配置1个；
- mapred.task.timeout=600000



- hive.exec.parallel=true  hive的执行job是否并行执行，默认不开启false，在很多操作如join时，子查询之间并无关联可独立运行，这种情况下开启并行运算可以大大加速。
- hive.exec.parallel.thread.number 并行运算开启时，允许多少作业同时计算，默认是8
- hive.exec.compress.output=true; 确定查询中最终map / reduce作业的输出是否已压缩。
- hive.exec.reducers.bytes.per.reducer 每个reducer的大小，默认是1G，输入文件如果是10G，那么就会起10个reducer；
- hive.exec.reducers.max：reducer的最大个数，如果在mapred.reduce.tasks设置为负值，那么hive将取该值作为reducers的最大可能值。当然还要依赖（输入文件大小/hive.exec.reducers.bytes.per.reducer）所得出的大小，取其小值作为reducer的个数,hive默认是999；
- hive.exec.max.created.files：一个mapreduce作业能创建的HDFS文件最大数，默认是100000（十万）；

# 3、SQL优化的参数

- hive.mergejob.maponly：试图生成一个只有map的任务去做merge，前提是支持CombineHiveInputFormat，默认开启true；

- hive.auto.convert.join=true; 根据输入文件的大小决定是否将普通join转换为mapjoin的一种优化，默认不开启false
- hive.mapjoin.smalltable.filesize=25000000;输入表文件的mapjoin阈值，如果输入文件的大小小于该值，则试图将普通join转化为mapjoin，默认25MB；
- hive.auto.convert.join.noconditionaltask.size=100000000;

- hive.groupby.skewindata 决定 group by 操作是否支持倾斜的数据。 默认值false

- hive.default.fileformat Hive默认的输出文件格式，与创建表时所指定的相同，可选项为 'TextFile' 、 'SequenceFile' 或者 'RCFile'。 默认值 TextFile'   

- hive.mapred.mode Map/Redure 模式，如果设置为 strict，将不允许笛卡尔积。 默认值 'nonstrict'


- hive.exec.dynamic.partition 是否打开动态分区。 默认 false
- hive.exec.dynamic.partition.mode 打开动态分区后动态分区的模式，有 strict 和 nonstrict 两个值可选，strict 要求至少包含一个静态分区列，nonstrict 无此要求。 
- hive.exec.max.dynamic.partitions  所允许的最大的动态分区的个数。 默认值1000
- hive.exec.max.dynamic.partitions.pernode 单个 reduce 结点所允许的最大的动态分区的个数。 默认值 100


# 4、压缩参数

- hive.exec.compress.output 控制hive的查询结果输出是否进行压缩，压缩方式在hadoop的mapred.output.compress中配置， 默认不压缩false；

- hive.exec.compress.intermediate 控制hive的查询中间结果是否进行压缩，同上条配置，默认不压缩false;





# 参考

- [hadoop YARN配置参数剖析—MapReduce相关参数](https://www.cnblogs.com/andy6/p/7689721.html)


- [hive开头的相关配置](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfigurationVariables)

- [hive参数配置终极总结](https://blog.csdn.net/aA518189/article/details/84763854)
