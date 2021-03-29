


Hadoop是很多组件的集合，主要包括但不限于MapReduce，HDFS，HBase，ZooKeeper。`MapReduce模仿了Google MapReduce，HDFS模仿了Google File System，HBase模仿了Google BigTable，ZooKeeper或多或少模仿了Google Chubby（没有前3个出名）`，所以下文就只提MapReduce、HDFS、HBase、ZooKeeper吧。



- HDFS和HBase是依靠外存（即硬盘）的[分布式文件]存储实现和[分布式表]存储实现。HDFS是一个分布式的“云存储”文件系统，它会把一个文件分块并分别保存，取用时分别再取出、合并。重要的是，这些分块通常会在3个节点（即集群内的服务器）上各有1个备份，因此即使出现少数节点的失效（如硬盘损坏、掉电等），文件也不会失效。`如果说HDFS是文件级别的存储，那HBase则是表级别的存储`。HBase是表模型，但比SQL数据库的表要简单的多，没有连接、聚集等功能。HBase的表是物理存储到HDFS的，比如把一个表分成4个HDFS文件并存储。由于HDFS级会做备份，所以HBase级不再备份。MapReduce则是一个计算模型，而不是存储模型；


- MapReduce通常与HDFS紧密配合。举个例子：假设你的手机通话信息保存在一个HDFS的文件callList.txt中，你想找到你与同事A的所有通话记录并排序。因为HDFS会把callLst.txt分成几块分别存，比如说5块，那么对应的Map过程就是找到这5块所在的5个节点，让它们分别找自己存的那块中关于同事A的通话记录，对应的Reduce过程就是把5个节点过滤后的通话记录合并在一块并按时间排序。MapReduce的计算模型通常把HDFS作为数据来源，很少会用到其它数据来源比如HBase。


- ZooKeeper本身是一个非常牢靠的记事本，用于记录一些概要信息。Hadoop依靠这个记事本来记录当前哪些节点正在用，哪些已掉线，哪些是备用等，以此来管理机群。


下列论文：

```
2003 The Google file system    

2005 MapReduce: Simplified data processing on large clusters  

2008 TheBigtable: A distributed storage system for structured data Google file system    

2010 S4: distributed stream computing platform    

2011 Fast Crash Recovery in RAMCloud
```










# 参考

- [Bigtable 具体是怎样一个东西？和 MapReduce, GFS 之间的关系是什么？](https://www.zhihu.com/question/19898246/answer/13289151)

