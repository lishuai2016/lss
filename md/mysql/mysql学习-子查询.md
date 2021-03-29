













首先，我们来明确几个概念：

子查询：可以是嵌套在另一个查询(select insert update delete)内，子查询也可以是嵌套在另一个子查询里面。

MySQL子查询称为内部查询，而包含子查询的查询称为外部查询。子查询可以在使用表达式的任何地方使用。

接下来我们使用以下表格来演示各种子查询：

```sql
CREATE TABLE class (
	id BIGINT NOT NULL auto_increment,
	class_num VARCHAR (10) COMMENT '课程编号',
	class_name VARCHAR (100) COMMENT '课程名称',
	pass_score INTEGER COMMENT '课程及格分数',
	PRIMARY KEY (id)
) COMMENT '课程';

CREATE TABLE student_class (
	id BIGINT NOT NULL auto_increment,
	student_name VARCHAR (100) COMMENT '学生姓名',
	class_num VARCHAR (10) COMMENT '课程编号',
	score INTEGER COMMENT '课程得分',
	PRIMARY KEY (id)
) COMMENT '学生选修课程信息';

INSERT INTO class (
	class_num,
	class_name,
	pass_score
)
VALUES
	('C0001', '语文', 60),
	('C002', '数学', 70),
	('C003', '英文', 60),
	('C004', '体育', 80),
	('C005', '音乐', 60),
	('C006', '美术', 70);

INSERT INTO student_class (
	student_name,
	class_num,
	score
)
VALUES
	('James', 'C001', 80),
	('Talor', 'C005', 75),
	('Kate', 'C002', 65),
	('David', 'C006', 82),
	('Ann', 'C004', 88),
	('Jan', 'C003', 70),
	('James', 'C002', 97),
	('Kate', 'C005', 90),
	('Jan', 'C005', 86),
	('Talor', 'C006', 92);
```

子查询的用法比较多，我们先来列举下有哪些子查询的使用方法。

# 1、子查询的使用方法

## 1、WHERE中的子查询

1、比较运算符

可以使用比较运算法，例如=，>，<将子查询返回的单个值与where子句表达式进行比较，如查找学生选择的编号最大的课程信息：

SELECT class.* FROM class WHERE class.class_num = ( SELECT MAX(class_num) FROM student_class );

2、in和not in

如果子查询返回多个值，则可以在WHERE子句中使用其他运算符，例如IN或NOT IN运算符。如查找学生都选择了哪些课程：

SELECT class.* FROM class WHERE class.class_num IN ( SELECT DISTINCT class_num FROM student_class );

3、FROM子查询

在FROM子句中使用子查询时，从子查询返回的结果集将用作临时表。该表称为派生表或实例化子查询。如 查找最热门和最冷门的课程分别有多少人选择：

SELECT max(count), min(count) FROM (SELECT class_num, count(1) as count FROM student_class group by class_num) as t1;









