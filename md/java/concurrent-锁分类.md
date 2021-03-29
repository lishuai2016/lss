
<!-- TOC -->

- [1、可重入锁/非可重入锁](#1可重入锁非可重入锁)
- [2、共享锁/独占锁(读锁和写锁)](#2共享锁独占锁读锁和写锁)
- [3、公平锁/非公平锁(ReentrantLock默认非公平，synchronized是非公平锁)](#3公平锁非公平锁reentrantlock默认非公平synchronized是非公平锁)
    - [1、对比公平和非公平的优缺点](#1对比公平和非公平的优缺点)
    - [2、ReentrantLock源码中的公平锁和非公平锁逻辑](#2reentrantlock源码中的公平锁和非公平锁逻辑)
- [4、悲观锁/乐观锁](#4悲观锁乐观锁)
    - [1、悲观锁(锁竞争激烈)](#1悲观锁锁竞争激烈)
    - [2、乐观锁(锁竞争不激烈)](#2乐观锁锁竞争不激烈)
    - [3、典型案例](#3典型案例)
    - [1、悲观锁：synchronized 关键字和 Lock 接口](#1悲观锁synchronized-关键字和-lock-接口)
    - [2、乐观锁：原子类](#2乐观锁原子类)
    - [3、悲观锁和乐观锁：数据库](#3悲观锁和乐观锁数据库)
- [5、自旋锁/非自旋锁](#5自旋锁非自旋锁)
    - [1、对比自旋和非自旋的获取锁的流程](#1对比自旋和非自旋的获取锁的流程)
    - [2、自旋锁的好处](#2自旋锁的好处)
    - [3、AtomicLong 的实现](#3atomiclong-的实现)
    - [4、自己实现一个可重入的自旋锁](#4自己实现一个可重入的自旋锁)
    - [5、自旋锁缺点](#5自旋锁缺点)
    - [6、自旋锁适用场景](#6自旋锁适用场景)
- [6、可中断锁/不可中断锁](#6可中断锁不可中断锁)
- [9、不同进程锁（分布式锁）](#9不同进程锁分布式锁)
    - [1、基于数据库](#1基于数据库)
    - [2、基于 Redis](#2基于-redis)
    - [3、基于 ZK](#3基于-zk)

<!-- /TOC -->




对于 Java 中的锁而言，一把锁也有可能同时占有多个标准，符合多种分类，比如 ReentrantLock 既是可中断锁，又是可重入锁。

根据分类标准我们把锁分为以下6大类别，分别是：

- 可重入锁/非可重入锁；
- 共享锁/独占锁(读锁和写锁)；
- 公平锁/非公平锁；
- 悲观锁/乐观锁；
- 自旋锁/非自旋锁；
- 可中断锁/不可中断锁;

# 1、可重入锁/非可重入锁

可重入锁指的是线程当前已经持有这把锁了，能在不释放这把锁的情况下，再次获取这把锁。同理，不可重入锁指的是虽然线程当前持有了这把锁，但是如果想再次获取这把锁，也必须要先释放锁后才能再次尝试获取。

对于可重入锁而言，最典型的就是 ReentrantLock 了，正如它的名字一样，reentrant 的意思就是可重入，它也是 Lock 接口最主要的一个实现类。

# 2、共享锁/独占锁(读锁和写锁)

共享锁指的是我们同一把锁可以被多个线程同时获得，而独占锁指的就是，这把锁只能同时被一个线程获得。我们的读写锁，就最好地诠释了共享锁和独占锁的理念。`读写锁中的读锁，是共享锁，而写锁是独占锁`。读锁可以被同时读，可以同时被多个线程持有，而写锁最多只能同时被一个线程持有。



所谓的读写锁`ReentrantreadwriteLock`，就是将一个锁拆分为读锁和写锁两个锁，然后你加锁的时候，可以加写锁，也可以加读锁。

- 如果有一个线程加了写锁，那么其他线程就不能加写锁了，同一时间只能允许一个线程加写锁。因为加了写锁就意味着有人要写一个共享数据，那同时就不能让其他人来写这个数据了。同时如果有线程加了写锁，其他线程就不能加读锁了，因为既然都有人在写数据了，你其他人当然不能来读数据了！

- 如果有一个线程加了读锁，别的线程是可以随意同时加读锁的，因为只是有线程在读数据而已，此时别的线程也是可以来读数据的！同理，如果一个线程加了读锁，此时其他线程是不可以加写锁的，因为既然有人在读数据，那就不能让你随意来写数据了！


# 3、公平锁/非公平锁(ReentrantLock默认非公平，synchronized是非公平锁)

公平锁的公平的含义在于如果线程现在拿不到这把锁，那么线程就都会进入等待，开始排队，在等待队列里等待时间长的线程会优先拿到这把锁，有先来先得的意思。而非公平锁就不那么“完美”了，它会在一定情况下，忽略掉已经在排队的线程，发生插队现象。

公平锁指的是按照线程请求的顺序，来分配锁；而非公平锁指的是不完全按照请求的顺序，在一定情况下，可以允许插队。但需要注意这里的非公平并不是指完全的随机，不是说线程可以任意插队，而是仅仅`“在合适的时机”插队`。

那么什么时候是合适的时机呢？假设当前线程在请求获取锁的时候，恰巧前一个持有锁的线程释放了这把锁，那么当前申请锁的线程就可以不顾已经等待的线程而选择立刻插队。但是如果当前线程请求的时候，前一个线程并没有在那一时刻释放锁，那么当前线程还是一样会进入等待队列。


看到这里，你可能不解，为什么要设置非公平策略呢，而且非公平还是 ReentrantLock的默认策略，如果我们不加以设置的话默认就是非公平的，难道我的这些排队的时间都白白浪费了吗，为什么别人比我有优先权呢？毕竟公平是一种很好的行为，而非公平是一种不好的行为。


让我们考虑一种情况，假设线程 A 持有一把锁，线程 B 请求这把锁，由于线程 A 已经持有这把锁了，所以线程 B 会陷入等待，在等待的时候线程 B 会被挂起，也就是进入阻塞状态，那么当线程 A 释放锁的时候，本该轮到线程 B 苏醒获取锁，但如果此时突然有一个线程 C 插队请求这把锁，那么根据非公平的策略，会把这把锁给线程 C，这是因为唤醒线程 B 是需要很大开销的，很有可能在唤醒之前，线程 C 已经拿到了这把锁并且执行完任务释放了这把锁。相比于等待唤醒线程 B 的漫长过程，插队的行为会让线程 C 本身跳过陷入阻塞的过程，如果在锁代码中执行的内容不多的话，线程 C 就可以很快完成任务，并且在线程 B 被完全唤醒之前，就把这个锁交出去，这样是一个双赢的局面，对于线程 C 而言，不需要等待提高了它的效率，而对于线程 B 而言，它获得锁的时间并没有推迟，因为等它被唤醒的时候，线程 C 早就释放锁了，因为线程 C 的执行速度相比于线程 B 的唤醒速度，是很快的，所以 Java 设计者设计非公平锁，是为了提高整体的运行效率。

> 1、公平的场景

下面我们用图示来说明公平和非公平的场景，先来看公平的情况。假设我们创建了一个公平锁，此时有 4 个线程按顺序来请求公平锁，线程 1 在拿到这把锁之后，线程 2、3、4 会在等待队列中开始等待，然后等线程 1 释放锁之后，线程 2、3、4 会依次去获取这把锁，线程 2 先获取到的原因是它等待的时间最长。

> 2、不公平的场景

下面我们再来看看非公平的情况，假设线程 1 在解锁的时候，突然有线程 5 尝试获取这把锁，那么根据我们的非公平策略，线程 5 是可以拿到这把锁的，尽管它没有进入等待队列，而且线程 2、3、4 等待的时间都比线程 5 要长，但是从整体效率考虑，这把锁此时还是会交给线程 5 持有。



> 3、代码案例：演示公平和非公平的效果

下面我们来用代码演示看下公平和非公平的实际效果，代码如下：

```java
//描述：演示公平锁，分别展示公平和不公平的情况，非公平锁会让现在持有锁的线程优先再次获取到锁。代码借鉴自Java并发编程实战手册2.7。
public class FairAndUnfair {
    public static void main(String args[]) {
        PrintQueue printQueue = new PrintQueue();//执行内部的方法需要加锁
        Thread thread[] = new Thread[10];
        for (int i = 0; i < 10; i++) {
            thread[i] = new Thread(new Job(printQueue), "Thread " + i);
        }


        for (int i = 0; i < 10; i++) {//初始化10个线程并发调用
            thread[i].start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}


class Job implements Runnable {
    private PrintQueue printQueue;
    public Job(PrintQueue printQueue) {
        this.printQueue = printQueue;
    }

    @Override
    public void run() {
        System.out.printf("%s: Going to print a job\n", Thread.currentThread().getName());
        printQueue.printJob(new Object());
        System.out.printf("%s: The document has been printed\n", Thread.currentThread().getName());
    }


}


class PrintQueue {
    private final Lock queueLock = new ReentrantLock(false);//

    public void printJob(Object document) {
        queueLock.lock();

        try {
            Long duration = (long) (Math.random() * 10000);
            System.out.printf("%s: PrintQueue: Printing a Job during %d seconds\n",
                    Thread.currentThread().getName(), (duration / 1000));
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            queueLock.unlock();
        }


        queueLock.lock();
        try {
            Long duration = (long) (Math.random() * 10000);
            System.out.printf("%s: PrintQueue: Printing a Job during %d seconds\n",
                    Thread.currentThread().getName(), (duration / 1000));
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            queueLock.unlock();
            }
    }
}
//我们可以通过改变 new ReentrantLock(false) 中的参数来设置公平/非公平锁。以上代码在公平的情况下的输出：

Thread 0: Going to print a job
Thread 0: PrintQueue: Printing a Job during 5 seconds
Thread 1: Going to print a job
Thread 2: Going to print a job
Thread 3: Going to print a job
Thread 4: Going to print a job
Thread 5: Going to print a job
Thread 6: Going to print a job
Thread 7: Going to print a job
Thread 8: Going to print a job
Thread 9: Going to print a job
Thread 1: PrintQueue: Printing a Job during 3 seconds
Thread 2: PrintQueue: Printing a Job during 4 seconds
Thread 3: PrintQueue: Printing a Job during 3 seconds
Thread 4: PrintQueue: Printing a Job during 9 seconds
Thread 5: PrintQueue: Printing a Job during 5 seconds
Thread 6: PrintQueue: Printing a Job during 7 seconds
Thread 7: PrintQueue: Printing a Job during 3 seconds
Thread 8: PrintQueue: Printing a Job during 9 seconds
Thread 9: PrintQueue: Printing a Job during 5 seconds
Thread 0: PrintQueue: Printing a Job during 8 seconds
Thread 0: The document has been printed
Thread 1: PrintQueue: Printing a Job during 1 seconds
Thread 1: The document has been printed
Thread 2: PrintQueue: Printing a Job during 8 seconds
Thread 2: The document has been printed
Thread 3: PrintQueue: Printing a Job during 2 seconds
Thread 3: The document has been printed
Thread 4: PrintQueue: Printing a Job during 0 seconds
Thread 4: The document has been printed
Thread 5: PrintQueue: Printing a Job during 7 seconds
Thread 5: The document has been printed
Thread 6: PrintQueue: Printing a Job during 3 seconds
Thread 6: The document has been printed
Thread 7: PrintQueue: Printing a Job during 9 seconds
Thread 7: The document has been printed
Thread 8: PrintQueue: Printing a Job during 5 seconds
Thread 8: The document has been printed
Thread 9: PrintQueue: Printing a Job during 9 seconds
Thread 9: The document has been printed

//可以看出，线程直接获取锁的顺序是完全公平的，先到先得。而以上代码在非公平的情况下的输出是这样的：

Thread 0: Going to print a job
Thread 0: PrintQueue: Printing a Job during 6 seconds
Thread 1: Going to print a job
Thread 2: Going to print a job
Thread 3: Going to print a job
Thread 4: Going to print a job
Thread 5: Going to print a job
Thread 6: Going to print a job
Thread 7: Going to print a job
Thread 8: Going to print a job
Thread 9: Going to print a job
Thread 0: PrintQueue: Printing a Job during 8 seconds
Thread 0: The document has been printed
Thread 1: PrintQueue: Printing a Job during 9 seconds
Thread 1: PrintQueue: Printing a Job during 8 seconds
Thread 1: The document has been printed
Thread 2: PrintQueue: Printing a Job during 6 seconds
Thread 2: PrintQueue: Printing a Job during 4 seconds
Thread 2: The document has been printed
Thread 3: PrintQueue: Printing a Job during 9 seconds
Thread 3: PrintQueue: Printing a Job during 8 seconds
Thread 3: The document has been printed
Thread 4: PrintQueue: Printing a Job during 4 seconds
Thread 4: PrintQueue: Printing a Job during 2 seconds
Thread 4: The document has been printed
Thread 5: PrintQueue: Printing a Job during 2 seconds
Thread 5: PrintQueue: Printing a Job during 5 seconds
Thread 5: The document has been printed
Thread 6: PrintQueue: Printing a Job during 2 seconds
Thread 6: PrintQueue: Printing a Job during 6 seconds
Thread 6: The document has been printed
Thread 7: PrintQueue: Printing a Job during 6 seconds
Thread 7: PrintQueue: Printing a Job during 4 seconds
Thread 7: The document has been printed
Thread 8: PrintQueue: Printing a Job during 3 seconds
Thread 8: PrintQueue: Printing a Job during 6 seconds
Thread 8: The document has been printed
Thread 9: PrintQueue: Printing a Job during 3 seconds
Thread 9: PrintQueue: Printing a Job during 5 seconds
Thread 9: The document has been printed
```


可以看出，非公平情况下，存在抢锁“插队”的现象，比如Thread 0 在释放锁后又能优先获取到锁，虽然此时在等待队列中已经有 Thread 1 ~ Thread 9 在排队了。

## 1、对比公平和非公平的优缺点

我们接下来对比公平和非公平的优缺点，如表格所示。

![](../../pic/2020-06-21/2020-06-21-16-26-53.png)

公平锁的优点在于各个线程公平平等，每个线程等待一段时间后，都有执行的机会，而它的缺点就在于整体执行速度更慢，吞吐量更小，相反非公平锁的优势就在于整体执行速度更快，吞吐量更大，但同时也可能产生线程饥饿问题，也就是说如果一直有线程插队，那么在等待队列中的线程可能长时间得不到运行。

## 2、ReentrantLock源码中的公平锁和非公平锁逻辑

下面我们来分析公平和非公平锁的源码，具体看下它们是怎样实现的，可以看到在 ReentrantLock 类包含一个 Sync 类，这个类继承自AQS（AbstractQueuedSynchronizer），代码如下：

```java

public class ReentrantLock implements Lock, java.io.Serializable {
 
private static final long serialVersionUID = 7373984872572414699L;
 
/** Synchronizer providing all implementation mechanics */
 
private final Sync sync;

//Sync 类，Sync 有公平锁 FairSync 和非公平锁 NonfairSync两个子类
abstract static class Sync extends AbstractQueuedSynchronizer {...}

static final class NonfairSync extends Sync {...}
static final class FairSync extends Sync {...}

//公平锁的锁获取源码如下：
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() && //这里判断了 hasQueuedPredecessors()
                compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) {
            throw new Error("Maximum lock count exceeded");
        }
        setState(nextc);
        return true;
    }
    return false;
}

//非公平锁的锁获取源码如下：
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (compareAndSetState(0, acquires)) { //这里没有判断，hasQueuedPredecessors()
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
        throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```


通过对比，我们可以明显的看出公平锁与非公平锁的 lock() 方法唯一的区别就在于公平锁在获取锁时多了一个限制条件：hasQueuedPredecessors() 为 false，这个方法就是判断在等待队列中是否已经有线程在排队了。`这也就是公平锁和非公平锁的核心区别，如果是公平锁，那么一旦已经有线程在排队了，当前线程就不再尝试获取锁；对于非公平锁而言，无论是否已经有线程在排队，都会尝试获取一下锁，获取不到的话，再去排队。`

这里有一个特例需要我们注意，针对 tryLock() 方法，它不遵守设定的公平原则。

例如，当有线程执行 tryLock() 方法的时候，一旦有线程释放了锁，那么这个正在 tryLock 的线程就能获取到锁，即使设置的是公平锁模式，即使在它之前已经有其他正在等待队列中等待的线程，简单地说就是 tryLock 可以插队。

看它的源码就会发现：

```java
public boolean tryLock() {
    return sync.nonfairTryAcquire(1);
}
```

这里调用的就是 nonfairTryAcquire()，表明了是不公平的，和锁本身是否是公平锁无关。

综上所述，公平锁就是会按照多个线程申请锁的顺序来获取锁，从而实现公平的特性。非公平锁加锁时不考虑排队等待情况，直接尝试获取锁，所以存在后申请却先获得锁的情况，但由此也提高了整体的效率。




# 4、悲观锁/乐观锁

悲观锁的概念是在获取资源之前，必须先拿到锁，以便达到“独占”的状态，当前线程在操作资源的时候，其他线程由于不能拿到锁，所以其他线程不能来影响我。而乐观锁恰恰相反，它并不要求在获取资源前拿到锁，也不会锁住资源；相反，乐观锁利用 CAS 理念，在不独占资源的情况下，完成了对资源的修改。


> 悲观锁和乐观锁的本质是什么？

`悲观锁和乐观锁是从是否锁住资源的角度进行分类的`

## 1、悲观锁(锁竞争激烈)

悲观锁比较悲观，它认为如果不锁住这个资源，别的线程就会来争抢，就会造成数据结果错误，所以悲观锁为了确保结果的正确性，会在每次获取并修改数据时，都把数据锁住，让其他线程无法访问该数据，这样就可以确保数据内容万无一失。


我们举个例子，假设线程 A 和 B 使用的都是悲观锁，所以它们在尝试获取同步资源时，必须要先拿到锁。

假设线程 A 拿到了锁，并且正在操作同步资源，那么此时线程 B 就必须进行等待。

而当线程 A 执行完毕后，CPU 才会唤醒正在等待这把锁的线程 B 再次尝试获取锁。

如果线程 B 现在获取到了锁，才可以对同步资源进行自己的操作。这就是悲观锁的操作流程。

## 2、乐观锁(锁竞争不激烈)

乐观锁比较乐观，认为自己在操作资源的时候不会有其他线程来干扰，所以并不会锁住被操作对象，不会不让别的线程来接触它，同时，为了确保数据正确性，在更新之前，会去对比在我修改数据期间，数据有没有被其他线程修改过：如果没被修改过，就说明真的只有我自己在操作，那我就可以正常的修改数据；如果发现数据和我一开始拿到的不一样了，说明其他线程在这段时间内修改过数据，那说明我迟了一步，所以我会放弃这次修改，并选择报错、重试等策略。


`乐观锁的实现一般都是利用 CAS 算法实现的`。我们举个例子，假设线程 A 此时运用的是乐观锁。那么它去操作同步资源的时候，不需要提前获取到锁，而是可以直接去读取同步资源，并且在自己的线程内进行计算。

当它计算完毕之后、准备更新同步资源之前，会先判断这个资源是否已经被其他线程所修改过。

如果这个时候同步资源没有被其他线程修改更新，也就是说此时的数据和线程 A 最开始拿到的数据是一致的话，那么此时线程 A 就会去更新同步资源，完成修改的过程。

而假设此时的同步资源已经被其他线程修改更新了，线程 A 会发现此时的数据已经和最开始拿到的数据不一致了，那么线程 A 不会继续修改该数据，而是会根据不同的业务逻辑去选择报错或者重试。

`悲观锁和乐观锁概念并不是 Java 中独有的，这是一种广义的思想，这种思想可以应用于其他领域，比如说在数据库中，同样也有对悲观锁和乐观锁的应用。`

## 3、典型案例

## 1、悲观锁：synchronized 关键字和 Lock 接口

Java 中悲观锁的实现包括 synchronized 关键字和 Lock 相关类等，我们以 Lock 接口为例，例如 Lock 的实现类 ReentrantLock，类中的 lock() 等方法就是执行加锁，而 unlock() 方法是执行解锁。处理资源之前必须要先加锁并拿到锁，等到处理完了之后再解开锁，这就是非常典型的悲观锁思想。

## 2、乐观锁：原子类

乐观锁的典型案例就是原子类，例如 AtomicInteger 在更新数据时，就使用了乐观锁的思想，多个线程可以同时操作同一个原子变量。

## 3、悲观锁和乐观锁：数据库

数据库中同时拥有悲观锁和乐观锁的思想。例如，我们如果在 MySQL 选择 select for update 语句，那就是悲观锁，在提交之前不允许第三方来修改该数据，这当然会造成一定的性能损耗，在高并发的情况下是不可取的。

相反，我们可以利用一个版本 version 字段在数据库中实现乐观锁。在获取及修改数据时都不需要加锁，但是我们在获取完数据并计算完毕，准备更新数据时，会检查版本号和获取数据时的版本号是否一致，如果一致就直接更新，如果不一致，说明计算期间已经有其他线程修改过这个数据了，那我就可以选择重新获取数据，重新计算，然后再次尝试更新数据。

SQL语句示例如下（假设取出数据的时候 version 为1）：

```sql
UPDATE student
    SET 
        name = ‘小李’,
        version= 2
    WHERE   id= 100
        AND version= 1
```


有一种说法认为，悲观锁由于它的操作比较重量级，不能多个线程并行执行，而且还会有上下文切换等动作，所以悲观锁的性能不如乐观锁好，应该尽量避免用悲观锁，这种说法是不正确的。

因为虽然悲观锁确实会让得不到锁的线程阻塞，但是这种开销是固定的。悲观锁的原始开销确实要高于乐观锁，但是特点是一劳永逸，就算一直拿不到锁，也不会对开销造成额外的影响。

反观乐观锁虽然一开始的开销比悲观锁小，但是如果一直拿不到锁，或者并发量大，竞争激烈，导致不停重试，那么消耗的资源也会越来越多，甚至开销会超过悲观锁。

所以，同样是悲观锁，在不同的场景下，效果可能完全不同，可能在今天的这种场景下是好的选择，在明天的另外的场景下就是坏的选择，这恰恰是“汝之蜜糖，彼之砒霜”。

因此，我们就来看一下两种锁各自的使用场景，把合适的锁用到合适的场景中去，把合理的资源分配到合理的地方去。

> 两种锁各自的使用场景

悲观锁适合用于并发写入多、临界区代码复杂、竞争激烈等场景，这种场景下悲观锁可以避免大量的无用的反复尝试等消耗。

乐观锁适用于大部分是读取，少部分是修改的场景，也适合虽然读写都很多，但是并发并不激烈的场景。在这些场景下，乐观锁不加锁的特点能让性能大幅提高。


# 5、自旋锁/非自旋锁

自旋锁的理念是如果线程现在拿不到锁，并不直接陷入阻塞或者释放 CPU 资源，而是开始利用循环，不停地尝试获取锁，这个循环过程被形象地比喻为“自旋”，就像是线程在“自我旋转”。相反，非自旋锁的理念就是没有自旋的过程，如果拿不到锁就直接放弃，或者进行其他的处理逻辑，例如去排队、陷入阻塞等。

首先，我们了解什么叫自旋？“自旋”可以理解为“自我旋转”，这里的“旋转”指“循环”，比如 while 循环或者 for 循环。“自旋”就是自己在这里不停地循环，直到目标达成。而不像普通的锁那样，如果获取不到锁就进入阻塞。

## 1、对比自旋和非自旋的获取锁的流程

下面我们用这样一张流程图来对比一下自旋锁和非自旋锁的获取锁的过程。

![](../../pic/2020-06-21/2020-06-21-20-27-58.png)

首先，我们来看自旋锁，它并不会放弃  CPU  时间片，而是通过自旋等待锁的释放，也就是说，它会不停地再次地尝试获取锁，如果失败就再次尝试，直到成功为止。

我们再来看下非自旋锁，非自旋锁和自旋锁是完全不一样的，如果它发现此时获取不到锁，它就把自己的线程切换状态，让线程休眠，然后 CPU 就可以在这段时间去做很多其他的事情，直到之前持有这把锁的线程释放了锁，于是 CPU 再把之前的线程恢复回来，让这个线程再去尝试获取这把锁。如果再次失败，就再次让线程休眠，如果成功，一样可以成功获取到同步资源的锁。

可以看出，非自旋锁和自旋锁最大的区别，就是如果它遇到拿不到锁的情况，它会把线程阻塞，直到被唤醒。而自旋锁会不停地尝试。那么，自旋锁这样不停尝试的好处是什么呢？

## 2、自旋锁的好处

首先，阻塞和唤醒线程都是需要高昂的开销的，如果同步代码块中的内容不复杂，那么可能转换线程带来的开销比实际业务代码执行的开销还要大。

在很多场景下，可能我们的同步代码块的内容并不多，所以需要的执行时间也很短，如果我们仅仅为了这点时间就去切换线程状态，那么其实不如让线程不切换状态，而是让它自旋地尝试获取锁，等待其他线程释放锁，有时我只需要稍等一下，就可以避免上下文切换等开销，提高了效率。

用一句话总结自旋锁的好处，那就是自旋锁用循环去不停地尝试获取锁，让线程始终处于 Runnable 状态，节省了线程状态切换带来的开销。

## 3、AtomicLong 的实现

在 Java 1.5 版本及以上的并发包中，也就是 java.util.concurrent 的包中，里面的原子类基本都是自旋锁的实现。

比如我们看一个 AtomicLong 的实现，里面有一个 getAndIncrement 方法，源码如下：

```java
public final long getAndIncrement() {
    return unsafe.getAndAddLong(this, valueOffset, 1L);
}
//可以看到它调用了一个 unsafe.getAndAddLong
public final long getAndAddLong (Object var1,long var2, long var4){
    long var6;
    do {
        var6 = this.getLongVolatile(var1, var2);//获取最新的值
    } while (!this.compareAndSwapLong(var1, var2, var6, var6 + var4));


    return var6;
}
```

这里的 do-while 循环就是一个自旋操作，如果在修改过程中遇到了其他线程竞争导致没修改成功的情况，就会 while 循环里进行死循环，直到修改成功为止。

## 4、自己实现一个可重入的自旋锁

```java
public class ReentrantSpinLock  {//实现一个可重入的自旋锁
 
    private AtomicReference<Thread> owner = new AtomicReference<>();
 
    //重入次数
    private int count = 0;
 
    public void lock() {
        Thread t = Thread.currentThread();
        if (t == owner.get()) {
            ++count;
            return;
        }
        //自旋获取锁
        while (!owner.compareAndSet(null, t)) {
            System.out.println("自旋了");
        }
    }
 
    public void unlock() {
        Thread t = Thread.currentThread();
        //只有持有锁的线程才能解锁
        if (t == owner.get()) {
            if (count > 0) {
                --count;
            } else {
                //此处无需CAS操作，因为没有竞争，因为只有线程持有者才能解锁
                owner.set(null);
            }
        }
    }
 
    public static void main(String[] args) {
        ReentrantSpinLock spinLock = new ReentrantSpinLock();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "开始尝试获取自旋锁");
                spinLock.lock();
                try {
                    System.out.println(Thread.currentThread().getName() + "获取到了自旋锁");
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    spinLock.unlock();
                    System.out.println(Thread.currentThread().getName() + "释放了了自旋锁");
                }
            }
        };
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        thread1.start();
        thread2.start();
    }
}
```

这段代码的运行结果是：

```java
Thread-0获取到了自旋锁
...
自旋了
自旋了
自旋了
自旋了
自旋了
自旋了
自旋了
自旋了
Thread-0释放了了自旋锁
Thread-1获取到了自旋锁
```

前面会打印出很多“自旋了”，说明自旋期间，CPU依然在不停运转。

## 5、自旋锁缺点

那么自旋锁有没有缺点呢？其实自旋锁是有缺点的。它最大的缺点就在于虽然避免了线程切换的开销，但是它在避免线程切换开销的同时也带来了新的开销，因为它需要不停得去尝试获取锁。如果这把锁一直不能被释放，那么这种尝试只是无用的尝试，会白白浪费处理器资源。也就是说，虽然一开始自旋锁的开销低于线程切换，但是随着时间的增加，这种开销也是水涨船高，后期甚至会超过线程切换的开销，得不偿失。

## 6、自旋锁适用场景

所以我们就要看一下自旋锁的适用场景。首先，自旋锁适用于并发度不是特别高的场景，以及临界区比较短小的情况，这样我们可以利用避免线程切换来提高效率。

可是如果临界区很大，线程一旦拿到锁，很久才会释放的话，那就不合适用自旋锁，因为自旋会一直占用 CPU 却无法拿到锁，白白消耗资源。


# 6、可中断锁/不可中断锁

在 Java 中，synchronized 关键字修饰的锁代表的是不可中断锁，一旦线程申请了锁，就没有回头路了，只能等到拿到锁以后才能进行其他的逻辑处理。而我们的 ReentrantLock 是一种典型的可中断锁，例如使用 lockInterruptibly 方法在获取锁的过程中，突然不想获取了，那么也可以在中断之后去做其他的事情，不需要一直傻等到获取到锁才离开。








# 9、不同进程锁（分布式锁）

`jdk自带的锁可以认为是同一进程内的锁`

备注：在自己设计一个锁的时候也需要考虑上面这些锁的特性。

## 1、基于数据库

可以创建一张表，将其中的某个字段设置为`唯一索引`，当多个请求过来的时候只有新建记录成功的请求才算获取到锁，当使用完毕删除这条记录的时候即释放锁。

存在的问题:
- 数据库单点问题，挂了怎么办？
- 不是重入锁，同一进程无法在释放锁之前再次获得锁，因为数据库中已经存在了一条记录了。
- 锁是非阻塞的，一旦 `insert` 失败则会立即返回，并不会进入阻塞队列只能下一次再次获取。
- 锁没有失效时间，如果那个进程解锁失败那就没有请求可以再次获取锁了。

解决方案:
- 数据库切换为主从，不存在单点。
- 在表中加入一个同步状态字段，每次获取锁的是加 1 ，释放锁的时候`-1`，当状态为 0 的时候就删除这条记录，即释放锁。
- 非阻塞的情况可以用 `while` 循环来实现，循环的时候记录时间，达到 X 秒记为超时，`break`。
- 可以开启一个定时任务每隔一段时间扫描找出多少 X 秒都没有被删除的记录，主动删除这条记录。

## 2、基于 Redis

使用 `setNX(key) setEX(timeout)` 命令，只有在该 `key` 不存在的时候创建这个 `key`，就相当于获取了锁。由于有超时时间，所以过了规定时间会自动删除，这样也可以避免死锁。

可以参考：

[基于 Redis 的分布式锁](http://crossoverjie.top/2018/03/29/distributed-lock/distributed-lock-redis/)

## 3、基于 ZK



