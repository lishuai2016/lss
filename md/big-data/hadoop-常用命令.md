
<!-- TOC -->

- [1.ls](#1ls)
- [2.put](#2put)
    - [2.1.moveFromLocal](#21movefromlocal)
    - [2.2.copyFromLocal](#22copyfromlocal)
- [3.get](#3get)
    - [3.1.moveToLocal](#31movetolocal)
    - [3.2.copyToLocal](#32copytolocal)
- [4.rm](#4rm)
- [5.mkdir](#5mkdir)
- [6.getmerge](#6getmerge)
- [7.cp](#7cp)
- [8.mv](#8mv)
- [9.count](#9count)
- [10.du](#10du)
- [11.text](#11text)
- [12.setrep](#12setrep)
- [13.stat](#13stat)
- [14.tail](#14tail)
- [15.archive](#15archive)
- [16.balancer](#16balancer)
- [17.dfsadmin](#17dfsadmin)
- [18.distcp](#18distcp)

<!-- /TOC -->


查看具体命令的用法

hadoop fs -help 命令

比如test的用法

hadoop fs -help test



hdfs dfsadmin -report查看集群中节点的情况

hadoop fs -cmd < args >

hadoop fs -cat /output/part-r-00000


hadoop fs -lsr /  递归输出个文件路径


hadoop fs -chmod 777 /output修改文件夹的权限

命令基本格式:

hadoop fs -cmd <args>

# 1.ls

hadoop fs -ls  /

列出hdfs文件系统根目录下的目录和文件

hadoop fs -ls -R /

列出hdfs文件系统所有的目录和文件

# 2.put

hadoop fs -put <local file> <hdfs file>

hdfs file的父目录一定要存在，否则命令不会执行

hadoop fs -put  <local file or dir >...< hdfs dir >

hdfs dir 一定要存在，否则命令不会执行

hadoop fs -put - < hdsf  file>

从键盘读取输入到hdfs file中，按Ctrl+D结束输入，hdfs file不能存在，否则命令不会执行

## 2.1.moveFromLocal

hadoop fs -moveFromLocal  < local src > ... < hdfs dst >

与put相类似，命令执行后源文件 local src 被删除，也可以从从键盘读取输入到hdfs file中

## 2.2.copyFromLocal

hadoop fs -copyFromLocal  < local src > ... < hdfs dst >

与put相类似，也可以从从键盘读取输入到hdfs file中

# 3.get

hadoop fs -get < hdfs file > < local file or dir>

local file不能和 hdfs file名字不能相同，否则会提示文件已存在，没有重名的文件会复制到本地

hadoop fs -get < hdfs file or dir > ... < local  dir >

拷贝多个文件或目录到本地时，本地要为文件夹路径

注意：如果用户不是root， local 路径要为用户文件夹下的路径，否则会出现权限问题，

## 3.1.moveToLocal

当前版本中还未实现此命令

## 3.2.copyToLocal

hadoop fs -copyToLocal < local src > ... < hdfs dst > 与get相类似

# 4.rm

hadoop fs -rm < hdfs file > ...

hadoop fs -rm -r < hdfs dir>...

每次可以删除多个文件或目录

# 5.mkdir

hadoop fs -mkdir < hdfs path>

只能一级一级的建目录，父目录不存在的话使用这个命令会报错

hadoop fs -mkdir -p < hdfs path>

所创建的目录如果父目录不存在就创建该父目录

# 6.getmerge

hadoop fs -getmerge < hdfs dir >  < local file >

将hdfs指定目录下所有文件排序后合并到local指定的文件中，文件不存在时会自动创建，文件存在时会覆盖里面的内容

hadoop fs -getmerge -nl  < hdfs dir >  < local file >

加上nl后，合并到local file中的hdfs文件之间会空出一行

# 7.cp

hadoop fs -cp  < hdfs file >  < hdfs file >

目标文件不能存在，否则命令不能执行，相当于给文件重命名并保存，源文件还存在

hadoop fs -cp < hdfs file or dir >... < hdfs dir >

目标文件夹要存在，否则命令不能执行

# 8.mv

hadoop fs -mv < hdfs file >  < hdfs file >

目标文件不能存在，否则命令不能执行，相当于给文件重命名并保存，源文件不存在

hadoop fs -mv  < hdfs file or dir >...  < hdfs dir >

源路径有多个时，目标路径必须为目录，且必须存在。

注意：跨文件系统的移动（local到hdfs或者反过来）都是不允许的

# 9.count

hadoop fs -count < hdfs path >

统计hdfs对应路径下的目录个数，文件个数，文件总计大小

显示为目录个数，文件个数，文件总计大小，输入路径

# 10.du

hadoop fs -du < hdsf path>

显示hdfs对应路径下每个文件夹和文件的大小

hadoop fs -du -s < hdsf path>

显示hdfs对应路径下所有文件和的大小

hadoop fs -du - h < hdsf path>

显示hdfs对应路径下每个文件夹和文件的大小,文件的大小用方便阅读的形式表示，例如用64M代替67108864

# 11.text

hadoop fs -text < hdsf file>

将文本文件或某些格式的非文本文件通过文本格式输出

# 12.setrep

hadoop fs -setrep -R 3 < hdfs path >

改变一个文件在hdfs中的副本个数，上述命令中数字3为所设置的副本个数，-R选项可以对一个人目录下的所有目录+文件递归执行改变副本个数的操作

# 13.stat

hdoop fs -stat [format] < hdfs path >

返回对应路径的状态信息

[format]可选参数有：%b（文件大小），%o（Block大小），%n（文件名），%r（副本个数），%y（最后一次修改日期和时间）

可以这样书写hadoop fs -stat %b%o%n < hdfs path >，不过不建议，这样每个字符输出的结果不是太容易分清楚

# 14.tail

hadoop fs -tail < hdfs file >

在标准输出中显示文件末尾的1KB数据

# 15.archive

hadoop archive -archiveName name.har -p < hdfs parent dir > < src >* < hdfs dst >

命令中参数name：压缩文件名，自己任意取；< hdfs parent dir > ：压缩文件所在的父目录；< src >：要压缩的文件名；< hdfs dst >：压缩文件存放路径

*示例：hadoop archive -archiveName hadoop.har -p /user 1.txt 2.txt /des

示例中将hdfs中/user目录下的文件1.txt，2.txt压缩成一个名叫hadoop.har的文件存放在hdfs中/des目录下，如果1.txt，2.txt不写就是将/user目录下所有的目录和文件压缩成一个名叫hadoop.har的文件存放在hdfs中/des目录下

显示har的内容可以用如下命令：

hadoop fs -ls /des/hadoop.jar

显示har压缩的是那些文件可以用如下命令

hadoop fs -ls -R har:///des/hadoop.har

注意：har文件不能进行二次压缩。如果想给.har加文件，只能找到原来的文件，重新创建一个。har文件中原来文件的数据并没有变化，har文件真正的作用是减少NameNode和DataNode过多的空间浪费。

# 16.balancer

hdfs balancer如果管理员发现某些DataNode保存数据过多，某些DataNode保存数据相对较少，可以使用上述命令手动启动内部的均衡过程

# 17.dfsadmin

hdfs dfsadmin -help

管理员可以通过dfsadmin管理HDFS，用法可以通过上述命令查看

hdfs dfsadmin -report

显示文件系统的基本数据

hdfs dfsadmin -safemode < enter | leave | get | wait >

enter：进入安全模式；leave：离开安全模式；get：获知是否开启安全模式；

wait：等待离开安全模式

# 18.distcp

用来在两个HDFS之间拷贝数据












# HDFS 常用 shell 命令

**1. 显示当前目录结构**

```shell
# 显示当前目录结构
hadoop fs -ls  <path>
# 递归显示当前目录结构
hadoop fs -ls  -R  <path>
# 显示根目录下内容
hadoop fs -ls  /
```

**2. 创建目录**

```shell
# 创建目录
hadoop fs -mkdir  <path> 
# 递归创建目录
hadoop fs -mkdir -p  <path>  
```

**3. 删除操作**

```shell
# 删除文件
hadoop fs -rm  <path>
# 递归删除目录和文件
hadoop fs -rm -R  <path> 
```

**4. 从本地加载文件到 HDFS**

```shell
# 二选一执行即可
hadoop fs -put  [localsrc] [dst] 
hadoop fs - copyFromLocal [localsrc] [dst] 
```


**5. 从 HDFS 导出文件到本地**

```shell
# 二选一执行即可
hadoop fs -get  [dst] [localsrc] 
hadoop fs -copyToLocal [dst] [localsrc] 
```

**6. 查看文件内容**

```shell
# 二选一执行即可
hadoop fs -text  <path> 
hadoop fs -cat  <path>  
```

**7. 显示文件的最后一千字节**

```shell
hadoop fs -tail  <path> 
# 和Linux下一样，会持续监听文件内容变化 并显示文件的最后一千字节
hadoop fs -tail -f  <path> 
```

**8. 拷贝文件**

```shell
hadoop fs -cp [src] [dst]
```

**9. 移动文件**

```shell
hadoop fs -mv [src] [dst] 
```


**10. 统计当前目录下各文件大小**  
+ 默认单位字节  
+ -s : 显示所有文件大小总和，
+ -h : 将以更友好的方式显示文件大小（例如 64.0m 而不是 67108864）
```shell
hadoop fs -du  <path>  
```

**11. 合并下载多个文件**
+ -nl  在每个文件的末尾添加换行符（LF）
+ -skip-empty-file 跳过空文件

```shell
hadoop fs -getmerge
# 示例 将HDFS上的hbase-policy.xml和hbase-site.xml文件合并后下载到本地的/usr/test.xml
hadoop fs -getmerge -nl  /test/hbase-policy.xml /test/hbase-site.xml /usr/test.xml
```

**12. 统计文件系统的可用空间信息**

```shell
hadoop fs -df -h /
```

**13. 更改文件复制因子**
```shell
hadoop fs -setrep [-R] [-w] <numReplicas> <path>
```
+ 更改文件的复制因子。如果 path 是目录，则更改其下所有文件的复制因子
+ -w : 请求命令是否等待复制完成

```shell
# 示例
hadoop fs -setrep -w 3 /user/hadoop/dir1
```

**14. 权限控制**  
```shell
# 权限控制和Linux上使用方式一致
# 变更文件或目录的所属群组。 用户必须是文件的所有者或超级用户。
hadoop fs -chgrp [-R] GROUP URI [URI ...]
# 修改文件或目录的访问权限  用户必须是文件的所有者或超级用户。
hadoop fs -chmod [-R] <MODE[,MODE]... | OCTALMODE> URI [URI ...]
# 修改文件的拥有者  用户必须是超级用户。
hadoop fs -chown [-R] [OWNER][:[GROUP]] URI [URI ]
```

**15. 文件检测**
```shell
hadoop fs -test - [defsz]  URI
```
可选选项：
+ -d：如果路径是目录，返回 0。
+ -e：如果路径存在，则返回 0。
+ -f：如果路径是文件，则返回 0。
+ -s：如果路径不为空，则返回 0。
+ -r：如果路径存在且授予读权限，则返回 0。
+ -w：如果路径存在且授予写入权限，则返回 0。
+ -z：如果文件长度为零，则返回 0。

```shell
# 示例
hadoop fs -test -e filename
```
