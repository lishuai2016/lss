

> 优化limit 分页


一个非常常见又非常头痛的场景：‘limit 1000,20’。这时MySQL需要查询1020条记录然后只返回最后20条，前面的1000条都将被抛弃，这样的代价非常高。如果所有页面的访问频率都相同，那么这样的查询平均需要访问半个表的数据。

- 第一种思路 在索引上分页:在索引上完成分页操作，最后根据主键关联回原表查询所需要的其他列的内容。例如：SELECT * FROM tb_user LIMIT 1000,10 可以优化成这样：

```sql
SELECT * FROM tb_user u 
INNER JOIN (SELECT id FROM tb_user LIMIT 1000,10) AS b ON b.id=u.id
```

- 第二种思路 将limit转换成位置查询:这种思路需要加一个参数来辅助，标记分页的开始位置：

SELECT * FROM tb_user WHERE id > 1000 LIMIT 10