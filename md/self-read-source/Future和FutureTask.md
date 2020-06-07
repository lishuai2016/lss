
Future

是用来获取异步执行的结果。FutureTask为其实现类。


![](../../pic/2020-02-22-16-21-13.png)

![](../../pic/2020-02-22-16-31-38.png)


RunnableFuture接口

```java
public interface RunnableFuture<V> extends Runnable, Future<V> {
    /**
     * Sets this Future to the result of its computation
     * unless it has been cancelled.
     */
    void run();
}

```


FutureTask实现类

![](../../pic/2020-01-11-11-40-19.png)



备注：runnable接口和runnableFuture接口都定义了run方法，在future中通过run方法实现。


Future接口主要是一些run在执行过程中的状态，在面对多线程的状态扭转，我们通常用状态机来实现。

```java
   /**
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;//init的时候
    private static final int COMPLETING   = 1;//表示已经执行完毕，但是结果变量却没有写outcome的状态。
    private static final int NORMAL       = 2;//成功
    private static final int EXCEPTIONAL  = 3;//返回异常
    private static final int CANCELLED    = 4;//取消
    private static final int INTERRUPTING = 5;//打断中
    private static final int INTERRUPTED  = 6;//打断结束

```

状态转化流程图：

![](../../pic/2020-02-01-11-41-02.png)


![](../../pic/2020-02-23-10-15-18.png)













# 参考

- [【并发设计模式】FutureTask](https://www.jianshu.com/p/60f661d95d53)

- [FutureTask源码解析(1)——预备知识](https://segmentfault.com/a/1190000016542779)

- [FutureTask源码解析(2)——深入理解FutureTask](https://segmentfault.com/a/1190000016572591)

- [系列文章](https://segmentfault.com/u/chiucheng/articles?page=1&sort=vote)