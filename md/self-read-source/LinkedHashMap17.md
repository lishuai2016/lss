

<!-- TOC -->

- [0、源码解析主要方法](#0源码解析主要方法)
- [1、Entry](#1entry)
- [2、LinkedHashIterator](#2linkedhashiterator)
- [3、EntryIterator迭代器](#3entryiterator迭代器)
- [4、KeyIterator](#4keyiterator)
- [5、ValueIterator](#5valueiterator)
- [6、利用LinkedHashMap实现LRU算法缓存](#6利用linkedhashmap实现lru算法缓存)
- [9、linkedhashmap的结构](#9linkedhashmap的结构)
- [参考](#参考)

<!-- /TOC -->


大多数情况下，只要不涉及线程安全问题，Map基本都可以使用HashMap，不过HashMap有一个问题，就是**迭代HashMap的顺序并不是HashMap放置的顺序**，也就是无序。HashMap的这一缺点往往会带来困扰，因为有些场景，我们期待一个有序的Map。

这个时候，LinkedHashMap就闪亮登场了，它虽然增加了时间和空间上的开销，但是**通过维护一个运行于所有条目的双向链表，LinkedHashMap保证了元素迭代的顺序。该迭代顺序可以是插入顺序或者是访问顺序**。

备注：简单可以理解为LinkedHashMap=hashmap+linkedlist


# 0、源码解析主要方法


继承自hashmap

![](../../pic/2020-04-06-21-32-44.png)


![](../../pic/2020-04-06-21-33-13.png)


```java
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V>
{
//属性

private transient Entry<K,V> header;//双向链表的头部。不放入hashtable中
private final boolean accessOrder;//迭代器顺序；true标识访问顺序，false为插入顺序

//构造函数同hashmap的构造函数
public LinkedHashMap() {
    super();//调用hashmap的无参数构造器
    accessOrder = false;//迭代器为插入的顺序
}

public LinkedHashMap(int initialCapacity) {
    super(initialCapacity);
    accessOrder = false;
}

public LinkedHashMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false;
}

public LinkedHashMap(Map<? extends K, ? extends V> m) {
    super(m);
    accessOrder = false;
}

//比hashmap多的一个构造函数，可以指定是否采用访问顺序构建链表。访问包含：get和set两个操作
public LinkedHashMap(int initialCapacity,
                        float loadFactor,
                        boolean accessOrder) {
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
}



//hashmap的构造函数中留的钩子函数
 @Override
void init() {
    header = new Entry<>(-1, null, null, null);//初始化双向链表的头节点
    header.before = header.after = header;
}



/**
* This override alters behavior of superclass put method. It causes newly
* allocated entry to get inserted at the end of the linked list and
* removes the eldest entry if appropriate.
*/
//调用put存值的时候会调用这里
void addEntry(int hash, K key, V value, int bucketIndex) {
    super.addEntry(hash, key, value, bucketIndex);//调用父类的添加逻辑
    // Remove eldest entry if instructed
    //删除最近最少使用元素的策略定义 ,可以支持FIFO算法
    Entry<K,V> eldest = header.after;
    if (removeEldestEntry(eldest)) {
        removeEntryForKey(eldest.key);
    }
}


protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
    return false;
}


 /**
     * Removes and returns the entry associated with the specified key
     * in the HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
final Entry<K,V> removeEntryForKey(Object key) {
    if (size == 0) {
        return null;
    }
    int hash = (key == null) ? 0 : hash(key);
    int i = indexFor(hash, table.length);//找位置
    Entry<K,V> prev = table[i];
    Entry<K,V> e = prev;

    while (e != null) {
        Entry<K,V> next = e.next;
        Object k;
        if (e.hash == hash &&
            ((k = e.key) == key || (key != null && key.equals(k)))) {
            modCount++;
            size--;
            if (prev == e)//表中的头结点原元素
                table[i] = next;
            else
                prev.next = next;
            e.recordRemoval(this);//把当前节点移除双向链表
            return e;
        }
        prev = e;
        e = next;
    }

    return e;
}


 /**
     * This override differs from addEntry in that it doesn't resize the
     * table or remove the eldest entry.
     */
//重写了父类创建entry的逻辑
void createEntry(int hash, K key, V value, int bucketIndex) {
    HashMap.Entry<K,V> old = table[bucketIndex];
    Entry<K,V> e = new Entry<>(hash, key, value, old);
    table[bucketIndex] = e;
    e.addBefore(header);//新增的逻辑在这里实现迭代器里返回按照插入的先后顺序，添加到双向链表的尾部了
    size++;
}



//获取key对应的值
public V get(Object key) {
    Entry<K,V> e = (Entry<K,V>)getEntry(key);//调用父类的函数
    if (e == null)
        return null;
    e.recordAccess(this);//这个是关键
    return e.value;
}


/**
    * Transfers all entries to new table array.  This method is called
    * by superclass resize.  It is overridden for performance, as it is
    * faster to iterate using our linked list.
    */
//扩容的时候调用
@Override
void transfer(HashMap.Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e = header.after; e != header; e = e.after) {//直接使用遍历双向链表来实现再hash迁移旧数据到新表，以前是遍历entry数组实现，直接使用遍历链表效率更高
        if (rehash)
            e.hash = (e.key == null) ? 0 : hash(e.key);
        int index = indexFor(e.hash, newCapacity);
        e.next = newTable[index];
        newTable[index] = e;
    }
}


 /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
public boolean containsValue(Object value) {//判断有没有有个value也是直接遍历双向链表
    // Overridden to take advantage of faster iterator
    if (value==null) {
        for (Entry e = header.after; e != header; e = e.after)
            if (e.value==null)
                return true;
    } else {
        for (Entry e = header.after; e != header; e = e.after)
            if (value.equals(e.value))
                return true;
    }
    return false;
}



/**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
//双向链表也要回到初始化状态
public void clear() {
    super.clear();
    header.before = header.after = header;
}



}

```

# 1、Entry

![](../../pic/2020-04-06-22-37-40.png)

next是用于维护HashMap指定table位置上hash冲突时连接的Entry的顺序的；before、After是用于维护Entry插入的先后顺序的，构建双向链表时使用的。

```java
    private static class Entry<K,V> extends HashMap.Entry<K,V> {
        // These fields comprise the doubly linked list used for iteration.
        Entry<K,V> before, after;//多了这两个构建双向链表，表示当前节点的前一个和后一个

        Entry(int hash, K key, V value, HashMap.Entry<K,V> next) {
            super(hash, key, value, next);
        }

        /**
         * Removes this entry from the linked list.
         */
        private void remove() {//新增的方法，把当前节点移除
            before.after = after;
            after.before = before;
        }

        /**
         * Inserts this entry before the specified existing entry in the list.
         */
         //1---2---4   在4前插入3
         //1---2---3---4
         //实现按照插入顺序访问
        private void addBefore(Entry<K,V> existingEntry) {//在existingEntry前插入当前对象
            after  = existingEntry;//修改当前节点的后一个为指定entry
            before = existingEntry.before;//当前节点的前一个为entry的前一个
            before.after = this;//前节点指向当前节点
            after.before = this;//后节点也指向当前节点
        }

        /**
         * This method is invoked by the superclass whenever the value
         * of a pre-existing entry is read by Map.get or modified by Map.set.
         * If the enclosing Map is access-ordered, it moves the entry
         * to the end of the list; otherwise, it does nothing.
         */
        //实现按照访问顺序输出
        void recordAccess(HashMap<K,V> m) {//这个方法在get和set的时候被调用
            LinkedHashMap<K,V> lm = (LinkedHashMap<K,V>)m;
            if (lm.accessOrder) {//在开启访问顺序时才生效否则没什么区别
                lm.modCount++;
                remove();//在双向链表移除当前节点
                addBefore(lm.header);//添加的链表的尾部
            }
        }

        void recordRemoval(HashMap<K,V> m) {
            remove();
        }
    }

```

# 2、LinkedHashIterator

```java
private abstract class LinkedHashIterator<T> implements Iterator<T> {
    Entry<K,V> nextEntry    = header.after;//第一个有效节点
    Entry<K,V> lastReturned = null;

    /**
        * The modCount value that the iterator believes that the backing
        * List should have.  If this expectation is violated, the iterator
        * has detected concurrent modification.
        */
    int expectedModCount = modCount;

    public boolean hasNext() {
        return nextEntry != header;//双向链表判断是否
    }

    public void remove() {
        if (lastReturned == null)
            throw new IllegalStateException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();

        LinkedHashMap.this.remove(lastReturned.key);
        lastReturned = null;
        expectedModCount = modCount;
    }

    Entry<K,V> nextEntry() {//这里实现迭代器的next
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        if (nextEntry == header)
            throw new NoSuchElementException();

        Entry<K,V> e = lastReturned = nextEntry;//记录上一次访问的赋值给lastReturned
        nextEntry = e.after;//更新下一个对象，遍历双向链表
        return e;
    }
}

```

# 3、EntryIterator迭代器

```java
 private class EntryIterator extends LinkedHashIterator<Map.Entry<K,V>> {
    public Map.Entry<K,V> next() { return nextEntry(); }//调用父类的方法
}

```

# 4、KeyIterator

```java
private class KeyIterator extends LinkedHashIterator<K> {
    public K next() { return nextEntry().getKey(); }
}

```

# 5、ValueIterator

```java
private class ValueIterator extends LinkedHashIterator<V> {
    public V next() { return nextEntry().value; }
}


```

# 6、利用LinkedHashMap实现LRU算法缓存

LRU即Least Recently Used，最近最少使用，也就是说，当缓存满了，会优先淘汰那些最近最不常访问的数据。比方说数据a，1天前访问了；数据b，2天前访问了，缓存满了，优先会淘汰数据b。

```java

public class LRUCache extends LinkedHashMap {
    public LRUCache(int maxSize) {
        super(maxSize, 0.75F, true);
        maxElements = maxSize;
    }

    protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
        return size() > maxElements;
    }

    private static final long serialVersionUID = 1L;
    protected int maxElements;
}
```






# 9、linkedhashmap的结构

![](../../pic/2020-04-06-22-08-55.png)

![](../../pic/2020-04-06-22-39-39.png)

第一张图为LinkedHashMap整体结构图，第二张图专门把循环双向链表抽取出来，直观一点，注意该循环双向链表的头部存放的是最久访问的节点或最先插入的节点，尾部为最近访问的或最近插入的节点，迭代器遍历方向是从链表的头部开始到链表尾部结束，在链表尾部有一个空的header节点，该节点不存放key-value内容，为LinkedHashMap类的成员属性，循环双向链表的入口。



# 参考

- [Java集合之LinkedHashMap](https://www.cnblogs.com/xiaoxi/p/6170590.html)



