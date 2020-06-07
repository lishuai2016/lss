

- [Java I/O 的工作机制](https://blog.csdn.net/l1394049664/article/details/82227022)

# 1、Java NIO简介以及与IO的对比 
Java NIO 是由 Java 1.4 引进的异步 IO.Java NIO 由以下几个核心部分组成:

- Channel
- Buffer
- Selector

## 1.1、NIO 和 IO 的对比

IO 和 NIO 的区别主要体现在三个方面:
- IO 基于流(Stream oriented), 而 NIO 基于 Buffer (Buffer oriented)
- IO 操作是阻塞的, 而 NIO 操作是非阻塞的
- IO 没有 selector 概念, 而 NIO 有 selector 概念.

### 1.1.1、基于 Stream 与基于 Buffer

传统的 IO 是面向字节流或字符流的, 而在 NIO 中, 我们抛弃了传统的 IO 流, 而是引入了 Channel 和 Buffer 的概念. 在 NIO 中, 我只能从 Channel 中读取数据到 Buffer 中或将数据从 Buffer 中写入到 Channel.

那么什么是 基于流 呢? 在一般的 Java IO 操作中, 我们以流式的方式顺序地从一个 Stream 中读取一个或多个字节, 因此我们也就不能随意改变读取指针的位置.而 基于 Buffer 就显得有点不同了. 我们首先需要从 Channel 中读取数据到 Buffer 中, 当 Buffer 中有数据后, 我们就可以对这些数据进行操作了. 不像 IO 那样是顺序操作, NIO 中我们可以随意地读取任意位置的数据.

### 1.1.2、阻塞和非阻塞

Java 提供的各种 Stream 操作都是阻塞的, 例如我们调用一个 read 方法读取一个文件的内容, 那么调用 read 的线程会被阻塞住, 直到 read 操作完成.而 NIO 的非阻塞模式允许我们非阻塞地进行 IO 操作. 例如我们需要从网络中读取数据, 在 NIO 的非阻塞模式中, 当我们调用 read 方法时, 如果此时有数据, 则 read 读取并返回; 如果此时没有数据, 则 read 直接返回, 而不会阻塞当前线程.

### 1.1.3、selector

selector 是 NIO 中才有的概念, 它是 Java NIO 之所以可以非阻塞地进行 IO 操作的关键.通过 Selector, 一个线程可以监听多个 Channel 的 IO 事件, 当我们向一个 Selector 中注册了 Channel 后, Selector 内部的机制就可以自动地为我们不断地查询(select) 这些注册的 Channel 是否有已就绪的 IO 事件(例如可读, 可写, 网络连接完成等). 通过这样的 Selector 机制, 我们就可以很简单地使用一个线程高效地管理多个 Channel 了.




## 1.2、Java NIO Channel

通常来说, 所有的 NIO 的 I/O 操作都是从 Channel 开始的. 一个 channel 类似于一个 stream.java Stream 和 NIO Channel 对比：

- 我们可以在同一个 Channel 中执行读和写操作, 然而同一个 Stream 仅仅支持读或写.
- Channel 可以异步地读写, 而 Stream 是阻塞的同步读写.
- Channel 总是从 Buffer 中读取数据, 或将数据写入到 Buffer 中.

Channel 类型有:
- FileChannel, 文件操作
- DatagramChannel, UDP 操作
- SocketChannel, TCP 操作
- ServerSocketChannel, TCP 操作, 使用在服务器端.

这些通道涵盖了 UDP 和 TCP网络 IO以及文件 IO.


## 1.3、Java NIO Buffer

当我们需要与 NIO Channel 进行交互时, 我们就需要使用到 NIO Buffer, 即数据从 Buffer读取到 Channel 中, 并且从 Channel 中写入到 Buffer 中.实际上, 一个 Buffer 其实就是一块内存区域, 我们可以在这个内存区域中进行数据的读写. NIO Buffer 其实是这样的内存块的一个封装, 并提供了一些操作方法让我们能够方便地进行数据的读写.Buffer 类型有:

- ByteBuffer
- CharBuffer
- DoubleBuffer
- FloatBuffer
- IntBuffer
- LongBuffer
- ShortBuffer

这些 Buffer 覆盖了能从 IO 中传输的所有的 Java 基本数据类型.

### 1.3.1、NIO Buffer 的基本使用

使用 NIO Buffer 的步骤如下:

- 1、将数据写入到 Buffer 中.
- 2、调用 Buffer.flip()方法, 将 NIO Buffer 转换为读模式.
- 3、从 Buffer 中读取数据
- 4、调用 Buffer.clear() 或 Buffer.compact()方法, 将 Buffer 转换为写模式.

当我们将数据写入到 Buffer 中时, Buffer 会记录我们已经写了多少的数据, 当我们需要从 Buffer 中读取数据时, 必须调用 Buffer.flip()将 Buffer 切换为读模式.一旦读取了所有的 Buffer 数据, 那么我们必须清理 Buffer, 让其从新可写, 清理 Buffer 可以调用 Buffer.clear() 或 Buffer.compact().


### 1.3.2、Buffer 属性

一个 Buffer 有三个属性: capacity、position、limit。其中 position 和 limit 的含义与 Buffer 处于读模式或写模式有关, 而 capacity 的含义与 Buffer 所处的模式无关.

> Capacity

一个内存块会有一个固定的大小, 即容量(capacity), 我们最多写入capacity 个单位的数据到 Buffer 中, 例如一个 DoubleBuffer, 其 Capacity 是100, 那么我们最多可以写入100个 double 数据.

> Position

当从一个 Buffer 中写入数据时, 我们是从 Buffer 的一个确定的位置(position)开始写入的. 在最初的状态时, position 的值是0. 每当我们写入了一个单位的数据后, position 就会递增一.当我们从 Buffer 中读取数据时, 我们也是从某个特定的位置开始读取的. 当我们调用了 filp()方法将 Buffer 从写模式转换到读模式时, position 的值会自动被设置为0, 每当我们读取一个单位的数据, position 的值递增1.position 表示了读写操作的位置指针.

> limit

limit - position 表示此时还可以写入/读取多少单位的数据.例如在写模式, 如果此时 limit 是10, position 是2, 则表示已经写入了2个单位的数据, 还可以写入 10 - 2 = 8 个单位的数据


### 分配 Buffer

为了获取一个 Buffer 对象, 我们首先需要分配内存空间. 每个类型的 Buffer 都有一个 allocate()方法, 我们可以通过这个方法分配 Buffer:

ByteBuffer buf = ByteBuffer.allocate(48);这里我们分配了48 * sizeof(Byte)字节的内存空间.

CharBuffer buf = CharBuffer.allocate(1024);这里我们分配了大小为1024个字符的 Buffer, 即 这个 Buffer 可以存储1024 个 Char, 其大小为 1024 * 2 个字节.


## 1.4、关于 Direct Buffer 和 Non-Direct Buffer 的区别

### 1.4.1、Direct Buffer

1、所分配的内存不在 JVM 堆上, 不受 GC 的管理.(但是 Direct Buffer 的 Java 对象是由 GC 管理的, 因此当发生 GC, 对象被回收时, Direct Buffer 也会被释放)

2、因为 Direct Buffer 不在 JVM 堆上分配, 因此 Direct Buffer 对应用程序的内存占用的影响就不那么明显(实际上还是占用了这么多内存, 但是 JVM 不好统计到非 JVM 管理的内存.)

3、申请和释放 Direct Buffer 的开销比较大. 因此正确的使用 Direct Buffer 的方式是在初始化时申请一个 Buffer, 然后不断复用此 buffer, 在程序结束后才释放此 buffer.

4、使用 Direct Buffer 时, 当进行一些底层的系统 IO 操作时, 效率会比较高, 因为此时 JVM 不需要拷贝 buffer 中的内存到中间临时缓冲区中.

### 1.4.2、Non-Direct Buffer

1、直接在 JVM 堆上进行内存的分配, 本质上是 byte[] 数组的封装.

2、因为 Non-Direct Buffer 在 JVM 堆中, 因此当进行操作系统底层 IO 操作中时, 会将此 buffer 的内存复制到中间临时缓冲区中. 因此 Non-Direct Buffer 的效率就较低.





## 1.5、Selector

Selector 允许一个单一的线程来操作多个 Channel. 如果我们的应用程序中使用了多个 Channel, 那么使用 Selector 很方便的实现这样的目的, 但是因为在一个线程中使用了多个 Channel, 因此也会造成了每个 Channel 传输效率的降低.

为了使用 Selector, 我们首先需要将 Channel 注册到 Selector 中, 随后调用 Selector 的 select()方法, 这个方法会阻塞, 直到注册在 Selector 中的 Channel 发送可读写事件. 当这个方法返回后, 当前的这个线程就可以处理 Channel 的事件了.

### 1.5.1、创建选择器

通过 Selector.open()方法, 我们可以创建一个选择器:Selector selector = Selector.open();

将 Channel 注册到选择器中为了使用选择器管理 Channel, 我们需要将 Channel 注册到选择器中:

```java
channel.configureBlocking(false);
SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
```

注意, 如果一个 Channel 要注册到 Selector 中, 那么这个 Channel 必须是非阻塞的, 即channel.configureBlocking(false);【因为 Channel 必须要是非阻塞的, 因此 FileChannel 是不能够使用选择器的, 因为 FileChannel 都是阻塞的】

注意到, 在使用 Channel.register()方法时, 第二个参数指定了我们对 Channel 的什么类型的事件感兴趣, 这些事件有:

- 1、Connect, 即连接事件(TCP 连接), 对应于SelectionKey.OP_CONNECT
- 2、Accept, 即确认事件, 对应于SelectionKey.OP_ACCEPT
- 3、Read, 即读事件, 对应于SelectionKey.OP_READ, 表示 buffer 可读.
- 4、Write, 即写事件, 对应于SelectionKey.OP_WRITE, 表示 buffer 可写.

一个 Channel发出一个事件也可以称为 对于某个事件, Channel 准备好了. 因此一个 Channel 成功连接到了另一个服务器也可以被称为 connect ready.我们可以使用或运算|来组合多个事件, 例如:

int interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;

注意, 一个 Channel 仅仅可以被注册到一个 Selector 一次, 如果将 Channel 注册到 Selector 多次, 那么其实就是相当于更新 SelectionKey 的 interest set. 例如:

channel.register(selector, SelectionKey.OP_READ);

channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

上面的 channel 注册到同一个 Selector 两次了, 那么第二次的注册其实就是相当于更新这个 Channel 的 interest set 为 SelectionKey.OP_READ | SelectionKey.OP_WRITE.

### 1.5.2、关于 SelectionKey

如上所示, 当我们使用 register 注册一个 Channel 时, 会返回一个 SelectionKey 对象, 这个对象包含了如下内容:

- 1、interest set, 即我们感兴趣的事件集, 即在调用 register 注册 channel 时所设置的 interest set.
- 2、ready set
- 3、channel
- 4、selector
- 5、attached object, 可选的附加对象

> interest set

我们可以通过如下方式获取 interest set:

```java
int interestSet = selectionKey.interestOps();

boolean isInterestedInAccept  = interestSet & SelectionKey.OP_ACCEPT;
boolean isInterestedInConnect = interestSet & SelectionKey.OP_CONNECT;
boolean isInterestedInRead    = interestSet & SelectionKey.OP_READ;
boolean isInterestedInWrite   = interestSet & SelectionKey.OP_WRITE; 

```

> ready set

代表了 Channel 所准备好了的操作.我们可以像判断 interest set 一样操作 Ready set, 但是我们还可以使用如下方法进行判断:

```java
int readySet = selectionKey.readyOps();

selectionKey.isAcceptable();
selectionKey.isConnectable();
selectionKey.isReadable();
selectionKey.isWritable();
```

> Channel 和 Selector

我们可以通过 SelectionKey 获取相对应的 Channel 和 Selector:

Channel  channel  = selectionKey.channel();

Selector selector = selectionKey.selector();  

> Attaching Object

我们可以在selectionKey中附加一个对象:

```java
selectionKey.attach(theObject);
Object attachedObj = selectionKey.attachment();
//或者在注册时直接附加:
SelectionKey key = channel.register(selector, SelectionKey.OP_READ, theObject);

```

### 1.5.4、通过 Selector 选择 Channel

我们可以通过 Selector.select()方法获取对某件事件准备好了的 Channel, 即如果我们在注册 Channel 时, 对其的可写事件感兴趣, 那么当 select()返回时, 我们就可以获取 Channel 了.注意, select()方法返回的值表示有多少个 Channel 可操作.

> 获取可操作的 Channel

如果 select()方法返回值表示有多个 Channel 准备好了, 那么我们可以通过 Selected key set 访问这个 Channel:

```java
 Set<SelectionKey> selectedKeys = selector.selectedKeys();
    
    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
    
    while(keyIterator.hasNext()) {
        
        SelectionKey key = keyIterator.next();
    
        if(key.isAcceptable()) {
            // a connection was accepted by a ServerSocketChannel.
    
        } else if (key.isConnectable()) {
            // a connection was established with a remote server.
    
        } else if (key.isReadable()) {
            // a channel is ready for reading
    
        } else if (key.isWritable()) {
            // a channel is ready for writing
        }
    
        keyIterator.remove();
    }
```

注意, 在每次迭代时, 我们都调用 "keyIterator.remove()" 将这个 key 从迭代器中删除, 因为 select() 方法仅仅是简单地将就绪的 IO 操作放到 selectedKeys 集合中, 因此如果我们从 selectedKeys 获取到一个 key, 但是没有将它删除, 那么下一次 select 时, 这个 key 所对应的 IO 事件还在 selectedKeys 中.

例如此时我们收到 OP_ACCEPT 通知, 然后我们进行相关处理, 但是并没有将这个 Key 从 SelectedKeys 中删除, 那么下一次 select() 返回时 我们还可以在 SelectedKeys 中获取到 OP_ACCEPT 的 key.

注意, 我们可以动态更改 SekectedKeys 中的 key 的 interest set. 例如在 OP_ACCEPT 中, 我们可以将 interest set 更新为 OP_READ, 这样 Selector 就会将这个 Channel 的 读 IO 就绪事件包含进来了.

### 1.5.3、Selector 的基本使用流程

1、通过 Selector.open() 打开一个 Selector.

2、将 Channel 注册到 Selector 中, 并设置需要监听的事件(interest set)

3、不断重复:

- 3.1、调用 select() 方法
- 3.2、调用 selector.selectedKeys() 获取 selected keys
- 3.3、迭代每个 selected key:
    - 从 selected key 中获取 对应的 Channel 和附加信息(如果有的话)
    - 判断是哪些 IO 事件已经就绪了, 然后处理它们. 如果是 OP_ACCEPT 事件, 则调用 "SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept()" 获取 SocketChannel, 并将它设置为 非阻塞的, 然后将这个 Channel 注册到 Selector 中.
    - 根据需要更改 selected key 的监听事件.
    - 将已经处理过的 key 从 selected keys 集合中删除.

4、关闭 Selector
当调用了 Selector.close()方法时, 我们其实是关闭了 Selector 本身并且将所有的 SelectionKey 失效, 但是并不会关闭 Channel.



# 附录。io/nio项目开发代码示例

## 1、OIO

```java
package io.netty.example.lishuai.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeClient {
    public static void main(String[] ars) {
        int port = 8080;
        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            socket = new Socket("127.0.0.1",port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));   //根据socket构建输入流
            out = new PrintWriter(socket.getOutputStream(),true); //根据socket构建输出流

            out.println("QUERY TIME ORDER");    //往输出流中写数据，发送请求
            System.out.println("send order 2 server succeed.");

            String res = in.readLine();//从输入流读取响应
            System.out.println("now is:" + res);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
        }
    }
}


package io.netty.example.lishuai.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeServer {

    public static void main(String[] ars) throws IOException {
        int port = 8080;
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("ServerSocket:端口" + port);
            Socket socket = null;
            while (true) {
                socket =server.accept();
                new Thread(new TimeServerHandler(socket)).start(); //把每个链接构建个新的线程进行处理
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                System.out.println("Server close" );
                server.close();
                server = null;
            }
        }
    }
}


package io.netty.example.lishuai.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeServerHandler implements  Runnable{
    private Socket socket;
    public TimeServerHandler(Socket socket) {
        this.socket = socket;
    }
    public void run() {

        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));  //根据socket构建输入流，读取用户请求
            out = new PrintWriter(this.socket.getOutputStream(),true);  //根据socket构建输出流，返回响应数据
            String currenttime = null;
            String body = null;
            while (true) {
                body = in.readLine();  //读取客户端的请求
                if (body == null) {
                    break;
                }
                System.out.println("the time receive order:" + body);
                currenttime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER" ;
                out.println(currenttime);  //响应客户端的请求
            }
        } catch (IOException e) {
           if (in != null) {
               try {
                   in.close();
               } catch (IOException e1) {
                   e1.printStackTrace();
               }
           }
            if (out != null) {
                out.close();
                out = null;
            }
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                this.socket = null;
            }
        }
    }
}



```


## 2、使用了线程池的OIO

client端一样，升级服务端不再一个请求一个线程而是使用了线程池

```java
package io.netty.example.lishuai.likeIO;



import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeServer {

    public static void main(String[] ars) throws IOException {
        int port = 8080;
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("ServerSocket:端口" + port);
            Socket socket = null;
            TimeServerHanderExecutePool executePool = new TimeServerHanderExecutePool(50,1000);
            while (true) {
                socket =server.accept();
                executePool.execute(new TimeServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                System.out.println("Server close" );
                server.close();
                server = null;
            }
        }
    }
}


package io.netty.example.lishuai.likeIO;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeServerHanderExecutePool {
    private ExecutorService executor;
    public TimeServerHanderExecutePool(int maxPoolSize, int queueSize) {
        executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),maxPoolSize,120L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(queueSize)
                );
    }
    public void execute(Runnable task) {
        executor.execute(task);
    }
}


package io.netty.example.lishuai.likeIO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeServerHandler implements  Runnable{
    private Socket socket;
    public TimeServerHandler(Socket socket) {
        this.socket = socket;
    }
    public void run() {

        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            out = new PrintWriter(this.socket.getOutputStream(),true);
            String currenttime = null;
            String body = null;
            while (true) {
                body = in.readLine();
                if (body == null) {
                    break;
                }
                System.out.println("the time receive order:" + body);
                currenttime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER" ;
                out.println(currenttime);
            }
        } catch (IOException e) {
           if (in != null) {
               try {
                   in.close();
               } catch (IOException e1) {
                   e1.printStackTrace();
               }
           }
            if (out != null) {
                out.close();
                out = null;
            }
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                this.socket = null;
            }
        }
    }
}

```

## 3、NIO

```java

import java.io.IOException;

/**
 * Created by lishuai on 2017/6/17.
 */
public class TimeServer {

    public static void main(String[] ars) throws IOException {
        int port = 8080;
        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);
        new Thread(timeServer,"MultiplexerTimeServer-001").start();
    }
}


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by lishuai on 2017/6/17.
 */
public class MultiplexerTimeServer implements Runnable {
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean stop;
    //初始化多路复用器，绑定监听的端口
    public MultiplexerTimeServer(int port) {
        try {

            selector = Selector.open(); // 打开selector
            serverChannel = ServerSocketChannel.open();// 打开服务端ServerSocketChannel
            serverChannel.configureBlocking(false); // 设置为非阻塞模式
            // 绑定一个本地端口，这样客户端便可以通过这个端口连接到服务器
            serverChannel.socket().bind(new InetSocketAddress(port), 1024);
            // 注意关心的事件是OP_ACCEPT，表示只关心接受事件即接受客户端到服务器的连接
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("ServerSocket:端口" + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    public void run() {

        while (!stop) {
            try {
                //休眠一秒，每隔一秒中selector被唤醒一次
                // select()阻塞直到注册的某个事件就绪并会更新SelectionKey的状态
                selector.select(1000);
                // 得到就绪的key集合，key中保存有就绪的事件以及对应的Channel通道
                Set<SelectionKey> SelectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = SelectionKeys.iterator();
                SelectionKey key = null;
                // 遍历选择键
                while(it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handlerInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }

                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        //关闭多路复用器，所有注册在其上的channel和pipe资源都会被释放
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handlerInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            //处理新接入的请求消息
            if (key.isAcceptable()) {
                //接受新的连接
                ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                //注册新的连接到selector
                sc.register(selector,SelectionKey.OP_READ);// 注意此处新增关心事件OP_READ
            }
            //
            if (key.isReadable()) {
                //读数据
                SocketChannel sc = (SocketChannel)key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBuffers = sc.read(readBuffer);//  把channel中的数据读到buffer中
                //readBuffers
                if (readBuffers > 0) {
                    readBuffer.flip();   //读写切换，把指针移到数据的开头
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);   //把buffer中的数据读到字节数组中
                    String body = new String(bytes,"UTF-8");   //根据字节数组构建请求数据
                    System.out.println("the time receive order:" + body);
                    String currenttime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER" ;
                    dowrite(sc,currenttime);  //把响应写回给客户端
                } else if (readBuffers < 0) {
                    //链路关闭，释放资源
                    key.cancel();
                    sc.close();
                } else {
                    //0 没有读取到字节
                }
            }
        }
    }

    private void dowrite(SocketChannel channel,String response) throws IOException {
        if(response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer wrietBuffer = ByteBuffer.allocate(bytes.length);
            wrietBuffer.put(bytes);//把响应数据写到buffer中
            wrietBuffer.flip();//读写切换
            channel.write(wrietBuffer); //把buffer中的
        }
    }
}



public class TimeClient {
    public static void main(String[] ars) {
        int port = 8080;
       new Thread(new TimeClientHandle("127.0.0.1",port),"TimeClientHandle-001").start();
    }
}



public class TimeClientHandle implements Runnable{

    private String host;
    private Integer port;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;
    public TimeClientHandle(String host,Integer port) {
        this.host = host == null ? "127.0.0.1" : host ;
        this.port = port;
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public void run() {
        try {
            doconnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        while (!stop) {
            try {
                selector.select(1000);
                Set<SelectionKey> SelectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = SelectionKeys.iterator();
                SelectionKey key = null;
                while(it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handlerInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlerInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            //判断是否连接成功
            SocketChannel sc = (SocketChannel)key.channel();
            if (key.isConnectable()) {
                if (sc.finishConnect()) {
                    sc.register(selector,SelectionKey.OP_READ);
                    doWrite(sc);
                } else {
                    System.exit(1);
                }
            }
            if (key.isReadable()) {
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBuffers = sc.read(readBuffer);
                if (readBuffers > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes,"UTF-8");
                    System.out.println("now is : " + body);
                    this.stop = true;
                } else if (readBuffers < 0) {
                    key.cancel();
                    sc.close();
                } else {
                    //0
                }
            }
        }


    }
    private void doconnect() throws IOException {
        //如果连接成功，则注册到多路复用器上，发送请求消息，读应答
        if (socketChannel.connect(new InetSocketAddress(host,port))) {
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
        } else {
            socketChannel.register(selector,SelectionKey.OP_CONNECT);
        }
    }
    private void doWrite(SocketChannel sc) throws IOException {
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        sc.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            System.out.println("send order 2 server succeed.");
        }
    }
}



```



## 4、基于netty开发的简单网络通信


```java
public class HelloWorldServer {

    private int port;

    public static void main(String[] args){
        new HelloWorldServer(8080).start();
    }

    public HelloWorldServer(int port) {
        this.port = port;
    }

    public void start() {
        /**
         * 创建两个EventLoopGroup，即两个线程池，boss线程池用于接收客户端的连接，一个线程监听一个端口，一般只会监听一个端口所以只需一个线程
         * work池用于处理网络连接数据读写或者后续的业务处理（可指定另外的线程处理业务，work完成数据读写）
         */
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup work = new NioEventLoopGroup();
        try {
            /**
             * 实例化一个服务端启动类，
             * group（）指定线程组
             * channel（）指定用于接收客户端连接的类，对应java.nio.ServerSocketChannel
             * childHandler（）设置编码解码及处理连接的类
             */
            ServerBootstrap server = new ServerBootstrap()
                    .group(boss, work)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("decoder", new StringDecoder())
                                    .addLast("encoder", new StringEncoder())
                                    .addLast(new HelloWorldServerHandler());
                        }
                    });
            //绑定端口
            ChannelFuture future = server.bind().sync();
            System.out.println("server started and listen " + port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    public static class HelloWorldServerHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("HelloWorldServerHandler active");
        }

    
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("server channelRead..");
            System.out.println(ctx.channel().remoteAddress()+"->Server :"+ msg.toString());
            ctx.write("server write："+msg);
            ctx.flush();
        }
    }
}



public class HelloWorldClient {
    private static final String HOST = "127.0.0.1";
    private static final int PORT= 8080;

    public static void main(String[] args){
        new HelloWorldClient().start(HOST, PORT);
    }

    public void start(String host, int port) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap client = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast("decoder", new StringDecoder())
                                    .addLast("encoder", new StringEncoder())
                                    .addLast(new HelloWorldClientHandler());
                        }
                    });
            ChannelFuture future = client.connect(host, port).sync();
            future.channel().writeAndFlush("Hello Netty Server ,I am a netty client");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static class HelloWorldClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("HelloWorldClientHandler Active");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("HelloWorldClientHandler read Message:"+msg);
        }
    }
}


```