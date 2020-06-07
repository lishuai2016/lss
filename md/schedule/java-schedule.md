
<!-- TOC -->

- [1、timer（单线程执行）](#1timer单线程执行)
    - [思考1：如果time/firstTime指定的时间，在当前时间之前，会发生什么呢？](#思考1如果timefirsttime指定的时间在当前时间之前会发生什么呢)
    - [思考2：schedule和scheduleAtFixedRate有什么区别？](#思考2schedule和scheduleatfixedrate有什么区别)
    - [思考3：如果执行task发生异常，是否会影响其他task的定时调度？](#思考3如果执行task发生异常是否会影响其他task的定时调度)
    - [思考4：Timer的一些缺陷？](#思考4timer的一些缺陷)
    - [9、使用示例](#9使用示例)
- [2、ScheduledThreadPoolExecutor](#2scheduledthreadpoolexecutor)
    - [9、使用样例](#9使用样例)
- [3、Quartz](#3quartz)
- [参考](#参考)

<!-- /TOC -->

# 1、timer（单线程执行）

Timer位于java.util包下，其内部包含且仅包含一个后台线程（TimeThread）对多个业务任务（TimeTask）进行定时定频率的调度。

schedule的四种用法和scheduleAtFixedRate的两种用法

![](../../pic/2020-04-13-09-42-21.png)


参数说明：

- task：所要执行的任务，需要extends TimeTask override run()

- time/firstTime：首次执行任务的时间

- period：周期性执行Task的时间间隔，单位是毫秒

- delay：执行task任务前的延时时间，单位是毫秒

很显然，通过上述的描述，我们可以实现：

- 延迟多久后执行一次任务；
- 指定时间执行一次任务；
- 延迟一段时间，并周期性执行任务；
- 指定时间，并周期性执行任务；


Timer其他需要关注的方法

- cancel()：终止Timer计时器，丢弃所有当前已安排的任务（TimeTask也存在cancel()方法，不过终止的是TimeTask）

- purge()：从计时器的任务队列中移除已取消的任务，并返回个数


## 思考1：如果time/firstTime指定的时间，在当前时间之前，会发生什么呢？

在时间等于或者超过time/firstTime的时候，会执行task！也就是说，如果time/firstTime指定的时间在当前时间之前，就会立即得到执行。

## 思考2：schedule和scheduleAtFixedRate有什么区别？

scheduleAtFixedRate：每次执行时间为上一次任务开始起向后推一个period间隔，也就是说下次执行时间相对于上一次任务开始的时间点，因此执行时间不会延后，但是存在任务并发执行的问题。

schedule：每次执行时间为上一次任务结束后推一个period间隔，也就是说下次执行时间相对于上一次任务结束的时间点，因此执行时间会不断延后。

## 思考3：如果执行task发生异常，是否会影响其他task的定时调度？

如果TimeTask抛出RuntimeException，那么Timer会停止所有任务的运行！

## 思考4：Timer的一些缺陷？

前面已经提及到Timer背后是一个单线程，因此Timer存在管理并发任务的缺陷：所有任务都是由同一个线程来调度，所有任务都是串行执行，意味着同一时间只能有一个任务得到执行，而前一个任务的延迟或者异常会影响到之后的任务。

其次，Timer的一些调度方式还算比较简单，无法适应实际项目中任务定时调度的复杂度。








## 9、使用示例

```java
public static void test_timer1() {
        //创建定时器对象
        Timer t=new Timer();
        //在3秒后执行MyTask类中的run方法,后面每3秒跑一次
        t.schedule(new MyTask(), 3000,3000);
    }

    static class MyTask extends TimerTask {
        @Override
        public void run() {
            System.out.println("hello world");
        }
    }
```




# 2、ScheduledThreadPoolExecutor

设计理念：每一个被调度的任务都会被线程池中的一个线程去执行，因此任务可以并发执行，而且相互之间不受影响。


> 接口方法

![](../../pic/2020-04-13-09-56-43.png)

```java
public interface ScheduledExecutorService extends ExecutorService {

//只执行一次
public ScheduledFuture<?> schedule(Runnable command,long delay, TimeUnit unit);

//只执行一次
public <V> ScheduledFuture<V> schedule(Callable<V> callable,long delay, TimeUnit unit);

//固定执行频率：initialDelay/nitialDelay+period/initialDelay + 2 * period。如果上一个任务执行时长超过period，后面的任务被延时，怎么个延时法？？？
public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,long initialDelay, long period, TimeUnit unit);


//以上一次执行结束作为执行周期计算的开始
public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,long initialDelay,long delay,TimeUnit unit);

}

```


直白地讲，scheduleAtFixedRate()为固定频率，scheduleWithFixedDelay()为固定延迟。固定频率是相对于任务执行的开始时间，而固定延迟是相对于任务执行的结束时间，这就是他们最根本的区别！





## 9、使用样例



```java
public static void test_ScheduledExecutorService() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
        //延迟1秒执行，执行周期是2秒
        executorService.scheduleAtFixedRate(new ScheduledExecutorServiceDemo(),1000,2000, TimeUnit.MILLISECONDS);
    }
    
    static class ScheduledExecutorServiceDemo implements Runnable{
        @Override
        public void run() {
            System.out.println("now:"+new Date());
        }
    }

```





> 问题:

- 1、怎么保证worker不会马上就从任务队列中获取任务然后直接执行呢(这样我们设定的延迟执行就没有效果了)？

其实就是修改任务队列的实现，通过将任务队列变成延迟队列，worker不会马上获取到任务队列中的任务了。只有任务的时间到了，worker线程才能从延迟队列中获取到任务并执行。在ScheduledThreadPoolExecutor中，定义了DelayedWorkQueue类来实现延迟队列。DelayedWorkQueue内部使用了最小堆的数据结构，当任务插入到队列中时，会根据执行的时间自动调整在堆中的位置，最后执行时间最近的那个会放在堆顶。当worker要去队列获取任务时，如果堆顶的执行时间还没到，那么worker就会阻塞一定时间后才能获取到那个任务，这样就实现了任务的延迟执行。


- 2、怎么保证任务执行完下一次在一定周期后还会再执行呢，也就是怎么保证任务的延迟执行和周期执行？

当任务执行完后，会检查自己是否是一个周期性执行的任务。如果是的话，就会重新计算下一次执行的时间，然后重新将自己放入任务队列中。


> Timer和ScheduledThreadPoolExecutor的区别

由于Timer是单线程的,如果一次执行多个定时任务，会导致某些任务被其他任务所阻塞。比如A任务每秒执行一次，B任务10秒执行一次，但是一次执行5秒，就会导致A任务在长达5秒都不会得到执行机会。而ScheduledThreadPoolExecutor是基于线程池的，可以动态的调整线程的数量，所以不会有这个问题

如果执行多个任务，在Timer中一个任务的崩溃会导致所有任务崩溃，从而所有任务都停止执行。而ScheduledThreadPoolExecutor则不会。

Timer的执行周期时间依赖于系统时间，timer中，获取到堆顶任务执行时间后，如果执行时间还没到，会计算出需要休眠的时间=(执行时间-系统时间),如果系统时间被调整，就会导致休眠时间无限拉长，后面就算改回来了任务也因为在休眠中而得不到执行的机会。ScheduledThreadPoolExecutor由于用是了nanoTime来计算执行周期的,所以和系统时间是无关的,无论系统时间怎么调整都不会影响到任务调度。





# 3、Quartz









# 参考

- [Java 定时任务实现原理详解](https://www.jianshu.com/p/25eea3863d14)
- [Java定时任务调度详解](https://www.jianshu.com/p/d732707ff194)















