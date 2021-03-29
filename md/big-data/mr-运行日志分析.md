

Hadoop 2.6包含两个大部分：DFS和Yarn，而Yarn里面又包含在Resource Manager的JVM中运行的部分和在Node Manager里面运行的JVM部分。所以整个系统（不考虑加装ZooKeeper的HA的情况）的log是分别放在3个log里面的。

- 1.对于DFS的log，在Name Node和Data Node里面，默认可以在${HADOOP_INSTALL}/logs里面看到。这个是非DFS的文件，直接可以通过Linux文件系统看到。

- 2.对于Yarn的log，在Resource Manager和Node Manager里面，默认可以在${HADOOP_INSTALL}/logs里面看到。这个也是非DFS的文件，直接可以通过Linux文件系统看到。

对于MapReduce任务的log，情况就比较的复杂了。在2.6里面，task是按照application->container的层次来管理的，所以在Name Node机器上运行mapreduce程序的时候，在console里面看到的log都可以通过在相应的data node/node manager里面的${HADOOP_INSTALL}/logs/userlogs下面找到。这个部分也是非DFS文件，直接可以通过Linux文件系统看到。

这些log也可以通过Hadoop Web管理页面看到，比较方便。

`mapper和reduce通过log输出的日志存再hdfs上某个目录下，在yarn-site.xml可以配置`

```xml
    <name>yarn.nodemanager.remote-app-log-dir</name>
    <value>/logs</value>
    <description>HDFS directory where the application logs are moved on application completion. Need to set appropriate permissions. Only applicable if log-aggregation is enabled. The default value is "/logs" or "/tmp/logs" </description>
  </property>
```








# 参考

- [Hadoop 2.6 日志文件和MapReduce的log文件研究心得](https://blog.csdn.net/infovisthinker/article/details/45370089)