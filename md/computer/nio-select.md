



nio select




https://zhuanlan.zhihu.com/p/63217085
https://www.zhihu.com/question/342093305/answer/801167091
https://www.zhihu.com/question/24322387/answer/282001188



https://zhuanlan.zhihu.com/p/23488863
https://www.zhihu.com/question/337609338/answer/775135962
https://zhuanlan.zhihu.com/p/95872805
https://www.zhihu.com/question/23084473/answer/334920663
https://mp.weixin.qq.com/s/l8hodrbiBO_XHx8Jtbmpyw






为了实现Selector管理多个SocketChannel，必须将具体的SocketChannel对象注册到Selector，并声明需要监听的事件（这样Selector才知道需要记录什么数据），一共有4种事件：

- 1、connect：客户端连接服务端事件，对应值为SelectionKey.OP_CONNECT(8)
- 2、accept：服务端接收客户端连接事件，对应值为SelectionKey.OP_ACCEPT(16)
- 3、read：读事件，对应值为SelectionKey.OP_READ(1)
- 4、write：写事件，对应值为SelectionKey.OP_WRITE(4)

这个很好理解，每次请求到达服务器，都是从connect开始，connect成功后，服务端开始准备accept，准备就绪，开始读数据，并处理，最后写回数据返回。

所以，当SocketChannel有对应的事件发生时，Selector都可以观察到，并进行相应的处理。

https://www.jianshu.com/p/0d497fe5484a
