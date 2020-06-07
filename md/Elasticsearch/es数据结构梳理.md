
<!-- TOC -->

- [01、基础概念](#01基础概念)
    - [1、ES和Lucene](#1es和lucene)
    - [2、shard相关知识点](#2shard相关知识点)
- [04、倒排索引](#04倒排索引)
    - [1、Posting List](#1posting-list)
    - [2、Term Dictionary](#2term-dictionary)
    - [3、Term Index](#3term-index)
- [05、数据持久化模型](#05数据持久化模型)
    - [1、index](#1index)
    - [2、inverted index](#2inverted-index)
    - [3、shard](#3shard)
    - [4、segment](#4segment)
- [06、数据持久化详情-增删改查](#06数据持久化详情-增删改查)
    - [1、创建 create](#1创建-create)
        - [1、write](#1write)
        - [2、refresh](#2refresh)
        - [3、flush](#3flush)
        - [4、segment合并](#4segment合并)
        - [5、Translog](#5translog)
        - [写操作](#写操作)
    - [2、更新 update 和删除 delete](#2更新-update-和删除-delete)
        - [更新操作](#更新操作)
    - [3、读read](#3读read)
        - [1、查询阶段](#1查询阶段)
        - [2、提取阶段](#2提取阶段)
        - [读操作](#读操作)
            - [1、_search结果解析](#1_search结果解析)
            - [2、分页查询以及深度分页存在的问题](#2分页查询以及深度分页存在的问题)
            - [3、相关度评分TF&IDF算法](#3相关度评分tfidf算法)
    - [4、搜索相关性](#4搜索相关性)
    - [5、多文档模式（批量操作）](#5多文档模式批量操作)
        - [1、使用 mget 取回多个文档](#1使用-mget-取回多个文档)
        - [2、使用 bulk 修改多个文档](#2使用-bulk-修改多个文档)
- [07、优化](#07优化)
    - [0、Elasticsearch的索引思路](#0elasticsearch的索引思路)
    - [1、写入构建索引优化](#1写入构建索引优化)
        - [1、批量写入](#1批量写入)
        - [2、多线程写入](#2多线程写入)
        - [3、增大refresh interval](#3增大refresh-interval)
        - [4、给pagecache预留memory](#4给pagecache预留memory)
        - [5、使用更好的硬件](#5使用更好的硬件)
        - [6、使用自动分配的id](#6使用自动分配的id)
        - [7、段合并](#7段合并)
        - [8、离线批量导入时关闭refresh和replica](#8离线批量导入时关闭refresh和replica)
        - [9、index buffer size](#9index-buffer-size)
    - [2、优化检索性能](#2优化检索性能)
- [08、Translog(预写日志) WAL(Write Ahead Log)](#08translog预写日志-walwrite-ahead-log)
- [09、分布式的三个C((共识(consensus)、并发(concurrency)和一致(consistency))的问题](#09分布式的三个c共识consensus并发concurrency和一致consistency的问题)
    - [1、共识——裂脑问题及法定票数的重要性](#1共识裂脑问题及法定票数的重要性)
        - [1、ES集群构成](#1es集群构成)
        - [2、节点发现](#2节点发现)
        - [3、Master选举](#3master选举)
            - [1 master选举谁发起，什么时候发起？](#1-master选举谁发起什么时候发起)
            - [2 当需要选举master时，选举谁？](#2-当需要选举master时选举谁)
            - [3 什么时候选举成功？](#3-什么时候选举成功)
            - [4 选举怎么保证不脑裂？](#4-选举怎么保证不脑裂)
        - [4、错误检测](#4错误检测)
        - [5、集群扩缩容](#5集群扩缩容)
            - [1 扩容DataNode](#1-扩容datanode)
            - [2 缩容DataNode](#2-缩容datanode)
            - [3 扩容MasterNode](#3-扩容masternode)
            - [4 缩容MasterNode](#4-缩容masternode)
        - [6、怎么使用Zookeeper实现ES的上述功能](#6怎么使用zookeeper实现es的上述功能)
        - [7、Master如何管理集群](#7master如何管理集群)
    - [2、并发（基于version的乐观锁）](#2并发基于version的乐观锁)
    - [3、一致——确保读写一致](#3一致确保读写一致)
- [10、近实时搜索](#10近实时搜索)
- [11、数据存储目录data](#11数据存储目录data)
    - [0、_state](#0_state)
    - [1、indices数据存储目录](#1indices数据存储目录)
    - [2、一个index对应的目录结构](#2一个index对应的目录结构)
    - [3、一个shard对应的目录结构](#3一个shard对应的目录结构)
    - [4、测试数据示例](#4测试数据示例)
- [12、Lucene](#12lucene)
    - [1、lucene基本概念](#1lucene基本概念)
        - [1、索引类型](#1索引类型)
    - [2、Lucene的段segment](#2lucene的段segment)
    - [3、segment文件内容](#3segment文件内容)
        - [1、lucene数据元信息文件（文件名为：segments_xxx）（commit point）](#1lucene数据元信息文件文件名为segments_xxxcommit-point)
        - [2、segment的元信息文件（文件后缀：.si）](#2segment的元信息文件文件后缀si)
        - [3、fields信息文件（文件后缀：.fnm）](#3fields信息文件文件后缀fnm)
        - [4、数据存储文件（文件后缀：.fdx, .fdt）](#4数据存储文件文件后缀fdx-fdt)
        - [5、倒排索引文件（索引后缀：.tip,.tim）](#5倒排索引文件索引后缀tiptim)
        - [6、倒排链文件（文件后缀：.doc, .pos, .pay）](#6倒排链文件文件后缀doc-pos-pay)
        - [7、列存文件（docvalues）（文件后缀：.dvm, .dvd）](#7列存文件docvalues文件后缀dvm-dvd)
    - [4、搜索与聚合在数据存储结构对比](#4搜索与聚合在数据存储结构对比)
        - [1、搜索采用的是倒排索引（也称反向索引）](#1搜索采用的是倒排索引也称反向索引)
        - [2、聚合分析（列存储）](#2聚合分析列存储)
    - [5、Lucene 查询原理](#5lucene-查询原理)
        - [1、Lucene 查询过程](#1lucene-查询过程)
        - [2、fst](#2fst)
        - [3、SkipList](#3skiplist)
        - [4、倒排合并](#4倒排合并)
        - [5、BKDTree](#5bkdtree)
        - [6、如何实现返回结果进行排序聚合](#6如何实现返回结果进行排序聚合)
        - [7、Lucene的代码目录结构](#7lucene的代码目录结构)
        - [8、查询性能测试](#8查询性能测试)
- [95、分布式系统](#95分布式系统)
    - [1、基于本地文件系统的分布式系统](#1基于本地文件系统的分布式系统)
    - [2、基于分布式文件系统的分布式系统（共享存储，比如hdfs）](#2基于分布式文件系统的分布式系统共享存储比如hdfs)
- [96、mapping映射](#96mapping映射)
    - [1、数据类型](#1数据类型)
    - [2、示例](#2示例)
- [97、列存储](#97列存储)
- [98、lucene字典实现原理FST（Finite State Transducers一种有限状态转移机）---term dictonary](#98lucene字典实现原理fstfinite-state-transducers一种有限状态转移机---term-dictonary)
    - [1、lucene字典](#1lucene字典)
    - [2、常用字典数据结构](#2常用字典数据结构)
    - [3、FST原理简析](#3fst原理简析)
- [99、使用案例](#99使用案例)
- [参考](#参考)

<!-- /TOC -->

# 01、基础概念



![](../../pic/2020-05-03-20-45-11.png)


![](../../pic/2020-05-03-21-06-12.png)


> 节点角色的设置

![](../../pic/2020-05-04-14-04-07.png)

备注：elasticsearch 提供几种类型的节点角色设置，需要在 elasticsearch.yml 配置中指定。

Elasticsearch就是一个分布式的文档数据存储系统。ES是分布式的。文档数据[json格式]。存储系统。并且可以基于Lucene的全文检索对存储的海量数据提供近实时的搜索和分析能力。

- 1、分布式：指的是数据分片存储；

- 2、文档数据：es可以存储和操作**json**文档类型的数据，而且这也是es的核心数据结构；

- 3、存储系统：es可以对json文档类型的数据进行存储，查询，创建，更新，删除，等等操作。其实已经起到了一个什么样的效果呢？其实ES满足了这些功能，就可以说已经是一个NoSQL的存储系统了。围绕着document在操作，其实就是把es当成了一个NoSQL存储引擎，一个可以存储文档类型数据的存储系统，在操作里面的document。

- 4、Near Realtime（NRT）：近实时，两个意思，从写入数据到数据可以被搜索到有一个小延迟（大概1秒）；基于es执行搜索和分析可以达到秒级。



简易的将Elasticsearch和关系型数据术语对照表:

![](../../pic/es1.png)


> Elasticsearch 与 Solr 的比较总结

- Solr 利用 Zookeeper 进行分布式管理，而 Elasticsearch 自身带有分布式协调管理功能;
- Solr 支持更多格式的数据，而 Elasticsearch 仅支持json文件格式；
- Solr 官方提供的功能更多，而 Elasticsearch 本身更注重于核心功能，高级功能多有第三方插件提供；
- Solr 在传统的搜索应用中表现好于 Elasticsearch，但在处理实时搜索应用时效率明显低于 Elasticsearch。
- Solr 是传统搜索应用的有力解决方案，但 Elasticsearch 更适用于新兴的实时搜索应用。


## 1、ES和Lucene

![](../../pic/2020-05-03-16-19-03.png)

一些基本概念:

- Cluster          包含多个Node的集群
- Node             集群服务单元
- Index             一个ES索引包含一个或多个物理分片shard，它只是这些分片的逻辑命名空间
- Type              一个index的不同分类，6.x后只能配置一个type，以后将移除
- mapping 是映射关系，定义了 index 中的 type,properties 中定义了字段名、字段类型的信息，mapping 不能修改字段类型；
- Document    最基础的可被索引的数据单元，如一个JSON串
- id 是文档的标识。

- Analyzer 对文档的内容进行分词处理；
- Term 是搜索的基本单位；

- Shards          一个分片是一个底层的工作单元，它仅保存全部数据中的一部分，它是一个Lucence实例 (一个lucene索引最大包含2,147,483,519 (= Integer.MAX_VALUE - 128)个文档数量)，完整的建立索引和处理请求的能力

- Replicas       分片备份，用于保障数据安全与分担检索压力。在索引创建之后，你可以在任何时候动态地改变副本的数量，但是你事后不能改变分片的数量。（两点好处：容灾和提供吞吐量）

- segment 是 Lucene 中的倒排索引，是通过词典（Term Dictionary）到文档列表（Postings List）的映射关系，设计的时候考虑到空间 size 的问题，为词典做了一层前缀索引（Term Index）,Lucene 4.0 以后采用的数据结构是 FST(Finite State Transducer)；


- DSL：ES通过Query DSL (基于json的查询语言)来查询数据，在ES内部，每次查询分成2个步骤:1、分散是指查询所有相关的分片；2、聚合是指把所有分片上的查询结果合并，排序，处理然后在返回给客户端。





ES依赖一个重要的组件Lucene，关于数据结构的优化通常来说是对Lucene的优化，它是集群的一个存储于检索工作单元，结构如下图：

![](../../pic/2020-05-03-16-20-31.png)

在Lucene中，分为索引(录入)与检索(查询)两部分，索引部分包含 分词器、过滤器、字符映射器 等，检索部分包含 查询解析器 等。

一个Lucene索引包含多个segments，一个segment包含多个文档，每个文档包含多个字段，每个字段经过分词后形成一个或多个term。



Elasticsearch 中的索引是组织数据的逻辑空间 (就好比数据库)。1 个 Elasticsearch 的索引有 1 个或者多个分片 (默认是 5 个)。分片对应实际存储数据的 Lucene 的索引，分片自身就是一个搜索引擎。每个分片有 0 或者多个副本 (默认是 1 个)。Elasticsearch 的索引还包含"type"(就像数据库中的表)，用于逻辑上隔离索引中的数据。在 Elasticsearch 的索引中，给定一个 type，它的所有文档会拥有相同的属性 (就像表的 schema)。

![](../../pic/2020-05-02-18-40-42.png)

图 a 展示了一个包含 3 个分片的 Elasticsearch 索引，每个分片拥有 1 个副本。这些分片组成了一个 Elasticsearch 索引，每个分片自身是一个 Lucene 索引。图 b 展示了 Elasticsearch 索引、分片、Lucene 索引和文档之间的逻辑关系。


> 关于ES索引与检索分片

ES中一个索引由一个或多个lucene索引构成，一个lucene索引由一个或多个segment构成，其中segment是最小的检索域。

数据具体被存储到哪个分片上： shard = hash(routing) % number_of_primary_shards

默认情况下 routing参数是文档ID (murmurhash3),可通过 URL中的 _routing 参数指定数据分布在同一个分片中，index和search的时候都需要一致才能找到数据，如果能明确根据_routing进行数据分区，则可减少分片的检索工作，以提高性能。


> 如何将数据写到同一个分片

构建索引的时候指定routing参数即可

```
curl -iXPOST 'localhost:9200/bank/account/1000?routing=Dillard&pretty'  -H 'Content-Type: application/json'  -d'
{"firstname":"Dillard"}
'

```


## 2、shard相关知识点

- 增减节点时，shard会自动在nodes中负载均衡
- primary shard和replica shard，每个document肯定只存在于某一个primary shard以及其对应的replica shard中，不可能存在于多个primary shard
- replica shard是primary shard的副本，负责容错，以及承担读请求负载
- primary shard的数量在创建索引的时候就固定了，replica shard的数量可以随时修改；primary shard的默认数量是5，replica默认是1，即一个index默认有10个shard，5个primary shard，5个replica shard
- primary shard不能和自己的replica shard放在同一个节点上（否则节点宕机，primary shard和副本都丢失，起不到容错的作用），但是可以和其他primary shard的replica shard放在同一个节点上。





# 04、倒排索引

Elasticsearch 使用了 Apache Lucene ，后者是 Doug Cutting( Apache Hadoop 之父) 使用 Java 开发的全文检索工具库，其内部使用的是被称为倒排索引的数据结构，其设计是为全文检索结果的低延迟提供服务。文档是 Elasticsearch 的数据单位，对文档中的词项进行分词，并创建去重词项的有序列表，将词项与其在文档中出现的位置列表关联，便形成了倒排索引。

这和一本书后面的索引非常类似，即书中包含的词汇与其出现的页码列表关联。当我们说文档被索引了，我们指的是倒排索引。我们来看下如下 2 个文档是如何被倒排索引的：

文档 1(Doc 1): Insight Data Engineering Fellows Program

文档 2(Doc 2): Insight Data Science Fellows Program

![](../../pic/2020-05-02-18-43-41.png)

如果我们想找包含词项"insight"的文档，我们可以扫描这个 (单词有序的) 倒排索引，找到"insight"并返回包含改词的文档 ID，示例中是 Doc 1 和 Doc 2。



[Elasticsearch分别为每个field都建立了一个倒排索引，类似传统数据库的表字段全部建立了索引]

倒排索引（英语：Inverted index），也常被称为反向索引、置入档案或反向档案，是一种索引方法，被用来存储在全文搜索下[某个单词]在一个文档或者一组文档中的存储位置的映射。它是文档检索系统中最常用的数据结构。



实例：

![倒排索引1](../../pic/倒排索引1.png)

![倒排索引2](../../pic/倒排索引2.png)

假设有这么几条数据:

| ID | Name | Age | Sex |
| --- | --- | --- | --- | 
| 1 | Kate         | 24 | Female |
| 2 | John         | 24 | Male |
| 3 | Bill         | 29 | Male |

ID是Elasticsearch自建的文档id，那么Elasticsearch建立的索引如下:

Name:

| Term | Posting List |
| --- |:----:|
| Kate | 1 |
| John | 2 |
| Bill | 3 |

Age:

| Term | Posting List |
| --- |:----:|
| 24 | [1,2] |
| 29 | 3 |

Sex:

| Term | Posting List |
| --- |:----:|
| Female | 1 |
| Male | [2,3] |

## 1、Posting List

Elasticsearch分别为每个field都建立了一个倒排索引，Kate, John, 24, Female这些叫term，而[1,2]就是Posting List。Posting list就是一个int的数组，存储了所有符合某个term的文档id。通过posting list这种索引方式似乎可以很快进行查找，比如要找age=24的同学，爱回答问题的小明马上就举手回答：我知道，id是1，2的同学。但是，如果这里有上千万的记录呢？如果是想通过name来查找呢？

## 2、Term Dictionary

Elasticsearch为了能快速找到某个term，将所有的term排个序，二分法查找term，logN的查找效率，就像通过字典查找一样，这就是Term Dictionary。现在再看起来，似乎和传统数据库通过B-Tree的方式类似啊，为什么说比B-Tree的查询快呢？

## 3、Term Index

B-Tree通过减少磁盘寻道次数来提高查询性能，Elasticsearch也是采用同样的思路，直接通过内存查找term，不读磁盘，但是如果term太多，term dictionary也会很大，放内存不现实，于是有了Term Index，就像字典里的索引页一样，A开头的有哪些term，分别在哪页，可以理解term index是一颗树：

![倒排索引3](../../pic/倒排索引3.png)

这棵树不会包含所有的term，它包含的是term的一些前缀。通过term index可以快速地定位到term dictionary的某个offset，然后从这个位置再往后顺序查找。

![倒排索引4](../../pic/倒排索引4.png)

所以term index不需要存下所有的term，而仅仅是他们的一些前缀与Term Dictionary的block之间的映射关系，再结合FST(Finite State Transducers)的压缩技术，可以使term index缓存到内存中。从term index查到对应的term dictionary的block位置之后，再去磁盘上找term，大大减少了磁盘随机读的次数。



# 05、数据持久化模型

![持久化模型](../../pic/2020-05-02-18-35-58.png)

![](../../pic/2020-05-03-16-40-54.png)

## 1、index

Elasticsearch 的 index 类似于关系型数据库中的库，index 是存储数据的单元总称。但实际上，这个只是程序角度，在 es 的内部实现角度，每个index相当于一个命名空间，这个空间指向一个或者多个shards 。

为什么用 index，为了更好地表达，数据存储与搜索的意思。

## 2、inverted index

Elasticsearch 是基于 Lucene 的，而 Lucene 是基于 inverted index 的，inverted index可以更好地服务于搜索。 inverted index(倒排索引)可以获取数据中唯一的 terms 或 token，然后记录哪些文档包含这些 terms 和 token。


## 3、shard

一个 shard (分片) 是 Lucene 的一个实例，它本身具备一个功能齐全的搜索引擎。 一个 index 可以由一个 shard 组成，但大部分情况下都是由多个 shard 组成，这样就可以允许 index 继续增长和分割到不同的机器上。

primary shard (主分片)是主要文档的入口，replica shard (副本分片)是主分片的复制集，并提供主分片所在的节点故障时的故障转移，同时也会增加读写吞吐量。

## 4、segment

每个 shard 包含多个 segment(段)，每个段就是一个 inverted index(倒排索引)，在分片中，每次搜索将会依次搜索每个段，然后将其结果合并到该分片的最终结果中，然后返回。

每次写入新文档时， Elasticsearch 会搜集这些新文档存储于内存中（为了安全，放在事务日志中），然后每隔 1s ，将一个新的小段写入磁盘，并 refreshes ，使其可以被搜索到。

这使得新段中的数据对搜索可见，但该段尚未与磁盘进行同步，因为数据依然可能会丢失。

然后，再每隔一段时间，Elasticsearch 将会执行flush，这意味着会执行段与磁盘同步的操作（commit），接着是清除事务日志，事物日志已经被写入到磁盘了，不再需要了。

分段越多，每次搜索所需要的时间越长。因此 Elasticsearch 会在后台执行合并操作，将大量相似的尺寸的小段合并到一个更大的段中。写入到大段后，旧段会被删掉。当有大量的小段时，这个过程会反复执行。

segment 是不可分的最小颗粒。当更新文档时，它实际上只会将旧文档标记为已删除，并为新文档创建新的索引，上述的合并过程还会消除段中这些标记删除的文档。


# 06、数据持久化详情-增删改查

> Lucence索引原理

- 新接到的数据写入新的索引文件里
- 动态更新索引时候，不修改已经生成的倒排索引，而是新生成一个段(segment)
- 每个段都是一个倒排索引，然后另外使用一个commit文件记录索引内所有的segment,而生成segment的数据来源则放在内存的buffer中

持久化主要有四个步骤，write->refresh->flush->merge

- 1、写入in-memory buffer和事务日志translog
- 2、定期refresh到段文件segment中 可以被检索到
- 3、定期flush segement落盘 清除translog
- 4、定期合并segment 优化流程


另外一种角度看，es 的写入流程（主分片节点）主要有下面的几步：

- 1、根据文档 id 获取文档版本信息，判断进行 add 或 update 操作；
- 2、写 lucene：这里只写内存，会定期进行成组提交到磁盘生成新分段；
- 3、写 translog：写入文件

除了上面的直接流程，还有三个相关的异步流程

- 4、定期进行 flush，对 lucene 进行 commit

- 5、定期对 translog 进行滚动（生成新文件），更新 check point 文件

- 6、定期执行 merge 操作，合并 lucene 分段，这是一个比较消耗资源的操作，但默认情况下都是配置了一个线程。


备注：这里写入一个document需要根据文档id判断之前是否存在，会影响写入的性能。

![](../../pic/2020-05-03-19-30-00.png)


## 1、创建 create

当我们发送索引一个新文档的请求到协调节点后，将发生如下一组操作：

1、Elasticsearch 集群中的每个节点都包含了该集群上分片的元数据信息。协调节点 (默认) 使用文档 ID 参与计算，以便为路由提供合适的分片。Elasticsearch 使用 MurMurHash3 函数对文档 ID 进行哈希，其结果再对分片数量取模，得到的结果即是索引文档的分片。

shard = hash(document_id) % (num_of_primary_shards)

2、当分片所在的节点接收到来自协调节点的请求后，会将该请求写入 translog并将文档加入内存缓冲。如果请求在主分片上成功处理，该请求会并行发送到该分片的副本上。当translog 被同步( fsync ) 到全部的主分片及其副本上后，客户端才会收到确认通知。

3、内存缓冲会被周期性刷新 (默认是 1 秒)，内容将被写到文件系统缓存的一个新段segment上。虽然这个段并没有被同步 (fsync)，但它是开放的，内容可以被搜索到。

4、每 30 分钟或者当 translog 很大的时候，translog 会被清空，文件系统缓存会被同步。这个过程在 Elasticsearch 中称为冲洗 (flush)。在冲洗过程中，内存中的缓冲将被清除，内容被写入一个新段。段的 fsync 将创建一个新的提交点，并将内容刷新到磁盘。旧的 translog 将被删除并开始一个新的 translog。


![](../../pic/2020-05-03-16-33-16.png)



> 细分步骤

### 1、write

es每新增一条数据记录时，都会把数据双写到translog和in-memory buffer内存缓冲区中

![](../../pic/2020-05-03-17-00-15.png)

这时候还不能会被检索到，而数据必须被refresh到segment后才能被检索到


### 2、refresh

默认情况下es每隔1s执行一次refresh,太耗性能，可以通过index.refresh_interval来修改这个刷新时间间隔。

整个refresh具体做了如下事情

- 1、所有在内存缓冲区的文档被写入到一个新的segment中，但是没有调用fsync，因此数据有可能丢失,此时segment首先被写到内核的文件系统中缓存
- 2、segment被打开是的里面的文档能够被见检索到
- 3、清空内存缓冲区in-memory buffer，清空后如下图

![](../../pic/2020-05-03-17-02-16.png)



### 3、flush

随着translog文件越来越大时要考虑把内存中的数据刷新到磁盘中，这个过程叫flush

- 1、把所有在内存缓冲区中的文档写入到一个新的segment中
- 2、清空内存缓冲区
- 3、往磁盘里写入commit point信息
- 4、文件系统的page cache(segments) fsync到磁盘
- 5、删除旧的translog文件，因此此时内存中的segments已经写入到磁盘中,就不需要translog来保障数据安全了，flush后效果如下

![](../../pic/2020-05-03-17-04-55.png)


备注：说明执行flush后内存中的数据全部持久化到磁盘中了。

> flush和fsync的区别

flush是把内存中的数据(包括translog和segments)都刷到磁盘,而fsync只是把translog刷新的磁盘(确保数据不丢失)。


### 4、segment合并

通过每隔一秒的自动刷新机制会创建一个新的segment，用不了多久就会有很多的segment。segment会消耗系统的文件句柄，内存，CPU时钟。最重要的是，每一次请求都会依次检查所有的segment。segment越多，检索就会越慢。

ES通过在后台merge这些segment的方式解决这个问题。小的segment merge到大的

这个过程也是那些被”删除”的文档真正被清除出文件系统的过程，因为被标记为删除的文档不会被拷贝到大的segment中。

![](../../pic/2020-05-03-17-08-29.png)

- 1、当在建立索引过程中，refresh进程会创建新的segments然后打开他们以供索引。
- 2、merge进程会选择一些小的segments然后merge到一个大的segment中。这个过程不会打断检索和创建索引。一旦merge完成，旧的segments将被删除。

![](../../pic/2020-05-03-17-09-24.png)

- 新的segment被flush到磁盘
- 一个新的提交点被写入，包括新的segment，排除旧的小的segments
- 新的segment打开以供索引
- 旧的segments被删除


### 5、Translog

Lucence基于节点宕机的考虑，每次写入都会落盘Translog,类似db binlog,不同的是db binlog 通过expire_logs_days=7 设定过期时间以天为单位 默认7天。而translog 是每次flush后会清除 可以通过几个维度的设定清除策略

```
index.translog.flush_threshold_ops,执行多少次操作后执行一次flush，默认无限制
index.translog.flush_threshold_size，translog的大小超过这个参数后flush，默认512mb
index.translog.flush_threshold_period,多长时间强制flush一次,默认30m
index.translog.interval,es多久去检测一次translog是否满足flush条件
```

translog日志提供了一个所有还未被flush到磁盘的操作的持久化记录。当ES启动的时候，它会使用最新的commit point从磁盘恢复所有已有的segments，然后将重现所有在translog里面的操作来添加更新，这些更新发生在最新的一次commit的记录之后还未被fsync。

translog日志也可以用来提供实时的CRUD。当你试图通过文档ID来读取、更新、删除一个文档时，它会首先检查translog日志看看有没有最新的更新，然后再从响应的segment中获得文档。这意味着它每次都会对最新版本的文档做操作，并且是实时的。

理论上设定好这个过期策略，在flush之前把translog拿到后做双机房同步或者进一步的消息通知处理，还可以有很大作为，可行性还有待尝试。

[问题]：es是否会丢失数据？丢失多久的数据？

文件被 fsync 到磁盘前，被写入的文件在重启之后就会丢失。默认 translog 是每 5 秒被 fsync 刷新到硬盘，或者在每次写请求完成之后执行。在 2.0 版本以后，为了保证不丢数据，每次 index、bulk、delete、update 完成的时候，一定触发刷新 translog 到磁盘上，才给请求返回 200。这个改变在提高数据安全性的同时当然也降低了一点性能。设置如下参数：

```
"index.translog.durability": "async"
"index.translog.sync_interval": "5s"

```


### 写操作

主分片+至少一个副分片全部写成功后才返回成功

![](../../pic/2020-05-03-17-18-26.png)


具体流程：

- 1、客户端向node1发起写入文档请求
- 2、Node1根据文档ID(_id字段)计算出该文档属于分片shard0,然后将请求路由到Node3 的主分片P0上。路由公式 shard = hash(routing) % number_of_primary_shards

- 3、Node3在p0上执行了写入请求后，如果成功则将请求并行的路由至Node1 Node2它的副本分片R0上，且都成功后Node1再报告至client

```
wait_for_active_shards 来配置副本分配同步策略
- 设置为1 表示仅写完主分片就返回
- 设置为all 表示等所有副本分片都写完才能返回
- 设置为1-number_of_replicas+1之间的数值，比如2个副本分片，有1个写成功即可返回

timeout 控制集群异常副本同步分片不可用时候的等待时间
```

我们在发送任何一个增删改操作的时候，比如说put /index/type/id，都可以带上一个consistency参数，指明我们想要的写一致性是什么？

put /index/type/id?consistency=quorum

- one：要求我们这个写操作，只要有一个primary shard是active活跃可用的，就可以执行
- all：要求我们这个写操作，必须所有的primary shard和replica shard都是活跃的，才可以执行这个写操作
- quorum：默认的值，要求所有的shard中，必须是大部分的shard都是活跃的，可用的，才可以执行这个写操作

quorum机制，写之前必须确保大多数shard都可用，int( (primary + number_of_replicas) / 2 ) + 1，当number_of_replicas>1时才生效

如果节点数少于quorum数量，可能导致quorum不齐全，进而导致无法执行任何写操作

quorum不齐全时，wait，默认1分钟，timeout，100，30s


> 总结：其实是根据文档id取模shard数来判断该document该存到那个shard上，然后由协调节点把请求转发到存储该primary shard的节点上，在该节点进行存储后，同步到该主分片存储的其他副本节点，然后返回给客户端该写请求成功。而且在同步备份shard数量的时提供了不同等级的安全级别，默认只要写入大部分replica分片即可保证数据的安全性。**简单来说写只能写主分片shard，然后同步replica副本分片**




## 2、更新 update 和删除 delete

删除和更新也都是写操作。但是 Elasticsearch 中的文档是不可变的，因此不能被删除或者改动以展示其变更。那么，该如何删除和更新文档呢？

磁盘上的每个段都有一个相应的.del文件。当删除请求发送后，文档并没有真的被删除，而是在.del文件中被标记为删除。该文档依然能匹配查询，但是会在结果中被过滤掉。当段合并 (我们将在本系列接下来的文章中讲到) 时，在.del文件中被标记为删除的文档将不会被写入新段。

接下来我们看更新是如何工作的。在新的文档被创建时，Elasticsearch 会为该文档指定一个版本号。当执行更新时，旧版本的文档在.del文件中被标记为删除，新版本的文档被索引到一个新段。旧版本的文档依然能匹配查询，但是会在结果中被过滤掉。

文档被索引或者更新后，我们就可以执行查询操作了。让我们看看在 Elasticsearch 中是如何处理查询请求的。




由于segments是不变的，所以文档不能从旧的segments中删除，也不能在旧的segments中更新来映射一个新的文档版本。取之的是，每一个提交点都会包含一个.del文件，列举了哪一个segment的哪一个文档已经被删除了。 当一个文档被”删除”了，它仅仅是在.del文件里被标记了一下。被”删除”的文档依旧可以被索引到，但是它将会在最终结果返回时被移除掉。

文档的更新同理：当文档更新时，旧版本的文档将会被标记为删除，新版本的文档在新的segment中建立索引。也许新旧版本的文档都会本检索到，但是旧版本的文档会在最终结果返回时被移除。



### 更新操作

更新操作其实就是先读然后写

![](../../pic/2020-05-03-17-24-22.png)

更新流程：

- 1、客户端将更新请求发给Node1
- 2、Node1根据文档ID(_id字段)计算出该文档属于分片shard0,而其主分片在Node上，于是将请求路由到Node3
- 3、Node3从p0读取文档，改变source字段的json内容，然后将修改后的数据在P0重新做索引。如果此时该文档被其他进程修改，那么将重新执行3步骤，这个过程如果超过retryon_confilct设置的重试次数，就放弃。
- 4、如果Node3成功更新了文档，它将并行的将新版本的文档同步到Node1 Node2的副本分片上重新建立索引，一旦所有的副本报告成功，Node3向被请求的Node1节点返回成功，然后Node1向client返回成功






## 3、读read

读操作包含 2 部分内容：

- 查询阶段
- 提取阶段

我们来看下每个阶段是如何工作的。

### 1、查询阶段

在这个阶段，协调节点会将查询请求路由到索引的全部分片 (主分片或者其副本) 上。每个分片独立执行查询，并为查询结果创建一个优先队列，以相关性得分排序 (我们将在本系列的后续文章中讲到)。全部分片都将匹配文档的 ID 及其相关性得分返回给协调节点。协调节点创建一个优先队列并对结果进行全局排序。会有很多文档匹配结果，但是，默认情况下，每个分片只发送前 10 个结果给协调节点，协调节点为全部分片上的这些结果创建优先队列并返回前 10 个作为 hit。

### 2、提取阶段

当协调节点在生成的全局有序的文档列表中，为全部结果排好序后，它将向包含原始文档的分片发起请求。全部分片填充文档信息并将其返回给协调节点。

下图展示了读请求及其数据流。

![](../../pic/2020-05-03-16-35-27.png)



如上所述，查询结果是按相关性排序的。接下来，让我们看看相关性是如何定义的。


### 读操作

一个文档可以在任意主副分片上读取

![](../../pic/2020-05-03-17-22-18.png)

读取流程：

- 1、客户端发起读请求到Node1
- 2、Node1根据文档ID(_id字段)计算出该文档属于分片shard0,在所有节点上都有，这次它根据负载均衡将请求路由至Node2
- 3、Node2将文档返回给Node1，Node1将文档返回给client



> 总结：查询的时候，把请求发送到协调节点，协调节点会计算出你查询的是那个分片，然后采用轮询的算法把你的这个请求发送到主分片或者副本分片**只会选择其中的一个**，返回请求后由协调节点返回给客户端。**简单来说在读取的时候可以读主分片也可以读副本分片来提高并发量，减轻负载**

#### 1、_search结果解析

```java
GET /_search

{
  "took": 6,
  "timed_out": false,
  "_shards": {
    "total": 6,
    "successful": 6,
    "failed": 0
  },
  "hits": {
    "total": 10,
    "max_score": 1,
    "hits": [
      {
        "_index": ".kibana",
        "_type": "config",
        "_id": "5.2.0",
        "_score": 1,
        "_source": {
          "buildNum": 14695
        }
      }
    ]
  }
}
```

took：整个搜索请求花费了多少毫秒

hits.total：本次搜索，返回了几条结果

hits.max_score：本次搜索的所有结果中，最大的相关度分数是多少，每一条document对于search的相关度，越相关，_score分数越大，排位越靠前

hits.hits：默认查询前10条数据，完整数据，_score降序排序

shards：shards fail的条件（primary和replica全部挂掉），不影响其他shard。默认情况下来说，一个搜索请求，会打到一个index的所有primary shard上去，当然了，每个primary shard都可能会有一个或多个replic shard，所以请求也可以到primary shard的其中一个replica shard上去。

timeout：默认无timeout，latency平衡completeness，手动指定timeout，timeout查询执行机制


#### 2、分页查询以及深度分页存在的问题

size，from

GET /_search?size=10

GET /_search?size=10&from=0

GET /_search?size=10&from=20

![](../../pic/es-36-deep-paging图解.png)


> 总结：简单来说就是协调节点需要接受大量的返回结果进行相关性得分，排序等操作，消耗资源。

虽然Elasticsearch中的变更不能立即可见，它还是提供了一个近实时的搜索引擎。如前一篇中所述，提交Lucene的变更到磁盘是一个代价昂贵的操作。为了避免在文档对查询依然有效的时候，提交变更到磁盘，Elasticsearch在内存缓冲和磁盘之间提供了一个文件系统缓存。内存缓存(默认情况下)每1秒刷新一次，在文件系统缓存中使用倒排索引创建一个新的段。这个段是开放的并对搜索有效。

文件系统缓存可以拥有文件句柄，文件可以是开放的、可读的或者是关闭的，但是它存在于内存之中。因为刷新间隔默认是1秒，变更不能立即可见，所以说是近实时的。因为translog是尚未落盘的变更持久化记录，它能有助于CRUD操作方面的近实时性。对于每次请求来说，在查找相关段之前，任何最近的变更都能从translog搜索到，因此客户端可以访问到所有的近实时变更。

你可以在创建/更新/删除操作后显式地刷新索引，使变更立即可见，但我并不推荐你这样做，因为这样会创建出来非常多的小segment而影响搜索性能。对于每次搜索请求来说，给定Elasticsearch索引分片中的全部Lucene段都会被搜索到，但是，对于Elasticsearch来说，获取全部匹配的文档或者很深结果页的文档是有害的。让我们来一起看看为什么是这样。

为什么深层分页在分布式搜索中是有害的？
当我们的一次搜索请求在Elasticsearch中匹配了很多的文档，默认情况下，返回的第一页只包含前10条结果。search API提供了from和size参数，用于指定对于匹配搜索的全部文档，要返回多深的结果。举例来说，如果我们想看到匹配搜索的文档中，排名为50到60之间的文档，可以设置from=50，size=10。当每个分片接收到这个搜索请求后，各自会创建一个容量为from+size的优先队列来存储该分片上的搜索结果，然后将结果返回给协调节点。



如果我们想看到排名为50,000到50,010的结果，那么每个分片要创建一个容量为50,010的优先队列来存储结果，而协调节点要在内存中对数量为shards * 50,010的结果进行排序。这个级别的分页有可能得到结果，也有可以无法实现，这取决于我们的硬件资源，但是这足以说明，我们得非常小心地使用深分页，因为这非常容易使我们的集群崩溃。

一种获取全部匹配结果文档的可行性方案是使用scroll API，它的角色更像关系数据库中的游标。使用scroll API无法进行排序，每个分片只要有匹配搜索的文档，就会持续发送结果给协调节点。

#### 3、相关度评分TF&IDF算法


relevance score算法，简单来说，就是计算出，一个索引中的文本，与搜索文本，他们之间的关联匹配程度

Elasticsearch使用的是 term frequency/inverse document frequency算法，简称为TF/IDF算法

Term frequency：搜索文本中的各个词条在field文本中出现了多少次，出现次数越多，就越相关

搜索请求：hello world

doc1：hello you, and world is very good

doc2：hello, how are you

Inverse document frequency：搜索文本中的各个词条在整个索引的所有文档中出现了多少次，出现的次数越多，就越不相关。

搜索请求：hello world

doc1：hello, today is very good

doc2：hi world, how are you

比如说，在index中有1万条document，hello这个单词在所有的document中，一共出现了1000次；world这个单词在所有的document中，一共出现了100次。doc2更相关

Field-length norm：field长度，field越长，相关度越弱

搜索请求：hello world

doc1：{ "title": "hello article", "content": "babaaba 1万个单词" }

doc2：{ "title": "my article", "content": "blablabala 1万个单词，hi world" }

hello world在整个index中出现的次数是一样多的

doc1更相关，title field更短



## 4、搜索相关性

相关性是由搜索结果中 Elasticsearch 打给每个文档的得分决定的。默认使用的排序算法是 tf/idf(词频 / 逆文档频率)。词频衡量了一个词项在文档中出现的次数 (频率越高 == 相关性越高)，逆文档频率衡量了词项在全部索引中出现的频率，是一个索引中文档总数的百分比 (频率越高 == 相关性越低)。最后的得分是 tf-idf 得分与其他因子比如 (短语查询中的) 词项接近度、(模糊查询中的) 词项相似度等的组合。

## 5、多文档模式（批量操作）

mget 和 bulk API 的模式类似于单文档模式。区别在于协调节点知道每个文档存在于哪个分片中。 它将整个多文档请求分解成 每个分片 的多文档请求，并且将这些请求并行转发到每个参与节点。协调节点一旦收到来自每个节点的应答，就将每个节点的响应收集整理成单个响应，返回给客户端。

### 1、使用 mget 取回多个文档

![](../../pic/2020-05-03-19-51-07.png)

- 1）客户端向 master 发送 mget 请求，该节点作为协调节点；
- 2）节点 1 为每个分片构建多文档获取请求，然后并行转发这些请求到托管在每个所需的主分片或者副本分片的节点上。一旦收到所有答复，节点 1 构建响应并将其返回给客户端。

### 2、使用 bulk 修改多个文档

![](../../pic/2020-05-03-19-51-38.png)

- 1）客户端向 master 发送 bluk 请求，该节点作为协调节点；
- 2）节点 1 为每个节点创建一个批量请求，并将这些请求并行转发到每个包含主分片的节点主机；
- 3）主分片一个接一个按顺序执行每个操作。当每个操作成功时，主分片并行转发新文档（或删除）到副本分片，然后执行下一个操作。一旦所有的副本分片报告所有操作成功，该节点将向协调节点报告成功，协调节点将这些响应收集整理并返回给客户端。



# 07、优化

## 0、Elasticsearch的索引思路

[将磁盘里的东西尽量搬进内存]，减少磁盘随机读取次数(同时也利用磁盘顺序读特性)，结合各种奇技淫巧的压缩算法，用及其苛刻的态度使用内存。所以，对于使用Elasticsearch进行索引时需要注意:

- 1、 不需要索引的字段，一定要明确定义出来，因为默认是自动建索引的
- 2、 同样的道理，对于String类型的字段，不需要analysis的也需要明确定义出来，因为默认也是会analysis的(分词)
- 3、 选择有规律的ID很重要，随机性太大的ID(比如java的UUID)不利于查询

关于最后一点，有多个因素:其中一个(也许不是最重要的)因素: 上面看到的压缩算法，都是对Posting list里的大量ID进行压缩的，那如果ID是顺序的，或者是有公共前缀等具有一定规律性的ID，压缩比会比较高；另外一个因素: 可能是最影响查询性能的，应该是最后通过Posting list里的ID到磁盘中查找Document信息的那步，因为Elasticsearch是分Segment存储的，根据ID这个大范围的Term定位到Segment的效率直接影响了最后查询的性能，如果ID是有规律的，可以快速跳过不包含该ID的Segment，从而减少不必要的磁盘读次数


## 1、写入构建索引优化

### 1、批量写入

看每条数据量的大小，一般都是几百到几千。批量写入比单条写入能够提供更好的性能。BulkProcessor可以通过三个阈值控制批量大小，分别是数据条数，数据size和时间间隔。通常来讲更大的批量会带来更好的性能，但也并不尽然，如果单个批次数据量过大，高并发的情况下会导致集群内存压力增大，官方建议是不要一次发送几十兆的数据，从我测试的结果来看，这个说法过于乐观，建议不超过5M。

### 2、多线程写入

写入线程数一般和机器数相当，可以配多种情况，在测试环境通过Kibana观察性能曲线。单线程批量发送数据无法达到最大效率。使用多线程或者多进程能够更充分地利用集群资源，同时也能减少fsync的开销，因为数据的集中发送可以减少磁盘交互。但要注意响应码TOO_MANY_REQUESTS (429)，在Java Client中会返回EsRejectedExecutionException异常，这说明数据总发送速度已经超过了ES集群的处理能力，这时就需要发送端限流，比如加入时间补偿机制。或者也可以调大ES的bulk queue size，通常并不建议这么做，但某些版本的bulk queue size确实过于小，比如5.1的默认值是50,而5.5是200，所以如果你使用的是5.1版本，则可以在elasticsearch.yml加入以下配置thread_pool.bulk.queue_size: 200

### 3、增大refresh interval

默认的index.refresh_interval是1秒，使用默认配置的index会每隔一秒钟创建一个新的segment，这会给ES带来比较大的merge压力，因此如果对延迟没有特别高的要求，可以适当调大index.refresh_interval，比如10秒


### 4、给pagecache预留memory

官方说了，一半内存给ES，另外一半内存给lucene，也就是文件系统缓存。这很好理解，在缓存中的操作肯定比磁盘IO要快得多。内存分配方面，很多文章已经提到，给系统50%的内存给Lucene做文件缓存，它任务很繁重，所以ES节点的内存需要比较多(比如每个节点能配置64G以上最好）。

### 5、使用更好的硬件

主要是磁盘，ES是重度依赖磁盘IO的软件。最好是SSD，这没什么好说的，就是快。

然后是多块盘做raid0，由于数据是分散在多块盘上，所以如果一块盘坏掉可能会导致所有index不能用，但可以通过replica来规避风险

### 6、使用自动分配的id

如果你在index request中指定了id，那么ES会先去检查这个id是否存在，如果不存在，就新增一条记录，如果存在，则覆盖，检查id的动作是非常耗时的，特别是数据量特别大的情况下。如果index request中不指定id，ES会自动分配一个唯一的id，省去了id检查的过程，写入速度就得到提高。

但如果你希望使用es的id来保证幂等性的话就没别的选择了，只能自己指定id

### 7、段合并

关于段合并，合并在后台定期执行，比较大的segment需要很长时间才能完成，为了减少对其他操作的影响(如检索)，elasticsearch进行阈值限制，默认是20MB/s，可配置的参数："indices.store.throttle.max_bytes_per_sec" : "200mb"  （根据磁盘性能调整）。合并线程数默认是：Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2))，如果是机械磁盘，可以考虑设置为1：index.merge.scheduler.max_thread_count: 1，

### 8、离线批量导入时关闭refresh和replica

如果你需要一次性从外部系统导入数据到ES，你可以通过设置index.refresh_interval为-1来关掉refresh，设置index.number_of_replicas为0来去掉备份，这样可以大大提升数据的导入速度。refresh关掉能够极大的减少segments，也减少了merge，而去掉备份之后数据只需要写入一次。导入完成之后可以再把refresh和replica打开。值得一提的是，增加备份只是简单的数据传输，这个过程很快，但如果是向一个有备份的index写入数据，实际上在primary shard和replica上都需要进行索引，因此开销会大很多

但要注意，虽然这可以提升导入性能，但风险也提高了，因为导入过程中如果有节点挂掉，那么这个节点上的shards就不可用了，但如果是离线导入就还好，大不了重新导入就行

### 9、index buffer size

如果你有重度写入需求，那么最好保证在一个节点上indices.memory.index_buffer_size / shards_count > 512MB（超过这个值索引性能并不会有太明显提高）。indices.memory.index_buffer_size的默认值是10%，也就是说如果机器内存是64G，你把一半32G分配给了ES，那么buffer size为3.2G，能够支撑6.4个疯狂写入的shards






## 2、优化检索性能

1、关闭不需要字段的doc values。

2、尽量使用keyword替代一些long或者int之类，term查询总比range查询好 (参考lucene说明 http://lucene.apache.org/core/7_4_0/core/org/apache/lucene/index/PointValues.html)。

3、关闭不需要查询字段的_source功能，不将此存储仅ES中，以节省磁盘空间。

4、评分消耗资源，如果不需要可使用filter过滤来达到关闭评分功能，score则为0，如果使用constantScoreQuery则score为1。

5、关于分页：

（1）from + size:  

每分片检索结果数最大为 from + size，假设from = 20, size = 20，则每个分片需要获取20 * 20 = 400条数据，多个分片的结果在协调节点合并(假设请求的分配数为5，则结果数最大为 400*5 = 2000条) 再在内存中排序后然后20条给用户。这种机制导致越往后分页获取的代价越高，达到50000条将面临沉重的代价，默认from + size默认如下：index.max_result_window ： 10000

(2)  search_after:  使用前一个分页记录的最后一条来检索下一个分页记录，在我们的案例中，首先使用from+size，检索出结果后再使用search_after，在页面上我们限制了用户只能跳5页，不能跳到最后一页。

(3)  scroll 用于大结果集查询，缺陷是需要维护scroll_id

6、关于排序：我们增加一个long字段，它用于存储时间和ID的组合(通过移位即可)，正排与倒排性能相差不明显。

7、关于CPU消耗，检索时如果需要做排序则需要字段对比，消耗CPU比较大，如果有可能尽量分配16cores以上的CPU，具体看业务压力。

8、关于合并被标记删除的记录，我们设置为0表示在合并的时候一定删除被标记的记录，默认应该是大于10%才删除： "merge.policy.expunge_deletes_allowed": "0"。

```json
{
    "mappings": {
        "data": {
            "dynamic": "false",
            "_source": {
                "includes": ["XXX"]  -- 仅将查询结果所需的数据存储仅_source中
            },
            "properties": {
                "state": {
                    "type": "keyword",   -- 虽然state为int值，但如果不需要做范围查询，尽量使用keyword，因为int需要比keyword增加额外的消耗。
                    "doc_values": false  -- 关闭不需要字段的doc values功能，仅对需要排序，汇聚功能的字段开启。
                },
                "b": {
                    "type": "long"    -- 使用了范围查询字段，则需要用long或者int之类 （构建类似KD-trees结构）
                }
            }
        }
    },
   "settings": {......}
}

```

# 08、Translog(预写日志) WAL(Write Ahead Log)

因为关系数据库的发展，预写日志(WAL)或者事务日志(translog)的概念早已遍及数据库领域。在发生故障的时候，translog能确保数据的完整性。translog的基本原理是，变更必须在数据实际的改变提交到磁盘上之前，被记录下来并提交。

当新的文档被索引或者旧的文档被更新时，Lucene索引将发生变更，这些变更将被提交到磁盘以持久化。如果在每个请求之后都被执行，这是一个很昂贵的操作。因此，这个操作在多个变更持久化到磁盘时被执行一次。Lucene提交的冲洗(flush)操作默认每30分钟执行一次或者当translog变得太大(默认512MB)时执行。在这样的情况下，有可能失去2个Lucene提交之间的所有变更。为了避免这种问题，Elasticsearch采用了translog。所有索引/删除/更新操作被写入到translog，在每个索引/删除/更新操作执行之后（默认情况下是每5秒），translog会被同步以确保变更被持久化。translog被同步到主分片和副本之后，客户端才会收到写请求的确认。

在两次Lucene提交之间发生硬件故障的情况下，可以通过重放translog来恢复自最后一次Lucene提交前的任何丢失的变更，所有的变更将会被索引所接受。

注意：建议在重启Elasticsearch实例之前显式地执行冲洗translog，这样启动会更快，因为要重放的translog被清空。POST /_all/_flush命令可用于冲洗集群中的所有索引。

使用translog的冲洗操作，在文件系统缓存中的段被提交到磁盘，使索引中的变更持久化。


# 09、分布式的三个C((共识(consensus)、并发(concurrency)和一致(consistency))的问题

## 1、共识——裂脑问题及法定票数的重要性

共识是分布式系统的一项基本挑战。它要求系统中的所有进程/节点必须对给定数据的值/状态达成共识。已经有很多共识算法诸如Raft、Paxos等，从数学上的证明了是行得通的。但是，Elasticsearch却实现了自己的共识系统(zen discovery)，Elasticsearch之父Shay Banon在这篇文章中解释了其中的原因。zen discovery模块包含两个部分：

- Ping: 执行节点使用ping来发现彼此

- 单播(Unicast):该模块包含一个主机名列表，用以控制哪些节点需要ping通

Elasticsearch是端对端的系统，其中的所有节点彼此相连，有一个master节点保持活跃，它会更新和控制集群内的状态和操作。建立一个新的Elasticsearch集群要经过一次选举，选举是ping过程的一部分，在所有符合条件的节点中选取一个master，其他节点将加入这个master节点。ping间隔参数ping_interval的默认值是1秒，ping超时参数ping_timeout的默认值是3秒。因为节点要加入，它们会发送一个请求给master节点，加入超时参数join_timeout的默认值是ping_timeout值的20倍。如果master出现问题，那么群集中的其他节点开始重新ping以启动另一次选举。这个ping的过程还可以帮助一个节点在忽然失去master时，通过其他节点发现master。

注意：默认情况下，client节点和data节点不参与这个选举过程。可以在elasticsearch.yml配置文件中，通过设置discovery.zen.master_election.filter_client属性和discovery.zen.master_election.filter_data属性为false来改变这种默认行为。

故障检测的原理是这样的，master节点会ping所有其他节点，以检查它们是否还活着；然后所有节点ping回去，告诉master他们还活着。

如果使用默认的设置，Elasticsearch有可能遭到裂脑问题的困扰。在网络分区的情况下，一个节点可以认为master死了，然后选自己作为master，这就导致了一个集群内出现多个master。这可能会导致数据丢失，也可能无法正确合并数据。可以按照如下公式，根据有资格参加选举的节点数，设置法定票数属性的值，来避免爆裂的发生。

discovery.zen.minimum_master_nodes = int(# of master eligible nodes/2)+1

![](../../pic/2020-05-03-20-28-13.png)

这个属性要求法定票数的节点加入新当选的master节点，来完成并获得新master节点接受的master身份。对于确保群集稳定性和在群集大小变化时动态地更新，这个属性是非常重要的。图a和b演示了在网络分区的情况下，设置或不设置minimum_master_nodes属性时，分别发生的现象。

注意：对于一个生产集群来说，建议使用3个节点专门做master，这3个节点将不服务于任何客户端请求，而且在任何给定时间内总是只有1个活跃。




> ZooKeeper 的 Quorums 机制对脑裂的防止

其实 master 选举问题由来以久。最早的比较完整的阐述称为 Paxos 算法。1990 年的一篇文章就对整个问题和算法进行了很完整的阐述。自文章问世以来，各个不同的工具都试图对这个问题进行一个实现。据我所知大都没有得到很广泛的应用。

ZooKeeper 是对结点管理的一个很强大的实现。 ZooKeeper 的选主过程使用的就是 paxos 算法。（ZooKeeper 的是数据复制使用的是 Zab (ZooKeeper atom broadcast) 算法，因为 paxos 无法保证多个写之间因果顺序，要实现的话只能串行执行，效率太低。）别的且不说，就脑裂这个问题，ZooKeeper 就提供了至少三种方式来认定整个集群是否可用，其中majority quorums 就是类似上面说的用结点个数限制的思想来实现的。即只有集群中超过半数节点投票才能选举出 master。这也是 ZooKeeper 的默认方式。还有两种一种是通过冗余通信，允许集群中采用多种通信方式来防止单一通信方式实效。另一种是通过共享资源，比如能看到共享资源就表示在集群中，反之则不是。


### 1、ES集群构成

首先，一个Elasticsearch集群(下面简称ES集群)是由许多节点(Node)构成的，Node可以有不同的类型，通过以下配置，可以产生四种不同类型的Node：

conf/elasticsearch.yml
```yml
node.master: true/false
node.data: true/false
```
四种不同类型的Node是一个node.master和node.data的true/false的两两组合。当然还有其他类型的Node，比如IngestNode(用于数据预处理等).

当node.master为true时，其表示这个node是一个master的候选节点，可以参与选举，在ES的文档中常被称作master-eligible node，类似于MasterCandidate。ES正常运行时只能有一个master(即leader)，多于1个时会发生脑裂。

当node.data为true时，这个节点作为一个数据节点，会存储分配在该node上的shard的数据并负责这些shard的写入、查询等。

此外，任何一个集群内的node都可以执行任何请求，其会负责将请求转发给对应的node进行处理，所以当node.master和node.data都为false时，这个节点可以作为一个类似proxy的节点，接受请求并进行转发、结果聚合等。

![](../../pic/2020-05-04-16-47-23.png)


上图是一个ES集群的示意图，其中NodeA是当前集群的Master，NodeB和NodeC是Master的候选节点，其中NodeA和NodeB同时也是数据节点(DataNode)，此外，NodeD是一个单纯的数据节点，Node_E是一个proxy节点。每个Node会跟其他所有Node建立连接。

到这里，我们提一个问题，供读者思考：一个ES集群应当配置多少个master-eligible node，当集群的存储或者计算资源不足，需要扩容时，新扩上去的节点应该设置为何种类型？


### 2、节点发现

ZenDiscovery是ES自己实现的一套用于节点发现和选主等功能的模块，没有依赖Zookeeper等工具，官方文档：https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-discovery-hosts-providers.html

简单来说，节点发现依赖以下配置：

```
conf/elasticsearch.yml:
    discovery.zen.ping.unicast.hosts: [1.1.1.1, 1.1.1.2, 1.1.1.3]
```
这个配置可以看作是，在本节点到每个hosts中的节点建立一条边，当整个集群所有的node形成一个联通图时，所有节点都可以知道集群中有哪些节点，不会形成孤岛。

### 3、Master选举

上面提到，集群中可能会有多个master-eligible node，此时就要进行master选举，保证只有一个当选master。如果有多个node当选为master，则集群会出现脑裂，脑裂会破坏数据的一致性，导致集群行为不可控，产生各种非预期的影响。

为了避免产生脑裂，ES采用了常见的分布式系统思路，保证选举出的master被多数派(quorum)的master-eligible node认可，以此来保证只有一个master。这个quorum通过以下配置进行配置：

```
conf/elasticsearch.yml:
    discovery.zen.minimum_master_nodes: 2
```
这个配置对于整个集群非常重要。


#### 1 master选举谁发起，什么时候发起？


master选举当然是由master-eligible节点发起，当一个master-eligible节点发现满足以下条件时发起选举：

- 1、该master-eligible节点的当前状态不是master。
- 2、该master-eligible节点通过ZenDiscovery模块的ping操作询问其已知的集群其他节点，没有任何节点连接到master。
- 3、包括本节点在内，当前已有超过minimum_master_nodes个节点没有连接到master。

总结一句话，即当一个节点发现包括自己在内的多数派的master-eligible节点认为集群没有master时，就可以发起master选举。

#### 2 当需要选举master时，选举谁？

- 1、当clusterStateVersion越大，优先级越高。这是为了保证新Master拥有最新的clusterState(即集群的meta)，避免已经commit的meta变更丢失。因为Master当选后，就会以这个版本的clusterState为基础进行更新。(一个例外是集群全部重启，所有节点都没有meta，需要先选出一个master，然后master再通过持久化的数据进行meta恢复，再进行meta同步)。

- 2、当clusterStateVersion相同时，节点的Id越小，优先级越高。即总是倾向于选择Id小的Node，这个Id是节点第一次启动时生成的一个随机字符串。之所以这么设计，应该是为了让选举结果尽可能稳定，不要出现都想当master而选不出来的情况。

#### 3 什么时候选举成功？

当一个master-eligible node(我们假设为Node_A)发起一次选举时，它会按照上述排序策略选出一个它认为的master。

- 假设Node_A选Node_B当Master：

Node_A会向Node_B发送join请求，那么此时：

(1) 如果Node_B已经成为Master，Node_B就会把Node_A加入到集群中，然后发布最新的cluster_state, 最新的cluster_state就会包含Node_A的信息。相当于一次正常情况的新节点加入。对于Node_A，等新的cluster_state发布到Node_A的时候，Node_A也就完成join了。

(2) 如果Node_B在竞选Master，那么Node_B会把这次join当作一张选票。对于这种情况，Node_A会等待一段时间，看Node_B是否能成为真正的Master，直到超时或者有别的Master选成功。

(3) 如果Node_B认为自己不是Master(现在不是，将来也选不上)，那么Node_B会拒绝这次join。对于这种情况，Node_A会开启下一轮选举。

- 假设Node_A选自己当Master：

此时NodeA会等别的node来join，即等待别的node的选票，当收集到超过半数的选票时，认为自己成为master，然后变更cluster_state中的master node为自己，并向集群发布这一消息。


按照上述流程，我们描述一个简单的场景来帮助大家理解：

假如集群中有3个master-eligible node，分别为Node_A、 Node_B、 Node_C, 选举优先级也分别为Node_A、Node_B、Node_C。三个node都认为当前没有master，于是都各自发起选举，选举结果都为Node_A(因为选举时按照优先级排序，如上文所述)。于是Node_A开始等join(选票)，Node_B、Node_C都向Node_A发送join，当Node_A接收到一次join时，加上它自己的一票，就获得了两票了(超过半数)，于是Node_A成为Master。此时cluster_state(集群状态)中包含两个节点，当Node_A再收到另一个节点的join时，cluster_state包含全部三个节点。

#### 4 选举怎么保证不脑裂？

基本原则还是多数派的策略，如果必须得到多数派的认可才能成为Master，那么显然不可能有两个Master都得到多数派的认可。

上述流程中，master候选人需要等待多数派节点进行join后才能真正成为master，就是为了保证这个master得到了多数派的认可。但是我这里想说的是，上述流程在绝大部份场景下没问题，听上去也非常合理，但是却是有bug的。

因为上述流程并没有限制在选举过程中，一个Node只能投一票，那么什么场景下会投两票呢？比如NodeB投NodeA一票，但是NodeA迟迟不成为Master，NodeB等不及了发起了下一轮选主，这时候发现集群里多了个Node0，Node0优先级比NodeA还高，那NodeB肯定就改投Node0了。假设Node0和NodeA都处在等选票的环节，那显然这时候NodeB其实发挥了两票的作用，而且投给了不同的人。

那么这种问题应该怎么解决呢，比如raft算法中就引入了选举周期(term)的概念，保证了每个选举周期中每个成员只能投一票，如果需要再投就会进入下一个选举周期，term+1。假如最后出现两个节点都认为自己是master，那么肯定有一个term要大于另一个的term，而且因为两个term都收集到了多数派的选票，所以多数节点的term是较大的那个，保证了term小的master不可能commit任何状态变更(commit需要多数派节点先持久化日志成功，由于有term检测，不可能达到多数派持久化条件)。这就保证了集群的状态变更总是一致的。

而ES目前(6.2版本)并没有解决这个问题，构造类似场景的测试case可以看到会选出两个master，两个node都认为自己是master，向全集群发布状态变更，这个发布也是两阶段的，先保证多数派节点“接受”这次变更，然后再要求全部节点commit这次变更。很不幸，目前两个master可能都完成第一个阶段，进入commit阶段，导致节点间状态出现不一致，而在raft中这是不可能的。那么为什么都能完成第一个阶段呢，因为第一个阶段ES只是将新的cluster_state做简单的检查后放入内存队列，如果当前cluster_state的master为空，不会对新的clusterstate中的master做检查，即在接受了NodeA成为master的cluster_state后(还未commit)，还可以继续接受NodeB成为master的cluster_state。这就使NodeA和NodeB都能达到commit条件，发起commit命令，从而将集群状态引向不一致。当然，这种脑裂很快会自动恢复，因为不一致发生后某个master再次发布cluster_state时就会发现无法达到多数派条件，或者是发现它的follower并不构成多数派而自动降级为candidate等。

这里要表达的是，ES的ZenDiscovery模块与成熟的一致性方案相比，在某些特殊场景下存在缺陷，下一篇文章讲ES的meta变更流程时也会分析其他的ES无法满足一致性的场景。




### 4、错误检测

1.MasterFaultDetection与NodesFaultDetection

这里的错误检测可以理解为类似心跳的机制，有两类错误检测，一类是Master定期检测集群内其他的Node，另一类是集群内其他的Node定期检测当前集群的Master。检查的方法就是定期执行ping请求。

如果Master检测到某个Node连不上了，会执行removeNode的操作，将节点从cluste_state中移除，并发布新的cluster_state。当各个模块apply新的cluster_state时，就会执行一些恢复操作，比如选择新的primaryShard或者replica，执行数据复制等。

如果某个Node发现Master连不上了，会清空pending在内存中还未commit的new cluster_state，然后发起rejoin，重新加入集群(如果达到选举条件则触发新master选举)。

2.rejoin

除了上述两种情况，还有一种情况是Master发现自己已经不满足多数派条件(>=minimumMasterNodes)了，需要主动退出master状态(退出master状态并执行rejoin)以避免脑裂的发生，那么master如何发现自己需要rejoin呢？


- 上面提到，当有节点连不上时，会执行removeNode。在执行removeNode时判断剩余的Node是否满足多数派条件，如果不满足，则执行rejoin。

- 在publish新的cluster_state时，分为send阶段和commit阶段，send阶段要求多数派必须成功，然后再进行commit。如果在send阶段没有实现多数派返回成功，那么可能是有了新的master或者是无法连接到多数派个节点等，则master需要执行rejoin。

- 在对其他节点进行定期的ping时，发现有其他节点也是master，此时会比较本节点与另一个master节点的cluster_state的version，谁的version大谁成为master，version小的执行rejoin。


### 5、集群扩缩容

#### 1 扩容DataNode

假设一个ES集群存储或者计算资源不够了，我们需要进行扩容，这里我们只针对DataNode，即配置为：

```
conf/elasticsearch.yml:
    node.master: false
    node.data: true
```

然后需要配置集群名、节点名等其他配置，为了让该节点能够加入集群，我们把discovery.zen.ping.unicast.hosts配置为集群中的master-eligible node。

```conf/elasticsearch.yml:
    cluster.name: es-cluster
    node.name: node_Z
    discovery.zen.ping.unicast.hosts: ["x.x.x.x", "x.x.x.y", "x.x.x.z"]
```

然后启动节点，节点会自动加入到集群中，集群会自动进行rebalance，或者通过reroute api进行手动操作。

#### 2 缩容DataNode

假设一个ES集群使用的机器数太多了，需要缩容，我们怎么安全的操作来保证数据安全，并且不影响可用性呢？

首先，我们选择需要缩容的节点，注意本节只针对DataNode的缩容，MasterNode缩容涉及到更复杂的问题，下面再讲。

然后，我们需要把这个Node上的Shards迁移到其他节点上，方法是先设置allocation规则，禁止分配Shard到要缩容的机器上，然后让集群进行rebalance。

```
PUT _cluster/settings
{
  "transient" : {
    "cluster.routing.allocation.exclude._ip" : "10.0.0.1"
  }
}
```

等这个节点上的数据全部迁移完成后，节点可以安全下线。

#### 3 扩容MasterNode

假如我们想扩容一个MasterNode(master-eligible node)， 那么有个需要考虑的问题是，上面提到为了避免脑裂，ES是采用多数派的策略，需要配置一个quorum数：

```
conf/elasticsearch.yml:
    discovery.zen.minimum_master_nodes: 2
```

假设之前3个master-eligible node，我们可以配置quorum为2，如果扩容到4个master-eligible node，那么quorum就要提高到3。

所以我们应该先把discovery.zen.minimum_master_nodes这个配置改成3，再扩容master，更改这个配置可以通过API的方式：

```
curl -XPUT localhost:9200/_cluster/settings -d '{
    "persistent" : {
        "discovery.zen.minimum_master_nodes" : 3
    }
}'
```

这个API发送给当前集群的master，然后新的值立即生效，然后master会把这个配置持久化到cluster meta中，之后所有节点都会以这个配置为准。

但是这种方式有个问题在于，配置文件中配置的值和cluster meta中的值很可能出现不一致，不一致很容易导致一些奇怪的问题，比如说集群重启后，在恢复cluster meta前就需要进行master选举，此时只可能拿配置中的值，拿不到cluster meta中的值，但是cluster meta恢复后，又需要以cluster meta中的值为准，这中间肯定存在一些正确性相关的边界case。

总之，动master节点以及相关的配置一定要谨慎，master配置错误很有可能导致脑裂甚至数据写坏、数据丢失等场景。

#### 4 缩容MasterNode

缩容MasterNode与扩容跟扩容是相反的流程，我们需要先把节点缩下来，再把quorum数调下来，不再详细描述。

### 6、怎么使用Zookeeper实现ES的上述功能

- 1、节点发现：每个节点的配置文件中配置一下Zookeeper服务器的地址，节点启动后到Zookeeper中某个目录中注册一个临时的znode。当前集群的master监听这个目录的子节点增减的事件，当发现有新节点时，将新节点加入集群。

- 2、master选举：当一个master-eligible node启动时，都尝试到固定位置注册一个名为master的临时znode，如果注册成功，即成为master，如果注册失败则监听这个znode的变化。当master出现故障时，由于是临时znode，会自动删除，这时集群中其他的master-eligible node就会尝试再次注册。使用Zookeeper后其实是把选master变成了抢master。

- 3、错误检测：由于节点的znode和master的znode都是临时znode，如果节点故障，会与Zookeeper断开session，znode自动删除。集群的master只需要监听znode变更事件即可，如果master故障，其他的候选master则会监听到master znode被删除的事件，尝试成为新的master。


- 4、集群扩缩容：扩缩容将不再需要考虑minimum_master_nodes配置的问题，会变得更容易。


> 使用Zookeeper的优劣点

使用Zookeeper的好处是，把一些复杂的分布式一致性问题交给Zookeeper来做，ES本身的逻辑就可以简化很多，正确性也有保证，这也是大部分分布式系统实践过的路子。而ES的这套ZenDiscovery机制经历过很多次bug fix，到目前仍有一些边角的场景存在bug，而且运维也不简单。

那为什么ES不使用Zookeeper呢，大概是官方开发觉得增加Zookeeper依赖后会多依赖一个组件，使集群部署变得更复杂，用户在运维时需要多运维一个Zookeeper。

那么在自主实现这条路上，还有什么别的算法选择吗？当然有的，比如raft。


> 与使用raft相比

raft算法是近几年很火的一个分布式一致性算法，其实现相比paxos简单，在各种分布式系统中也得到了应用。这里不再描述其算法的细节，我们单从master选举算法角度，比较一下raft与ES目前选举算法的异同点：

>> 相同点
- 多数派原则：必须得到超过半数的选票才能成为master。
- 选出的leader一定拥有最新已提交数据：在raft中，数据更新的节点不会给数据旧的节点投选票，而当选需要多数派的选票，则当选人一定有最新已提交数据。在es中，version大的节点排序优先级高，同样用于保证这一点。

>> 不同点

- 正确性论证：raft是一个被论证过正确性的算法，而ES的算法是一个没有经过论证的算法，只能在实践中发现问题，做bug fix，这是我认为最大的不同。

- 是否有选举周期term：raft引入了选举周期的概念，每轮选举term加1，保证了在同一个term下每个参与人只能投1票。ES在选举时没有term的概念，不能保证每轮每个节点只投一票。

- 选举的倾向性：raft中只要一个节点拥有最新的已提交的数据，则有机会选举成为master。在ES中，version相同时会按照NodeId排序，总是NodeId小的人优先级高。


raft从正确性上看肯定是更好的选择，而ES的选举算法经过几次bug fix也越来越像raft。当然，在ES最早开发时还没有raft，而未来ES如果继续沿着这个方向走很可能最终就变成一个raft实现。

raft不仅仅是选举，下一篇介绍meta数据一致性时也会继续比较ES目前的实现与raft的异同。



### 7、Master如何管理集群



## 2、并发（基于version的乐观锁）

Elasticsearch是一个分布式系统，支持并发请求。当创建/更新/删除请求到达主分片时，它也会被平行地发送到分片副本上。但是，这些请求到达的顺序可能是乱序的。在这种情况下，Elasticsearch使用乐观并发控制，来确保文档的较新版本不会被旧版本覆盖。

每个被索引的文档都拥有一个版本号，版本号在每次文档变更时递增并应用到文档中。这些版本号用来确保有序接受变更。为了确保在我们的应用中更新不会导致数据丢失，Elasticsearch的API允许我们指定文件的当前版本号，以使变更被接受。如果在请求中指定的版本号比分片上存在的版本号旧，请求失败，这意味着文档已经被另一个进程更新了。如何处理失败的请求，可以在应用层面来控制。

当我们发送并发请求到Elasticsearch后，接下来面对的问题是——如何保证这些请求的读写一致？现在，还无法清楚回答，Elasticsearch应落在CAP三角形的哪条边上，我不打算在这篇文章里解决这个素来已久的争辩。

![](../../pic/2020-05-03-20-30-29.png)

但是，我们要一起看下如何使用Elasticsearch实现写读一致。

## 3、一致——确保读写一致

对于写操作而言，Elasticsearch支持的一致性级别，与大多数其他的数据库不同，允许预检查，来查看有多少允许写入的可用分片。可选的值有quorum、one和all。默认的设置为quorum，也就是说只有当大多数分片可用时才允许写操作。即使大多数分片可用，还是会因为某种原因发生写入副本失败，在这种情况下，副本被认为故障，分片将在一个不同的节点上重建。

对于读操作而言，新的文档只有在刷新时间间隔之后，才能被搜索到。为了确保搜索请求的返回结果包含文档的最新版本，可设置replication为sync(默认)，这将使操作在主分片和副本碎片都完成后才返回写请求。在这种情况下，搜索请求从任何分片得到的返回结果都包含的是文档的最新版本。即使我们的应用为了更高的索引率而设置了replication=async，我们依然可以为搜索请求设置参数_preference为primary。这样，搜索请求将查询主分片，并确保结果中的文档是最新版本。


# 10、近实时搜索

虽然Elasticsearch中的变更不能立即可见，它还是提供了一个近实时的搜索引擎。如前一篇中所述，提交Lucene的变更到磁盘是一个代价昂贵的操作。为了避免在文档对查询依然有效的时候，提交变更到磁盘，Elasticsearch在内存缓冲和磁盘之间提供了一个文件系统缓存。内存缓存(默认情况下)每1秒刷新一次，在文件系统缓存中使用倒排索引创建一个新的段。这个段是开放的并对搜索有效。

文件系统缓存可以拥有文件句柄，文件可以是开放的、可读的或者是关闭的，但是它存在于内存之中。因为刷新间隔默认是1秒，变更不能立即可见，所以说是近实时的。因为translog是尚未落盘的变更持久化记录，它能有助于CRUD操作方面的近实时性。对于每次请求来说，在查找相关段之前，任何最近的变更都能从translog搜索到，因此客户端可以访问到所有的近实时变更。

你可以在创建/更新/删除操作后显式地刷新索引，使变更立即可见，但我并不推荐你这样做，因为这样会创建出来非常多的小segment而影响搜索性能。对于每次搜索请求来说，给定Elasticsearch索引分片中的全部Lucene段都会被搜索到，但是，对于Elasticsearch来说，获取全部匹配的文档或者很深结果页的文档是有害的。让我们来一起看看为什么是这样。





# 11、数据存储目录data

Elasticsearch配置了多个路径：

```
path.home：运行Elasticsearch进程的用户的主目录。默认为Java系统属性user.dir，它是进程所有者的默认主目录。

path.conf：包含配置文件的目录。这通常通过设置Java系统属性es.config来设置，因为在找到配置文件之前它必然会被解析。

path.plugins：子文件夹为Elasticsearch插件的目录。这里支持Sym-links，当从同一个可执行文件运行多个Elasticsearch实例时，可以使用它来有选择地启用/禁用某个Elasticsearch实例的一组插件。

path.logs：存储生成的日志的位置。如果其中一个卷的磁盘空间不足，则将它放在与数据目录不同的卷上可能是有意义的。

path.data：包含Elasticsearch存储的数据的文件夹的路径。

```




比如：basedir=D:\anzhuangbao\elasticsearch-6.5.3\data

![](../../pic/2020-05-03-22-52-34.png)

basedir\nodes\0目录下的文件：

- _state（文件夹）：为节点状态目录
- indices（文件夹）：为数据存储目录
- node.lock：文件用于确保一次只能从一个数据目录读取/写入


## 0、_state

![](../../pic/2020-05-04-10-14-31.png)

global-35.st文件。 global-前缀表示这是一个全局状态文件，而.st扩展名表示这是一个包含元数据的状态文件。此二进制文件包含有关您的集群的全局元数据，前缀后的数字表示集群元数据版本，遵循跟随您的集群严格增加的版本控制方案。


## 1、indices数据存储目录

![](../../pic/2020-05-03-22-59-01.png)

针对每个index这里会出现一个uuid为名称的文件夹，存放该index的数据。

uuid可以在kibana的索引管理界面查看，也可以用api查看（GET /_cat/indices?v 可以查看所有索引的信息）

通过head插件查看

![](../../pic/2020-05-03-23-04-01.png)

通过http://localhost:9200/_cat/indices?v查看

![](../../pic/2020-05-03-23-04-36.png)

通过kibana查看

![](../../pic/2020-05-03-23-07-15.png)

## 2、一个index对应的目录结构

![](../../pic/2020-05-03-23-08-46.png)

_stat为此索引的状态信息

![](../../pic/2020-05-04-10-17-14.png)

这里的0\1\2\3\4表示各个分片，每个分片对应一个目录。（因为在Windows上只启动一个实例，全部五个分片都在当前节点上）

## 3、一个shard对应的目录结构

![](../../pic/2020-05-03-23-11-57.png)


- _state目录为此分片的状态信息

![](../../pic/2020-05-04-10-18-15.png)

- translog目录为此分片的操作日志

![](../../pic/2020-05-03-23-14-43.png)

- index 索引和数据存储文件（各个后缀的文件含义参考下面的Lucene文件内容）

![](../../pic/2020-05-03-23-15-47.png)


备注：这里的segment_x  应该就是commit point文件；.liv删除文档的标记文件。

![](../../pic/2020-05-04-11-14-51.png)


## 4、测试数据示例

下面我们以真实的数据作为示例，看看lucene中各类型数据的容量占比。

写100w数据，有一个uuid字段，写入的是长度为36位的uuid，字符串总为3600w字节，约为35M。

数据使用一个shard，不带副本，使用默认的压缩算法，写入完成后merge成一个segment方便观察。

使用线上默认的配置，uuid存为不分词的字符串类型。创建如下索引：

```json
PUT test_field
{
  "settings": {
    "index": {
      "number_of_shards": "1",
      "number_of_replicas": "0",
      "refresh_interval": "30s"
    }
  },
  "mappings": {
    "type": {
      "_all": {
        "enabled": false
      }, 
      "properties": {
        "uuid": {
          "type": "string",
          "index": "not_analyzed"
        }
      }
    }
  }
}
```

首先写入100w不同的uuid，使用磁盘容量细节如下：

```
health status index      pri rep docs.count docs.deleted store.size pri.store.size 
green  open   test_field   1   0    1000000            0    122.7mb        122.7mb 

-rw-r--r--  1 weizijun  staff    41M Aug 19 21:23 _8.fdt//Field Index 存储了正排存储数据，写入的原文存储在这
-rw-r--r--  1 weizijun  staff    17K Aug 19 21:23 _8.fdx//正排存储文件的元数据信息
-rw-r--r--  1 weizijun  staff   688B Aug 19 21:23 _8.fnm//保存了fields的相关信息
-rw-r--r--  1 weizijun  staff   494B Aug 19 21:23 _8.si//segment的元数据文件
-rw-r--r--  1 weizijun  staff   265K Aug 19 21:23 _8_Lucene50_0.doc//保存了每个term的doc id列表和term在doc中的词频
-rw-r--r--  1 weizijun  staff    44M Aug 19 21:23 _8_Lucene50_0.tim//Term Dictionary 倒排索引的元数据信息
-rw-r--r--  1 weizijun  staff   340K Aug 19 21:23 _8_Lucene50_0.tip//倒排索引文件，存储了所有的倒排索引数据
-rw-r--r--  1 weizijun  staff    37M Aug 19 21:23 _8_Lucene54_0.dvd//lucene的docvalues文件，即数据的列式存储，用作聚合和排序
-rw-r--r--  1 weizijun  staff   254B Aug 19 21:23 _8_Lucene54_0.dvm
-rw-r--r--  1 weizijun  staff   195B Aug 19 21:23 segments_2//文件记录了lucene包下面的segment文件数量
-rw-r--r--  1 weizijun  staff     0B Aug 19 21:20 write.lock
```


可以看到正排数据、倒排索引数据，列存数据容量占比几乎相同，正排数据和倒排数据还会存储Elasticsearch的唯一id字段，所以容量会比列存多一些。

35M的uuid存入Elasticsearch后，数据膨胀了3倍，达到了122.7mb。Elasticsearch竟然这么消耗资源，不要着急下结论，接下来看另一个测试结果。

![](../../pic/2020-05-03-23-52-54.png)

这回35M的数据Elasticsearch容量只有13.2mb，其中还有主要的占比还是Elasticsearch的唯一id，100w的uuid几乎不占存储容积。

所以在Elasticsearch中建立索引的字段如果基数越大(count distinct)，越占用磁盘空间。









# 12、Lucene


Lucene中最重要的就是它的几种数据结构，这决定了数据是如何被检索的，本文再简单描述一下几种数据结构：

- FST：保存term字典，可以在FST上实现单Term、Term范围、Term前缀和通配符查询等。
- 倒排链：保存了每个term对应的docId的列表，采用skipList的结构保存，用于快速跳跃。
- BKD-Tree：BKD-Tree是一种保存多维空间点的数据结构，用于数值类型(包括空间点)的快速查找。
- DocValues：基于docId的列式存储，由于列式存储的特点，可以有效提升排序聚合的性能。


> 组合条件的结果合并

了解了Lucene的数据结构和基本查询原理，我们知道：

- 对单个词条进行查询，Lucene会读取该词条的倒排链，倒排链中是一个有序的docId列表。
- 对字符串范围/前缀/通配符查询，Lucene会从FST中获取到符合条件的所有Term，然后就可以根据这些Term再查找倒排链，找到符合条件的doc。
- 对数字类型进行范围查找，Lucene会通过BKD-Tree找到符合条件的docId集合，但这个集合中的docId并非有序的。

现在的问题是，如果给一个组合查询条件，Lucene怎么对各个单条件的结果进行组合，得到最终结果。简化的问题就是如何求两个集合的交集和并集。


## 1、lucene基本概念


![](../../pic/2020-05-04-17-29-40.png)



- Index（索引）：类似数据库的表的概念，但是与传统表的概念会有很大的不同。传统关系型数据库或者NoSQL数据库的表，在创建时至少要定义表的Scheme，定义表的主键或列等，会有一些明确定义的约束。而Lucene的Index，则完全没有约束。Lucene的Index可以理解为一个文档收纳箱，你可以往内部塞入新的文档，或者从里面拿出文档，但如果你要修改里面的某个文档，则必须先拿出来修改后再塞回去。这个收纳箱可以塞入各种类型的文档，文档里的内容可以任意定义，Lucene都能对其进行索引。

- segment : lucene内部的数据是由一个个segment组成的，写入lucene的数据并不直接落盘，而是先写在内存中，经过了refresh间隔，lucene才将该时间段写入的全部数据refresh成一个segment，segment多了之后会进行merge成更大的segment。lucene查询时会遍历每个segment完成。由于lucene* 写入的数据是在内存中完成，所以写入效率非常高。但是也存在丢失数据的风险，所以Elasticsearch基于此现象实现了translog，只有在segment数据落盘后，Elasticsearch才会删除对应的translog。Lucene的Segment设计思想，与LSM类似但又有些不同，继承了LSM中数据写入的优点，但是在查询上只能提供近实时而非实时查询。Lucene中的数据写入会先写内存的一个Buffer（类似LSM的MemTable，但是不可读），当Buffer内数据到一定量后会被flush成一个Segment，每个Segment有自己独立的索引，可独立被查询，但数据永远不能被更改。这种模式避免了随机写，数据写入都是Batch和Append，能达到很高的吞吐量。Segment中写入的文档不可被修改，但可被删除，删除的方式也不是在文件内部原地更改，而是会由另外一个文件保存需要被删除的文档的DocID，保证数据文件不可被修改。Index的查询需要对多个Segment进行查询并对结果进行合并，还需要处理被删除的文档，为了对查询进行优化，Lucene会有策略对多个Segment进行合并，这点与LSM对SSTable的Merge类似。

- doc : doc表示lucene中的一条记录。类似数据库内的行或者文档数据库内的文档的概念，一个Index内会包含多个Document。写入Index的Document会被分配一个唯一的ID，即Sequence Number（更多被叫做DocId）。

- field ：field表示记录中的字段概念，一个doc由若干个field组成。Field是Lucene中数据索引的最小定义单位。

- term ：term是lucene中索引的最小单位，某个field对应的内容如果是全文检索类型，会将内容进行分词，分词的结果就是由term组成的。如果是不分词的字段，那么该字段的内容就是一个term。Lucene中索引和搜索的最小单位，一个Field会由一个或多个Term组成，Term是由Field经过Analyzer（分词）产生。Term Dictionary即Term词典，是根据条件查找Term的基本索引。

- 倒排索引（inverted index）: lucene索引的通用叫法，即实现了term到doc list的映射。

- 正排数据：搜索引擎的通用叫法，即原始数据，可以理解为一个doc list。

- docvalues :Elasticsearch中的列式存储的名称，Elasticsearch除了存储原始存储、倒排索引，还存储了一份docvalues，用作分析和排序。


- Sequence Number（也叫DocId）是Lucene中一个很重要的概念，数据库内通过主键来唯一标识一行，而Lucene的Index通过DocId来唯一标识一个Doc。不过有几点要特别注意：1、DocId实际上并不在Index内唯一，而是Segment内唯一，Lucene这么做主要是为了做写入和压缩优化。那既然在Segment内才唯一，又是怎么做到在Index级别来唯一标识一个Doc呢？方案很简单，Segment之间是有顺序的，举个简单的例子，一个Index内有两个Segment，每个Segment内分别有100个Doc，在Segment内DocId都是0-100，转换到Index级的DocId，需要将第二个Segment的DocId范围转换为100-200；2、DocId在Segment内唯一，取值从0开始递增。但不代表DocId取值一定是连续的，如果有Doc被删除，那可能会存在空洞；3、一个文档对应的DocId可能会发生变化，主要是发生在Segment合并时。


Lucene内最核心的倒排索引，本质上就是Term到所有包含该Term的文档的DocId列表的映射。所以Lucene内部在搜索的时候会是一个两阶段的查询，第一阶段是通过给定的Term的条件找到所有Doc的DocId列表，第二阶段是根据DocId查找Doc。Lucene提供基于Term的搜索功能，也提供基于DocId的查询功能。

DocId采用一个从0开始底层的Int32值，是一个比较大的优化，同时体现在数据压缩和查询效率上。例如数据压缩上的Delta策略、ZigZag编码，以及倒排列表上采用的SkipList等。




Lucene 索引文件结构主要的分为：词典、倒排表、正向文件、DocValues等，如下图:

![](../../pic/2020-05-03-17-47-01.png)

Lucene 随机三次磁盘读取比较耗时。其中.fdt文件保存数据值损耗空间大，.tim和.doc则需要SSD存储提高随机读写性能。另外一个比较消耗性能的是打分流程，不需要则可屏蔽。





> 关于DocValues：

倒排索引解决从词快速检索到相应文档ID, 但如果需要对结果进行排序、分组、聚合等操作的时候则需要根据文档ID快速找到对应的值。

通过倒排索引代价缺很高：需迭代索引里的每个词项并收集文档的列里面 token。这很慢而且难以扩展：随着词项和文档的数量增加，执行时间也会增加。

在lucene 4.0版本前通过FieldCache，原理是通过按列逆转倒排表将（field value ->doc）映射变成（doc -> field value）映射，问题为逐步构建时间长并且消耗大量内存，容易造成OOM。

DocValues是一种列存储结构，能快速通过文档ID找到相关需要排序的字段。在ES中，默认开启所有(除了标记需analyzed的字符串字段)字段的doc values，如果不需要对此字段做任何排序等工作，则可关闭以减少资源消耗。


### 1、索引类型

Lucene中支持丰富的字段类型，每种字段类型确定了支持的数据类型以及索引方式，目前支持的字段类型包括LongPoint、TextField、StringField、NumericDocValuesField等。

![](../../pic/2020-05-04-17-46-05.png)

如图是Lucene中对于不同类型Field定义的一个基本关系，所有字段类都会继承自Field这个类，Field包含3个重要属性：name(String)、fieldsData(BytesRef)和type(FieldType)。name即字段的名称，fieldsData即字段值，所有类型的字段的值最终都会转换为二进制字节流来表示。type是字段类型，确定了该字段被索引的方式。

FieldType是一个很重要的类，包含多个重要属性，这些属性的值决定了该字段被索引的方式。

Lucene提供的多种不同类型的Field，本质区别就两个：一是不同类型值到fieldData定义了不同的转换方式；二是定义了FieldType内不同属性不同取值的组合。这种模式下，你也能够通过自定义数据以及组合FieldType内索引参数来达到定制类型的目的。

要理解Lucene能够提供哪些索引方式，只需要理解FieldType内每个属性的具体含义，我们来一个一个看：

- stored: 代表是否需要保存该字段，如果为false，则lucene不会保存这个字段的值，而搜索结果中返回的文档只会包含保存了的字段。

- tokenized: 代表是否做分词，在lucene中只有TextField这一个字段需要做分词。

- termVector: 这篇文章很好的解释了term vector的概念，简单来说，term vector保存了一个文档内所有的term的相关信息，包括Term值、出现次数（frequencies）以及位置（positions）等，是一个per-document inverted index，提供了根据docid来查找该文档内所有term信息的能力。对于长度较小的字段不建议开启term verctor，因为只需要重新做一遍分词即可拿到term信息，而针对长度较长或者分词代价较大的字段，则建议开启term vector。Term vector的用途主要有两个，一是关键词高亮，二是做文档间的相似度匹配（more-like-this）。

- omitNorms: Norms是normalization的缩写，lucene允许每个文档的每个字段都存储一个normalization factor，是和搜索时的相关性计算有关的一个系数。Norms的存储只占一个字节，但是每个文档的每个字段都会独立存储一份，且Norms数据会全部加载到内存。所以若开启了Norms，会消耗额外的存储空间和内存。但若关闭了Norms，则无法做index-time boosting（elasticsearch官方建议使用query-time boosting来替代）以及length normalization。

- indexOptions: Lucene提供倒排索引的5种可选参数（NONE、DOCS、DOCS_AND_FREQS、DOCS_AND_FREQS_AND_POSITIONS、DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS），用于选择该字段是否需要被索引，以及索引哪些内容。

- docValuesType: DocValue是Lucene 4.0引入的一个正向索引（docid到field的一个列存），大大优化了sorting、faceting或aggregation的效率。DocValues是一个强schema的存储结构，开启DocValues的字段必须拥有严格一致的类型，目前Lucene只提供NUMERIC、BINARY、SORTED、SORTED_NUMERIC和SORTED_SET五种类型。

- dimension：Lucene支持多维数据的索引，采取特殊的索引来优化对多维数据的查询，这类数据最典型的应用场景是地理位置索引，一般经纬度数据会采取这个索引方式。


## 2、Lucene的段segment

Lucene索引是由多个段组成，段本身是一个功能齐全的倒排索引。段是不可变的，允许Lucene将新的文档增量地添加到索引中，而不用从头重建索引。对于每一个搜索请求而言，索引中的所有段都会被搜索，并且每个段会消耗CPU的时钟周、文件句柄和内存。这意味着段的数量越多，搜索性能会越低。

为了解决这个问题，Elasticsearch会合并小段到一个较大的段（如下图所示），提交新的合并段到磁盘，并删除那些旧的小段。

![](../../pic/2020-05-03-20-06-44.png)


这会在后台自动执行而不中断索引或者搜索。由于段合并会耗尽资源，影响搜索性能，Elasticsearch会节制合并过程，为搜索提供足够的可用资源。


## 3、segment文件内容

lucene包的文件是由很多segment文件组成的，segments_xxx文件记录了lucene包下面的segment文件数量。每个segment会包含如下的文件。

![](../../pic/2020-05-03-23-33-36.png)

![](../../pic/2020-05-04-10-20-26.png)



- 存储原文_source的文件.fdt  .fdx; 
- 存储倒排索引的文件.tim .tip .doc;
- 用于聚合排序的列存文件.dvd .dvm; 
- 全文检索文件.pos .pay .nvd .nvm等。 
- 加载到内存中的文件有.fdx .tip

其中.tip占用内存最大，而.fdt . tim .dvd文件占用磁盘最大，另外segment较小时文件内容是保存在.cfs文件中，.cfe文件保存Lucene各文件在.cfs文件的位置信息，这是为了减少Lucene打开的文件句柄数。


### 1、lucene数据元信息文件（文件名为：segments_xxx）（commit point）

该文件为lucene数据文件的元信息文件，记录所有segment的元数据信息。

该文件主要记录了目前有多少segment，每个segment有一些基本信息，更新这些信息定位到每个segment的元信息文件。

lucene元信息文件还支持记录userData，Elasticsearch可以在此记录translog的一些相关信息。

![](../../pic/2020-05-03-23-58-53.png)


### 2、segment的元信息文件（文件后缀：.si）

每个segment都有一个.si文件，记录了该segment的元信息。

segment元信息文件中记录了segment的文档数量，segment对应的文件列表等信息。

![](../../pic/2020-05-04-00-00-12.png)


### 3、fields信息文件（文件后缀：.fnm）

该文件存储了fields的基本信息。

fields信息中包括field的数量，field的类型，以及IndexOpetions，包括是否存储、是否索引，是否分词，是否需要列存等等。

### 4、数据存储文件（文件后缀：.fdx, .fdt）

索引文件为.fdx，数据文件为.fdt，数据存储文件功能为根据自动的文档id，得到文档的内容，搜索引擎的术语习惯称之为正排数据，即doc_id -> content，es的_source数据就存在这

索引文件记录了快速定位文档数据的索引信息，数据文件记录了所有文档id的具体内容。

### 5、倒排索引文件（索引后缀：.tip,.tim）

倒排索引也包含索引文件和数据文件，.tip为索引文件，.tim为数据文件，索引文件包含了每个字段的索引元信息，数据文件有具体的索引内容。

5.5.0版本的倒排索引实现为FST tree，FST tree的最大优势就是内存空间占用非常低 ，具体可以参看下这篇文章：http://www.cnblogs.com/bonelee/p/6226185.html

http://examples.mikemccandless.com/fst.py?terms=&cmd=Build+it 为FST图实例，可以根据输入的数据构造出FST图

输入到 FST 中的数据为:

String inputValues[] = {"mop","moth","pop","star","stop","top"};

long outputValues[] = {0,1,2,3,4,5};


### 6、倒排链文件（文件后缀：.doc, .pos, .pay）

.doc保存了每个term的doc id列表和term在doc中的词频

全文索引的字段，会有.pos文件，保存了term在doc中的位置

全文索引的字段，使用了一些像payloads的高级特性才会有.pay文件，保存了term在doc中的一些高级特性


### 7、列存文件（docvalues）（文件后缀：.dvm, .dvd）

索引文件为.dvm，数据文件为.dvd。

lucene实现的docvalues有如下类型：

- 1、NONE 不开启docvalue时的状态
- 2、NUMERIC 单个数值类型的docvalue主要包括（int，long，float，double）
- 3、BINARY 二进制类型值对应不同的codes最大值可能超过32766字节，
- 4、SORTED 有序增量字节存储，仅仅存储不同部分的值和偏移量指针，值必须小于等于32766字节
- 5、SORTED_NUMERIC 存储数值类型的有序数组列表
- 6、SORTED_SET 可以存储多值域的docvalue值，但返回时，仅仅只能返回多值域的第一个docvalue
- 7、对应not_anaylized的string字段，使用的是SORTED_SET类型，number的类型是SORTED_NUMERIC类型

其中SORTED_SET 的 SORTED_SINGLE_VALUED类型包括了两类数据 ： binary + numeric， binary是按ord排序的term的列表，numeric是doc到ord的映射。




## 4、搜索与聚合在数据存储结构对比

### 1、搜索采用的是倒排索引（也称反向索引）

即将文档的所有内容通过分析、过滤、转化等操作抽取关键字，然后建立一个按字符顺序排列的关键字列表。

假设有两篇文章：

文章1的内容是： Tom lives in Guangzhou, I live in Guangzhou too.

文章2的内容是： He once lived in Shanghai.

文章内容经过 Elasticsearch 的分析器处理后，得到如下的关键词：

文章1的关键词是： tom live guangzhou i live guagnzhou

文章2的关键词是： he live shanghai

得到上述文章的关键字之后，可以构造出如下的索引表

![](../../pic/2020-05-04-10-49-28.png)

当然，仅仅知道关键词出现在那些文章还是不够的，我们希望还能够得到文章的关键词出现的次数和出现频率。而 lucene 中采用的索引结构类似为：

![](../../pic/2020-05-04-10-49-58.png)

我们以 live 这样说明该结构。live 在文章1中出现了2次，文章2中出现了一次。它的出现位置为 2、5、2 代表的含义需要结合文章号和出现频率来分析。文章1中出现了2次，那么 2、5 就表示 live 在文章1中出现的两个位置，文章2中出现了一次，剩下的 2 就表示 live 是文章 2 中第 2 个关键字。

上述的关键字是按字符排序排列的，因此 lucene 可以用二元搜索算法快速定位关键词。实现时 lucene 将上面表格的三列分别作为词典文件、频率文件、位置文件保存。此种词典文件不仅保留关键词，还保留了指向频率文件和位置文件的指针。

假设要查询单词 live ，lucene 先对词典二元查找，找到改词后，通过指向频率文件的指针读取所有文章号。因此词典文件本身特别小，因而整个过程是毫秒级的。

相比如普通的顺序匹配，对文章内容进行字符串匹配，lucene 的倒排索引是相当快速的。


### 2、聚合分析（列存储）

聚合分析和搜索还是有很大的不同，典型的应用场景，比如计算文章1中每个关键词出现的次数，反向索引就略显无力。首先需要扫描整个词典文件，才能找到该文档包含的所有关键词，然后在进行聚合统计。在数据量大的情况下，扫描整个方向索引，性能肯定要受不小影响。

Elasticsearch 为聚合计算引入名为 fielddata 的数据结构，即采用我们平时常见正向索引，即文档到关键词的映射。类似于下表：

![](../../pic/2020-05-04-10-54-11.png)


对文章在进行聚合计算时，就只要根据文章 ID 查找就好。因为聚合计算也好，排序也好，通常是针对某些列，实际上 Elasticsearch 为了做聚合分析，生成的是文档到 field 的多个列式索引。在做聚合就分析，fielddata（类似上表） 是保存在内存中，这么做会有内存不够用的风险，而且内存是从 JVM （java 虚拟机）上分配的，JVM 对大内存的垃圾收集有一定不稳性。当数据量大时，内存不够是正常的，同时，也不可能一次性所有字段都加载，如果未命中搜索，还需要在内存中建立 fielddata ，这都影响响应时间。

为了解决 fielddata 的内存有限和 JVM 对大内存的垃圾回收导致的不稳定问题，Elasticsearch 引入如 DocValue。DocValue 也是和 fielddata 类似的结构。不同的是，DocValue 是持久存取在文件中。由于这消耗了额外的存储空间，但对于 JVM 的内存需求降低，多余的内存留给操作系统的文件缓存使用。加上DocValue 是预先构建的，查询时剩去了不命中是构建 fielddata 的时间。DocValue 比内存慢10%~25%，但是稳定性大幅提升。



## 5、Lucene 查询原理

在lucene中，读写路径是分离的。写入的时候创建一个IndexWriter，而读的时候会创建一个IndexSearcher。下面是一个简单的代码示例，如何使用lucene的IndexWriter建索引以及如何使用indexSearch进行搜索查询。

```java
Analyzer analyzer = new StandardAnalyzer();
    // Store the index in memory:
    Directory directory = new RAMDirectory();
    // To store an index on disk, use this instead:
    //Directory directory = FSDirectory.open("/tmp/testindex");
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter iwriter = new IndexWriter(directory, config);
    Document doc = new Document();
    String text = "This is the text to be indexed.";
    doc.add(new Field("fieldname", text, TextField.TYPE_STORED));
    iwriter.addDocument(doc);
    iwriter.close();

    // Now search the index:
    DirectoryReader ireader = DirectoryReader.open(directory);
    IndexSearcher isearcher = new IndexSearcher(ireader);
    // Parse a simple query that searches for "text":
    QueryParser parser = new QueryParser("fieldname", analyzer);
    Query query = parser.parse("text");
    ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
    //assertEquals(1, hits.length);
    // Iterate through the results:
    for (int i = 0; i < hits.length; i++) {
         Document hitDoc = isearcher.doc(hits[i].doc);
         System.out.println(hitDoc.get("fieldname"));
    }
    ireader.close();
    directory.close();
```

从这个示例中可以看出，lucene的读写有各自的操作类。在使用IndexSearcher类的时候，需要一个DirectoryReader和QueryParser，其中DirectoryReader需要对应写入时候的Directory实现。QueryParser主要用来解析你的查询语句，例如你想查 “A and B"，lucene内部会有机制解析出是term A和term B的交集查询。在具体执行Search的时候指定一个最大返回的文档数目，因为可能会有过多命中，我们可以限制单词返回的最大文档数，以及做分页返回。


### 1、Lucene 查询过程

在lucene中查询是基于segment。每个segment可以看做是一个独立的subindex，在建立索引的过程中，lucene会不断的flush内存中的数据持久化形成新的segment。多个segment也会不断的被merge成一个大的segment，在老的segment还有查询在读取的时候，不会被删除，没有被读取且被merge的segement会被删除。这个过程类似于LSM数据库的merge过程。下面我们主要看在一个segment内部如何实现高效的查询。

为了方便大家理解，我们以人名字，年龄，学号为例，如何实现查某个名字（有重名）的列表。

![](../../pic/2020-05-05-21-10-10.png)

在lucene中为了查询name=XXX的这样一个条件，会建立基于name的倒排链。以上面的数据为例，倒排链如下：

姓名

![](../../pic/2020-05-05-21-10-40.png)

如果我们还希望按照年龄查询，例如想查年龄=18的列表，我们还可以建立另一个倒排链：

![](../../pic/2020-05-05-21-11-00.png)


在这里，Alice，Alan，18，这些都是term。所以倒排本质上就是基于term的反向列表，方便进行属性查找。到这里我们有个很自然的问题，如果term非常多，如何快速拿到这个倒排链呢？在lucene里面就引入了term dictonary的概念，也就是term的字典。term字典里我们可以按照term进行排序，那么用一个二分查找就可以定为这个term所在的地址。这样的复杂度是logN，在term很多，内存放不下的时候，效率还是需要进一步提升。可以用一个hashmap，当有一个term进入，hash继续查找倒排链。这里hashmap的方式可以看做是term dictionary的一个index。 从lucene4开始，为了方便实现rangequery或者前缀，后缀等复杂的查询语句，lucene使用FST数据结构来存储term字典，下面就详细介绍下FST的存储结构。


### 2、fst

参考下面的98描述


这样你就得到了一个有向无环图，有这样一个数据结构，就可以很快查找某个人名是否存在。FST在单term查询上可能相比hashmap并没有明显优势，甚至会慢一些。但是在范围，前缀搜索以及压缩率上都有明显的优势。

在通过FST定位到倒排链后，有一件事情需要做，就是倒排链的合并。因为查询条件可能不止一个，例如上面我们想找name="alan" and age="18"的列表。lucene是如何实现倒排链的合并呢。这里就需要看一下倒排链存储的数据结构


### 3、SkipList

为了能够快速查找docid，lucene采用了SkipList这一数据结构。SkipList有以下几个特征：

- 1、元素排序的，对应到我们的倒排链，lucene是按照docid进行排序，从小到大。
- 2、跳跃有一个固定的间隔，这个是需要建立SkipList的时候指定好，例如下图以间隔是3
- 3、SkipList的层次，这个是指整个SkipList有几层

![](../../pic/2020-05-05-21-21-13.png)

有了这个SkipList以后比如我们要查找docid=12，原来可能需要一个个扫原始链表，1，2，3，5，7，8，10，12。有了SkipList以后先访问第一层看到是然后大于12，进入第0层走到3，8，发现15大于12，然后进入原链表的8继续向下经过10和12。

有了FST和SkipList的介绍以后，我们大体上可以画一个下面的图来说明lucene是如何实现整个倒排结构的：

![](../../pic/2020-05-05-21-22-57.png)

[问题]：是term dict index还是term dict基于fst实现的？？？

有了这张图，我们可以理解为什么基于lucene可以快速进行倒排链的查找和docid查找，下面就来看一下有了这些后如何进行倒排链合并返回最后的结果。


### 4、倒排合并

todo

### 5、BKDTree

在lucene中如果想做范围查找，根据上面的FST模型可以看出来，需要遍历FST找到包含这个range的一个点然后进入对应的倒排链，然后进行求并集操作。但是如果是数值类型，比如是浮点数，那么潜在的term可能会非常多，这样查询起来效率会很低。所以为了支持高效的数值类或者多维度查询，lucene引入类BKDTree。BKDTree是基于KDTree，对数据进行按照维度划分建立一棵二叉树确保树两边节点数目平衡。在一维的场景下，KDTree就会退化成一个二叉搜索树，在二叉搜索树中如果我们想查找一个区间，logN的复杂度就会访问到叶子结点得到对应的倒排链。如下图所示：

![](../../pic/2020-05-05-21-28-25.png)


### 6、如何实现返回结果进行排序聚合

通过之前介绍可以看出lucene通过倒排的存储模型实现term的搜索，那对于有时候我们需要拿到另一个属性的值进行聚合，或者希望返回结果按照另一个属性进行排序。在lucene4之前需要把结果全部拿到再读取原文进行排序，这样效率较低，还比较占用内存，为了加速lucene实现了fieldcache，把读过的field放进内存中。这样可以减少重复的IO，但是也会带来新的问题，就是占用较多内存。新版本的lucene中引入了DocValues，DocValues是一个基于docid的列式存储。当我们拿到一系列的docid后，进行排序就可以使用这个列式存储，结合一个堆排序进行。当然额外的列式存储会占用额外的空间，lucene在建索引的时候可以自行选择是否需要DocValue存储和哪些字段需要存储。



### 7、Lucene的代码目录结构

介绍了lucene中几个主要的数据结构和查找原理后，我们在来看下lucene的代码结构，后续可以深入代码理解细节。lucene的主要有下面几个目录：

- analysis模块主要负责词法分析及语言处理而形成Term。
- codecs模块主要负责之前提到的一些数据结构的实现，和一些编码压缩算法。包括skiplist，docvalue等。
- document模块主要包括了lucene各类数据类型的定义实现。
- index模块主要负责索引的创建，里面有IndexWriter。
- store模块主要负责索引的读写。
- search模块主要负责对索引的搜索。
- geo模块主要为geo查询相关的类实现
- util模块是bkd，fst等数据结构实现。


### 8、查询性能测试

https://zhuanlan.zhihu.com/p/47951652



# 95、分布式系统


## 1、基于本地文件系统的分布式系统

![](../../pic/2020-05-04-14-28-45.png)

上图中是一个基于本地磁盘存储数据的分布式系统。Index一共有3个Shard，每个Shard除了Primary Shard外，还有一个Replica Shard。当Node 3机器宕机或磁盘损坏的时候，首先确认P3已经不可用，重新选举R3位Primary Shard，此Shard发生主备切换。然后重新找一台机器Node 7，在Node7 上重新启动P3的新Replica。由于数据都会存在本地磁盘，此时需要将Shard 3的数据从Node 6上拷贝到Node7上。如果有200G数据，千兆网络，拷贝完需要1600秒。如果没有replica，则这1600秒内这些Shard就不能服务。

为了保证可靠性，就需要冗余Shard，会导致更多的物理资源消耗。

这种思想的另外一种表现形式是使用双集群，集群级别做备份。

在这种架构中，如果你的数据是在其他存储系统中生成的，比如HDFS/HBase，那么你还需要一个数据传输系统，将准备好的数据分发到相应的机器上。

这种架构中为了保证可用性和可靠性，需要双集群或者Replica才能用于生产环境。

Elasticsearch使用的就是这种架构方式。


## 2、基于分布式文件系统的分布式系统（共享存储，比如hdfs）

![](../../pic/2020-05-04-14-30-34.png)


针对第一种架构中的问题，另一种思路是：存储和计算分离。

第一种思路的问题根源是数据量大，拷贝数据耗时多，那么有没有办法可以不拷贝数据？为了实现这个目的，一种思路是底层存储层使用共享存储，每个Shard只需要连接到一个分布式文件系统中的一个目录/文件即可，Shard中不含有数据，只含有计算部分。相当于每个Node中只负责计算部分，存储部分放在底层的另一个分布式文件系统中，比如HDFS。

上图中，Node 1 连接到第一个文件；Node 2连接到第二个文件；Node3连接到第三个文件。当Node 3机器宕机后，只需要在Node 4机器上新建一个空的Shard，然后构造一个新连接，连接到底层分布式文件系统的第三个文件即可，创建连接的速度是很快的，总耗时会非常短。

这种是一种典型的存储和计算分离的架构，优势有以下几个方面：

- 在这种架构下，资源可以更加弹性，当存储不够的时候只需要扩容存储系统的容量；当计算不够的时候，只需要扩容计算部分容量。

- 存储和计算是独立管理的，资源管理粒度更小，管理更加精细化，浪费更少，结果就是总体成本可以更低。

- 负载更加突出，抗热点能力更强。一般热点问题基本都出现在计算部分，对于存储和计算分离系统，计算部分由于没有绑定数据，可以实时的扩容、缩容和迁移，当出现热点的时候，可以第一时间将计算调度到新节点上。

这种架构同时也有一个不足：

-访问分布式文件系统的性能可能不及访问本地文件系统。在上一代分布式文件系统中，这是一个比较明显的问题，但是目前使用了各种用户态协议栈后，这个差距已经越来越小了。

HBase使用的就是这种架构方式。

Solr也支持这种形式的架构。














# 96、mapping映射

## 1、数据类型

![](../../pic/2020-05-04-11-52-57.png)





## 2、示例

```json
PUT /my-index
{
  "mappings": {
    "properties": {
      "age":    { "type": "integer" },  
      "email":  { "type": "keyword"  }, 
      "name":   { "type": "text"  }     
    }
  }
}
```

```json
GET /my_index


{
  "my_index" : {
    "aliases" : { },
    "mappings" : {
      "doc" : {
        "dynamic" : "false",
        "properties" : {//定义的字段和字段类型
          "age" : {
            "type" : "integer"
          },
          "name" : {
            "type" : "keyword"
          },
          "title" : {
            "type" : "text"
          }
        }
      }
    },
    "settings" : {
      "index" : {
        "creation_date" : "1588564233502",
        "number_of_shards" : "5",
        "number_of_replicas" : "1",
        "uuid" : "i1umjq9vSCG4T_kGu3bdTA",
        "version" : {
          "created" : "6050399"
        },
        "provided_name" : "my_index"
      }
    }
  }
}

```

mappings 中一个字段field定义可以设置的参数：

```json
"field": {  
         "type":  "text", //数据类型，文本类型  
         
         "index": "analyzed",//分词，不分词是：not_analyzed ，设置成false，字段将不会被索引  
         
         "analyzer":"ik"//指定分词器  
         
         "boost":1.23,//字段级别的分数加权  
         
         "doc_values":false//对not_analyzed字段，默认都是开启，analyzed字段不能使用，对排序和聚合能提升较大性能，节约内存,如果您确定不需要对字段进行排序或聚合，或者从script访问字段值，则可以禁用doc值以节省磁盘空间：
         
         "fielddata":{"loading" : "eager" },//Elasticsearch 加载内存 fielddata 的默认行为是 延迟 加载 。 当 Elasticsearch 第一次查询某个字段时，它将会完整加载这个字段所有 Segment 中的倒排索引到内存中，以便于以后的查询能够获取更好的性能。
         
         "fields":{"keyword": {"type": "keyword","ignore_above": 256}}, //可以对一个字段提供多种索引模式，同一个字段的值，一个分词，一个不分词  
         
         "ignore_above":100 ,//超过100个字符的文本，将会被忽略，不被索引
           
         "include_in_all":ture,//设置是否此字段包含在_all字段中，默认是true，除非index设置成no选项  
         
         "index_options":"docs",//4个可选参数docs（索引文档号） ,freqs（文档号+词频），positions（文档号+词频+位置，通常用来距离查询），offsets（文档号+词频+位置+偏移量，通常被使用在高亮字段）分词字段默认是position，其他的默认是docs  
         
         "norms":{"enable":true,"loading":"lazy"},//分词字段默认配置，不分词字段：默认{"enable":false}，存储长度因子和索引时boost，建议对需要参与评分字段使用 ，会额外增加内存消耗量  
         
         "null_value":"NULL",//设置一些缺失字段的初始化值，只有string可以使用，分词字段的null值也会被分词  
         
         "position_increament_gap":0,//影响距离查询或近似查询，可以设置在多值字段的数据上火分词字段上，查询时可指定slop间隔，默认值是100  
         
         "store":false,//是否单独设置此字段的是否存储而从_source字段中分离，默认是false，只能搜索，不能获取值  
         
         "search_analyzer":"ik",//设置搜索时的分词器，默认跟ananlyzer是一致的，比如index时用standard+ngram，搜索时用standard用来完成自动提示功能  
         
         "similarity":"BM25",//默认是TF/IDF算法，指定一个字段评分策略，仅仅对字符串型和分词类型有效  
         
         "term_vector":"no",//默认不存储向量信息，支持参数yes（term存储），with_positions（term+位置）,with_offsets（term+偏移量），with_positions_offsets(term+位置+偏移量) 对快速高亮fast vector highlighter能提升性能，但开启又会加大索引体积，不适合大数据量用  
       }  


```





# 97、列存储

![](../../pic/2020-05-04-10-37-06.png)

![](../../pic/2020-05-04-10-45-51.png)

> 数据压缩(字典表)

下面中才是那张表本来的样子。经过字典表进行数据压缩后，表中的字符串才都变成数字了。正因为每个字符串在字典表里只出现一次了，所以达到了压缩的目的(有点像规范化和非规范化Normalize和Denomalize)

![](../../pic/2020-05-04-11-03-25.png)

通过一条查询的执行过程说明列式存储(以及数据压缩)的优点：

![](../../pic/2020-05-04-11-03-51.png)

备注：结果集中的第6行即是所要找的数据。


关键步骤如下：

- 1.去字典表里找到字符串对应数字(只进行一次字符串比较)。

- 2.用数字去列表里匹配，匹配上的位置设为1。

- 3.把不同列的匹配结果进行位运算得到符合所有条件的记录下标。

- 4.使用这个下标组装出最终的结果集。



# 98、lucene字典实现原理FST（Finite State Transducers一种有限状态转移机）---term dictonary

## 1、lucene字典

使用lucene进行查询不可避免都会使用到其提供的字典功能，即根据给定的term找到该term所对应的倒排文档id列表等信息。实际上lucene索引文件后缀名为tim和tip的文件实现的就是lucene的字典功能。

怎么实现一个字典呢？我们马上想到排序数组，即term字典是一个已经按字母顺序排序好的数组，数组每一项存放着term和对应的倒排文档id列表。每次载入索引的时候只要将term数组载入内存，通过二分查找即可。这种方法查询时间复杂度为Log(N)，N指的是term数目，占用的空间大小是O(N*str(term))。排序数组的缺点是消耗内存，即需要完整存储每一个term，当term数目多达上千万时，占用的内存将不可接受。

![](../../pic/2020-05-04-00-16-18.png)


## 2、常用字典数据结构

很多数据结构均能完成字典功能，总结如下。

![](../../pic/2020-05-04-00-16-49.png)

- [SkipList 跳表](https://www.iteye.com/blog/kenby-1187303)
- [Ternary Search Trees](https://www.drdobbs.com/database/ternary-search-trees/184410528)


## 3、FST原理简析

lucene从4开始大量使用的数据结构是FST（Finite State Transducer）。FST有两个优点：1）空间占用小。通过对词典中单词前缀和后缀的重复利用，压缩了存储空间；2）查询速度快。O(len(str))的查询时间复杂度。

下面简单描述下FST的构造过程（工具演示：http://examples.mikemccandless.com/fst.py?terms=&cmd=Build+it%21）。我们对“cat”、 “deep”、 “do”、 “dog” 、“dogs”这5个单词进行插入构建FST（注：必须已排序）。

1）插入“cat”

插入cat，每个字母形成一条边，其中t边指向终点。

![](../../pic/2020-05-04-00-22-01.png)

 

2）插入“deep”

与前一个单词“cat”进行最大前缀匹配，发现没有匹配则直接插入，P边指向终点。

![](../../pic/2020-05-04-00-22-22.png)

3）插入“do”

与前一个单词“deep”进行最大前缀匹配，发现是d，则在d边后增加新边o，o边指向终点。

![](../../pic/2020-05-04-00-22-39.png)

4）插入“dog”

与前一个单词“do”进行最大前缀匹配，发现是do，则在o边后增加新边g，g边指向终点。

![](../../pic/2020-05-04-00-22-56.png)

5）插入“dogs”

与前一个单词“dog”进行最大前缀匹配，发现是dog，则在g后增加新边s，s边指向终点。

![](../../pic/2020-05-04-00-23-08.png)

最终我们得到了如上一个有向无环图。利用该结构可以很方便的进行查询，如给定一个term “dog”，我们可以通过上述结构很方便的查询存不存在，甚至我们在构建过程中可以将单词与某一数字、单词进行关联，从而实现key-value的映射。












# 99、使用案例

- ElasticSearch，和Solr一样，是底层基于Apache Lucene，且具备高可靠性的企业级搜索引擎。
- 维基百科使用Elasticsearch来进行全文搜做并高亮显示关键词，以及提供search-as-you-type、did-you-mean等搜索建议功能。
- 英国卫报使用Elasticsearch来处理访客日志，以便能将公众对不同文章的反应实时地反馈给各位编辑。
- StackOverflow将全文搜索与地理位置和相关信息进行结合，以提供more-like-this相关问题的展现。
- GitHub使用Elasticsearch来检索超过1300亿行代码。
- 每天，Goldman Sachs使用它来处理5TB数据的索引，还有很多投行使用它来分析股票市场的变动。

但是Elasticsearch并不只是面向大型企业的，它还帮助了很多类似DataDog以及Klout的创业公司进行了功能的扩展。







# 参考

- [es中文社区](https://elasticsearch.cn/)

- [知乎 es专栏](https://zhuanlan.zhihu.com/Elasticsearch)

- [官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)

- [Elasticsearch 的存储模型和读写操作](https://www.infoq.cn/article/analysis-of-elasticsearch-cluster-part01/)


- [ElasticSearch原理](https://www.jianshu.com/p/3b68f351bdc7)

详细讲解了读写过程

- [Elasticsearch基础整理-来自程序员的浪漫](https://www.jianshu.com/p/e8226138485d)

讲解了存储模型

- [剖析 Elasticsearch 集群系列第一篇 Elasticsearch 的存储模型和读写操作](https://www.infoq.cn/article/analysis-of-elasticsearch-cluster-part01/)


- [理解 Elasticsearch 数据持久化模型](https://www.linpx.com/p/understanding-elastic-search-data-persistence-model.html)

- [es权威指南](https://es.xiaoleilu.com/030_Data/30_Create.html)

讲解了es和Lucene关系已经Lucene结构

- [elasticsearch 百亿级数据检索案例与原理](https://www.cnblogs.com/mikevictor07/p/10006553.html)

写入数据的优化

- [Elasticsearch 高并发写入优化的开源协同经历](https://www.infoq.cn/article/HAdBrlW6FAO0FmshXKpZ)


介绍了批量操作
- [ElasticSearch 存储原理解析（上）](https://www.infoq.cn/article/UotLIglvj6TcUE2vxC5X)
- [ElasticSearch 存储原理解析（下）](https://www.infoq.cn/article/DMw6z0XcCSgCWS6TyTF1)

- [insightdatascience](https://blog.insightdatascience.com/)


- [剖析Elasticsearch集群：近实时搜索、深层分页问题和搜索相关性权衡之道](https://mp.weixin.qq.com/s?__biz=MzU1NDA4NjU2MA==&mid=2247486450&idx=1&sn=0c0e53bc568c0ebe369f15369a90b70d&chksm=fbe9b23dcc9e3b2b45f763fca90f94c015020fe87608cef375910564a1a9c4bdd34f0f46e666&scene=27#wechat_redirect)

- [剖析Elasticsearch集群：分布式的三个C、translog和Lucene段](https://www.infoq.cn/article/anatomy-of-an-elasticsearch-cluster-part02)

- [Elasticsearch学习笔记](https://juejin.im/post/5b9292b75188255c6b64eee5)


介绍文件存储结构
- [Elasticsearch中数据是如何存储的](https://elasticsearch.cn/article/6178)


- [lucene字典实现原理——FST](https://www.cnblogs.com/LBSer/p/4119841.html)

- [Elasticsearch存储深入详解](https://cloud.tencent.com/developer/article/1351712)

- [Elasticsearch 搜索与聚合在数据存储结构方面的理解](https://www.jianshu.com/p/76ab92de2786)

- [几张图看懂列式存储](https://blog.csdn.net/dc_726/article/details/41143175)

- [编程随笔-ElasticSearch知识导图(4)：搜索](https://www.jianshu.com/p/f7e2988f637d)

- [Elasticsearch写入速度优化](http://kane-xie.github.io/2017/09/09/2017-09-09_Elasticsearch%E5%86%99%E5%85%A5%E9%80%9F%E5%BA%A6%E4%BC%98%E5%8C%96/)


实战调优

- [ElasticSearch 性能优化实战，让你的 ES 飞起来！](https://mp.weixin.qq.com/s?__biz=MzU2NjIzNDk5NQ==&mid=2247487603&idx=1&sn=d450e7ada939862b389cfc092400953b&chksm=fcaeca6fcbd9437970cafe120ff0748716afc7631674b3b06fe49ce71c9b85f06a4a488ad9e7&scene=27#wechat_redirect)
