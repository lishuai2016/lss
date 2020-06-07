
# AQS-ReentrantLock解析

<!-- TOC -->

- [AQS-ReentrantLock解析](#aqs-reentrantlock解析)
    - [1、ReentrantLock和AQS的关系](#1reentrantlock和aqs的关系)
    - [2、ReentrantLock加锁和释放锁的底层原理概述](#2reentrantlock加锁和释放锁的底层原理概述)
    - [3、ReentrantLock源码分析](#3reentrantlock源码分析)
        - [1、锁类型](#1锁类型)
        - [2、获取锁](#2获取锁)
            - [1、公平锁获取锁](#1公平锁获取锁)
                - [1、写入队列](#1写入队列)
                - [2、挂起等待线程](#2挂起等待线程)
            - [2、非公平锁获取锁](#2非公平锁获取锁)
        - [3、释放锁](#3释放锁)
    - [总结](#总结)

<!-- /TOC -->


## 1、ReentrantLock和AQS的关系

AQS(AbstractQueuedSynchronizer，抽象队列同步器)

用java并发包下的ReentrantLock来加锁和释放锁，是个什么样的感觉？

```java
public class Test_ReentrantLock {
    private static ReentrantLock reentrantLock = new ReentrantLock();

    void method() {
        reentrantLock.lock();
        //业务逻辑处理
        reentrantLock.unlock();

    }
}
```

上面那段代码应该不难理解吧，无非就是搞一个Lock对象，然后加锁和释放锁。你这时可能会问，这个跟AQS有啥关系？关系大了去了！因为java并发包下很多API都是基于AQS来实现的加锁和释放锁等功能的，AQS是java并发包的基础类。

举个例子，比如说ReentrantLock、ReentrantReadWriteLock底层都是基于AQS来实现的。

### AQS是什么？

那么AQS的全称是什么呢？AbstractQueuedSynchronizer，抽象队列同步器。给大家画一个图先，看一下ReentrantLock和AQS之间的关系。

![ReentrantLock和AQS](../../pic//ReentrantLock和AQS.png) 

我们来看上面的图。说白了，ReentrantLock内部包含了一个AQS对象，也就是AbstractQueuedSynchronizer类型的对象。这个AQS对象就是ReentrantLock可以实现加锁和释放锁的关键性的核心组件。

## 2、ReentrantLock加锁和释放锁的底层原理概述

那么现在如果有一个线程过来尝试用ReentrantLock的lock()方法进行加锁，会发生什么事情呢？很简单，这个AQS对象内部有一个核心的变量叫做state，是int类型的，代表了加锁的状态。初始状态下，这个state的值是0。另外，这个AQS内部还有一个关键变量，用来记录当前加锁的是哪个线程，初始化状态下，这个变量是null。

![AQS1](../../pic//AQS1.png)

接着线程1跑过来调用ReentrantLock的lock()方法尝试进行加锁，这个加锁的过程，直接就是用CAS操作将state值从0变为1。如果之前没人加过锁，那么state的值肯定是0，此时线程1就可以加锁成功。一旦线程1加锁成功了之后，就可以设置当前加锁线程是自己。所以大家看下面的图，就是线程1跑过来加锁的一个过程。

![AQS2](../../pic//AQS2.png)

其实看到这儿，大家应该对所谓的AQS有感觉了。说白了，就是并发包里的一个核心组件，里面有state变量、加锁线程变量等核心的东西，维护了加锁状态。你会发现，ReentrantLock这种东西只是一个外层的API，内核中的锁机制实现都是依赖AQS组件的。


这个ReentrantLock之所以用Reentrant打头，意思就是他是一个可重入锁。可重入锁的意思，就是你可以对一个ReentrantLock对象多次执行lock()加锁和unlock()释放锁，也就是可以对一个锁加多次，叫做可重入加锁。

大家看明白了那个state变量之后，就知道了如何进行可重入加锁！

其实每次线程1可重入加锁一次，会判断一下当前加锁线程就是自己，那么他自己就可以可重入多次加锁，每次加锁就是把state的值给累加1，别的没啥变化。


接着，如果线程1加锁了之后，线程2跑过来加锁会怎么样呢？

我们来看看锁的互斥是如何实现的？线程2跑过来一下看到，哎呀！state的值不是0啊？所以CAS操作将state从0变为1的过程会失败，因为state的值当前为1，说明已经有人加锁了！接着线程2会看一下，是不是自己之前加的锁啊？当然不是了，“加锁线程”这个变量明确记录了是线程1占用了这个锁，所以线程2此时就是加锁失败。
给大家来一张图，一起来感受一下这个过程：

![AQS3](../../pic//AQS3.png)

接着，线程2会将自己放入AQS中的一个等待队列，因为自己尝试加锁失败了，此时就要将自己放入队列中来等待，等待线程1释放锁之后，自己就可以重新尝试加锁了

所以大家可以看到，AQS是如此的核心！AQS内部还有一个等待队列，专门放那些加锁失败的线程！同样，给大家来一张图，一起感受一下：

![AQS4](../../pic//AQS4.png)

接着，线程1在执行完自己的业务逻辑代码之后，就会释放锁！他释放锁的过程非常的简单，就是将AQS内的state变量的值递减1，如果state值为0，则彻底释放锁，会将“加锁线程”变量也设置为null！整个过程，参见下图：

![AQS5](../../pic//AQS5.png)

接下来，会从等待队列的队头唤醒线程2重新尝试加锁。好！线程2现在就重新尝试加锁，这时还是用CAS操作将state从0变为1，此时就会成功，成功之后代表加锁成功，就会将state设置为1。此外，还要把“加锁线程”设置为线程2自己，同时线程2自己就从等待队列中出队了。最后再来一张图，大家来看看这个过程。

![AQS6](../../pic//AQS6.png)

> 总结

其实一句话总结AQS就是一个并发包的基础组件，用来实现各种锁，各种同步组件的。它包含了state变量、加锁线程、等待队列等并发中的核心组件。


## 3、ReentrantLock源码分析

类中的方法：

![](../../pic/2020-02-01-12-52-24.png)


使用 `synchronized` 来做同步处理时，锁的获取和释放都是隐式的，实现的原理是通过编译后加上不同的机器指令来实现。

而 `ReentrantLock` 就是一个普通的类，它是基于 `AQS(AbstractQueuedSynchronizer)`来实现的。

是一个**重入锁**：一个线程获得了锁之后仍然可以**反复**的加锁，不会出现自己阻塞自己的情况。

> `AQS` 是 `Java` 并发包里实现锁、同步的一个重要的基础框架。


### 1、锁类型

ReentrantLock 分为**公平锁**和**非公平锁**，可以通过构造方法来指定具体类型：

```java
    //默认非公平锁
    public ReentrantLock() {
        sync = new NonfairSync();
    }
    
    //公平锁
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
```

默认一般使用**非公平锁**，它的效率和吞吐量都比公平锁高的多(后面会分析具体原因)。

### 2、获取锁

通常的使用方式如下:

```java
    private ReentrantLock lock = new ReentrantLock();
    public void run() {
        lock.lock();
        try {
            //do bussiness
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
```

#### 1、公平锁获取锁

首先看下获取锁的过程：

```java
    public void lock() {
        sync.lock();
    }
```

可以看到是使用 `sync`的方法，而这个方法是一个抽象方法，具体是由其子类(`FairSync`)来实现的，以下是公平锁的实现:

```java
        final void lock() {
            acquire(1);
        }
        
        //AbstractQueuedSynchronizer 中的 acquire()
        public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    	}
```

第一步是尝试获取锁(`tryAcquire(arg)`),这个也是由其子类实现：

```java
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }
```

首先会判断 `AQS` 中的 `state` 是否等于 0，0 表示目前没有其他线程获得锁，当前线程就可以尝试获取锁。

**注意**:尝试之前会利用 `hasQueuedPredecessors()` 方法来判断 AQS 的队列中中是否有其他线程，如果有则不会尝试获取锁(**这是公平锁特有的情况**)。

如果队列中没有线程就利用 CAS 来将 AQS 中的 state 修改为1，也就是获取锁，获取成功则将当前线程置为获得锁的独占线程(`setExclusiveOwnerThread(current)`)。

如果 `state` 大于 0 时，说明锁已经被获取了，则需要判断获取锁的线程是否为当前线程(`ReentrantLock` 支持重入)，是则需要将 `state + 1`，并将值更新。


##### 1、写入队列

如果 `tryAcquire(arg)` 获取锁失败，则需要用 `addWaiter(Node.EXCLUSIVE)` 将当前线程写入队列中。

写入之前需要将当前线程包装为一个 `Node` 对象(`addWaiter(Node.EXCLUSIVE)`)。

> AQS 中的队列是由 Node 节点组成的双向链表实现的。


包装代码:

```java
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

```

首先判断队列是否为空，不为空时则将封装好的 `Node` 利用 `CAS` 写入队尾，如果出现并发写入失败就需要调用 `enq(node);` 来写入了。

```java
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
```

这个处理逻辑就相当于`自旋`加上 `CAS` 保证一定能写入队列。

##### 2、挂起等待线程

写入队列之后需要将当前线程挂起(利用`acquireQueued(addWaiter(Node.EXCLUSIVE), arg)`)：

```java
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

首先会根据 `node.predecessor()` 获取到上一个节点是否为头节点，如果是则尝试获取一次锁，获取成功就万事大吉了。

如果不是头节点，或者获取锁失败，则会根据上一个节点的 `waitStatus` 状态来处理(`shouldParkAfterFailedAcquire(p, node)`)。

`waitStatus` 用于记录当前节点的状态，如节点取消、节点等待等。

`shouldParkAfterFailedAcquire(p, node)` 返回当前线程是否需要挂起，如果需要则调用 `parkAndCheckInterrupt()`：

```java
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }
```

他是利用 `LockSupport` 的 `part` 方法来挂起当前线程的，直到被唤醒。


#### 2、非公平锁获取锁

公平锁与非公平锁的差异主要在获取锁：

公平锁就相当于买票，后来的人需要排到队尾依次买票，**不能插队**。

而非公平锁则没有这些规则，是**抢占模式**，每来一个人不会去管队列如何，直接尝试获取锁。

非公平锁:
```java
        final void lock() {
            //直接尝试获取锁
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }
```

公平锁:
```java
        final void lock() {
            acquire(1);
        }
```

还要一个重要的区别是在尝试获取锁时`tryAcquire(arg)`，非公平锁是不需要判断队列中是否还有其他线程，也是直接尝试获取锁：

```java
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                //没有 !hasQueuedPredecessors() 判断
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
```

### 3、释放锁

公平锁和非公平锁的释放流程都是一样的：

```java
    public void unlock() {
        sync.release(1);
    }
    
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
            	   //唤醒被挂起的线程
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
    
    //尝试释放锁
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }        
```

首先会判断当前线程是否为获得锁的线程，由于是重入锁所以需要将 `state` 减到 0 才认为完全释放锁。

释放之后需要调用 `unparkSuccessor(h)` 来唤醒被挂起的线程。


## 总结

由于公平锁需要关心队列的情况，得按照队列里的先后顺序来获取锁(会造成大量的线程上下文切换)，而非公平锁则没有这个限制。

所以也就能解释非公平锁的效率会被公平锁更高。








[参考：石杉的架构笔记]