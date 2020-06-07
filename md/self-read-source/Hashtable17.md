

![](../../pic/2020-04-07-08-59-06.png)

# 1、Dictionary

![](../../pic/2020-04-07-09-07-10.png)


```java
public abstract
class Dictionary<K,V> {
public Dictionary() {
    }


abstract public int size();

abstract public boolean isEmpty();

abstract public Enumeration<K> keys();//key列表

abstract public Enumeration<V> elements();//value列表

abstract public V get(Object key);

abstract public V put(K key, V value);

abstract public V remove(Object key);


}
```






# 9、HashTable和HashMap区别

## 1、继承的父类不同
 
Hashtable继承自Dictionary类，而HashMap继承自AbstractMap类。但二者都实现了Map接口。

## 2、线程安全性不同(修改会被覆盖)

javadoc中关于hashmap的一段描述如下：此实现不是同步的。如果多个线程同时访问一个哈希映射，而其中至少一个线程从结构上修改了该映射，则它必须保持外部同步。

Hashtable 中的方法是Synchronize的，而HashMap中的方法在缺省情况下是非Synchronize的。在多线程并发的环境下，可以直接使用Hashtable，不需要自己为它的方法实现同步，但使用HashMap时就必须要自己增加同步处理。（结构上的修改是指添加或删除一个或多个映射关系的任何操作；仅改变与实例已经包含的键关联的值不是结构上的修改。）这一般通过对自然封装该映射的对象进行同步操作来完成。如果不存在这样的对象，则应该使用 Collections.synchronizedMap 方法来“包装”该映射。最好在创建时完成这一操作，以防止对映射进行意外的非同步访问，如下所示：

Map m = Collections.synchronizedMap(new HashMap(...));

Hashtable 线程安全很好理解，因为它每个方法中都加入了Synchronize。这里我们分析一下HashMap为什么是线程不安全的：


HashMap底层是一个Entry数组，当发生hash冲突的时候，hashmap是采用链表的方式来解决的，在对应的数组位置存放链表的头结点。对链表而言，新加入的节点会从头结点加入。

我们来分析一下多线程访问：

> （1）在hashmap做put操作的时候会调用下面方法：

 ```java
void createEntry(int hash, K key, V value, int bucketIndex) {
    Entry<K,V> e = table[bucketIndex];//这里采用头插法构建冲突链表，每次新加入的节点在链表的头部
    table[bucketIndex] = new Entry<>(hash, key, value, e);
    size++;
}

 ```

在hashmap做put操作的时候会调用到以上的方法。现在假如A线程和B线程同时对同一个数组位置调用createEntry，两个线程会同时得到现在的头结点，然后A写入新的头结点之后，B也写入新的头结点，那B的写入操作就会覆盖A的写入操作造成A的写入操作丢失。

> （2）删除键值对的代码

```java
final Entry<K,V> removeEntryForKey(Object key) {
    if (size == 0) {
        return null;
    }
    int hash = (key == null) ? 0 : hash(key);//计算hash值
    int i = indexFor(hash, table.length);//找在数组中的位置
    Entry<K,V> prev = table[i];//指向链表的头部
    Entry<K,V> e = prev;//查找的临时变量
    //本质单链表删除节点
    while (e != null) {//如果遍历完链表没有找到返回null
        Entry<K,V> next = e.next;
        Object k;
        if (e.hash == hash &&
            ((k = e.key) == key || (key != null && key.equals(k)))) {//找到了要删除的节点
            modCount++;
            size--;
            if (prev == e)//针对头节点满足的情况
                table[i] = next;
            else //非头结点，直接通过修改指针跳过要删除的节点e即可
                prev.next = next;
            e.recordRemoval(this);
            return e;
        }
        prev = e;//更新变量，到这里prev和e会指向不同的对象，初始化会指向同一个表头节点
        e = next;
    }

    return e;
}


```

当多个线程同时操作同一个数组位置的时候，也都会先取得现在状态下该位置存储的头结点，然后各自去进行计算操作，之后再把结果写会到该数组位置去，其实写回的时候可能其他的线程已经就把这个位置给修改过了，就会覆盖其他线程的修改

> （3）addEntry中当加入新的键值对后键值对总数量超过门限值的时候会调用一个resize操作，代码如下：

```java
//扩容函数
void resize(int newCapacity) {
    Entry[] oldTable = table;
    int oldCapacity = oldTable.length;
    if (oldCapacity == MAXIMUM_CAPACITY) {//表的容量已经达到最大值，这个时候只能把门限值也改成最大了
        threshold = Integer.MAX_VALUE;
        return;
    }

    Entry[] newTable = new Entry[newCapacity];//创建一个新数组
    transfer(newTable, initHashSeedAsNeeded(newCapacity));
    table = newTable;//表数组指向新建后的数组
    threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);//更新门限值
}

```

这个操作会新生成一个新的容量的数组，然后对原数组的所有键值对重新进行计算和写入新的数组，之后指向新生成的数组。

当多个线程同时检测到总数量超过门限值的时候就会同时调用resize操作，各自生成新的数组并rehash后赋给该map底层的数组table，结果最终只有最后一个线程生成的新数组被赋给table变量，其他线程的均会丢失。而且当某些线程已经完成赋值而其他线程刚开始的时候，就会用已经被赋值的table作为原始数组，这样也会有问题。

## 3、是否提供contains方法
 
HashMap把Hashtable的contains方法去掉了，改成containsValue和containsKey，因为contains方法容易让人引起误解。

Hashtable则保留了contains，containsValue和containsKey三个方法，其中contains和containsValue功能相同。

## 4、key和value是否允许null值
 
其中key和value都是对象，并且不能包含重复key，但可以包含重复的value。

通过上面的ContainsKey方法和ContainsValue的源码我们可以很明显的看出：

Hashtable中，key和value都不允许出现null值。但是如果在Hashtable中有类似put(null,null)的操作，编译同样可以通过，因为key和value都是Object类型，但运行时会抛出NullPointerException异常，这是JDK的规范规定的。


HashMap中，null可以作为键，这样的键只有一个；可以有一个或多个键所对应的值为null。当get()方法返回null值时，可能是 HashMap中没有该键，也可能使该键所对应的值为null。因此，在HashMap中不能由get()方法来判断HashMap中是否存在某个键， 而应该用containsKey()方法来判断。

## 5、两个遍历方式的内部实现上不同
      
Hashtable、HashMap都使用了 Iterator。而由于历史原因，Hashtable还使用了Enumeration的方式 。

## 6、hash值不同

哈希值的使用不同，HashTable直接使用对象的hashCode。而HashMap重新计算hash值。

hashCode是jdk根据对象的地址或者字符串或者数字算出来的int类型的数值。

Hashtable计算hash值，直接用key的hashCode()，而HashMap重新计算了key的hash值，Hashtable在求hash值对应的位置索引时，用取模运算，而HashMap在求位置索引时，则用与运算，且这里一般先用hash&0x7FFFFFFF后，再对length取模，&0x7FFFFFFF的目的是为了将负的hash值转化为正值，因为hash值有可能为负数，而&0x7FFFFFFF后，只有符号外改变，而后面的位都不变。

## 7、内部实现使用的数组初始化和扩容方式不同

HashTable在不指定容量的情况下的默认容量为11，而HashMap为16，Hashtable不要求底层数组的容量一定要为2的整数次幂，而HashMap则要求一定为2的整数次幂。

Hashtable扩容时，将容量变为原来的2倍加1，而HashMap扩容时，将容量变为原来的2倍。

Hashtable和HashMap它们两个内部实现方式的数组的初始大小和扩容的方式。HashTable中hash数组默认大小是11，增加的方式是 old*2+1。



# 参考

- [HashTable和HashMap的区别详解](https://www.cnblogs.com/williamjie/p/9099141.html)

