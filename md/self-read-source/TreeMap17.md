

<!-- TOC -->

- [1、SortedMap](#1sortedmap)
- [2、NavigableMap](#2navigablemap)
- [3、TreeMap](#3treemap)
    - [1、Entry保存key-value值](#1entry保存key-value值)
    - [2、PrivateEntryIterator](#2privateentryiterator)
    - [3、EntryIterator](#3entryiterator)
    - [4、EntrySet](#4entryset)
    - [5、KeyIterator](#5keyiterator)
    - [6、ValueIterator](#6valueiterator)
- [4、常用方法](#4常用方法)
    - [1、插入](#1插入)
    - [2、删除](#2删除)
    - [3、查找](#3查找)
    - [4、遍历](#4遍历)
- [参考](#参考)

<!-- /TOC -->





备注：`treemap底层数据结构通过红黑树实现`

![](../../pic/2020-04-05-14-29-07.png)


# 1、SortedMap

![](../../pic/2020-04-05-23-27-47.png)


```java
public interface SortedMap<K,V> extends Map<K,V> {

Comparator<? super K> comparator();//返回用于key排序的比较器，如果使用Comparable自然顺序返回null

SortedMap<K,V> subMap(K fromKey, K toKey);//返回[fromKey,toKey)范围的数据，如果fromKey=toKey返回null

SortedMap<K,V> headMap(K toKey);//小于这个toKey的集合[最小,toKey)范围

SortedMap<K,V> tailMap(K fromKey);//大于等于这个fromKey的集合[fromKey,最大]范围

K firstKey();//返回最小的key

K lastKey();//返回最大的key

//下面三个函数和map中的定义一样，区别？
Set<K> keySet();//set中的key升序排列

Collection<V> values();//value升序

Set<Map.Entry<K, V>> entrySet();//entry升序
}

```

# 2、NavigableMap

![](../../pic/2020-04-05-23-39-31.png)

```java
public interface NavigableMap<K,V> extends SortedMap<K,V> {

Map.Entry<K,V> lowerEntry(K key);// <key   返回小于key而且最接近key的entry
K lowerKey(K key);

Map.Entry<K,V> floorEntry(K key);// <=key
K floorKey(K key);

Map.Entry<K,V> ceilingEntry(K key);//>=key
K ceilingKey(K key);

Map.Entry<K,V> higherEntry(K key);// > key
K higherKey(K key);

Map.Entry<K,V> firstEntry();//最小的entry
Map.Entry<K,V> lastEntry();//最大的entry

Map.Entry<K,V> pollFirstEntry();//移除并返回最小的
Map.Entry<K,V> pollLastEntry();//移除并返回最大的

NavigableMap<K,V> descendingMap();//返回一个逆序的视图

NavigableSet<K> navigableKeySet();//升序的keyset

NavigableSet<K> descendingKeySet();//逆序的keyset

NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,K toKey,   boolean toInclusive);//可以指定区间的开闭

NavigableMap<K,V> headMap(K toKey, boolean inclusive);

NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);


//下面3个方法好像和sortmap一样？
SortedMap<K,V> subMap(K fromKey, K toKey);
SortedMap<K,V> headMap(K toKey);
SortedMap<K,V> tailMap(K fromKey);

}

```

# 3、TreeMap

```java
public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
{

//属性
private final Comparator<? super K> comparator;//比较器
private transient Entry<K,V> root = null;//树的根节点
private transient int size = 0;//数中entry个数
private transient int modCount = 0;//修改次数

//第一次被请求时初始化这个entrySet
private transient EntrySet entrySet = null;
private transient KeySet<K> navigableKeySet = null;
private transient NavigableMap<K,V> descendingMap = null;

private static final Object UNBOUNDED = new Object();


// Red-black mechanics
private static final boolean RED   = false;//红黑树的节点颜色--红色
private static final boolean BLACK = true;//红黑树的节点颜色--黑色


//构造函数

public TreeMap() {
    comparator = null;
}

public TreeMap(Comparator<? super K> comparator) {
    this.comparator = comparator;
}

public TreeMap(Map<? extends K, ? extends V> m) {
    comparator = null;
    putAll(m);
}

public TreeMap(SortedMap<K, ? extends V> m) {
    comparator = m.comparator();
    try {
        buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
    } catch (java.io.IOException cannotHappen) {
    } catch (ClassNotFoundException cannotHappen) {
    }
}


//存入一个键值对
//主要分为两个步骤，第一：构建排序二叉树，第二：平衡二叉树。
public V put(K key, V value) {
    Entry<K,V> t = root;
    //放如整棵树的第一个节点
    if (t == null) {
        compare(key, key); // type (and possibly null) check这个函数的功能仅仅是null和类型检测

        root = new Entry<>(key, value, null);//新建一个entry对象赋值给root对象
        size = 1;
        modCount++;
        return null;
    }
    int cmp;//比较的结果，用于定位在插入左子树还是右子树
    Entry<K,V> parent;
    // split comparator and comparable paths 根据是否设置了自定义比较器分为两条路径处理
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {//自定义了比较器
        do {
            parent = t;//保存父节点
            cmp = cpr.compare(key, t.key);//当前key比较小，朝向左子树
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)//右子树
                t = t.right;
            else
                return t.setValue(value);//相等更新value值返回
        } while (t != null);
    } else {
        if (key == null)
            throw new NullPointerException();
        Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)//左子树
                t = t.left;
            else if (cmp > 0)//右子树
                t = t.right;
            else
                return t.setValue(value);//更新value值
        } while (t != null);
    }
    //到这里说明key在树上不存在
    Entry<K,V> e = new Entry<>(key, value, parent);//新建一个节点
    if (cmp < 0)
        parent.left = e;//说明当前的key值比较小，放到左子树
    else
        parent.right = e;//放到右子树
    fixAfterInsertion(e);//第二：平衡二叉树
    size++;//新增个数
    modCount++;//修改次数加1
    return null;
}

//传入的key如果没有实现Comparable会报错
final int compare(Object k1, Object k2) {
    return comparator==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
        : comparator.compare((K)k1, (K)k2);
}
//其实可以简化为三种情况需要调整：大前提是父节点为红，z为要插入的节点
//1、z的叔叔y是红色的
//2、z的叔叔y是黑色的，且z是右孩子。设置叔父节点为黑色，祖父节点为红色，进行左旋。
//3、z的叔叔y是黑色的，且z是左孩子。设置叔父节点为黑色，祖父节点为红色，进行右旋。
//备注：针对2,3情况需要考虑要插入接的父节点是其祖父节点的左子树还是右子树，如果是左子树，需要先把插入的节点变化为父节点的左子树后再进行改变颜色右旋；如果是右子树，需要把插入的节点变成父节点的右子树再进行改变颜色左旋；

/** From CLR */
private void fixAfterInsertion(Entry<K,V> x) {
    x.color = RED;//新加入的节点为红色

    while (x != null && x != root && x.parent.color == RED) {//因为插入节点为红色，如果父节点还是红色需要调整，否则不需要调整
        if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {//如果X节点的父节点是x的祖父节点的左子树[这里的判断是为了找父节点的兄弟节点]
            Entry<K,V> y = rightOf(parentOf(parentOf(x)));//父节点的兄弟节点
            if (colorOf(y) == RED) {//父节点的兄弟节点为红色【对应第1种情况】
                setColor(parentOf(x), BLACK);//父节点设置为黑色
                setColor(y, BLACK);//父节点的兄弟节点设置为黑色
                setColor(parentOf(parentOf(x)), RED);//祖父节点设置为红色
                x = parentOf(parentOf(x));//祖父节点作为新增节点进行递归处理
            } else {//如果X的叔节点（U为黑色）；这里会存在两种情况（情况2、情况3）
                if (x == rightOf(parentOf(x))) {//新增节点作为父节点的右子树，则以新增节点的父节点进行左旋【对应第2种情况】，旋转之后变成情况3了
                    x = parentOf(x);//把x的父节点赋值给x
                    rotateLeft(x);//左旋
                }
                //（情况3） 经过上面的左旋之后x变为指向原来x的父节点，这个节点变成新增的节点了
                setColor(parentOf(x), BLACK);//将X的父节点（P）设置为黑色【因为这里的设置循环结束】
                setColor(parentOf(parentOf(x)), RED);//将X的父节点的父节点（G）设置红色
                rotateRight(parentOf(parentOf(x)));//以X的父节点的父节点（G）为中心右旋转
            }
        } else {//X节点的父节点是x的祖父节点的右子树
            Entry<K,V> y = leftOf(parentOf(parentOf(x)));//父节点的兄弟节点
            if (colorOf(y) == RED) {//父节点的兄弟节点为红色【对应第1种情况】
                setColor(parentOf(x), BLACK);//父节点设置为黑色【因为这里的设置循环结束】
                setColor(y, BLACK);//父节点的兄弟节点设置为黑色
                setColor(parentOf(parentOf(x)), RED);//祖父节点设置为红色
                x = parentOf(parentOf(x));//祖父节点作为新增节点进行递归处理
            } else {//如果X的叔节点（U为黑色）；这里会存在两种情况（情况四、情况五）
                if (x == leftOf(parentOf(x))) {//新增节点作为父节点的左子树，则以新增节点的父节点进行右旋【对应第四种情况】
                    x = parentOf(x);//把x的父节点赋值给x
                    rotateRight(x);//右旋
                }
                //（情况五）
                setColor(parentOf(x), BLACK);
                setColor(parentOf(parentOf(x)), RED);
                rotateLeft(parentOf(parentOf(x)));
            }
        }
    }
    root.color = BLACK;
}

//查找节点的父节点
private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
    return (p == null ? null: p.parent);
}
//找节点左子树
private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
    return (p == null) ? null: p.left;
}
//找节点右子树
private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
    return (p == null) ? null: p.right;
}
//找节点的颜色
private static <K,V> boolean colorOf(Entry<K,V> p) {
    return (p == null ? BLACK : p.color);
}
//给指定节点设置颜色
private static <K,V> void setColor(Entry<K,V> p, boolean c) {
    if (p != null)
        p.color = c;
}



 /** From CLR */
 //左旋：新节点的左子树指向原来的父节点；
 //从下面可以看出分为三步
private void rotateLeft(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> r = p.right;//r可能为新增节点
        //1、p节点的右子树指针数据的修改（因为每个节点都需要执行父节点，父节点也需要执行子节点，所以修改是双向进行的）
        p.right = r.left;//因为p节点要下沉变成r节点的左子树，需要把r节点左子树的值赋值给p节点的右子树
        if (r.left != null)//r节点左子树不为空
            r.left.parent = p;//更改节点左子树的父节点为p
        //2、调整R节点和新父节点直接的指向（简单来说就是我指向父节点，父节点也要指向我）
        r.parent = p.parent;//更改r子树的父节点为p节点的父节点
        if (p.parent == null)//如果p节点为根节点，那么左旋后r节点就是根节点
            root = r;
        else if (p.parent.left == p)//p节点是其父节点的左子树
            p.parent.left = r;//让p节点的父节点左指针指向r
        else
            p.parent.right = r;//让p节点的父节点右指针指向r
        //3、r节点的左子树的修改（因为每个节点都需要执行父节点，父节点也需要执行子节点，所以修改是双向进行的）
        r.left = p;//r的左指针指向p
        p.parent = r;//p的父节点指向r
    }
}

/** From CLR */
//右旋：新节点的右子树指向原来的父节点
//画图，步骤同左旋
private void rotateRight(Entry<K,V> p) {
    if (p != null) {
        Entry<K,V> l = p.left;//l可能为新增节点
        //1、
        p.left = l.right;
        if (l.right != null) 
            l.right.parent = p;
        //2、
        l.parent = p.parent;
        if (p.parent == null)
            root = l;
        else if (p.parent.right == p)
            p.parent.right = l;
        else 
            p.parent.left = l;
        //3、
        l.right = p;
        p.parent = l;
    }
}


//移除元素
public V remove(Object key) {
    Entry<K,V> p = getEntry(key);//找到对应的entry对象
    if (p == null)
        return null;

    V oldValue = p.value;
    deleteEntry(p);//删除这个对象
    return oldValue;
}

//根据key找到对应的entry对象，相当于在二叉搜索树上进行查找
final Entry<K,V> getEntry(Object key) {
    // Offload comparator-based version for sake of performance
    if (comparator != null)
        return getEntryUsingComparator(key);
    if (key == null)
        throw new NullPointerException();
    Comparable<? super K> k = (Comparable<? super K>) key;
    Entry<K,V> p = root;
    while (p != null) {//相当于在二叉搜索树上进行查找
        int cmp = k.compareTo(p.key);
        if (cmp < 0)
            p = p.left;
        else if (cmp > 0)
            p = p.right;
        else
            return p;
    }
    return null;
}

//删除一个entry对象
//核心：二叉查找树会把要删除有两个孩子的节点的情况转化为删除只有一个孩子的节点的情况，该节点是欲被删除节点的前驱和后继。主要三件事：
//1、如果待删除节点 P 有两个孩子，则先找到 P 的后继 S，然后将 S 中的值拷贝到 P 中，并让 P 指向 S
//2、如果最终被删除节点 P（P 现在指向最终被删除节点）的孩子不为空，则用其孩子节点替换掉
//3、如果最终被删除的节点是黑色的话，调用 fixAfterDeletion 方法进行修复
//总结:找后继 -> 替换 -> 修复
private void deleteEntry(Entry<K,V> p) {
    modCount++;
    size--;

    // If strictly internal, copy successor's element to p and then make p
    // point to successor.
    if (p.left != null && p.right != null) {//要删除的节点有两个子节点
        Entry<K,V> s = successor(p);//这里返回的是p节点右子树最小的节点
        p.key = s.key;
        p.value = s.value;
        p = s;//p指向s，这里的p不可能还有左子树，有可能存在右子树
    } // p has 2 children

    // Start fixup at replacement node, if it exists.  
    Entry<K,V> replacement = (p.left != null ? p.left : p.right);//

    
    if (replacement != null) {//这里p指向要删除的节点，replacement指向替换删除节点位置的节点
        // Link replacement to parent
        //1、修改双向指针，替换节点指向父节点，父节点也要指向这个替换节点
        replacement.parent = p.parent;
        if (p.parent == null)
            root = replacement;
        else if (p == p.parent.left)
            p.parent.left  = replacement;
        else
            p.parent.right = replacement;

        // Null out links so they are OK to use by fixAfterDeletion.
        p.left = p.right = p.parent = null;//在树中清理掉这个替换节点

        // Fix replacement 删除的节点为黑色需要进行调整
        if (p.color == BLACK)
            fixAfterDeletion(replacement);
    } else if (p.parent == null) { // return if we are the only node. 要删除的节点为根节点，而且没有子节点，直接设置为null
        root = null;
    } else { //  No children. Use self as phantom replacement and unlink. 要删除的节点没有孩子
        if (p.color == BLACK)//如果当前节点为黑色
            fixAfterDeletion(p);

        if (p.parent != null) {//判断p是左子树还是右子树，然后设置为null，即将p从树中移除
            if (p == p.parent.left)
                p.parent.left = null;
            else if (p == p.parent.right)
                p.parent.right = null;
            p.parent = null;
        }
    }
}


/**
    * Returns the successor of the specified Entry, or null if no such.
    */
//找到要删除节点的后继节点：
static <K,V> TreeMap.Entry<K,V> successor(Entry<K,V> t) {
    if (t == null)
        return null;
    else if (t.right != null) {//1、找右子树中选取最小值节点
        Entry<K,V> p = t.right;//遍历右子树一直找其左节点
        while (p.left != null)
            p = p.left;
        return p;
    } else {// 如果当前节点的右子树为null  获取到其父节点返回
        Entry<K,V> p = t.parent;//当前节点的父节点
        Entry<K,V> ch = t;//指向当前节点
        while (p != null && ch == p.right) {
            ch = p;
            p = p.parent;
        }
        return p;
    }
}

 /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
static <K,V> Entry<K,V> predecessor(Entry<K,V> t) {
    if (t == null)
        return null;
    else if (t.left != null) {
        Entry<K,V> p = t.left;//t的左子树，找左子树最大的节点
        while (p.right != null)
            p = p.right;
        return p;
    } else {
        Entry<K,V> p = t.parent;
        Entry<K,V> ch = t;
        while (p != null && ch == p.left) {
            ch = p;
            p = p.parent;
        }
        return p;
    }
}


 /** From CLR */
 //删除节点后调整
private void fixAfterDeletion(Entry<K,V> x) {
    while (x != root && colorOf(x) == BLACK) {//非根节点而且是黑色
        if (x == leftOf(parentOf(x))) {//x是左子树
            Entry<K,V> sib = rightOf(parentOf(x));//兄弟节点
            //兄弟节点为红色，按情况2处理
            if (colorOf(sib) == RED) {
                setColor(sib, BLACK);//兄弟节点设置为黑色
                setColor(parentOf(x), RED);//父节点设置为红色
                rotateLeft(parentOf(x));//左旋
                sib = rightOf(parentOf(x));
            }
            //s的孩子节点为黑色，按照情况3、4处理
            if (colorOf(leftOf(sib))  == BLACK &&
                colorOf(rightOf(sib)) == BLACK) {
                setColor(sib, RED);
                x = parentOf(x);
            } else {//s的右节点为黑色，左节点为红色，情况5
                if (colorOf(rightOf(sib)) == BLACK) {
                    setColor(leftOf(sib), BLACK);
                    setColor(sib, RED);
                    rotateRight(sib);
                    sib = rightOf(parentOf(x));
                }
                //s的右节点为红，s的左节点均可，情况6
                setColor(sib, colorOf(parentOf(x)));
                setColor(parentOf(x), BLACK);
                setColor(rightOf(sib), BLACK);
                rotateLeft(parentOf(x));
                x = root;
            }
        } else { // symmetric
            Entry<K,V> sib = leftOf(parentOf(x));

            if (colorOf(sib) == RED) {
                setColor(sib, BLACK);
                setColor(parentOf(x), RED);
                rotateRight(parentOf(x));
                sib = leftOf(parentOf(x));
            }

            if (colorOf(rightOf(sib)) == BLACK &&
                colorOf(leftOf(sib)) == BLACK) {
                setColor(sib, RED);
                x = parentOf(x);
            } else {
                if (colorOf(leftOf(sib)) == BLACK) {
                    setColor(rightOf(sib), BLACK);
                    setColor(sib, RED);
                    rotateLeft(sib);
                    sib = leftOf(parentOf(x));
                }
                setColor(sib, colorOf(parentOf(x)));
                setColor(parentOf(x), BLACK);
                setColor(leftOf(sib), BLACK);
                rotateRight(parentOf(x));
                x = root;
            }
        }
    }

    setColor(x, BLACK);
}



//获取一个元素，先查找这个key对应的entry
public V get(Object key) {
    Entry<K,V> p = getEntry(key);
    return (p==null ? null : p.value);
}

//获取树最左侧的那个节点
final Entry<K,V> getFirstEntry() {
    Entry<K,V> p = root;
    if (p != null)
        while (p.left != null)
            p = p.left;
    return p;
}


//生成key的迭代器升序
Iterator<K> keyIterator() {
    return new KeyIterator(getFirstEntry());//依据树的最左侧的节点构建
}
//生成key的迭代器降序
Iterator<K> descendingKeyIterator() {
    return new DescendingKeyIterator(getLastEntry());
}



}

```


## 1、Entry保存key-value值

实现了Map.Entry<K,V>接口，定义为一个红黑树节点。对比普通的map节点，这里多个三个指针left、right和parent，分别指向左右孩子和父节点。

```java
    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left = null;//左孩子
        Entry<K,V> right = null;//右孩子
        Entry<K,V> parent;//父节点
        boolean color = BLACK;//默认黑色true

        /**
         * Make a new cell with given key, value, and parent, and with
         * {@code null} child links, and BLACK color.
         */
        Entry(K key, V value, Entry<K,V> parent) {//默认构建的节点需要指定父节点，颜色为黑色
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;

            return valEquals(key,e.getKey()) && valEquals(value,e.getValue());
        }

        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return key + "=" + value;
        }
    }

```


## 2、PrivateEntryIterator

```java

abstract class PrivateEntryIterator<T> implements Iterator<T> {
    Entry<K,V> next;//需要返回的
    Entry<K,V> lastReturned;
    int expectedModCount;

    PrivateEntryIterator(Entry<K,V> first) {//传入第一个节点
        expectedModCount = modCount;
        lastReturned = null;
        next = first;
    }

    public final boolean hasNext() {
        return next != null;
    }

    final Entry<K,V> nextEntry() {//下一条entry
        Entry<K,V> e = next;
        if (e == null)
            throw new NoSuchElementException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        next = successor(e);//这里找后继节点
        lastReturned = e;
        return e;
    }

    final Entry<K,V> prevEntry() {//前一条entry
        Entry<K,V> e = next;
        if (e == null)
            throw new NoSuchElementException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        next = predecessor(e);//获取前驱节点
        lastReturned = e;
        return e;
    }

    public void remove() {
        if (lastReturned == null)
            throw new IllegalStateException();
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        // deleted entries are replaced by their successors
        if (lastReturned.left != null && lastReturned.right != null)
            next = lastReturned;
        deleteEntry(lastReturned);//调用删除entry实现
        expectedModCount = modCount;
        lastReturned = null;
    }
}
```


## 3、EntryIterator

```java
final class EntryIterator extends PrivateEntryIterator<Map.Entry<K,V>> {
    EntryIterator(Entry<K,V> first) {
        super(first);
    }
    public Map.Entry<K,V> next() {
        return nextEntry();
    }
}

```

## 4、EntrySet

```java

class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public Iterator<Map.Entry<K,V>> iterator() {
        return new EntryIterator(getFirstEntry());//获取最左侧那个节点作为第一个节点来构建迭代器
    }

    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
        V value = entry.getValue();
        Entry<K,V> p = getEntry(entry.getKey());
        return p != null && valEquals(p.getValue(), value);
    }

    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
        V value = entry.getValue();
        Entry<K,V> p = getEntry(entry.getKey());
        if (p != null && valEquals(p.getValue(), value)) {
            deleteEntry(p);
            return true;
        }
        return false;
    }

    public int size() {
        return TreeMap.this.size();
    }

    public void clear() {
        TreeMap.this.clear();
    }
}
```

## 5、KeyIterator

```java
final class KeyIterator extends PrivateEntryIterator<K> {
    KeyIterator(Entry<K,V> first) {
        super(first);
    }
    public K next() {
        return nextEntry().key;//朝后找
    }
    }

final class DescendingKeyIterator extends PrivateEntryIterator<K> {
        DescendingKeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return prevEntry().key;//朝前找
        }
    }
```


```java
    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final NavigableMap<E, Object> m;
    KeySet(NavigableMap<E,Object> map) { m = map; }

    public Iterator<E> iterator() {
        if (m instanceof TreeMap)
            return ((TreeMap<E,Object>)m).keyIterator();
        else
            return (Iterator<E>)(((TreeMap.NavigableSubMap)m).keyIterator());
    }

    public Iterator<E> descendingIterator() {
        if (m instanceof TreeMap)
            return ((TreeMap<E,Object>)m).descendingKeyIterator();
        else
            return (Iterator<E>)(((TreeMap.NavigableSubMap)m).descendingKeyIterator());
    }

    public int size() { return m.size(); }
    public boolean isEmpty() { return m.isEmpty(); }
    public boolean contains(Object o) { return m.containsKey(o); }
    public void clear() { m.clear(); }
    public E lower(E e) { return m.lowerKey(e); }
    public E floor(E e) { return m.floorKey(e); }
    public E ceiling(E e) { return m.ceilingKey(e); }
    public E higher(E e) { return m.higherKey(e); }
    public E first() { return m.firstKey(); }
    public E last() { return m.lastKey(); }
    public Comparator<? super E> comparator() { return m.comparator(); }
    public E pollFirst() {
        Map.Entry<E,Object> e = m.pollFirstEntry();
        return (e == null) ? null : e.getKey();
    }
    public E pollLast() {
        Map.Entry<E,Object> e = m.pollLastEntry();
        return (e == null) ? null : e.getKey();
    }
    public boolean remove(Object o) {
        int oldSize = size();
        m.remove(o);
        return size() != oldSize;
    }
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                    E toElement,   boolean toInclusive) {
        return new KeySet<>(m.subMap(fromElement, fromInclusive,
                                        toElement,   toInclusive));
    }
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new KeySet<>(m.headMap(toElement, inclusive));
    }
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new KeySet<>(m.tailMap(fromElement, inclusive));
    }
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
    public NavigableSet<E> descendingSet() {
        return new KeySet(m.descendingMap());
    }
}
```



## 6、ValueIterator

```java
final class ValueIterator extends PrivateEntryIterator<V> {
    ValueIterator(Entry<K,V> first) {
        super(first);
    }
    public V next() {
        return nextEntry().value;
    }
}

```


```java
class Values extends AbstractCollection<V> {
    public Iterator<V> iterator() {
        return new ValueIterator(getFirstEntry());//使用getFirstEntry获取树的第一个节点
    }

    public int size() {
        return TreeMap.this.size();
    }

    public boolean contains(Object o) {
        return TreeMap.this.containsValue(o);
    }

    public boolean remove(Object o) {
        for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
            if (valEquals(e.getValue(), o)) {
                deleteEntry(e);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        TreeMap.this.clear();
    }
}

```






# 4、常用方法


## 1、插入


## 2、删除



## 3、查找



## 4、遍历










# 参考


- [TreeMap底层实现原理](https://blog.csdn.net/cyywxy/article/details/81151104)

- [TreeMap 源码分析](https://cloud.tencent.com/developer/article/1113703)


