
<!-- TOC -->

- [1、三次握手涉及的状态](#1三次握手涉及的状态)
    - [1、LISTEN](#1listen)
    - [2、SYN_SENT](#2syn_sent)
    - [3、SYN_RCVD](#3syn_rcvd)
    - [4、ESTABLISHED](#4established)
- [2、TCP四次挥手过程的状态变迁](#2tcp四次挥手过程的状态变迁)
    - [1、FIN_WAIT_1](#1fin_wait_1)
    - [2、CLOSE_WAIT](#2close_wait)
    - [3、FIN_WAIT_2](#3fin_wait_2)
    - [4、LAST_ACK](#4last_ack)
    - [5、CLOSING](#5closing)
    - [6、TIME_WAIT](#6time_wait)
- [参考](#参考)

<!-- /TOC -->

![](../../pic/2021-02-02/2021-02-02-23-15-49.png)


# 1、三次握手涉及的状态

- CLOSED：起始点，在超时或者连接关闭时候进入此状态，这并不是一个真正的状态，而是这个状态图的假想起点和终点。

## 1、LISTEN

服务器端等待连接的状态。服务器经过 socket，bind，listen 函数之后进入此状态，开始监听客户端发过来的连接请求。此称为应用程序被动打开（等到客户端连接请求）。

## 2、SYN_SENT

第一次握手发生阶段，客户端发起连接。客户端调用 connect，发送 SYN 给服务器端，然后进入 SYN_SENT 状态，等待服务器端确认（三次握手中的第二个报文）。如果服务器端不能连接，则直接进入CLOSED状态。

## 3、SYN_RCVD

第二次握手发生阶段，跟 SYN_SENT 对应，这里是服务器端接收到了客户端的 SYN，此时服务器由 LISTEN 进入 SYN_RCVD状态，同时服务器端回应一个 ACK，然后再发送一个 SYN 即 SYN+ACK 给客户端。状态图中还描绘了这样一种情况，当客户端在发送 SYN 的同时也收到服务器端的 SYN请求，即两个同时发起连接请求，那么客户端就会从 SYN_SENT 转换到 SYN_REVD 状态。

## 4、ESTABLISHED

第三次握手发生阶段，客户端接收到服务器端的 ACK 包（ACK，SYN）之后，也会发送一个 ACK 确认包，客户端进入 ESTABLISHED 状态，表明客户端这边已经准备好，但TCP 需要两端都准备好才可以进行数据传输。服务器端收到客户端的 ACK 之后会从 SYN_RCVD 状态转移到 ESTABLISHED 状态，表明服务器端也准备好进行数据传输了。这样客户端和服务器端都是 ESTABLISHED 状态，就可以进行后面的数据传输了。所以 ESTABLISHED 也可以说是一个数据传送状态。


`SYN_SENT 状态表示已经客户端已经发送了 SYN 报文，SYN_RCVD 状态表示服务器端已经接收到了 SYN 报文。`

# 2、TCP四次挥手过程的状态变迁

## 1、FIN_WAIT_1

第一次挥手。主动关闭的一方（执行主动关闭的一方既可以是客户端，也可以是服务器端，这里以客户端执行主动关闭为例），终止连接时，发送 FIN 给对方，然后等待对方返回 ACK 。调用 close() 第一次挥手就进入此状态。

## 2、CLOSE_WAIT

接收到FIN 之后，被动关闭的一方进入此状态。具体动作是接收到 FIN，同时发送 ACK。之所以叫 CLOSE_WAIT 可以理解为被动关闭的一方此时正在等待上层应用程序发出关闭连接指令。前面已经说过，TCP关闭是全双工过程，这里客户端执行了主动关闭，被动方服务器端接收到FIN 后也需要调用 close 关闭，这个 CLOSE_WAIT 就是处于这个状态，等待发送 FIN，发送了FIN 则进入 LAST_ACK 状态。

## 3、FIN_WAIT_2

主动端（这里是客户端）先执行主动关闭发送FIN，然后接收到被动方返回的 ACK 后进入此状态。

## 4、LAST_ACK

被动方（服务器端）发起关闭请求，由状态2 进入此状态，具体动作是发送 FIN给对方，同时在接收到ACK 时进入CLOSED状态。


## 5、CLOSING

两边同时发起关闭请求时（即主动方发送FIN，等待被动方返回ACK，同时被动方也发送了FIN，主动方接收到了FIN之后，发送ACK给被动方），主动方会由FIN_WAIT_1 进入此状态，等待被动方返回ACK。

## 6、TIME_WAIT

从状态变迁图会看到，四次挥手操作最后都会经过这样一个状态然后进入CLOSED状态。共有三个状态会进入该状态





# 参考

- [【Unix 网络编程】TCP状态转换图详解](https://blog.csdn.net/wenqian1991/article/details/40110703)