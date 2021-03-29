


MySQL-fetchsize



在Statement和ResultSet接口中都有setFetchSize方法

void setFetchSize(int rows) throws SQLException

查看API文档

- Statement接口中是这样解释的：为 JDBC 驱动程序提供一个提示，它提示此 Statement 生成的 ResultSet 对象需要更多行时应该从数据库获取的行数。指定的行数仅影响使用此语句创建的结果集合。如果指定的值为 0，则忽略该提示。默认值为 0。

- ResultSet中是这样解释的：为 JDBC 驱动程序设置此 ResultSet 对象需要更多行时应该从数据库获取的行数。如果指定的获取大小为零，则 JDBC 驱动程序忽略该值，随意对获取大小作出它自己的最佳猜测。默认值由创建结果集的 Statement 对象设置。获取大小可以在任何时间更改。


缺省时，驱动程序一次从查询里获取所有的结果。这样可能对于大的数据集来说是不方便的， 因此 JDBC 驱动提供了一个用于设置从一个数据库游标抽取若干行的 ResultSet 的方法。在连接的客户端这边缓冲了一小部分数据行，并且在用尽之后， 则通过重定位游标检索下一个数据行块。


setFetchSize最主要是为了减少网络交互次数设计的。访问ResultSet时，如果它每次只从服务器上取一行数据，则会产生大量的开销。setFetchSize的意思是当调用rs.next时，ResultSet会一次性从服务器上取得多少行数据回来，这样在下次rs.next时，它可以直接从内存中获取出数据而不需要网络交互，提高了效率。 这个设置可能会被某些JDBC驱动忽略的，而且设置过大也会造成内存的上升。


另外在《Best practices to improve performance in JDBC》一文中也提及该方法的使用用于提高查询效率，有名词将之成为batch retrieval

> 具体设置方式


```java
package com.seven.dbTools.DBTools;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
 
public class JdbcHandleMySQLBigResultSet {
 
	public static long importData(String sql){
		String url = "jdbc:mysql://ipaddress:3306/test?user=username&password=password";
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		long allStart = System.currentTimeMillis();
		long count =0;
 
		Connection con = null;
		PreparedStatement ps = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			con = DriverManager.getConnection(url);
			//核心是这里
			ps = (PreparedStatement) con.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,
		              ResultSet.CONCUR_READ_ONLY);
					  
			ps.setFetchSize(Integer.MIN_VALUE);
			
			ps.setFetchDirection(ResultSet.FETCH_REVERSE);
 
			rs = ps.executeQuery();
 
 
			while (rs.next()) {
				
				//此处处理业务逻辑
				count++;
				if(count%600000==0){
					System.out.println(" 写入到第  "+(count/600000)+" 个文件中！");
					long end = System.currentTimeMillis();
				}
				
			}
			System.out.println("取回数据量为  "+count+" 行！");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if(rs!=null){
					rs.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if(ps!=null){
					ps.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if(con!=null){
					con.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return count;
 
	}
 
	public static void main(String[] args) throws InterruptedException {
 
		String sql = "select * from test.bigTable ";
		importData(sql);
 
	}
 
}
```


1、当statement设置以下属性时，采用的是流数据接收方式，每次只从服务器接收部份数据，直到所有数据处理完毕，不会发生JVM OOM。

```java
setResultSetType(ResultSet.TYPE_FORWARD_ONLY);
setFetchSize(Integer.MIN_VALUE); 
```


2、调用statement的enableStreamingResults方法，实际上enableStreamingResults方法内部封装的就是第1种方式。

3、设置连接属性useCursorFetch=true (5.0版驱动开始支持)，statement以TYPE_FORWARD_ONLY打开，再设置fetch size参数，表示采用服务器端游标，每次从服务器取fetch_size条数据。



# 参考

- [聊聊jdbc statement的fetchSize](https://www.cnblogs.com/fnlingnzb-learner/p/10245971.html)

- [正确使用MySQL JDBC setFetchSize()方法解决JDBC处理大结果集 java.lang.OutOfMemoryError: Java heap space](https://blog.csdn.net/seven_3306/article/details/9303879)



