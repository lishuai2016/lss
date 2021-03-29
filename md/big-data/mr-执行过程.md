<!-- TOC -->

- [0、MapReduce整体处理流程](#0mapreduce整体处理流程)
- [1、输入分片（input split）](#1输入分片input-split)
- [2、map阶段](#2map阶段)
- [3、combiner阶段](#3combiner阶段)
- [4、shuffle（重点优化）](#4shuffle重点优化)
    - [0、shuffle原理](#0shuffle原理)
    - [1、Map端shuffle](#1map端shuffle)
    - [2、Reduce端shuffle](#2reduce端shuffle)
- [5、reduce阶段](#5reduce阶段)
- [9、不同版本MapReduce的运行机制差异](#9不同版本mapreduce的运行机制差异)
    - [1、1.x版本处理流程](#11x版本处理流程)
        - [1、Mapreduce的相关问题](#1mapreduce的相关问题)
            - [1、jobtracker的单点故障](#1jobtracker的单点故障)
            - [2、mapreduce计算输出](#2mapreduce计算输出)
            - [3、InputFormat和OutputFormat](#3inputformat和outputformat)
    - [2、2.x版本处理流程](#22x版本处理流程)
- [参考](#参考)

<!-- /TOC -->






# 0、MapReduce整体处理流程

![](../../pic/2020-05-02-17-14-05.png)

- 1、一个Map/Reduce作业（job）通常会把指定要处理数据集切分为若干独立的数据块，map调用InputFormat()方法来生成可处理的<key,value>对.

- 2、Map：根据用户提供的函数，处理上面输入的键值对数据，生成新的键值对数据，提供给reduce处理；

- 3、Shuffle：把从map任务输出到reducer任务输入之间的map/reduce框架所做的工作

- 4、Reduce：以拉取到的数据作为输入，并依次为每个键值对执行reduce函数。并将结果写入HDFS中。每个reduce进程会对应一个输出文件，名称以part-开头。


# 1、输入分片（input split）

`注意：大于设置的分片splitSize需要进行切分，小于的直接作为一个分片，而一个分片由一个map处理，会造成如果存在大量的小文件会造成需要大量的map来进行处理。`


在进行map计算之前，mapreduce会根据输入文件计算输入分片（input split），每个输入分片（input split）针对一个map任务，输入分片（input split）存储的并非数据本身，而是一个分片长度和一个记录数据的位置的数组，输入分片（input split）往往和hdfs的block（块）关系很密切，假如我们设定hdfs的块的大小是64mb，如果我们输入有三个文件，大小分别是3mb、65mb和127mb，那么mapreduce会把3mb文件分为一个输入分片（input split），65mb则是两个输入分片（input split）而127mb也是两个输入分片（input split），换句话说我们如果在map计算前做输入分片调整，例如合并小文件，那么就会有5个map任务将执行，而且每个map执行的数据大小不均，这个也是mapreduce优化计算的一个关键点。


split只是将源文件的内容分片形成一系列的 InputSplit，每个 InputSpilt 中存储着对 应分片的数据信息（例如，文件块信息、起始位置、数据长度、所在节点列表…），并不是将源文件分割成多个小文件，每个InputSplit 都由一个 mapper 进行后续处理。

每个分片大小参数是很重要的，splitSize 是组成分片规则很重要的一个参数，该参数由三个值来确定：

- minSize：splitSize 的最小值，由 mapred-site.xml 配置文件中 mapred.min.split.size 参数确定。

- maxSize：splitSize 的最大值，由 mapred-site.xml 配置文件中mapreduce.jobtracker.split.metainfo.maxsize 参数确定。

- blockSize：HDFS 中文件存储的快大小，由 hdfs-site.xml 配置文件中 dfs.block.size 参数确定。

`splitSize的确定规则：splitSize=max{minSize，min{maxSize，blockSize}}`


minSize的默认值是1,而maxSize的默认值是long类型的最大值,即可得切片的默认大小是blockSize(128M);maxSize参数如果调得比blocksize小，则会让切片变小，而且就等于配置的这个参数的值;minSize参数调的比blockSize大，则可以让切片变得比blocksize还大;

hadoop为每个分片构建一个map任务,可以并行处理多个分片上的数据,整个数据的处理过程将得到很好的负载均衡,因为一台性能较强的计算机能处理更多的数据分片.分片也不能切得太小,否则多个map和reduce间数据的传输时间,管理分片,构建多个map任务的时间将决定整个作业的执行时间.(大部分时间都不在计算上)

`如果文件大小小于128M,则该文件不会被切片,不管文件多小都会是一个单独的切片,交给一个maptask处理.如果有大量的小文件,将导致产生大量的maptask,大大降低集群性能.`

数据格式化（Format）操作：

将划分好的 InputSplit 格式化成键值对形式的数据。`其中 key 为偏移量，value 是每一行的内容。`

值得注意的是，在map任务执行过程中，会不停的执行数据格式化操作，每生成一个键值对就会将其传入 map，进行处理。所以map和数据格式化操作并不存在前后时间差，而是同时进行的。

> 大量小文件的优化策略:

- 1、在数据处理的前端就将小文件整合成大文件，再上传到hdfs上，即避免了hdfs不适合存储小文件的缺点,又避免了后期使用mapreduce处理大量小文件的问题。(最提倡的做法)

- 2、小文件已经存在hdfs上了，可以使用另一种inputformat来做切片(`CombineFileInputFormat`),它的切片逻辑和FileInputFormat（默认）不同，它可以将多个小文件在逻辑上规划到一个切片上，交给一个maptask处理。


> MapTask和ReduceTask的并行度

有几个maptask是由程序决定的，默认情况下使用FileInputFormat读入数据，maptask数量的依据有一下几点：

1.文件大小小于128M（默认）的情况下，有几个文件就有几个maptask

2.大于128M的文件，根据切片规则，有几个分片就有几个maptask

3.并不是maptask数量越多越好，太多maptask可能会占用大量数据传输等时间，降低集群计算时间，降低性能。大文件可适当增加blocksize的大小，如将128M的块大小改为256M或512M，这样切片的大小也会增大，切片数量也就减少了，相应地减少maptask的数量。如果小文件太多，可用上述提到过的小文件优化策略减少maptask的数量。

有几个reducetask是用户决定的，用户可以根据需求，自定义相应的partition函数，将数据分成几个区，相应地将reducetask的数量设置成分区数量。（设置5个reducetask，job.setNumReduceTasks(5)）

# 2、map阶段

就是程序员编写好的map函数了，因此map函数效率相对好控制，而且一般map操作都是本地化操作也就是在数据存储节点上进行；



# 3、combiner阶段

combiner阶段是程序员可以选择的，combiner其实也是一种reduce操作，因此我们看见WordCount类里是用reduce进行加载的。Combiner是一个本地化的reduce操作，它是map运算的后续操作，主要是在map计算出中间文件前做一个简单的合并重复key值的操作，例如我们对文件里的单词频率做统计，map计算时候如果碰到一个hadoop的单词就会记录为1，但是这篇文章里hadoop可能会出现n多次，那么map输出文件冗余就会很多，因此在reduce计算前对相同的key做一个合并操作，那么文件会变小，这样就提高了宽带的传输效率，毕竟hadoop计算力宽带资源往往是计算的瓶颈也是最为宝贵的资源，但是combiner操作是有风险的，使用它的原则是combiner的输入不会影响到reduce计算的最终输入，例如：如果计算只是求总数，最大值，最小值可以使用combiner，但是做平均值计算使用combiner的话，最终的reduce计算结果就会出错。

# 4、shuffle（重点优化）

![](../../pic/2020-05-02-17-16-25.png)

Shuffle横跨Map端和Reduce端，在Map端包括Spill过程，在Reduce端包括copy和sort过程。

将map的输出作为reduce的输入的过程就是shuffle了，由于shuffle涉及到了磁盘的读写和网络的传输，因此`shuffle性能的高低直接影响到了整个程序的运行效率`。


Shuffle 过程是指Mapper 产生的直接输出结果，经过一系列的处理，成为最终的 Reducer 直接输入数据为止的整个过程。这是mapreduce的核心过程。该过程可以分为两个阶段：

- Mapper 端的Shuffle：由 Mapper 产生的结果并不会直接写入到磁盘中，而是先存储在内存中，当内存中的数据量达到设定的阀值时，一次性写入到本地磁盘中。并同时进行 sort（排序）、combine（合并）、partition（分片）等操作。其中，sort 是把 Mapper 产 生的结果按照 key 值进行排序；combine 是把key值相同的记录进行合并；partition 是把 数据均衡的分配给 Reducer。

- Reducer 端的 Shuffle：由于Mapper和Reducer往往不在同一个节点上运行，所以 Reducer 需要从多个节点上下载Mapper的结果数据，并对这些数据进行处理，然后才能被 Reducer处理。


## 0、shuffle原理

Shuffle一开始就是map阶段做输出操作，一般mapreduce计算的都是海量数据，map输出时候不可能把所有文件都放到内存操作，因此map写入磁盘的过程十分的复杂，更何况map输出时候要对结果进行排序，内存开销是很大的，map在做输出时候会在内存里开启一个环形内存缓冲区，这个缓冲区专门用来输出的，`默认大小是100mb`，并且在配置文件里为这个缓冲区设定了一个阀值，默认是0.80（这个大小和阀值都是可以在配置文件里进行配置的），同时map还会为输出操作启动一个守护线程，如果缓冲区的内存达到了阀值的80%时候，这个守护线程就会把内容写到磁盘上，这个过程叫`spill`，另外的20%内存可以继续写入要写进磁盘的数据，写入磁盘和写入内存操作是互不干扰的，如果缓存区被撑满了，那么map就会阻塞写入内存的操作，让写入磁盘操作完成后再继续执行写入内存操作，前面我讲到`写入磁盘前会有个排序操作，这个是在写入磁盘操作时候进行，不是在写入内存时候进行的`，如果我们定义了combiner函数，那么排序前还会执行`combiner`操作。


每次spill操作也就是写入磁盘操作时候就会写一个溢出文件，也就是说在做map输出有几次spill就会产生多少个溢出文件，等map输出全部做完后，map会合并这些输出文件。这个过程里还会有一个Partitioner操作，对于这个操作很多人都很迷糊，其实Partitioner操作和map阶段的输入分片（Input split）很像，一个Partitioner对应一个reduce作业，如果我们mapreduce操作只有一个reduce操作，那么Partitioner就只有一个，如果我们有多个reduce操作，那么Partitioner对应的就会有多个，Partitioner因此就是reduce的输入分片，这个程序员可以编程控制，主要是根据实际key和value的值，根据实际业务类型或者为了更好的reduce负载均衡要求进行，这是提高reduce效率的一个关键所在。到了reduce阶段就是合并map输出文件了，Partitioner会找到对应的map输出文件，然后进行复制操作，复制操作时reduce会开启几个复制线程，这些线程默认个数是5个，程序员也可以在配置文件更改复制线程的个数，这个复制过程和map写入磁盘过程类似，也有阀值和内存大小，阀值一样可以在配置文件里配置，而内存大小是直接使用reduce的tasktracker的内存大小，复制时候reduce还会进行排序操作和合并文件操作，这些操作完了就会进行reduce计算了。


## 1、Map端shuffle


每个map task都有一个`内存缓冲区`，存储map的输出结果，当缓冲区快满的时候需要将缓冲区的数据以一个临时文件的方式存放到磁盘，当整个map task结束后再对磁盘中这个map task产生的所有临时文件做合并，生成最终的正式输出文件，然后等待reduce task来拉数据。

![](../../pic/2020-05-02-17-18-35.png)

每个Map任务以<key, value>对的形式把数据输出到在内存中构造的一个环形数据结构中。该数据结构是个字节数组，称为KVbuffer。Kvbuffer中不仅存放<key, value>数据，还放置了一些索引数据。索引数据称为kvmeta。

数据在写入缓冲区之前，会通过Partitioner进行分区，指定那些数据交给哪个reduce task处理，对key进行hash后，再以reducetask数量取模
kvmeta，包括：value的起始位置、key的起始位置、partition值（指定哪个reduce处理）、value的长度。

用一个分界点来划分两者，初始的分界点是0，<key, value>数据的存储方向是向上增长，索引数据的存储方向是向下增长。


当缓冲区的空间不够时，需要将数据刷到硬盘上，这个过程称为spill。如果把Kvbuffer用完时候再开始Spill，那Map任务就需要等Spill完成腾出空间之后才能继续写数据；如果Kvbuffer只是满到一定程度，比如`80%的时候就开始Spill`，那在Spill的同时，Map任务还能继续写数据。

一开始bufstart=bufend，如果达到溢写条件，令bufend=bufindex，并将[bufstart,bufend]之间的数据写到磁盘上；溢写完成之后，令bufstart=butend=newEquator，完成分界点的转移。

在spill之前，会先把数据按照partition值和key两个关键字升序排序，移动的只是索引数据，排序结果是Kvmeta中数据按照partition为单位聚集在一起，同一partition内的按照key有序。

如果用户设置了combiner，还会对数据进行合并操作，计算规则与reduce一致。

Spill线程根据排过序的Kvmeta挨个partition的把<key, value>数据写入磁盘文件中，一个partition对应的数据写完之后顺序地写下个partition，直到把所有的partition遍历完。每次spill都会产生一个溢写文件，而最终`每个map task只会有一个输出文件`，因此会对这些中间结果进行归并，称为merge。

merge时，会进行一次排序，排序算法是多路归并排序；如果设置了combier，也会进行合并。最终生成的文件格式与单个溢出文件一致，也是按分区顺序存储，分区内按照key排序。

![](../../pic/2020-05-02-17-23-36.png)


## 2、Reduce端shuffle

![](../../pic/2020-05-02-17-24-43.png)

Copy：Reduce进程启动一些数据copy线程(Fetcher)，通过HTTP方式请求map task所在的TaskTracker获取map task的输出文件.Copy过来的数据会先放入内存缓冲区中，这里的缓冲区大小要比 map 端的更为灵活，它基于 JVM 的heap size设置，`reduce会使用其heapsize的70%来在内存中缓存数据`。当内存被使用到了一定的限度，就会开始往磁盘刷（刷磁盘前会先做sort）。这个限度阈值也是可以通过参数 mapred.job.shuffle.merge.percent（default 0.66）来设定。与map 端类似，这也是溢写的过程，这个过程中如果你设置有Combiner，也是会启用的。在远程copy数据的同时，Reduce Task在后台启动了两个后台线程对内存和磁盘上的数据文件做合并操作，以防止内存使用过多或磁盘生的文件过多。这种merge方式一直在运行，直到没有map端的数据时才结束，然后启动磁盘到磁盘的merge方式生成最终的那个文件。



# 5、reduce阶段

和map函数一样也是程序员编写的，最终结果是存储在hdfs上的。

![](../../pic/mapreduce过程1.png)
![](../../pic/mapreduce过程2.png)
![](../../pic/mapreduce过程3.png)





# 9、不同版本MapReduce的运行机制差异

## 1、1.x版本处理流程

首先是客户端要编写好mapreduce程序，配置好mapreduce的作业也就是job，接下来就是提交job了，提交job是提交到JobTracker上的，
这个时候JobTracker就会构建这个job，具体就是分配一个新的job任务的ID值，接下来它会做检查操作，这个检查就是确定输出目录是否存在，如果存在那么job就不能正常运行下去，JobTracker会抛出错误给客户端，接下来还要检查输入目录是否存在，如果不存在同样抛出错误，
如果存在JobTracker会根据输入计算输入分片（Input Split），如果分片计算不出来也会抛出错误，至于输入分片后面会做讲解的，
这些都做好了JobTracker就会配置Job需要的资源了。分配好资源后，JobTracker就会初始化作业，`初始化主要做的是将Job放入一个内部的队列`，让配置好的作业调度器能调度到这个作业，作业调度器会初始化这个job，初始化就是创建一个正在运行的job对象（封装任务和记录信息），以便JobTracker跟踪job的状态和进程。

初始化完毕后，作业调度器会获取输入分片信息（input split），每个分片创建一个map任务。接下来就是任务分配了，这个时候tasktracker会运行一个简单的循环机制定期发送心跳给jobtracker，心跳间隔是5秒，程序员可以配置这个时间，心跳就是jobtracker和tasktracker沟通的桥梁，通过心跳，jobtracker可以监控tasktracker是否存活，也可以获取tasktracker处理的状态和问题，同时tasktracker也可以通过心跳里的返回值获取jobtracker给它的操作指令。任务分配好后就是执行任务了。

在任务执行时候jobtracker可以通过心跳机制监控tasktracker的状态和进度，同时也能计算出整个job的状态和进度，而tasktracker也可以本地监控自己的状态和进度。当jobtracker获得了最后一个完成指定任务的tasktracker操作成功的通知时候，jobtracker会把整个job状态置为成功，然后当客户端查询job运行状态时候（注意：这个是异步操作），客户端会查到job完成的通知的。如果job中途失败，mapreduce也会有相应机制处理，一般而言如果不是程序员程序本身有bug，mapreduce错误处理机制都能保证提交的job能正常完成。




### 1、Mapreduce的相关问题

#### 1、jobtracker的单点故障

jobtracker和hdfs的namenode一样也存在单点故障，单点故障一直是hadoop被人诟病的大问题，为什么hadoop的做的文件系统和mapreduce计算框架都是高容错的，但是最重要的管理节点的故障机制却如此不好，我认为主要是namenode和jobtracker在实际运行中都是在内存操作，而做到内存的容错就比较复杂了，只有当内存数据被持久化后容错才好做，namenode和jobtracker都可以备份自己持久化的文件，但是这个持久化都会有延迟，因此真的出故障，任然不能整体恢复，另外hadoop框架里包含zookeeper框架，zookeeper可以结合jobtracker，用几台机器同时部署jobtracker，保证一台出故障，有一台马上能补充上，不过这种方式也没法恢复正在跑的mapreduce任务。

#### 2、mapreduce计算输出

做mapreduce计算时候，输出一般是一个文件夹，而且该文件夹是不能存在，而且这个检查做的很早，当我们提交job时候就会进行，mapreduce之所以这么设计是保证数据可靠性，如果输出目录存在reduce就搞不清楚你到底是要追加还是覆盖，不管是追加和覆盖操作都会有可能导致最终结果出问题，mapreduce是做海量数据计算，一个生产计算的成本很高，
例如一个job完全执行完可能要几个小时，因此一切影响错误的情况mapreduce是零容忍的。

#### 3、InputFormat和OutputFormat

Mapreduce还有一个InputFormat和OutputFormat，我们在编写map函数时候发现map方法的参数是之间操作行数据，没有牵涉到InputFormat，这些事情在我们new Path时候mapreduce计算框架帮我们做好了，而OutputFormat也是reduce帮我们做好了，我们使用什么样的输入文件，就要调用什么样的InputFormat，InputFormat是和我们输入的文件类型相关的，mapreduce里常用的InputFormat有FileInputFormat普通文本文件，SequenceFileInputFormat是指hadoop的序列化文件，另外还有KeyValueTextInputFormat。OutputFormat就是我们想最终存储到hdfs系统上的文件格式了，这个根据你需要定义了，hadoop有支持很多文件格式。




## 2、2.x版本处理流程

客户端的配置信息mapreduce.framework.name为yarn时，客户端会启动YarnRunner（yarn的客户端程序），并将mapreduce作业提交给yarn平台处理。

1.向ResourceManager请求运行一个mapreduce程序。

2.ResourceManager返回hdfs地址，告诉客户端将作业运行相关的资源文件上传到hdfs。

3.客户端提交mr程序运行所需的文件（包括作业的jar包，作业的配置文件，分片信息等）到hdfs上。

4.作业相关信息提交完成后，客户端用过调用ResourcrManager的submitApplication()方法提交作业。

5.ResourceManager将作业传递给调度器，调度器的默认调度策略是先进先出。

6.调度器寻找一台空闲的节点，并在该节点隔离出一个容器（container），容器中分配了cpu，内存等资源，并启动MRAppmaster进程。

7.MRAppmaster根据需要运行多少个map任务，多少个reduce任务向ResourceManager请求资源。

8.ResourceManager分配相应数量的容器，并告知MRAppmaster容器在哪。

9.MRAppmaster启动maptask。

10.maptask从HDFS获取分片数据执行map逻辑。

11.map逻辑执行结束后，MRAppmaster启动reducetask。

12.reducetask从maptask获取属于自己的分区数据执行reduce逻辑。

13.reduce逻辑结束后将结果数据保存到HDFS上。

14.mapreduce作业结束后，MRAppmaster通知ResourceManager结束自己，让ResourceManager回收所有资源。

详情见：[yarn-简介](./yarn-简介.md)


# 参考

- [Hadoop的MapReduce执行流程图](https://www.cnblogs.com/52mm/p/p15.html)

- [MapReduce过程详解(基于hadoop2.x架构)](https://www.cnblogs.com/52mm/p/p15.html)
