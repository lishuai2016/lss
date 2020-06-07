

我们在进行表设计以及数据库搭建的时候需要预估表一行数据可能占用空间大小以及没有会增加多少行，这些数据保留多久以便于设计MySQL数据所需要的配置大小。



```sql
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `age` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;


INSERT INTO `user` VALUES ('1', '33', 'gfds');
```
> 使用存储过程造数据，这样填充的是一样的数据。

```sql
drop procedure idata
delimiter ;;
create procedure idata()
begin
  declare i int;
  set i=0;
  while i<300 do
    insert into user(age,name) values('33', 'gfds');
    set i=i+1;
  end while;
end;;
delimiter ;
 
call idata();

```


按照定义一行占用的空间大小不应该是：4+4+(3+1))=12


一共三行记录，为什么会出现一行平均大小为5461字节，一共16384字节？


![](../../pic/2020-03-01-12-20-22.png)

![](../../pic/2020-03-01-12-21-13.png)

备注：上面两个图形可以看到即使只有几条数据表数据大小也是16384字节，而且平均每个字节的大小等于，表数据大小/记录数，当记录数小的时候这个单条记录占用的字节数并不准确，随着下面记录数的增加，平均每条记录所占用的空间大小也在降低。针对为什么初始化话表记录占用的总空间数是16384,16384=1024*16，在MySQL底层一个数据页的大小默认是16K,猜测在初始化的时候分配给该表一个数据页，就算在该表所占的空间上。


![](../../pic/2020-03-07-21-41-09.png)

![](../../pic/2020-03-07-21-46-33.png)

理论上上面一行是12字节

一个数据页16384/12=1365。理论上可以插入这么多数据，但是插入到1346条的时候就触发了增加数据页的操作，而且是一下增了4页

![](../../pic/2020-03-07-21-56-07.png)

```sql
SELECT TABLE_NAME,DATA_LENGTH,INDEX_LENGTH,TABLE_ROWS FROM information_schema.TABLES WHERE TABLE_SCHEMA='dservice' AND TABLE_NAME='download_task'

show table status like '%user%'
```


备注：使用上面两个命令查看表空间大小的分配都是以数据页16K为基本单位进行分配了，而且平均每一条记录占用的大小大于表中定义的各个字段和。

# 参考

- [字段类型与合理的选择字段类型](https://www.kancloud.cn/thinkphp/mysql-design-optimalize/39325)




