



在相关实例实际上是代理类时提供获取委托实例能力的 JDBC 类的接口。

许多 JDBC 驱动程序实现使用包装器模式提供超越传统 JDBC API 的扩展，传统 JDBC API 是特定于数据源的。开发人员可能希望访问那些被包装（代理）为代表实际资源代理类实例的资源。此接口描述访问那些由代理代表的包装资源的标准机制，以允许对资源代理的直接访问。


# 参考

- [java.sql.Wrapper 接口源码分析](https://blog.csdn.net/u010647035/article/details/103550135)
- [java-api](http://itmyhome.com/java-api/java/sql/Wrapper.html)