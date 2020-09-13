


SortedMap中定义的接口返回的是map（相当于list查询），这里接口返回的是一个Entry对象（相当于selectOne查询）


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