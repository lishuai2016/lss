# BitSet


该类在java的java.util包中。

BitSet类的原理主要是利用long型有64个bit，每个bit存储一个数值，这样一个long型数字就能存储64个数字，进而节省了空间。


## 一、什么是BitSet？

BitSet类实现了一个按需增长的位向量。位Set的每一个组件都有一个boolean值。用非负的整数将BitSet的位编入索引。可以对每个编入索引的位进行测试、设置或者清除。通过逻辑与、逻辑或和逻辑异或操作，可以使用一个 BitSet修改另一个 BitSet的内容。 

默认情况下，set 中所有位的初始值都是false。 

每个位 set 都有一个当前大小，也就是该位 set 当前所用空间的位数。注意，这个大小与位 set 的实现有关，所以它可能随实现的不同而更改。位 set 的长度与位 set 的逻辑长度有关，并且是与实现无关而定义的。 



## 二、Java BitSet实现原理

```java
public class BitSet implements Cloneable, java.io.Serializable 
{
    private final static int ADDRESS_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private final static int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;

    private static final ObjectStreamField[] serialPersistentFields =
     {
        new ObjectStreamField("bits", long[].class),
    };

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private long[] words;
    
    .....
}
```
可以看到，BitSet的底层实现是使用long数组作为内部存储结构的，所以BitSet的大小为long类型大小(64位)的整数倍。

它有两个构造函数：

1、BitSet()：创建一个新的位 set，默认大小是64位。

```java
public BitSet() 
{
        initWords(BITS_PER_WORD);
        sizeIsSticky = false;
}
```


2、BitSet(int nbits)：创建一个位set，它的初始大小足以显式表示索引范围在 0 到 nbits-1 的位。

```java
public BitSet(int nbits)
     {
        // nbits can't be negative; size 0 is OK
        if (nbits < 0)
            throw new NegativeArraySizeException("nbits < 0: " + nbits);
        initWords(nbits);
        sizeIsSticky = true;
    }
```


注：

　　1、如果指定了bitset的初始化大小，那么会把他规整到一个大于或者等于这个数字的64的整倍数。比如64位，bitset的大小是1个long，而65位时，bitset大小是2个long，即128位。做这么一个规定，主要是为了内存对齐，同时避免考虑到不要处理特殊情况，简化程序。

　　2：BitSet的size方法：返回此 BitSet 表示位值时实际使用空间的位数，值是64的整数倍

　　　length方法：返回此 BitSet 的“逻辑大小”：BitSet 中最高设置位的索引加 1　　

## 三、使用场景

常见的应用场景是对海量数据进行一些统计工作，比如日志分析、用户数统计等。

有1千万个随机数，随机数的范围在1到1亿之间。现在要求写出一种算法，将1到1亿之间没有在随机数中的数求出来？



```java
public class Alibaba
{
    public static void main(String[] args)
    {
        Random random=new Random();
        
        List<Integer> list=new ArrayList<>();
        for(int i=0;i<10000000;i++)
        {
            int randomResult=random.nextInt(100000000);
            list.add(randomResult);
        }
        System.out.println("产生的随机数有");
        for(int i=0;i<list.size();i++)
        {
            System.out.println(list.get(i));
        }
        BitSet bitSet=new BitSet(100000000);
        for(int i=0;i<10000000;i++)
        {
            bitSet.set(list.get(i));
        }
        
        System.out.println("0~1亿不在上述随机数中有"+bitSet.size());
        for (int i = 0; i < 100000000; i++)
        {
            if(!bitSet.get(i))
            {
                System.out.println(i);
            }
        }     
    }
}
```


备注：当我们调用set设置的时候，set的值就相当于索引在set指定的位置。比如：

```java
BitSet bitSet = new BitSet();
bitSet.set(0);
System.out.println(bitSet.size());//输出64
bitSet.set(64);
System.out.println(bitSet.size());//输出128
```




# 参考
 
- [Java BitSet使用场景和示例](https://www.cnblogs.com/xujian2014/p/5491286.html)



