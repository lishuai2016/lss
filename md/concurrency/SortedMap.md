


功能：定义了如何获取map中最大最小key、或者一段范围的map值。简单来说可以认为支持范围查询了



```java
public interface SortedMap<K,V> extends Map<K,V> {

Comparator<? super K> comparator();//返回用于key排序的比较器，如果使用Comparable自然顺序返回null

SortedMap<K,V> subMap(K fromKey, K toKey);//返回[fromKey,toKey)范围的数据，如果fromKey=toKey返回null

SortedMap<K,V> headMap(K toKey);//小于这个toKey的集合[最小,toKey)范围   即 < toKey

SortedMap<K,V> tailMap(K fromKey);//大于等于这个fromKey的集合[fromKey,最大]范围

K firstKey();//返回最小的key

K lastKey();//返回最大的key

//下面三个函数和map中的定义一样，区别？
Set<K> keySet();//set中的key升序排列

Collection<V> values();//value升序

Set<Map.Entry<K, V>> entrySet();//entry升序
}

```