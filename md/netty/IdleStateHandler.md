# IdleStateHandler

## 1、基础概念

### 1、心跳机制

心跳是在TCP长连接中，客户端和服务端定时向对方发送数据包通知对方自己还在线，保证连接的有效性的一种机制。在服务器和客户端之间一定时间内没有数据交互时, 即处于 idle 状态时, 客户端或服务器会发送一个特殊的数据包给对方, 当接收方收到这个数据报文后, 也立即发送一个特殊的数据报文, 回应发送方, 此即一个 PING-PONG 交互. 自然地, 当某一端收到心跳消息后, 就知道了对方仍然在线, 这就确保 TCP 连接的有效性.

### 2、心跳实现

使用TCP协议层的Keeplive机制，但是该机制默认的心跳时间是2小时，依赖操作系统实现不够灵活；应用层实现自定义心跳机制，比如Netty实现心跳机制；


> 总结
- 1、IdleStateHandler心跳检测主要是通过向线程任务队列中添加定时任务，判断channelRead()方法或write()方法是否调用空闲超时，如果超时则触发超时事件执行自定义userEventTrigger()方法；

- 2、Netty通过IdleStateHandler实现最常见的心跳机制不是一种双向心跳的PING-PONG模式，而是客户端发送心跳数据包，服务端接收心跳但不回复，因为如果服务端同时有上千个连接，心跳的回复需要消耗大量网络资源；如果服务端一段时间内内有收到客户端的心跳数据包则认为客户端已经下线，将通道关闭避免资源的浪费；在这种心跳模式下服务端可以感知客户端的存活情况，无论是宕机的正常下线还是网络问题的非正常下线，服务端都能感知到，而客户端不能感知到服务端的非正常下线；

- 3、要想实现客户端感知服务端的存活情况，需要进行双向的心跳；Netty中的channelInactive()方法是通过Socket连接关闭时挥手数据包触发的，因此可以通过channelInactive()方法感知正常的下线情况，但是因为网络异常等非正常下线则无法感知；


### 3、如何使用呢？

IdleStateHandler 既是出站处理器也是入站处理器，继承了 ChannelDuplexHandler 。通常在 initChannel 方法中将 IdleStateHandler 添加到 pipeline 中。然后在自己的 handler 中重写 userEventTriggered 方法，当发生空闲事件（读或者写），就会触发这个方法，并传入具体事件。
这时，你可以通过 Context 对象尝试向目标 Socekt 写入数据，并设置一个 监听器，如果发送失败就关闭 Socket （Netty 准备了一个 ChannelFutureListener.CLOSE_ON_FAILURE 监听器用来实现关闭 Socket 逻辑）。
这样，就实现了一个简单的心跳服务。







# 参考

- [Netty学习（五）—IdleStateHandler心跳机制](https://blog.csdn.net/u013967175/article/details/78591810)

- [Netty 心跳服务之 IdleStateHandler 源码分析](https://www.jianshu.com/p/f2ed73cf4df8)