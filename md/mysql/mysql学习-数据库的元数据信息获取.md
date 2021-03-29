





原理：根据jdbc的数据库驱动加载各自的数据库链接，通过原始的connection、statement、resultset来实现。需要考虑各个数据源的支持的SQL语句。以及系统库`information_schema`中获取

```sql
show databses;
show tables;
desc tablename;

-- 1、获取当前链接下的全部数据库
show databases;

-- 2、获取库下面的表
select table_name tableName, engine, table_comment tableComment, create_time createTime from information_schema.tables where table_schema = (select database()) and table_name = #{tableName}

-- 3、获取表中的列信息
select column_name columnName, data_type dataType, column_comment columnComment, column_key columnKey, extra from information_schema.columns where table_name = #{tableName} and table_schema = (select database()) order by ordinal_position

```

