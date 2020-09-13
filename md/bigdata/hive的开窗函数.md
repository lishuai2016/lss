


# 一、什么是开窗函数

开窗函数/分析函数：over()

开窗函数也叫分析函数，有两类：一类是聚合开窗函数，一类是排序开窗函数。

开窗函数的调用格式为：

函数名(列名) OVER(partition by 列名 order by列名) 。

如果你没听说过开窗函数，看到上面开窗函数的调用方法，你可能还会有些疑惑。但只要你了解聚合函数，那么理解开窗函数就非常容易了。

我们知道聚合函数对一组值执行计算并返回单一的值，如sum()，count()，max()，min()， avg()等，这些函数常与group by子句连用。除了 COUNT 以外，聚合函数忽略空值。

但有时候一组数据只返回一组值是不能满足需求的，如我们经常想知道各个地区的前几名、各个班或各个学科的前几名。这时候需要每一组返回多个值。用开窗函数解决这类问题非常方便。

开窗函数和聚合函数的区别如下：

（1）SQL 标准允许将所有聚合函数用作开窗函数，用OVER 关键字区分开窗函数和聚合函数。

（2）聚合函数每组只返回一个值，开窗函数每组可返回多个值。

注：常见主流数据库目前都支持开窗函数，但mysql数据库目前还不支持。


1.分区排序：row_number () over()

查询每门课程course_name前三名的学生姓名及成绩

2.几个排序函数row_number() over()、rank() over()、dense_rank() over()、ntile() over()的区别

（1） row_number() over()：对相等的值不进行区分，相等的值对应的排名相同，序号从1到n连续。

（2） rank() over()：相等的值排名相同，但若有相等的值，则序号从1到n不连续。如果有两个人都排在第3名，则没有第4名。

（3） dense_rank() over()：对相等的值排名相同，但序号从1到n连续。如果有两个人都排在第一名，则排在第2名（假设仅有1个第二名）的人是第3个人。

（4） ntile( n ) over()：可以看作是把有序的数据集合平均分配到指定的数量n的桶中,将桶号分配给每一行，排序对应的数字为桶号。如果不能平均分配，则较小桶号的桶分配额外的行，并且各个桶中能放的数据条数最多相差1。






常用开窗函数：
- 1.为每条数据显示聚合信息.(聚合函数() over())
- 2.为每条数据提供分组的聚合函数结果(聚合函数() over(partition by 字段) as 别名) --按照字段分组，分组后进行计算
- 3.与排名函数一起使用(row number() over(order by 字段) as 别名)

常用分析函数：（最常用的应该是1.2.3 的排序）

```
1、row_number() over(partition by ... order by ...)
2、rank() over(partition by ... order by ...)
3、dense_rank() over(partition by ... order by ...)
4、count() over(partition by ... order by ...)
5、max() over(partition by ... order by ...)
6、min() over(partition by ... order by ...)
7、sum() over(partition by ... order by ...)
8、avg() over(partition by ... order by ...)
9、first_value() over(partition by ... order by ...)
10、last_value() over(partition by ... order by ...)
11、lag() over(partition by ... order by ...)
12、lead() over(partition by ... order by ...)
lag 和lead 可以 获取结果集中，按一定排序所排列的当前行的上下相邻若干offset 的某个行的某个列(不用结果集的自关联）；
lag ，lead 分别是向前，向后；
lag 和lead 有三个参数，第一个参数是列名，第二个参数是偏移的offset，第三个参数是 超出记录窗口时的默认值）
```




# 参考

- [开窗函数](https://www.douban.com/group/topic/155112949/)


