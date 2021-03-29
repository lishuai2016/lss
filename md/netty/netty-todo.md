
# 线程太多的坏处？

- 1、每个线程都需要调用栈，其默认大小区间64KB到1MB，取决于操作系统；

- 2、线程的上下文切换带来的开销；



netty做的更多：
- 支持常用的应用层协议；
- 解决传输问题：粘包、半包现象；
- 支持流量整形；
- 完善的断链、idle等异常处理；



netty做的更好
- 1、经典的epoll bug：异常唤醒空转导致CPU100%；（方案：检测问题发生然后处理）；


使用项目：
- 数据库：Cassandra
- 大数据处理：spark、hadoop
- mq: rocketMQ
- 检索：es
- 框架：grpc、dubbo、spring5
- 分布式协调器：zookeeper
- 工具类：async-http-client




