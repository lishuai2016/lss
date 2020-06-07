

```
第一章：初识 Netty：背景、现状与趋势 (7讲)

01 | 课程介绍
02 | 内容综述
03 | 揭开Netty面纱
04 | 为什么舍近求远：不直接用JDK NIO？
05 | 为什么孤注一掷：独选Netty？
06 | Netty的前尘往事
07 | Netty的现状与趋势

第二章：Netty源码：从“点”（领域知识）的角度剖析 (9讲)

08 | Netty怎么切换三种I/O模式？
09 | 源码剖析：Netty对I/O模式的支持
10 | Netty如何支持三种Reactor？
11 | 源码剖析：Netty对Reactor的支持
12 | TCP粘包/半包Netty全搞定
13 | 源码剖析：Netty对处理粘包/半包的支持
14 | 常用的“二次”编解码方式
15 | 源码剖析：Netty对常用编解码的支持
16 | keepalive与idle监测

```

<!-- TOC -->

- [01 | 课程介绍](#01--课程介绍)
- [02 | 内容综述](#02--内容综述)
- [03 | 揭开Netty面纱](#03--揭开netty面纱)
- [04 | 为什么舍近求远：不直接用JDK NIO？](#04--为什么舍近求远不直接用jdk-nio)
- [05 | 为什么孤注一掷：独选Netty？](#05--为什么孤注一掷独选netty)
- [06 | Netty的前尘往事](#06--netty的前尘往事)
- [07 | Netty的现状与趋势](#07--netty的现状与趋势)
- [08 | Netty怎么切换三种I/O模式？](#08--netty怎么切换三种io模式)
- [09 | 源码剖析：Netty对I/O模式的支持](#09--源码剖析netty对io模式的支持)
- [10 | Netty如何支持三种Reactor？](#10--netty如何支持三种reactor)
- [11 | 源码剖析：Netty对Reactor的支持](#11--源码剖析netty对reactor的支持)
- [12 | TCP粘包/半包Netty全搞定](#12--tcp粘包半包netty全搞定)
- [13 | 源码剖析：Netty对处理粘包/半包的支持](#13--源码剖析netty对处理粘包半包的支持)
- [14 | 常用的“二次”编解码方式](#14--常用的二次编解码方式)
- [15 | 源码剖析：Netty对常用编解码的支持](#15--源码剖析netty对常用编解码的支持)
- [16 | keepalive与idle监测](#16--keepalive与idle监测)
- [17 Netty 的那些“锁”事](#17-netty-的那些锁事)
- [18 Netty 如何玩转内存使用](#18-netty-如何玩转内存使用)
- [19 | Netty如何玩转内存使用](#19--netty如何玩转内存使用)
- [20 | 源码解析：Netty对堆外内存和内存池的支持](#20--源码解析netty对堆外内存和内存池的支持)
- [21 | Netty代码编译与总览](#21--netty代码编译与总览)
- [22 | 源码剖析：启动服务](#22--源码剖析启动服务)
- [23 | 源码剖析：构建连接](#23--源码剖析构建连接)
- [24 | 源码剖析：接收数据](#24--源码剖析接收数据)
- [25 | 源码剖析：业务处理](#25--源码剖析业务处理)
- [26 | 源码剖析：发送数据](#26--源码剖析发送数据)
- [27 | 源码剖析：断开连接](#27--源码剖析断开连接)
- [28 | 源码剖析：关闭服务](#28--源码剖析关闭服务)
- [29 | 编写网络应用程序的基本步骤](#29--编写网络应用程序的基本步骤)
- [30 | 案例介绍和数据结构设计](#30--案例介绍和数据结构设计)
- [31 | 实现服务器端编解码](#31--实现服务器端编解码)
- [32 | 实现一个服务器端](#32--实现一个服务器端)
- [33 | 实现客户端编解码](#33--实现客户端编解码)
- [34 | 完成一个客户端雏形](#34--完成一个客户端雏形)
- [35 | 引入"响应分发"完善客户端](#35--引入响应分发完善客户端)
- [36 | Netty编码中易错点解析](#36--netty编码中易错点解析)
- [37 | 调优参数：调整System参数夯实基础](#37--调优参数调整system参数夯实基础)
- [38 | 调优参数：权衡Netty核心参数](#38--调优参数权衡netty核心参数)
- [39 | 调优参数：图解费脑的三个参数](#39--调优参数图解费脑的三个参数)
- [40 | 跟踪诊断：如何让应用易诊断？](#40--跟踪诊断如何让应用易诊断)
- [41 | 跟踪诊断：应用能可视，心里才有底](#41--跟踪诊断应用能可视心里才有底)
- [42 | 跟踪诊断：让应用内存不“泄露”？](#42--跟踪诊断让应用内存不泄露)
- [43 | 优化使用：用好自带注解省点心](#43--优化使用用好自带注解省点心)
- [44 | 优化使用：“整改”线程模型让"响应"健步如飞](#44--优化使用整改线程模型让响应健步如飞)
- [45 | 优化使用：增强写，延迟与吞吐量的抉择](#45--优化使用增强写延迟与吞吐量的抉择)
- [46 | 优化使用：如何让应用丝般“平滑”？](#46--优化使用如何让应用丝般平滑)
- [47 | 优化使用：为不同平台开启native](#47--优化使用为不同平台开启native)
- [48 | 安全增强：设置“高低水位线”等保护好自己](#48--安全增强设置高低水位线等保护好自己)
- [49 | 安全增强：启用空闲监测](#49--安全增强启用空闲监测)
- [50 | 安全增强：简单有效的黑白名单](#50--安全增强简单有效的黑白名单)
- [51 | 安全增强：少不了的自定义授权](#51--安全增强少不了的自定义授权)
- [52 | 安全增强：拿来即用的SSL-对话呈现表象](#52--安全增强拿来即用的ssl-对话呈现表象)
- [53 | 安全增强：拿来即用的SSL-抓包暴露本质](#53--安全增强拿来即用的ssl-抓包暴露本质)
- [54 | 安全增强：拿来即用的SSL-轻松融入案例](#54--安全增强拿来即用的ssl-轻松融入案例)
- [55 | Cassandra如何使用Netty ？](#55--cassandra如何使用netty-)
- [56 | Dubbo如何使用Netty ？](#56--dubbo如何使用netty-)
- [57 | Hadoop如何使用Netty ？](#57--hadoop如何使用netty-)
- [58 | 赏析Netty之美](#58--赏析netty之美)
- [59 | 如何给Netty贡献代码？](#59--如何给netty贡献代码)
- [60 | 课程回顾与总结](#60--课程回顾与总结)

<!-- /TOC -->



[netty4官方文档](https://netty.io/wiki/user-guide-for-4.x.html)

第一章：初识Netty：背景、现状与趋势 (7讲)

# 01 | 课程介绍
# 02 | 内容综述
# 03 | 揭开Netty面纱
# 04 | 为什么舍近求远：不直接用JDK NIO？
# 05 | 为什么孤注一掷：独选Netty？
# 06 | Netty的前尘往事
# 07 | Netty的现状与趋势

> 1、为什么不直接使用jdk的nio?

![](../../pic/2019-10-18-22-32-40.png)

![](../../pic/2019-10-18-22-33-01.png)

![](../../pic/2019-10-18-22-33-13.png)

![](../../pic/2019-10-18-22-33-45.png)

![](../../pic/2019-10-18-22-34-14.png)

![](../../pic/2019-10-18-22-34-38.png)

![](../../pic/2019-10-18-22-35-02.png)

![](../../pic/2019-10-18-22-35-14.png)

![](../../pic/2019-10-18-22-35-38.png)

![](../../pic/2019-10-18-22-35-55.png)

![](../../pic/2019-10-18-22-36-14.png)

![](../../pic/2019-10-18-22-36-26.png)


> 2、为什么独选netty？

![](../../pic/2019-10-18-22-38-09.png)

![](../../pic/2019-10-18-22-38-27.png)

![](../../pic/2019-10-18-22-38-44.png)

![](../../pic/2019-10-18-22-38-59.png)


> 3、netty的版本历史？和netty的现状

![](../../pic/2019-10-18-22-40-10.png)

![](../../pic/2019-10-18-22-40-28.png)

![](../../pic/2019-10-18-22-40-45.png)

![](../../pic/2019-10-18-22-40-59.png)

![](../../pic/2019-10-18-22-41-10.png)

![](../../pic/2019-10-18-22-41-22.png)

![](../../pic/2019-10-18-22-41-38.png)

最新版本

- Netty 4.1.39.Final （2019 年8 月）
- Netty 4.0.56.Final （2018 年2 月）
- Netty 3.10.6.Final （2016 年6 月）

应用现状：

- 截止2019 年9 月，30000+ 项目在使用
- 统计方法：依赖项中声明io.netty:netty-all
- 未考虑的情况：非开源软件和Netty 3.x 使用者

你以为没有Netty 的地方其实大多都有！



一些典型项目：

- 数据库： Cassandra
- 大数据处理： Spark、Hadoop
- Message Queue：RocketMQ

![](../../pic/2019-10-18-22-44-03.png)

![](../../pic/2019-10-18-22-44-22.png)



第二章：Netty源码：从“点”（领域知识）的角度剖析 (13讲)

# 08 | Netty怎么切换三种I/O模式？

- 什么是经典的三种I/O 模式
- Netty 对三种I/O 模式的支持
- 为什么Netty 仅支持NIO 了？
- 为什么Netty 有多种NIO 实现？
- NIO 一定优于BIO 么？
- 源码解读Netty 怎么切换I/O 模式？


> 什么是经典的三种I/O 模式

![](../../pic/2019-10-18-22-54-54.png)

![](../../pic/2019-10-18-22-55-19.png)


```
阻塞与非阻塞
• 菜没好，要不要死等-> 数据就绪前要不要等待？
• 阻塞：没有数据传过来时，读会阻塞直到有数据；缓冲区满时，写操作也会阻塞。
非阻塞遇到这些情况，都是直接返回。
• 同步与异步
• 菜好了，谁端-> 数据就绪后，数据操作谁完成？
• 数据就绪后需要自己去读是同步，数据就绪直接读好再回调给程序是异步。

```



> Netty 对三种I/O 模式的支持

![](../../pic/2019-10-18-22-56-42.png)


> 为什么Netty 仅支持NIO 了？

```
1、为什么不建议（deprecate）阻塞I/O（BIO/OIO）?
连接数高的情况下：阻塞-> 耗资源、效率低

2、为什么删掉已经做好的AIO 支持？
• Windows 实现成熟，但是很少用来做服务器。
• Linux 常用来做服务器，但是AIO 实现不够成熟。
• Linux 下AIO 相比较NIO 的性能提升不明显。
```


> 为什么Netty 有多种NIO 实现？

![](../../pic/2019-10-18-22-58-23.png)

```
通用的NIO 实现（Common）在Linux 下也是使用epoll，为什么自己单独实现？
实现得更好！
• Netty 暴露了更多的可控参数，例如：
1、JDK 的NIO 默认实现是水平触发
2、Netty 是边缘触发（默认）和水平触发可切换
• Netty 实现的垃圾回收更少、性能更好

```


> NIO 一定优于BIO 么？

• BIO 代码简单。

• 特定场景：连接数少，并发度低，BIO 性能不输NIO。


> 源码解读Netty 怎么切换I/O 模式？

```
• 怎么切换？
• 原理是什么？
• 为什么服务器开发并不需要切换客户端对应Socket ？
```
![](../../pic/2019-10-18-23-00-52.png)



# 09 | 源码剖析：Netty对I/O模式的支持
# 10 | Netty如何支持三种Reactor？

-  什么是Reactor 及三种版本

```
生活场景：饭店规模变化
• 一个人包揽所有：迎宾、点菜、做饭、上菜、送客等；
• 多招几个伙计：大家一起做上面的事情；
• 进一步分工：搞一个或者多个人专门做迎宾。
```
![](../../pic/2019-10-18-23-17-57.png)

![](../../pic/2019-10-18-23-18-26.png)


Thread-Per-Connection 模式

![](../../pic/2019-10-18-23-18-41.png)

![](../../pic/2019-10-18-23-19-16.png)


Reactor 模式V1：单线程

![](../../pic/2019-10-18-23-19-44.png)

Reactor 模式V2：多线程

![](../../pic/2019-10-18-23-20-08.png)

Reactor 模式V3：主从多线程

![](../../pic/2019-10-18-23-20-33.png)


-  如何在Netty 中使用Reactor 模式

![](../../pic/2019-10-18-23-21-24.png)



-  解析Netty 对Reactor 模式支持的常见疑问




# 11 | 源码剖析：Netty对Reactor的支持
# 12 | TCP粘包/半包Netty全搞定
# 13 | 源码剖析：Netty对处理粘包/半包的支持
# 14 | 常用的“二次”编解码方式
# 15 | 源码剖析：Netty对常用编解码的支持
# 16 | keepalive与idle监测

# 17 Netty 的那些“锁”事
# 18 Netty 如何玩转内存使用

# 19 | Netty如何玩转内存使用
# 20 | 源码解析：Netty对堆外内存和内存池的支持

第三章：Netty源码：从“线”（请求处理）的角度剖析 (8讲)

# 21 | Netty代码编译与总览
# 22 | 源码剖析：启动服务

![](../../pic/2020-01-05-16-52-32.png)

```
启动服务的本质：
Selector selector = sun.nio.ch.SelectorProviderImpl.openSelector()
ServerSocketChannel serverSocketChannel = provider.openServerSocketChannel()
selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
javaChannel().bind(localAddress, config.getBacklog());
selectionKey.interestOps(OP_ACCEPT);




• Selector 是在new NioEventLoopGroup()（创建一批NioEventLoop）时创建。
• 第一次Register 并不是监听OP_ACCEPT，而是0:
selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this) 。
• 最终监听OP_ACCEPT 是通过bind 完成后的fireChannelActive() 来触发的。
• NioEventLoop 是通过Register 操作的执行来完成启动的。
• 类似ChannelInitializer，一些Hander 可以设计成一次性的，用完就移除，例如授权。

```




# 23 | 源码剖析：构建连接

![](../../pic/2020-01-05-16-54-49.png)

```
接受连接本质：
selector.select()/selectNow()/select(timeoutMillis) 发现OP_ACCEPT 事件，处理：
• SocketChannel socketChannel = serverSocketChannel.accept()
• selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
• selectionKey.interestOps(OP_READ);



• 创建连接的初始化和注册是通过pipeline.fireChannelRead 在ServerBootstrapAcceptor 中完成的。
• 第一次Register 并不是监听OP_READ ，而是0 ：
selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this) 。
• 最终监听OP_READ 是通过“Register”完成后的fireChannelActive
（io.netty.channel.AbstractChannel.AbstractUnsafe#register0中）来触发的
• Worker’s NioEventLoop 是通过Register 操作执行来启动。
• 接受连接的读操作，不会尝试读取更多次（16次）。
```



# 24 | 源码剖析：接收数据

```
读数据技巧

1 自适应数据大小的分配器（AdaptiveRecvByteBufAllocator）：
发放东西时，拿多大的桶去装？小了不够，大了浪费，所以会自己根据实际装的情况猜一猜下次情况，
从而决定下次带多大的桶。

2 连续读（defaultMaxMessagesPerRead）：
发放东西时，假设拿的桶装满了，这个时候，你会觉得可能还有东西发放，所以直接拿个新桶等着装，
而不是回家，直到后面出现没有装上的情况或者装了很多次需要给别人一点机会等原因才停止，回家。

```

![](../../pic/2020-01-05-17-15-43.png)


```

• 读取数据本质：sun.nio.ch.SocketChannelImpl#read(java.nio.ByteBuffer)
• NioSocketChannel read() 是读数据， NioServerSocketChannel read() 是创建连接
• pipeline.fireChannelReadComplete(); 一次读事件处理完成
pipeline.fireChannelRead(byteBuf); 一次读数据完成，一次读事件处理可能会包含多次读数据操作。
• 为什么最多只尝试读取16 次？“雨露均沾”
• AdaptiveRecvByteBufAllocator 对bytebuf 的猜测：放大果断，缩小谨慎（需要连续2 次判断）
```


# 25 | 源码剖析：业务处理

![](../../pic/2020-01-05-17-17-11.png)

![](../../pic/2020-01-05-17-18-02.png)

```
• 处理业务本质：数据在pipeline 中所有的handler 的channelRead() 执行过程
Handler 要实现io.netty.channel.ChannelInboundHandler#channelRead (ChannelHandlerContext ctx,
Object msg)，且不能加注解@Skip 才能被执行到。
中途可退出，不保证执行到Tail Handler。
• 默认处理线程就是Channel 绑定的NioEventLoop 线程，也可以设置其他：
pipeline.addLast(new UnorderedThreadPoolEventExecutor(10), serverHandler)
```


# 26 | 源码剖析：发送数据

![](../../pic/2020-01-05-17-19-16.png)


```
写数据要点

1 对方仓库爆仓时，送不了的时候，会停止送，协商等电话通知什么时候好了，再送。
Netty 写数据，写不进去时，会停止写，然后注册一个OP_WRITE 事件，来通知什么时候可以写进去了再写。

2 发送快递时，对方仓库都直接收下，这个时候再发送快递时，可以尝试发送更多的快递试试，这样效果更好。
Netty 批量写数据时，如果想写的都写进去了，接下来的尝试写更多（调整maxBytesPerGatheringWrite）。

3 发送快递时，发到某个地方的快递特别多，我们会连续发，但是快递车毕竟有限，也会考虑下其他地方。
Netty 只要有数据要写，且能写的出去，则一直尝试，直到写不出去或者满16 次（writeSpinCount）。

4 揽收太多，发送来不及时，爆仓，这个时候会出个告示牌：收不下了，最好过2 天再来邮寄吧。
Netty 待写数据太多，超过一定的水位线（writeBufferWaterMark.high()），会将可写的标志位改成
false ，让应用端自己做决定要不要发送数据了。



• 写的本质：
• Single write: sun.nio.ch.SocketChannelImpl#write(java.nio.ByteBuffer)
• gathering write：sun.nio.ch.SocketChannelImpl#write(java.nio.ByteBuffer[], int, int)
• 写数据写不进去时，会停止写，注册一个OP_WRITE 事件，来通知什么时候可以写进去了。
• OP_WRITE 不是说有数据可写，而是说可以写进去，所以正常情况，不能注册，否则一直触发。




• 批量写数据时，如果尝试写的都写进去了，接下来会尝试写更多（maxBytesPerGatheringWrite）。
• 只要有数据要写，且能写，则一直尝试，直到16 次（writeSpinCount），写16 次还没有写完，就直
接schedule 一个task 来继续写，而不是用注册写事件来触发，更简洁有力。
• 待写数据太多，超过一定的水位线（writeBufferWaterMark.high()），会将可写的标志位改成false ，
让应用端自己做决定要不要继续写。
• channelHandlerContext.channel().write() ：从TailContext 开始执行；
channelHandlerContext.write() : 从当前的Context 开始。

```
![](../../pic/2020-01-05-17-21-11.png)






# 27 | 源码剖析：断开连接

```
• 多路复用器（Selector）接收到OP_READ 事件:
• 处理OP_READ 事件：NioSocketChannel.NioSocketChannelUnsafe.read()：
• 接受数据
• 判断接受的数据大小是否< 0 , 如果是，说明是关闭，开始执行关闭：
• 关闭channel（包含cancel 多路复用器的key）。
• 清理消息：不接受新信息，fail 掉所有queue 中消息。
• 触发fireChannelInactive 和fireChannelUnregistered 。



• 关闭连接本质：
• java.nio.channels.spi.AbstractInterruptibleChannel#close
• java.nio.channels.SelectionKey#cancel
• 要点：
• 关闭连接，会触发OP_READ 方法。读取字节数是-1 代表关闭。
• 数据读取进行时，强行关闭，触发IO Exception，进而执行关闭。
• Channel 的关闭包含了SelectionKey 的cancel 。

```

# 28 | 源码剖析：关闭服务

![](../../pic/2020-01-05-17-23-27.png)

```
• 关闭服务本质：
• 关闭所有连接及Selector ：
• java.nio.channels.Selector#keys
• java.nio.channels.spi.AbstractInterruptibleChannel#close
• java.nio.channels.SelectionKey#cancel
• selector.close()
• 关闭所有线程：退出循环体for (;;)



• 关闭服务要点：
• 优雅（DEFAULT_SHUTDOWN_QUIET_PERIOD）
• 可控（DEFAULT_SHUTDOWN_TIMEOUT）
• 先不接活，后尽量干完手头的活（先关boss 后关worker：不是100%保证）

```




第四章：Netty实战入门：写一个“玩具”项目 (8讲)

# 29 | 编写网络应用程序的基本步骤

![](../../pic/2020-01-04-11-10-19.png)

![](../../pic/2020-01-04-11-11-04.png)

# 30 | 案例介绍和数据结构设计

![](../../pic/2020-01-04-11-11-37.png)

![](../../pic/2020-01-04-11-12-07.png)

![](../../pic/2020-01-04-11-12-32.png)

![](../../pic/2020-01-04-11-13-13.png)

![](../../pic/2020-01-04-11-13-49.png)




# 31 | 实现服务器端编解码
# 32 | 实现一个服务器端
# 33 | 实现客户端编解码
# 34 | 完成一个客户端雏形
# 35 | 引入"响应分发"完善客户端
# 36 | Netty编码中易错点解析



第五章：Netty实战进阶：把“玩具”变成产品 (18讲)


# 37 | 调优参数：调整System参数夯实基础

```
1、Linux 系统参数
例如：/proc/sys/net/ipv4/tcp_keepalive_time

2、Netty 支持的系统参数：
例如：serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);
• SocketChannel -> .childOption
• ServerSocketChannel -> .option
```


```
Linux 系统参数
• 进行TCP 连接时，系统为每个TCP 连接创建一个socket 句柄，也就是一个文件句柄，但是Linux
对每个进程打开的文件句柄数量做了限制，如果超出：报错“Too many open file”。
ulimit -n [xxx]
注意：ulimit 命令修改的数值只对当前登录用户的目前使用环境有效，系统重启或者用户退出后就会失效，所以可以作为程序启动脚本一部分，让它程序启动前执行。

```


```
Netty 支持的系统参数(ChannelOption.[XXX] ) 讨论：
• 不考虑UDP :
    • IP_MULTICAST_TTL
• 不考虑OIO 编程：
    • ChannelOption<Integer> SO_TIMEOUT = ("SO_TIMEOUT");
```

![](../../pic/2020-01-05-02-26-13.png)

![](../../pic/2020-01-05-02-26-35.png)


```
参数调整要点：
    • option/childOption 傻傻分不清：不会报错，但是会不生效；
    • 不懂不要动，避免过早优化。
    • 可配置（动态配置更好）

• 需要调整的参数：
    • 最大打开文件数
    • TCP_NODELAY SO_BACKLOG SO_REUSEADDR（酌情处理）

• ChannelOption
    • childOption(ChannelOption.[XXX], [YYY])
    • option(ChannelOption.[XXX], [YYY])

• System property
    • -Dio.netty.[XXX] = [YYY]
```

![](../../pic/2020-01-05-02-29-23.png)

![](../../pic/2020-01-05-02-30-00.png)


```
• System property (-Dio.netty.xxx，50+ ）
    • 多种实现的切换：-Dio.netty.noJdkZlibDecoder
    • 参数的调优： -Dio.netty.eventLoopThreads
    • 功能的开启关闭： -Dio.netty.noKeySetOptimization

```

![](../../pic/2020-01-05-02-30-38.png)

```

• 补充说明
• 一些其他重要的参数：
    • NioEventLoopGroup workerGroup = new NioEventLoopGroup();workerGroup.setIoRatio(50);

• 注意参数的关联
    • 临时存放native 库的目录： -> io.netty.native.workdir > io.netty.tmpdir

• 注意参数的变更
    • io.netty.noResourceLeakDetection -> io.netty.leakDetection.level
```

```
• SO_REUSEADDR
• SO_LINGER
• ALLOW_HALF_CLOSURE

```

![](../../pic/2020-01-05-02-32-35.png)

![](../../pic/2020-01-05-02-32-53.png)

![](../../pic/2020-01-05-02-33-05.png)



```
跟踪诊断：如何让应用易诊断
• 完善“线程名”
• 完善“Handler ”名称
• 使用好Netty 的日志


• Netty 日志的原理及使用
    • Netty 日志框架原理
    • 修改JDK logger 级别
    • 使用slf4j + log4j 示例
    • 衡量好logging handler 的位置和级别

```


```
跟踪诊断：应用能可视，心里才有底
• Netty 可视化案例演示：
• 实现一个小目标：统计并展示当前系统连接数
• Console 日志定时输出
• JMX 实时展示
• ELKK、TIG、etc

```

![](../../pic/2020-01-05-02-35-12.png)

![](../../pic/2020-01-05-02-35-25.png)

![](../../pic/2020-01-05-02-35-42.png)


```
跟踪诊断：让应用内存不“泄露”？
• 本节的Netty 内存泄漏是指什么？
• Netty 内存泄漏检测核心思路
• Netty 内存泄漏检测的源码解析
• 示例：用Netty 内存泄漏检测工具做检测



本节的Netty 内存泄漏是指什么？
• 原因：“忘记”release
ByteBuf buffer = ctx.alloc().buffer();
buffer.release() / ReferenceCountUtil.release(buffer)
• 后果：资源未释放-> OOM
• 堆外：未free（PlatformDependent.freeDirectBuffer(buffer)）；
• 池化：未归还（recyclerHandle.recycle(this)）


Netty 内存泄漏检测核心思路：引用计数（buffer.refCnt()）+ 弱引用（Weak reference）
• 引用计数
• 判断历史人物到底功大于过，还是过大于功？
功+1， 过-1， = 0 时：尘归尘，土归土，资源也该释放了
• 那什么时候判断？“盖棺定论”时-> 对象被GC 后



• 强引用与弱引用
• String 我是战斗英雄型强保镖= new String(我是主人));
• WeakReference<String> 我是爱写作的弱保镖= new WeakReference<String>(new String(我是主人));
只有一个爱写作的保镖（弱引用）守护（引用）时：刺客（GC）来袭，主人（referent）必挂（GC掉）。
不过主人挂掉的（被GC 掉）时候，我还是可以发挥写作特长：把我自己记到“小本本”上去。
• WeakReference<String> 我是爱写作的弱保镖= new WeakReference<String>(new String(我是主人)，
我的小本本ReferenceQueue);

```
![](../../pic/2020-01-05-02-37-28.png)


[ResourceLeakDetector核心检测类]

```
Netty 内存泄漏检测的源码解析
• 全样本？抽样？： PlatformDependent.threadLocalRandom().nextInt(samplingInterval)
• 记录访问信息：new Record() : record extends Throwable
• 级别/开关：io.netty.util.ResourceLeakDetector.Level
• 信息呈现：logger.error
• 触发汇报时机： AbstractByteBufAllocator#buffer() ：io.netty.util.ResourceLeakDetector#track0



• 用Netty 内存泄漏检测工具做检测
• 方法：-Dio.netty.leakDetection.level=PARANOID
• 注意：
• 默认级别SIMPLE，不是每次都检测
• GC 后，才有可能检测到
• 注意日志级别
• 上线前用最高级别，上线后用默认
```

```
优化使用：用好自带注解省点心
• @Sharable
• @Skip
• @UnstableApi
• @SuppressJava6Requirement
• @SuppressForbidden




• @Sharable： 标识handler 提醒可共享，不标记共享的不能重复加入pipeline
• @Skip： 跳过handler 的执行
• @UnstableApi：提醒不稳定，慎用

```




```
优化使用：整改线程模型让响应健步如飞
• 业务的常用两种场景：
• CPU 密集型：运算型
• IO 密集型：等待型



• CPU 密集型
• 保持当前线程模型：
• Runtime.getRuntime().availableProcessors() * 2
• io.netty.availableProcessors * 2
• io.netty.eventLoopThreads




• IO 密集型
• 整改线程模型：独立出“线程池”来处理业务
• 在handler 内部使用JDK Executors
• 添加handler 时，指定1个：
EventExecutorGroup eventExecutorGroup = new UnorderedThreadPoolEventExecutor(10);
pipeline.addLast(eventExecutorGroup, serverHandler)
为什么案例中不用new NioEventLoopGroup()？

只会使用一个线程进行处理。
```


```
优化使用：增强写，延迟与吞吐量的抉择
• 写的“问题”
• 改进方式1：channelReadComplete
• 改进方式2：flushConsolidationHandler




优化使用：增强写，延迟与吞吐量的抉择
• 改进方式1：利用channelReadComplete
• 缺点：
• 不适合异步业务线程(不复用NIO event loop) 处理：
channelRead 中的业务处理结果的write 很可能发生在channelReadComplete 之
后
• 不适合更精细的控制：例如连读16 次时，第16 次是flush，但是如果保持连续的次
数不变，如何做到3 次就flush?






• 改进方式2： flushConsolidationHandler
• 源码分析
• 使用

```

同步：read -->writeAndFlash-->readComplete

异步：read -->readComplete-->writeAndFlash

```
优化使用：如何让应用丝般“平滑”
• 流量整形的用途
• Netty 内置的三种流量整形
• Netty 流量整形的源码分析与总结
• 示例：流量整形的使用





优化使用：如何让应用丝般“平滑”
• Netty 流量整形的源码分析与总结
• 读写流控判断：按一定时间段checkInterval （1s） 来统计。writeLimit/readLimit 设置的值为
0时，表示关闭写整形/读整形
• 等待时间范围控制：10ms （MINIMAL_WAIT） -> 15s （maxTime）
• 读流控：取消读事件监听，让读缓存区满，然后对端写缓存区满，然后对端写不进去，对端对数
据进行丢弃或减缓发送。
• 写流控：待发数据入Queue。等待超过4s (maxWriteDelay) || 单个channel 缓存的数据超过
4M(maxWriteSize) || 所有缓存数据超过400M (maxGlobalWriteSize)时修改写状态为不可写。
```


![](../../pic/2020-01-05-11-15-53.png)


![](../../pic/2020-01-05-11-24-20.png)





```
优化使用: 为不同平台开启Native
• 如何开启Native
• 源码分析Native 库的加载逻辑
• 常见问题



• 如何开启Native
• 修改代码
• NioServerSocketChannel -> [Prefix]ServerSocketChannel
• NioEventLoopGroup -> [Prefix]EventLoopGroup
• 准备好native 库：自己build/Netty jar也自带了一些

```




```
安全增强: 设置“高低水位线”等保护好自己
• Netty OOM 的根本原因
• Netty OOM – ChannelOutboundBuffer
• Netty OOM – TrafficShapingHandler
• Netty OOM 的对策





• Netty OOM 的根本原因：
• 根源：进（读速度）大于出（写速度）
• 表象：
    • 上游发送太快：任务重
    • 自己：处理慢/不发或发的慢：处理能力有限，流量控制等原因
    • 网速：卡
    • 下游处理速度慢：导致不及时读取接受Buffer 数据，然后反馈到这边，发送速度降速




• Netty OOM – ChannelOutboundBuffer 原因
• 存的对象：Linked list 存ChannelOutboundBuffer.Entry
• 解决方式：判断totalPendingSize > writeBufferWaterMark.high() 设置unwritable
• ChannelOutboundBuffer:

```

![](../../pic/2020-01-05-11-39-38.png)



```
安全增强: 启用空闲监测
• 示例：实现一个小目标
• 服务器加上read idle check – 服务器10s 接受不到channel 的请求就断掉连接
• 保护自己、瘦身（及时清理空闲的连接）
• 客户端加上write idle check + keepalive – 客户端5s 不发送数据就发一个keepalive
• 避免连接被断
• 启用不频繁的keepalive

```


```
安全增强：简单有效的黑白名单
• Netty 中的“cidrPrefix” 是什么？
• Netty 地址过滤功能源码分析
• 示例：使用黑名单增强安全




• Netty 地址过滤功能源码分析
• 同一个IP 只能有一个连接
• IP 地址过滤：黑名单、白名单

```

![](../../pic/2020-01-05-12-04-00.png)







```
安全增强: 拿来即用的SSL - 对话呈现表象
• 什么是SSL ?
• 一段聊天记录揭示SSL 的功能与设计
Note: 本节示例基于“单向验证+ 交换秘钥方式为RSA 方式”

```

![](../../pic/2020-01-05-12-17-15.png)

![](../../pic/2020-01-05-12-18-17.png)

![](../../pic/2020-01-05-12-19-52.png)

![](../../pic/2020-01-05-12-20-23.png)

![](../../pic/2020-01-05-12-20-58.png)

![](../../pic/2020-01-05-12-21-45.png)

![](../../pic/2020-01-05-12-22-57.png)

![](../../pic/2020-01-05-12-23-15.png)

![](../../pic/2020-01-05-12-23-27.png)





抓包源码[工具wireshark]

![](../../pic/2020-01-05-12-24-22.png)




```
安全增强: 拿来即用的SSL - 轻松融入案例
• 在Netty 中使用SSL：单向认证
• 服务器端准备证书：自签或购买
• 服务器端加上SSL 功能
• 导入证书到客户端
• 客户端加入SSL 功能

```


# 38 | 调优参数：权衡Netty核心参数
# 39 | 调优参数：图解费脑的三个参数
# 40 | 跟踪诊断：如何让应用易诊断？
# 41 | 跟踪诊断：应用能可视，心里才有底
# 42 | 跟踪诊断：让应用内存不“泄露”？
# 43 | 优化使用：用好自带注解省点心
# 44 | 优化使用：“整改”线程模型让"响应"健步如飞
# 45 | 优化使用：增强写，延迟与吞吐量的抉择
# 46 | 优化使用：如何让应用丝般“平滑”？
# 47 | 优化使用：为不同平台开启native
# 48 | 安全增强：设置“高低水位线”等保护好自己
# 49 | 安全增强：启用空闲监测
# 50 | 安全增强：简单有效的黑白名单
# 51 | 安全增强：少不了的自定义授权
# 52 | 安全增强：拿来即用的SSL-对话呈现表象
# 53 | 安全增强：拿来即用的SSL-抓包暴露本质
# 54 | 安全增强：拿来即用的SSL-轻松融入案例


第六章：成长为Netty的贡献者 (6讲)

# 55 | Cassandra如何使用Netty ？

```
Cassandra 如何使用Netty？
• Cassandra 是什么？
• Cassandra 传输数据结构
• Cassandra 使用Netty 概况
• 总览
• Pipeline
• 线程模型
• Cassandra 使用Netty 的一些技巧


Cassandra 是什么？
• Apache Cassandra 是一个开源的、分布式、去中心化、弹性可扩展、高可用性、容错、
一致性可调、面向行的数据库；
• 诞生于2007 年，于2008年开源；
• 它最初由Facebook 创建，用于储存收件箱等简单格式数据。此后，由于扩展性良好，被
Digg、Twitter 等知名网站所采纳，成为了一种流行的分布式结构化数据存储方案。

```

![](../../pic/2020-01-05-12-39-54.png)

![](../../pic/2020-01-05-12-40-24.png)


![](../../pic/2020-01-05-12-40-46.png)

![](../../pic/2020-01-05-12-42-29.png)

![](../../pic/2020-01-05-12-43-20.png)

![](../../pic/2020-01-05-12-43-40.png)

![](../../pic/2020-01-05-12-43-59.png)

![](../../pic/2020-01-05-12-44-19.png)

![](../../pic/2020-01-05-12-44-35.png)



# 56 | Dubbo如何使用Netty ？

```
Dubbo 如何使用Netty？
• Dubbo 是什么？
• Dubbo 传输数据结构
• Dubbo 使用Netty 概况
• 总览
• Pipeline
• 线程模型
• Dubbo 使用Netty 的一些技巧
```
2008年诞生，一款高性能、轻量级的开源Java RPC 框架，源于阿里巴巴，它提供了三大核
心能力：面向接口的远程方法调用，智能容错和负载均衡，以及服务自动注册和发现。


![](../../pic/2020-01-05-12-45-33.png)

![](../../pic/2020-01-05-12-45-55.png)

![](../../pic/2020-01-05-12-46-36.png)

![](../../pic/2020-01-05-12-46-59.png)

![](../../pic/2020-01-05-12-47-15.png)

Dubbo 中使用Netty 的一些技巧

（1）配置驱动：直接取值
<dubbo:protocol name=“dubbo” port=“9090” server=“netty” client=“netty” codec=“dubbo”
serialization=“hessian2” charset=“UTF-8” threadpool=“fixed” threads=“100” queues=“0”
iothreads=“9” buffer=“8192” accepts=“1000” payload=“8388608” />

![](../../pic/2020-01-05-12-49-38.png)

![](../../pic/2020-01-05-12-50-07.png)

（2）合理的Idle 时间： Server Idle（触发断连） 时间>= Client Idle（触发心跳消息） 时间* 2

![](../../pic/2020-01-05-12-50-38.png)

（3）心跳包的设计：Data: null, Event : 1

![](../../pic/2020-01-05-12-51-02.png)

（4）用事件触发handler 的移除

![](../../pic/2020-01-05-12-51-24.png)

（5）委托handler 的执行

![](../../pic/2020-01-05-12-51-42.png)


![](../../pic/2020-01-05-12-51-56.png)

（6）使用代理Socks5 ( VS “http proxy”)

![](../../pic/2020-01-05-12-52-15.png)




# 57 | Hadoop如何使用Netty ？

```
Hadoop 如何使用Netty？
• Hadoop 是什么？
• Hadoop 使用Netty 概况
• Hadoop 如何使用Netty 做http 服务器
• 小结：Casssandra/Dubbo/Hadoop 对Netty 使用




2004 年诞生，一套用于在大型集群上运行应用程序的框架。它实现了Map/Reduce 编程范型，
计算任务会被分割成小任务并运行在不同的节点上。除此之外，它还提供了一款分布式文件系
统（HDFS），用来存储相关的计算数据。
• Hadoop
• HDFS（数据存储: 分布式存储系统）
• Mapreduce（数据处理：分布式并行计算）
• Hadoop VS JDK 的ForkJoinPool
```
![](../../pic/2020-01-05-12-53-50.png)

Hadoop 如何使用Netty 做http 服务器？

![](../../pic/2020-01-05-12-54-22.png)

（1）https：是否支持，由dfs.http.policy 决定，而细节由其他配置决定（例如：
hadoop.ssl.require.client.cert）

![](../../pic/2020-01-05-12-55-05.png)

（2）过滤器（Filter）: dfs.datanode.httpserver.filter.handlers

![](../../pic/2020-01-05-12-55-36.png)


org.apache.hadoop.hdfs.server.datanode.web.RestCsrfPreventionFilterHandler：

• RestCsrfPreventionFilterHandler 继承SimpleChannelInboundHandler

• RestCsrfPreventionFilterHandler 含有RestCsrfPreventionFilter （javax.servlet.Filter）

![](../../pic/2020-01-05-12-56-07.png)

• 传递：ctx.fireChannelRead(req);

• 响应：ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);

![](../../pic/2020-01-05-12-56-32.png)

（3）Chucked Write： 8K: io.netty.handler.stream.ChunkedStream#DEFAULT_CHUNK_SIZE

![](../../pic/2020-01-05-12-56-51.png)


（4）URL Dispatcher

![](../../pic/2020-01-05-12-57-11.png)

业务处理示例：GET http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=GETFILECHECKSUM

![](../../pic/2020-01-05-12-57-38.png)


小结：Casssandra/Dubbo/Hadoop 对Netty 使用

![](../../pic/2020-01-05-12-58-24.png)





# 58 | 赏析Netty之美

```
• 应用广泛
• 更新频繁
• 对性能的极致追求
• 认真严谨的态度
• 代码质量高
• 易于使用，也易于扩展
• 包含知识广泛
赏析Netty 之美

```
![](../../pic/2020-01-05-12-59-22.png)


```
6 易于使用，也易于扩展

• 写一个ping – pong 消息服务器： < 100 行代码；
• 从NIO 切换到OIO 或者Native NIO 实现： 2-3 行代码；
• 内置常见功能（空闲监测、流量整形、安全等），使用这些功能： 2-3 行代码；
• 写业务实际就是编写可插拔的handler 而已；


7 包含知识广泛
• 网络编程: OIO/NIO/AIO
• 各种协议知识: 传输层协议TCP/UDP/SCTP、应用层协议Http、SMTP等
• Java知识: 设计模式、JNI、内存使用（堆外内存、内存池）、注解、多线程、垃圾回
收（弱引用等）、Maven（插件、配置）等等
• 扩展知识：流量整形、安全（IpFilter/SSL）等


```


# 59 | 如何给Netty贡献代码？

```
如何给Netty 贡献代码？
• 为什么要贡献代码？
• 贡献代码难不难？
• 贡献代码的7 个起点
• 贡献代码的7 个准则
```

# 60 | 课程回顾与总结

![](../../pic/2020-01-05-13-01-35.png)


```
进阶学习建议
• 推荐书籍：
• 网络知识：《TCP/IP详解》、《图解TCP/IP》、《Wireshark网络分析就这么简单》
• Java 网络编程：《Java 网络编程》、《Java TCP/IP Socket编程》
• Netty 相关:
• 《Netty权威指南》
• 《Netty实战》（译自《Netty in action》: Norman Maurer）
• 《Netty进阶之路：跟着案例学Netty》



进阶学习建议
• 扩展学习/实践：
• 传输层：UDP 编程
• 应用层：Http、Websocket 及其他应用层协议编程
• 研究实现细节：内存分配Jemalloc 等

```
