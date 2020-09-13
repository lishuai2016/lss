

<!-- TOC -->

- [00、Driver](#00driver)
- [01、Connection](#01connection)
- [02、Statement](#02statement)
    - [1、PreparedStatement.addBatch()方法](#1preparedstatementaddbatch方法)
- [03、PreparedStatement继承Statement](#03preparedstatement继承statement)
- [04、ResultSet](#04resultset)
- [10、ResultSetMetaData](#10resultsetmetadata)

<!-- /TOC -->



# 00、Driver

驱动类建议是单例，代码简洁。当驱动类被加载的时候，应该实例化一个对象，并把该对象注册到DriverManager，这意味着用户可以通过 Class.forName("foo.bah.Driver")这样方式加载和注册驱动类。

```java

public interface Driver {

    //通过给定的URL获取一个连向数据库的链接，如果URL不正确，驱动返回null。
    Connection connect(String url, java.util.Properties info)
        throws SQLException;

    //当前驱动是否可以接受该URL创建链接，如果可以返回true；
    boolean acceptsURL(String url) throws SQLException;


    DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info)
                         throws SQLException;


    //驱动的主版本号
    int getMajorVersion();
    int getMinorVersion();
    boolean jdbcCompliant();

    //------------------------- JDBC 4.1 -----------------------------------
    public Logger getParentLogger() throws SQLFeatureNotSupportedException;

}
```



# 01、Connection

```java

public interface Connection  extends Wrapper, AutoCloseable {

     //Statement是用来发送无参数的SQL到数据库，如果同样的SQL被执行多次建议使用PreparedStatement
    Statement createStatement() throws SQLException;

     //预编译PreparedStatement提交待参数的SQL（有些驱动可能不支持预编译）
    PreparedStatement prepareStatement(String sql)
        throws SQLException;

     //存储过程？
    CallableStatement prepareCall(String sql) throws SQLException;

    //类似于格式化SQL
    String nativeSQL(String sql) throws SQLException;

    //设置事务的提交模式
    void setAutoCommit(boolean autoCommit) throws SQLException;

    //获取事务的提交模式
    boolean getAutoCommit() throws SQLException;

    //手动提交事务，持久化改变并释放当前connection持有的数据库锁
    void commit() throws SQLException;

    //回滚改变，并释放当前connection持有的数据库锁
    void rollback() throws SQLException;

    //关闭链接
    void close() throws SQLException;

    //close()被调用或者发生严重错误，链接被标记位关闭状态。不能用来判断链接的有效性。
    boolean isClosed() throws SQLException;

    
    // 高级特性 --------------------------------------------------------------
   
    //当前链接关联数据库的信息，包括数据库的表、支持的语法、存储过程以及链接的能力等等；
    DatabaseMetaData getMetaData() throws SQLException;

    //设置当前链接为readOnly模式，方便数据库进行优化。注意，该方法不能再一个事务中被调用
    void setReadOnly(boolean readOnly) throws SQLException;
    boolean isReadOnly() throws SQLException;

   
    //给当前数据库链接设置一个子空间，如果驱动不支持，忽略。对该链接之前创建的Statement没影响。
    void setCatalog(String catalog) throws SQLException;
    String getCatalog() throws SQLException;

    
    int TRANSACTION_NONE             = 0;//不支持事务
    int TRANSACTION_READ_UNCOMMITTED = 1;//事务隔离级别-读未提交
    int TRANSACTION_READ_COMMITTED   = 2;//事务隔离级别-读已提交
    int TRANSACTION_REPEATABLE_READ  = 4;//事务隔离级别-可重复读
    int TRANSACTION_SERIALIZABLE     = 8;//事务隔离级别-串性话

    //设置事务的隔离级别
    void setTransactionIsolation(int level) throws SQLException;
    int getTransactionIsolation() throws SQLException;

    SQLWarning getWarnings() throws SQLException;
    void clearWarnings() throws SQLException;


    //--------------------------JDBC 2.0-----------------------------
     //指定参数创建Statement：resultSetType=ResultSet.TYPE_FORWARD_ONLY\ResultSet.TYPE_SCROLL_INSENSITIVE\ResultSet.TYPE_SCROLL_SENSITIVE
     //resultSetConcurrency=ResultSet.CONCUR_READ_ONLY\ResultSet.CONCUR_UPDATABLE
    Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException;

    //同上
    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency)
        throws SQLException;
    
    //同上
    CallableStatement prepareCall(String sql, int resultSetType,
                                  int resultSetConcurrency) throws SQLException;

    
    java.util.Map<String,Class<?>> getTypeMap() throws SQLException;
    void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException;

    //--------------------------JDBC 3.0-----------------------------

    // 取值 ResultSet.HOLD_CURSORS_OVER_COMMIT、ResultSet.CLOSE_CURSORS_AT_COMMIT
    void setHoldability(int holdability) throws SQLException;
    int getHoldability() throws SQLException;

    
     //在当前事务中创建一个没有名字的安全点，并且返回
    Savepoint setSavepoint() throws SQLException;
    //安全点指定名称
    Savepoint setSavepoint(String name) throws SQLException;

    //回滚指定安全点后的所有操作。（该方法只能在关闭事务的自动提交功能被调用）
    void rollback(Savepoint savepoint) throws SQLException;

    //移除当前事务以及子事务上的安全点
    void releaseSavepoint(Savepoint savepoint) throws SQLException;

    //重载上面的方法，多了一个参数resultSetHoldability取值，ResultSet.HOLD_CURSORS_OVER_COMMIT、ResultSet.CLOSE_CURSORS_AT_COMMIT
    Statement createStatement(int resultSetType, int resultSetConcurrency,
                              int resultSetHoldability) throws SQLException;

    //同上
    PreparedStatement prepareStatement(String sql, int resultSetType,
                                       int resultSetConcurrency, int resultSetHoldability)
        throws SQLException;

    //同上
    CallableStatement prepareCall(String sql, int resultSetType,
                                  int resultSetConcurrency,
                                  int resultSetHoldability) throws SQLException;


    //autoGeneratedKeys取值Statement.RETURN_GENERATED_KEYS、Statement.NO_GENERATED_KEYS，应该是自动生成主键，如果不是INSERT语句将会被忽略
    PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException;
    //列的顺序数组
    PreparedStatement prepareStatement(String sql, int columnIndexes[])
        throws SQLException;
    //列的名字数组
    PreparedStatement prepareStatement(String sql, String columnNames[])
        throws SQLException;

    Clob createClob() throws SQLException;
    Blob createBlob() throws SQLException; 
    NClob createNClob() throws SQLException;
    SQLXML createSQLXML() throws SQLException;


    //校验链接的有效性。如果链接没有被关闭，并且有效返回true。当方法被调用的时候驱动应该发送一个查询或者其他的机制来保证链接的有效性
    boolean isValid(int timeout) throws SQLException;

    //设置客户端的属性值
    void setClientInfo(String name, String value)
        throws SQLClientInfoException;
    void setClientInfo(Properties properties)
        throws SQLClientInfoException;
    String getClientInfo(String name)
        throws SQLException;
    Properties getClientInfo()
        throws SQLException;

    //
    Array createArrayOf(String typeName, Object[] elements) throws
    SQLException;

    //
    Struct createStruct(String typeName, Object[] attributes)
    throws SQLException;

   //--------------------------JDBC 4.1 -----------------------------

    void setSchema(String schema) throws SQLException;
    String getSchema() throws SQLException;

    //标记位关链接闭close状态，使用线程池去释放资源
    void abort(Executor executor) throws SQLException;

     //链接被创建的最大等待时间
    void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException;
    int getNetworkTimeout() throws SQLException;
}

```




# 02、Statement

```java



//接口功能：执行一个SQL语句并且返回结果。默认的一个Statement对象只能产生一个ResultSet。
public interface Statement extends Wrapper, AutoCloseable {

    //执行查询select类型SQL返回结果。该方法不能再PreparedStatement和CallableStatement中被调用
    ResultSet executeQuery(String sql) throws SQLException;

    //执行DML的INSERT、UPDATE、DELETE或者其他SQL（没有返回值的），比如ddl。
    int executeUpdate(String sql) throws SQLException;

    void close() throws SQLException;

    //----------------------------------------------------------------------

    int getMaxFieldSize() throws SQLException;
    void setMaxFieldSize(int max) throws SQLException;

    int getMaxRows() throws SQLException;
    void setMaxRows(int max) throws SQLException;
    //默认true
    void setEscapeProcessing(boolean enable) throws SQLException;

    //单位是秒，等待执行结果返回最大等待时间，超了报错
    int getQueryTimeout() throws SQLException;
    //默认是没有限制，0。会被应用到execute、executeQuery、executeUpdate，另外建议ResultSet。
    void setQueryTimeout(int seconds) throws SQLException;

    //一个线程去取消另外一个线程执行的查询
    void cancel() throws SQLException;

    
    SQLWarning getWarnings() throws SQLException;
    void clearWarnings() throws SQLException;

    void setCursorName(String name) throws SQLException;

    //----------------------- Multiple Results --------------------------

    
    //执行SQL有可能返回多个结果。如果第一个结果是ResultSet对象返回true；如果是update更新或者没有返回值返回false
    boolean execute(String sql) throws SQLException;

    //只能被调用一次
    ResultSet getResultSet() throws SQLException;

    //如果结果是ResultSet或者没有返回值返回-1，否则返回更新的行数
    int getUpdateCount() throws SQLException;

    //类似于游标，下一个也是ResultSet对象时返回true
    boolean getMoreResults() throws SQLException;


    //--------------------------JDBC 2.0-----------------------------

     //默认ResultSet.FETCH_FORWARD，还可取ResultSet.FETCH_REVERSE、ResultSet.FETCH_UNKNOWN
    void setFetchDirection(int direction) throws SQLException;
    int getFetchDirection() throws SQLException;

    //每一次和数据库交互的返回行数。默认0，没有限制
    void setFetchSize(int rows) throws SQLException;
    int getFetchSize() throws SQLException;

    //返回值ResultSet.CONCUR_READ_ONLY、ResultSet.CONCUR_UPDATABLE
    int getResultSetConcurrency() throws SQLException;

    //ResultSet.TYPE_FORWARD_ONLY、ResultSet.TYPE_SCROLL_INSENSITIVE、ResultSet.TYPE_SCROLL_SENSITIVE
    int getResultSetType()  throws SQLException;


    //添加一批SQL，然后通过调用executeBatch批量执行。
    void addBatch( String sql ) throws SQLException;
    //对应上面，情况保存的批量SQL
    void clearBatch() throws SQLException;

    //批量提交执行SQL，返回每条SQL执行的更新数，数组的顺序和添加SQL的顺序一致
    int[] executeBatch() throws SQLException;

    //返回产生当前对象的链接对象
    Connection getConnection()  throws SQLException;

  //--------------------------JDBC 3.0-----------------------------

  
    int CLOSE_CURRENT_RESULT = 1;//调用getMoreResults方法返回值，表示ResultSet关闭了
    int KEEP_CURRENT_RESULT = 2;//调用getMoreResults方法返回值，表示ResultSet没有关闭
    int CLOSE_ALL_RESULTS = 3;//调用getMoreResults方法返回值，表示前一个ResultSet还处于打开应该关闭

    //批处理执行成功，但是没有影响记录，比如根据id更新一个不存在的即可？
    int SUCCESS_NO_INFO = -2;
    //批处理SQL执行失败
    int EXECUTE_FAILED = -3;

    
    int RETURN_GENERATED_KEYS = 1;
    int NO_GENERATED_KEYS = 2;

    /**
     * Moves to this <code>Statement</code> object's next result, deals with
     * any current <code>ResultSet</code> object(s) according  to the instructions
     * specified by the given flag, and returns
     * <code>true</code> if the next result is a <code>ResultSet</code> object.
     *
     * <P>There are no more results when the following is true:
     * <PRE>{@code
     *     // stmt is a Statement object
     *     ((stmt.getMoreResults(current) == false) && (stmt.getUpdateCount() == -1))
     * }</PRE>
     *
     * @param current one of the following <code>Statement</code>
     *        constants indicating what should happen to current
     *        <code>ResultSet</code> objects obtained using the method
     *        <code>getResultSet</code>:
     *        <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *        <code>Statement.KEEP_CURRENT_RESULT</code>, or
     *        <code>Statement.CLOSE_ALL_RESULTS</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there are no
     *         more results
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code> or the argument
         *         supplied is not one of the following:
     *        <code>Statement.CLOSE_CURRENT_RESULT</code>,
     *        <code>Statement.KEEP_CURRENT_RESULT</code> or
     *        <code>Statement.CLOSE_ALL_RESULTS</code>
     *@exception SQLFeatureNotSupportedException if
     * <code>DatabaseMetaData.supportsMultipleOpenResults</code> returns
     * <code>false</code> and either
     *        <code>Statement.KEEP_CURRENT_RESULT</code> or
     *        <code>Statement.CLOSE_ALL_RESULTS</code> are supplied as
     * the argument.
     * @since 1.4
     * @see #execute
     */
    boolean getMoreResults(int current) throws SQLException;

    /**
     * Retrieves any auto-generated keys created as a result of executing this
     * <code>Statement</code> object. If this <code>Statement</code> object did
     * not generate any keys, an empty <code>ResultSet</code>
     * object is returned.
     *
     *<p><B>Note:</B>If the columns which represent the auto-generated keys were not specified,
     * the JDBC driver implementation will determine the columns which best represent the auto-generated keys.
     *
     * @return a <code>ResultSet</code> object containing the auto-generated key(s)
     *         generated by the execution of this <code>Statement</code> object
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>Statement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.4
     */
    ResultSet getGeneratedKeys() throws SQLException;

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the
     * auto-generated keys produced by this <code>Statement</code> object
     * should be made available for retrieval.  The driver will ignore the
     * flag if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *<p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     * @param sql an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *        should be made available for retrieval;
     *         one of the following constants:
     *         <code>Statement.RETURN_GENERATED_KEYS</code>
     *         <code>Statement.NO_GENERATED_KEYS</code>
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     *  this method is called on a closed <code>Statement</code>, the given
     *            SQL statement returns a <code>ResultSet</code> object,
     *            the given constant is not one of those allowed, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.4
     */
    int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException;

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *<p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     * @param sql an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param columnIndexes an array of column indexes indicating the columns
     *        that should be returned from the inserted row
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the SQL
     * statement returns a <code>ResultSet</code> object,the second argument
     * supplied to this method is not an
     * <code>int</code> array whose elements are valid column indexes, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.4
     */
    int executeUpdate(String sql, int columnIndexes[]) throws SQLException;

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     *<p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     * @param sql an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     * @param columnNames an array of the names of the columns that should be
     *        returned from the inserted row
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
     *         or <code>DELETE</code> statements, or 0 for SQL statements
     *         that return nothing
     * @exception SQLException if a database access error occurs,
     *  this method is called on a closed <code>Statement</code>, the SQL
     *            statement returns a <code>ResultSet</code> object, the
     *            second argument supplied to this method is not a <code>String</code> array
     *            whose elements are valid column names, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.4
     */
    int executeUpdate(String sql, String columnNames[]) throws SQLException;

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that any
     * auto-generated keys should be made available
     * for retrieval.  The driver will ignore this signal if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <P>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <P>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *<p>
     *<strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     * @param sql any SQL statement
     * @param autoGeneratedKeys a constant indicating whether auto-generated
     *        keys should be made available for retrieval using the method
     *        <code>getGeneratedKeys</code>; one of the following constants:
     *        <code>Statement.RETURN_GENERATED_KEYS</code> or
     *        <code>Statement.NO_GENERATED_KEYS</code>
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there are
     *         no results
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the second
     *         parameter supplied to this method is not
     *         <code>Statement.RETURN_GENERATED_KEYS</code> or
     *         <code>Statement.NO_GENERATED_KEYS</code>,
     * the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     *
     * @since 1.4
     */
    boolean execute(String sql, int autoGeneratedKeys) throws SQLException;

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.  This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <P>
     * Under some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <P>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *<p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     * @param sql any SQL statement
     * @param columnIndexes an array of the indexes of the columns in the
     *        inserted row that should be  made available for retrieval by a
     *        call to the method <code>getGeneratedKeys</code>
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there
     *         are no results
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the
     *            elements in the <code>int</code> array passed to this method
     *            are not valid column indexes, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     *
     * @since 1.4
     */
    boolean execute(String sql, int columnIndexes[]) throws SQLException;

    /**
     * Executes the given SQL statement, which may return multiple results,
     * and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval. This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available.  The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <P>
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <P>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *<p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     * @param sql any SQL statement
     * @param columnNames an array of the names of the columns in the inserted
     *        row that should be made available for retrieval by a call to the
     *        method <code>getGeneratedKeys</code>
     * @return <code>true</code> if the next result is a <code>ResultSet</code>
     *         object; <code>false</code> if it is an update count or there
     *         are no more results
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>,the
     *          elements of the <code>String</code> array passed to this
     *          method are not valid column names, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @see #getResultSet
     * @see #getUpdateCount
     * @see #getMoreResults
     * @see #getGeneratedKeys
     *
     * @since 1.4
     */
    boolean execute(String sql, String columnNames[]) throws SQLException;

   /**
     * Retrieves the result set holdability for <code>ResultSet</code> objects
     * generated by this <code>Statement</code> object.
     *
     * @return either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>Statement</code>
     *
     * @since 1.4
     */
    int getResultSetHoldability() throws SQLException;

    /**
     * Retrieves whether this <code>Statement</code> object has been closed. A <code>Statement</code> is closed if the
     * method close has been called on it, or if it is automatically closed.
     * @return true if this <code>Statement</code> object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    boolean isClosed() throws SQLException;

        /**
         * Requests that a <code>Statement</code> be pooled or not pooled.  The value
         * specified is a hint to the statement pool implementation indicating
         * whether the application wants the statement to be pooled.  It is up to
         * the statement pool manager as to whether the hint is used.
         * <p>
         * The poolable value of a statement is applicable to both internal
         * statement caches implemented by the driver and external statement caches
         * implemented by application servers and other applications.
         * <p>
         * By default, a <code>Statement</code> is not poolable when created, and
         * a <code>PreparedStatement</code> and <code>CallableStatement</code>
         * are poolable when created.
         * <p>
         * @param poolable              requests that the statement be pooled if true and
         *                                              that the statement not be pooled if false
         * <p>
         * @throws SQLException if this method is called on a closed
         * <code>Statement</code>
         * <p>
         * @since 1.6
         */
        void setPoolable(boolean poolable)
                throws SQLException;

        /**
         * Returns a  value indicating whether the <code>Statement</code>
         * is poolable or not.
         * <p>
         * @return              <code>true</code> if the <code>Statement</code>
         * is poolable; <code>false</code> otherwise
         * <p>
         * @throws SQLException if this method is called on a closed
         * <code>Statement</code>
         * <p>
         * @since 1.6
         * <p>
         * @see java.sql.Statement#setPoolable(boolean) setPoolable(boolean)
         */
        boolean isPoolable()
                throws SQLException;

    //--------------------------JDBC 4.1 -----------------------------

    /**
     * Specifies that this {@code Statement} will be closed when all its
     * dependent result sets are closed. If execution of the {@code Statement}
     * does not produce any result sets, this method has no effect.
     * <p>
     * <strong>Note:</strong> Multiple calls to {@code closeOnCompletion} do
     * not toggle the effect on this {@code Statement}. However, a call to
     * {@code closeOnCompletion} does effect both the subsequent execution of
     * statements, and statements that currently have open, dependent,
     * result sets.
     *
     * @throws SQLException if this method is called on a closed
     * {@code Statement}
     * @since 1.7
     */
    public void closeOnCompletion() throws SQLException;

    /**
     * Returns a value indicating whether this {@code Statement} will be
     * closed when all its dependent result sets are closed.
     * @return {@code true} if the {@code Statement} will be closed when all
     * of its dependent result sets are closed; {@code false} otherwise
     * @throws SQLException if this method is called on a closed
     * {@code Statement}
     * @since 1.7
     */
    public boolean isCloseOnCompletion() throws SQLException;


    //--------------------------JDBC 4.2 -----------------------------

    /**
     *  Retrieves the current result as an update count; if the result
     * is a <code>ResultSet</code> object or there are no more results, -1
     *  is returned. This method should be called only once per result.
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     *<p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @return the current result as an update count; -1 if the current result
     * is a <code>ResultSet</code> object or there are no more results
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>Statement</code>
     * @see #execute
     * @since 1.8
     */
    default long getLargeUpdateCount() throws SQLException {
        throw new UnsupportedOperationException("getLargeUpdateCount not implemented");
    }

    /**
     * Sets the limit for the maximum number of rows that any
     * <code>ResultSet</code> object  generated by this <code>Statement</code>
     * object can contain to the given number.
     * If the limit is exceeded, the excess
     * rows are silently dropped.
     * <p>
     * This method should be used when the row limit may exceed
     * {@link Integer#MAX_VALUE}.
     *<p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @param max the new max rows limit; zero means there is no limit
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>
     *            or the condition {@code max >= 0} is not satisfied
     * @see #getMaxRows
     * @since 1.8
     */
    default void setLargeMaxRows(long max) throws SQLException {
        throw new UnsupportedOperationException("setLargeMaxRows not implemented");
    }

    /**
     * Retrieves the maximum number of rows that a
     * <code>ResultSet</code> object produced by this
     * <code>Statement</code> object can contain.  If this limit is exceeded,
     * the excess rows are silently dropped.
     * <p>
     * This method should be used when the returned row limit may exceed
     * {@link Integer#MAX_VALUE}.
     *<p>
     * The default implementation will return {@code 0}
     *
     * @return the current maximum number of rows for a <code>ResultSet</code>
     *         object produced by this <code>Statement</code> object;
     *         zero means there is no limit
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>Statement</code>
     * @see #setMaxRows
     * @since 1.8
     */
    default long getLargeMaxRows() throws SQLException {
        return 0;
    }

    /**
     * Submits a batch of commands to the database for execution and
     * if all commands execute successfully, returns an array of update counts.
     * The <code>long</code> elements of the array that is returned are ordered
     * to correspond to the commands in the batch, which are ordered
     * according to the order in which they were added to the batch.
     * The elements in the array returned by the method {@code executeLargeBatch}
     * may be one of the following:
     * <OL>
     * <LI>A number greater than or equal to zero -- indicates that the
     * command was processed successfully and is an update count giving the
     * number of rows in the database that were affected by the command's
     * execution
     * <LI>A value of <code>SUCCESS_NO_INFO</code> -- indicates that the command was
     * processed successfully but that the number of rows affected is
     * unknown
     * <P>
     * If one of the commands in a batch update fails to execute properly,
     * this method throws a <code>BatchUpdateException</code>, and a JDBC
     * driver may or may not continue to process the remaining commands in
     * the batch.  However, the driver's behavior must be consistent with a
     * particular DBMS, either always continuing to process commands or never
     * continuing to process commands.  If the driver continues processing
     * after a failure, the array returned by the method
     * <code>BatchUpdateException.getLargeUpdateCounts</code>
     * will contain as many elements as there are commands in the batch, and
     * at least one of the elements will be the following:
     *
     * <LI>A value of <code>EXECUTE_FAILED</code> -- indicates that the command failed
     * to execute successfully and occurs only if a driver continues to
     * process commands after a command fails
     * </OL>
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     *<p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @return an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code> or the
     * driver does not support batch statements. Throws {@link BatchUpdateException}
     * (a subclass of <code>SQLException</code>) if one of the commands sent to the
     * database fails to execute properly or attempts to return a result set.
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     *
     * @see #addBatch
     * @see DatabaseMetaData#supportsBatchUpdates
     * @since 1.8
     */
    default long[] executeLargeBatch() throws SQLException {
        throw new UnsupportedOperationException("executeLargeBatch not implemented");
    }

    /**
     * Executes the given SQL statement, which may be an <code>INSERT</code>,
     * <code>UPDATE</code>, or <code>DELETE</code> statement or an
     * SQL statement that returns nothing, such as an SQL DDL statement.
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *<p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @return either (1) the row count for SQL Data Manipulation Language
     * (DML) statements or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the given
     * SQL statement produces a <code>ResultSet</code> object, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.8
     */
    default long executeLargeUpdate(String sql) throws SQLException {
        throw new UnsupportedOperationException("executeLargeUpdate not implemented");
    }

    /**
     * Executes the given SQL statement and signals the driver with the
     * given flag about whether the
     * auto-generated keys produced by this <code>Statement</code> object
     * should be made available for retrieval.  The driver will ignore the
     * flag if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *        should be made available for retrieval;
     *         one of the following constants:
     *         <code>Statement.RETURN_GENERATED_KEYS</code>
     *         <code>Statement.NO_GENERATED_KEYS</code>
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     *  this method is called on a closed <code>Statement</code>, the given
     *            SQL statement returns a <code>ResultSet</code> object,
     *            the given constant is not one of those allowed, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method with a constant of Statement.RETURN_GENERATED_KEYS
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.8
     */
    default long executeLargeUpdate(String sql, int autoGeneratedKeys)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("executeLargeUpdate not implemented");
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the indexes of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param columnIndexes an array of column indexes indicating the columns
     *        that should be returned from the inserted row
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     *
     * @exception SQLException if a database access error occurs,
     * this method is called on a closed <code>Statement</code>, the SQL
     * statement returns a <code>ResultSet</code> object,the second argument
     * supplied to this method is not an
     * <code>int</code> array whose elements are valid column indexes, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.8
     */
    default long executeLargeUpdate(String sql, int columnIndexes[]) throws SQLException {
        throw new SQLFeatureNotSupportedException("executeLargeUpdate not implemented");
    }

    /**
     * Executes the given SQL statement and signals the driver that the
     * auto-generated keys indicated in the given array should be made available
     * for retrieval.   This array contains the names of the columns in the
     * target table that contain the auto-generated keys that should be made
     * available. The driver will ignore the array if the SQL statement
     * is not an <code>INSERT</code> statement, or an SQL statement able to return
     * auto-generated keys (the list of such statements is vendor-specific).
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param sql an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     * @param columnNames an array of the names of the columns that should be
     *        returned from the inserted row
     * @return either the row count for <code>INSERT</code>, <code>UPDATE</code>,
     *         or <code>DELETE</code> statements, or 0 for SQL statements
     *         that return nothing
     * @exception SQLException if a database access error occurs,
     *  this method is called on a closed <code>Statement</code>, the SQL
     *            statement returns a <code>ResultSet</code> object, the
     *            second argument supplied to this method is not a <code>String</code> array
     *            whose elements are valid column names, the method is called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.8
     */
    default long executeLargeUpdate(String sql, String columnNames[])
            throws SQLException {
        throw new SQLFeatureNotSupportedException("executeLargeUpdate not implemented");
    }
}

```

## 1、PreparedStatement.addBatch()方法

https://blog.csdn.net/blueling51/article/details/6928755

- 1.建立链接： Connection    connection =getConnection();

- 2.不自动 Commit：connection.setAutoCommit(false);   

- 3.预编译SQL语句,只编译一回哦,效率高：PreparedStatement statement = connection.prepareStatement("INSERT INTO TABLEX VALUES(?, ?)");   

- 4.一个个添加

```sql
//记录1
statement.setInt(1, 1);
statement.setString(2, "Cujo");
statement.addBatch();   

//记录2
statement.setInt(1, 2);
statement.setString(2, "Fred");
statement.addBatch();   

//记录3
statement.setInt(1, 3);
statement.setString(2, "Mark");
statement.addBatch();   

```

- 5、批量执行上面3条语句： int [] counts = statement.executeBatch();   

- 6、connection.commit();

# 03、PreparedStatement继承Statement


```java

package java.sql;

import java.math.BigDecimal;
import java.util.Calendar;
import java.io.Reader;
import java.io.InputStream;

/**
 * An object that represents a precompiled SQL statement.
 * <P>A SQL statement is precompiled and stored in a
 * <code>PreparedStatement</code> object. This object can then be used to
 * efficiently execute this statement multiple times.
 *
 * <P><B>Note:</B> The setter methods (<code>setShort</code>, <code>setString</code>,
 * and so on) for setting IN parameter values
 * must specify types that are compatible with the defined SQL type of
 * the input parameter. For instance, if the IN parameter has SQL type
 * <code>INTEGER</code>, then the method <code>setInt</code> should be used.
 *
 * <p>If arbitrary parameter type conversions are required, the method
 * <code>setObject</code> should be used with a target SQL type.
 * <P>
 * In the following example of setting a parameter, <code>con</code> represents
 * an active connection:
 * <PRE>
 *   PreparedStatement pstmt = con.prepareStatement("UPDATE EMPLOYEES
 *                                     SET SALARY = ? WHERE ID = ?");
 *   pstmt.setBigDecimal(1, 153833.00)
 *   pstmt.setInt(2, 110592)
 * </PRE>
 *
 * @see Connection#prepareStatement
 * @see ResultSet
 */

public interface PreparedStatement extends Statement {

    /**
     * Executes the SQL query in this <code>PreparedStatement</code> object
     * and returns the <code>ResultSet</code> object generated by the query.
     *
     * @return a <code>ResultSet</code> object that contains the data produced by the
     *         query; never <code>null</code>
     * @exception SQLException if a database access error occurs;
     * this method is called on a closed  <code>PreparedStatement</code> or the SQL
     *            statement does not return a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    ResultSet executeQuery() throws SQLException;

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which must be an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     * @exception SQLException if a database access error occurs;
     * this method is called on a closed  <code>PreparedStatement</code>
     * or the SQL statement returns a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     */
    int executeUpdate() throws SQLException;

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     *
     * <P><B>Note:</B> You must specify the parameter's SQL type.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType the SQL type code defined in <code>java.sql.Types</code>
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception SQLFeatureNotSupportedException if <code>sqlType</code> is
     * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
     * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
     * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
     *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
     * or  <code>STRUCT</code> data type and the JDBC driver does not support
     * this data type
     */
    void setNull(int parameterIndex, int sqlType) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>boolean</code> value.
     * The driver converts this
     * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement;
     * if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setBoolean(int parameterIndex, boolean x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>byte</code> value.
     * The driver converts this
     * to an SQL <code>TINYINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setByte(int parameterIndex, byte x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>short</code> value.
     * The driver converts this
     * to an SQL <code>SMALLINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setShort(int parameterIndex, short x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>int</code> value.
     * The driver converts this
     * to an SQL <code>INTEGER</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setInt(int parameterIndex, int x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>long</code> value.
     * The driver converts this
     * to an SQL <code>BIGINT</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setLong(int parameterIndex, long x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>float</code> value.
     * The driver converts this
     * to an SQL <code>REAL</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setFloat(int parameterIndex, float x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>double</code> value.
     * The driver converts this
     * to an SQL <code>DOUBLE</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setDouble(int parameterIndex, double x) throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.math.BigDecimal</code> value.
     * The driver converts this to an SQL <code>NUMERIC</code> value when
     * it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java <code>String</code> value.
     * The driver converts this
     * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>VARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setString(int parameterIndex, String x) throws SQLException;

    /**
     * Sets the designated parameter to the given Java array of bytes.  The driver converts
     * this to an SQL <code>VARBINARY</code> or <code>LONGVARBINARY</code>
     * (depending on the argument's size relative to the driver's limits on
     * <code>VARBINARY</code> values) when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setBytes(int parameterIndex, byte x[]) throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value
     * using the default time zone of the virtual machine that is running
     * the application.
     * The driver converts this
     * to an SQL <code>DATE</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setDate(int parameterIndex, java.sql.Date x)
            throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value.
     * The driver converts this
     * to an SQL <code>TIME</code> value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setTime(int parameterIndex, java.sql.Time x)
            throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.
     * The driver
     * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
     * database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>     */
    void setTimestamp(int parameterIndex, java.sql.Timestamp x)
            throws SQLException;

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setAsciiStream(int parameterIndex, java.io.InputStream x, int length)
            throws SQLException;

    /**
     * Sets the designated parameter to the given input stream, which
     * will have the specified number of bytes.
     *
     * When a very large Unicode value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from Unicode to the database char format.
     *
     *The byte format of the Unicode stream must be a Java UTF-8, as defined in the
     *Java Virtual Machine Specification.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x a <code>java.io.InputStream</code> object that contains the
     *        Unicode parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @deprecated Use {@code setCharacterStream}
     */
    @Deprecated
    void setUnicodeStream(int parameterIndex, java.io.InputStream x,
                          int length) throws SQLException;

    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java input stream which contains the binary parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void setBinaryStream(int parameterIndex, java.io.InputStream x,
                         int length) throws SQLException;

    /**
     * Clears the current parameter values immediately.
     * <P>In general, parameter values remain in force for repeated use of a
     * statement. Setting a parameter value automatically clears its
     * previous value.  However, in some cases it is useful to immediately
     * release the resources used by the current parameter values; this can
     * be done by calling the method <code>clearParameters</code>.
     *
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     */
    void clearParameters() throws SQLException;

    //----------------------------------------------------------------------
    // Advanced features:

   /**
    * Sets the value of the designated parameter with the given object.
    *
    * This method is similar to {@link #setObject(int parameterIndex,
    * Object x, int targetSqlType, int scaleOrLength)},
    * except that it assumes a scale of zero.
    *
    * @param parameterIndex the first parameter is 1, the second is 2, ...
    * @param x the object containing the input parameter value
    * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
    *                      sent to the database
    * @exception SQLException if parameterIndex does not correspond to a parameter
    * marker in the SQL statement; if a database access error occurs or this
    * method is called on a closed PreparedStatement
    * @exception SQLFeatureNotSupportedException if
    * the JDBC driver does not support the specified targetSqlType
    * @see Types
    */
    void setObject(int parameterIndex, Object x, int targetSqlType)
      throws SQLException;

    /**
     * <p>Sets the value of the designated parameter using the given object.
     *
     * <p>The JDBC specification specifies a standard mapping from
     * Java <code>Object</code> types to SQL types.  The given argument
     * will be converted to the corresponding SQL type before being
     * sent to the database.
     *
     * <p>Note that this method may be used to pass datatabase-
     * specific abstract data types, by using a driver-specific Java
     * type.
     *
     * If the object is of a class implementing the interface <code>SQLData</code>,
     * the JDBC driver should call the method <code>SQLData.writeSQL</code>
     * to write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
     *  <code>Struct</code>, <code>java.net.URL</code>, <code>RowId</code>, <code>SQLXML</code>
     * or <code>Array</code>, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     * <P>
     *<b>Note:</b> Not all databases allow for a non-typed Null to be sent to
     * the backend. For maximum portability, the <code>setNull</code> or the
     * <code>setObject(int parameterIndex, Object x, int sqlType)</code>
     * method should be used
     * instead of <code>setObject(int parameterIndex, Object x)</code>.
     *<p>
     * <b>Note:</b> This method throws an exception if there is an ambiguity, for example, if the
     * object is of a class implementing more than one of the interfaces named above.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     *  this method is called on a closed <code>PreparedStatement</code>
     * or the type of the given object is ambiguous
     */
    void setObject(int parameterIndex, Object x) throws SQLException;

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which may be any kind of SQL statement.
     * Some prepared statements return multiple results; the <code>execute</code>
     * method handles these complex statements as well as the simpler
     * form of statements handled by the methods <code>executeQuery</code>
     * and <code>executeUpdate</code>.
     * <P>
     * The <code>execute</code> method returns a <code>boolean</code> to
     * indicate the form of the first result.  You must call either the method
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result; you must call <code>getMoreResults</code> to
     * move to any subsequent result(s).
     *
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     *         object; <code>false</code> if the first result is an update
     *         count or there is no result
     * @exception SQLException if a database access error occurs;
     * this method is called on a closed <code>PreparedStatement</code>
     * or an argument is supplied to this method
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @see Statement#execute
     * @see Statement#getResultSet
     * @see Statement#getUpdateCount
     * @see Statement#getMoreResults

     */
    boolean execute() throws SQLException;

    //--------------------------JDBC 2.0-----------------------------

    /**
     * Adds a set of parameters to this <code>PreparedStatement</code>
     * object's batch of commands.
     *
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @see Statement#addBatch
     * @since 1.2
     */
    void addBatch() throws SQLException;

    /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader the <code>java.io.Reader</code> object that contains the
     *        Unicode data
     * @param length the number of characters in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    void setCharacterStream(int parameterIndex,
                          java.io.Reader reader,
                          int length) throws SQLException;

    /**
     * Sets the designated parameter to the given
     *  <code>REF(&lt;structured-type&gt;)</code> value.
     * The driver converts this to an SQL <code>REF</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x an SQL <code>REF</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    void setRef (int parameterIndex, Ref x) throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Blob</code> object.
     * The driver converts this to an SQL <code>BLOB</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    void setBlob (int parameterIndex, Blob x) throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Clob</code> object.
     * The driver converts this to an SQL <code>CLOB</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    void setClob (int parameterIndex, Clob x) throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Array</code> object.
     * The driver converts this to an SQL <code>ARRAY</code> value when it
     * sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x an <code>Array</code> object that maps an SQL <code>ARRAY</code> value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.2
     */
    void setArray (int parameterIndex, Array x) throws SQLException;

    /**
     * Retrieves a <code>ResultSetMetaData</code> object that contains
     * information about the columns of the <code>ResultSet</code> object
     * that will be returned when this <code>PreparedStatement</code> object
     * is executed.
     * <P>
     * Because a <code>PreparedStatement</code> object is precompiled, it is
     * possible to know about the <code>ResultSet</code> object that it will
     * return without having to execute it.  Consequently, it is possible
     * to invoke the method <code>getMetaData</code> on a
     * <code>PreparedStatement</code> object rather than waiting to execute
     * it and then invoking the <code>ResultSet.getMetaData</code> method
     * on the <code>ResultSet</code> object that is returned.
     * <P>
     * <B>NOTE:</B> Using this method may be expensive for some drivers due
     * to the lack of underlying DBMS support.
     *
     * @return the description of a <code>ResultSet</code> object's columns or
     *         <code>null</code> if the driver cannot return a
     *         <code>ResultSetMetaData</code> object
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    ResultSetMetaData getMetaData() throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
     * which the driver then sends to the database.  With
     * a <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the date
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
            throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
     * which the driver then sends to the database.  With
     * a <code>Calendar</code> object, the driver can calculate the time
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the time
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    void setTime(int parameterIndex, java.sql.Time x, Calendar cal)
            throws SQLException;

    /**
     * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
     * using the given <code>Calendar</code> object.  The driver uses
     * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
     * which the driver then sends to the database.  With a
     *  <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom timezone.  If no
     * <code>Calendar</code> object is specified, the driver uses the default
     * timezone, which is that of the virtual machine running the application.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the <code>Calendar</code> object the driver will use
     *            to construct the timestamp
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.2
     */
    void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal)
            throws SQLException;

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * This version of the method <code>setNull</code> should
     * be used for user-defined types and REF type parameters.  Examples
     * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
     * named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully-qualified SQL type name when specifying
     * a NULL user-defined or REF parameter.  In the case of a user-defined type
     * the name is the type name of the parameter itself.  For a REF
     * parameter, the name is the type name of the referenced type.  If
     * a JDBC driver does not need the type code or type name information,
     * it may ignore it.
     *
     * Although it is intended for user-defined and Ref parameters,
     * this method may be used to set a null parameter of any JDBC type.
     * If the parameter does not have a user-defined or REF type, the given
     * typeName is ignored.
     *
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param sqlType a value from <code>java.sql.Types</code>
     * @param typeName the fully-qualified name of an SQL user-defined type;
     *  ignored if the parameter is not a user-defined type or REF
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @exception SQLFeatureNotSupportedException if <code>sqlType</code> is
     * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
     * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
     * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
     *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
     * or  <code>STRUCT</code> data type and the JDBC driver does not support
     * this data type or if the JDBC driver does not support this method
     * @since 1.2
     */
  void setNull (int parameterIndex, int sqlType, String typeName)
    throws SQLException;

    //------------------------- JDBC 3.0 -----------------------------------

    /**
     * Sets the designated parameter to the given <code>java.net.URL</code> value.
     * The driver converts this to an SQL <code>DATALINK</code> value
     * when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the <code>java.net.URL</code> object to be set
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.4
     */
    void setURL(int parameterIndex, java.net.URL x) throws SQLException;

    /**
     * Retrieves the number, types and properties of this
     * <code>PreparedStatement</code> object's parameters.
     *
     * @return a <code>ParameterMetaData</code> object that contains information
     *         about the number, types and properties for each
     *  parameter marker of this <code>PreparedStatement</code> object
     * @exception SQLException if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @see ParameterMetaData
     * @since 1.4
     */
    ParameterMetaData getParameterMetaData() throws SQLException;

    //------------------------- JDBC 4.0 -----------------------------------

    /**
     * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
     * driver converts this to a SQL <code>ROWID</code> value when it sends it
     * to the database
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
    void setRowId(int parameterIndex, RowId x) throws SQLException;


    /**
     * Sets the designated parameter to the given <code>String</code> object.
     * The driver converts this to a SQL <code>NCHAR</code> or
     * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
     * (depending on the argument's
     * size relative to the driver's limits on <code>NVARCHAR</code> values)
     * when it sends it to the database.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; if a database access error occurs; or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setNString(int parameterIndex, String value) throws SQLException;

    /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @param length the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; if a database access error occurs; or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException;

    /**
     * Sets the designated parameter to a <code>java.sql.NClob</code> object. The driver converts this to a
     * SQL <code>NCLOB</code> value when it sends it to the database.
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; if a database access error occurs; or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setNClob(int parameterIndex, NClob value) throws SQLException;

    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     *This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs; this method is called on
     * a closed <code>PreparedStatement</code> or if the length specified is less than zero.
     *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setClob(int parameterIndex, Reader reader, long length)
       throws SQLException;

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.  The inputstream must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     * @param parameterIndex index of the first parameter is 1,
     * the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @param length the number of bytes in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     * this method is called on a closed <code>PreparedStatement</code>;
     *  if the length specified
     * is less than zero or if the number of bytes in the inputstream does not match
     * the specified length.
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException;
    /**
     * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
     * of characters specified by length otherwise a <code>SQLException</code> will be
     * generated when the <code>PreparedStatement</code> is executed.
     * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the length specified is less than zero;
     * if the driver does not support national character sets;
     * if the driver can detect that a data conversion
     *  error could occur;  if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setNClob(int parameterIndex, Reader reader, long length)
       throws SQLException;

     /**
      * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object.
      * The driver converts this to an
      * SQL <code>XML</code> value when it sends it to the database.
      * <p>
      *
      * @param parameterIndex index of the first parameter is 1, the second is 2, ...
      * @param xmlObject a <code>SQLXML</code> object that maps an SQL <code>XML</code> value
      * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
      *  this method is called on a closed <code>PreparedStatement</code>
      * or the <code>java.xml.transform.Result</code>,
      *  <code>Writer</code> or <code>OutputStream</code> has not been closed for
      * the <code>SQLXML</code> object
      * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
      *
      * @since 1.6
      */
     void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException;

    /**
     * <p>Sets the value of the designated parameter with the given object.
     *
     * If the second argument is an <code>InputStream</code> then the stream must contain
     * the number of bytes specified by scaleOrLength.  If the second argument is a
     * <code>Reader</code> then the reader must contain the number of characters specified
     * by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the prepared statement is executed.
     *
     * <p>The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     *
     * If the object has a custom mapping (is of a class implementing the
     * interface <code>SQLData</code>),
     * the JDBC driver should call the method <code>SQLData.writeSQL</code> to
     * write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
     *  <code>Struct</code>, <code>java.net.URL</code>,
     * or <code>Array</code>, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     *
     * <p>Note that this method may be used to pass database-specific
     * abstract data types.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
     * sent to the database. The scale argument may further qualify this type.
     * @param scaleOrLength for <code>java.sql.Types.DECIMAL</code>
     *          or <code>java.sql.Types.NUMERIC types</code>,
     *          this is the number of digits after the decimal point. For
     *          Java Object types <code>InputStream</code> and <code>Reader</code>,
     *          this is the length
     *          of the data in the stream or reader.  For all other types,
     *          this value will be ignored.
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     * this method is called on a closed <code>PreparedStatement</code> or
     *            if the Java Object specified by x is an InputStream
     *            or Reader object and the value of the scale parameter is less
     *            than zero
     * @exception SQLFeatureNotSupportedException if
     * the JDBC driver does not support the specified targetSqlType
     * @see Types
     *
     */
    void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
            throws SQLException;
   /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
    */
    void setAsciiStream(int parameterIndex, java.io.InputStream x, long length)
            throws SQLException;
    /**
     * Sets the designated parameter to the given input stream, which will have
     * the specified number of bytes.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java input stream which contains the binary parameter value
     * @param length the number of bytes in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    void setBinaryStream(int parameterIndex, java.io.InputStream x,
                         long length) throws SQLException;
        /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader the <code>java.io.Reader</code> object that contains the
     *        Unicode data
     * @param length the number of characters in the stream
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @since 1.6
     */
    void setCharacterStream(int parameterIndex,
                          java.io.Reader reader,
                          long length) throws SQLException;
    //-----
    /**
     * Sets the designated parameter to the given input stream.
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code>. Data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from ASCII to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setAsciiStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the Java input stream that contains the ASCII parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
       * @since 1.6
    */
    void setAsciiStream(int parameterIndex, java.io.InputStream x)
            throws SQLException;
    /**
     * Sets the designated parameter to the given input stream.
     * When a very large binary value is input to a <code>LONGVARBINARY</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. The data will be read from the
     * stream as needed until end-of-file is reached.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBinaryStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the java input stream which contains the binary parameter value
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
    void setBinaryStream(int parameterIndex, java.io.InputStream x)
    throws SQLException;
        /**
     * Sets the designated parameter to the given <code>Reader</code>
     * object.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The data will be read from the stream
     * as needed until end-of-file is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setCharacterStream</code> which takes a length parameter.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param reader the <code>java.io.Reader</code> object that contains the
     *        Unicode data
     * @exception SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
    void setCharacterStream(int parameterIndex,
                          java.io.Reader reader) throws SQLException;
  /**
     * Sets the designated parameter to a <code>Reader</code> object. The
     * <code>Reader</code> reads the data till end-of-file is reached. The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.

     * <P><B>Note:</B> This stream object can either be a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNCharacterStream</code> which takes a length parameter.
     *
     * @param parameterIndex of the first parameter is 1, the second is 2, ...
     * @param value the parameter value
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; if a database access error occurs; or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setNCharacterStream(int parameterIndex, Reader value) throws SQLException;

    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setClob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs; this method is called on
     * a closed <code>PreparedStatement</code>or if parameterIndex does not correspond to a parameter
     * marker in the SQL statement
     *
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     * @since 1.6
     */
     void setClob(int parameterIndex, Reader reader)
       throws SQLException;

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.
     * This method differs from the <code>setBinaryStream (int, InputStream)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setBlob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1,
     * the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     * this method is called on a closed <code>PreparedStatement</code> or
     * if parameterIndex does not correspond
     * to a parameter marker in the SQL statement,
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException;
    /**
     * Sets the designated parameter to a <code>Reader</code> object.
     * This method differs from the <code>setCharacterStream (int, Reader)</code> method
     * because it informs the driver that the parameter value should be sent to
     * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
     * driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>setNClob</code> which takes a length parameter.
     *
     * @param parameterIndex index of the first parameter is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement;
     * if the driver does not support national character sets;
     * if the driver can detect that a data conversion
     *  error could occur;  if a database access error occurs or
     * this method is called on a closed <code>PreparedStatement</code>
     * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
     *
     * @since 1.6
     */
     void setNClob(int parameterIndex, Reader reader)
       throws SQLException;

    //------------------------- JDBC 4.2 -----------------------------------

    /**
     * <p>Sets the value of the designated parameter with the given object.
     *
     * If the second argument is an {@code InputStream} then the stream
     * must contain the number of bytes specified by scaleOrLength.
     * If the second argument is a {@code Reader} then the reader must
     * contain the number of characters specified by scaleOrLength. If these
     * conditions are not true the driver will generate a
     * {@code SQLException} when the prepared statement is executed.
     *
     * <p>The given Java object will be converted to the given targetSqlType
     * before being sent to the database.
     *
     * If the object has a custom mapping (is of a class implementing the
     * interface {@code SQLData}),
     * the JDBC driver should call the method {@code SQLData.writeSQL} to
     * write it to the SQL data stream.
     * If, on the other hand, the object is of a class implementing
     * {@code Ref}, {@code Blob}, {@code Clob},  {@code NClob},
     *  {@code Struct}, {@code java.net.URL},
     * or {@code Array}, the driver should pass it to the database as a
     * value of the corresponding SQL type.
     *
     * <p>Note that this method may be used to pass database-specific
     * abstract data types.
     *<P>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type to be sent to the database. The
     * scale argument may further qualify this type.
     * @param scaleOrLength for {@code java.sql.JDBCType.DECIMAL}
     *          or {@code java.sql.JDBCType.NUMERIC types},
     *          this is the number of digits after the decimal point. For
     *          Java Object types {@code InputStream} and {@code Reader},
     *          this is the length
     *          of the data in the stream or reader.  For all other types,
     *          this value will be ignored.
     * @exception SQLException if parameterIndex does not correspond to a
     * parameter marker in the SQL statement; if a database access error occurs
     * or this method is called on a closed {@code PreparedStatement}  or
     *            if the Java Object specified by x is an InputStream
     *            or Reader object and the value of the scale parameter is less
     *            than zero
     * @exception SQLFeatureNotSupportedException if
     * the JDBC driver does not support the specified targetSqlType
     * @see JDBCType
     * @see SQLType
     * @since 1.8
     */
    default void setObject(int parameterIndex, Object x, SQLType targetSqlType,
             int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException("setObject not implemented");
    }

    /**
     * Sets the value of the designated parameter with the given object.
     *
     * This method is similar to {@link #setObject(int parameterIndex,
     * Object x, SQLType targetSqlType, int scaleOrLength)},
     * except that it assumes a scale of zero.
     *<P>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the object containing the input parameter value
     * @param targetSqlType the SQL type to be sent to the database
     * @exception SQLException if parameterIndex does not correspond to a
     * parameter marker in the SQL statement; if a database access error occurs
     * or this method is called on a closed {@code PreparedStatement}
     * @exception SQLFeatureNotSupportedException if
     * the JDBC driver does not support the specified targetSqlType
     * @see JDBCType
     * @see SQLType
     * @since 1.8
     */
    default void setObject(int parameterIndex, Object x, SQLType targetSqlType)
      throws SQLException {
        throw new SQLFeatureNotSupportedException("setObject not implemented");
    }

    /**
     * Executes the SQL statement in this <code>PreparedStatement</code> object,
     * which must be an SQL Data Manipulation Language (DML) statement,
     * such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     * <p>
     * This method should be used when the returned row count may exceed
     * {@link Integer#MAX_VALUE}.
     * <p>
     * The default implementation will throw {@code UnsupportedOperationException}
     *
     * @return either (1) the row count for SQL Data Manipulation Language
     * (DML) statements or (2) 0 for SQL statements that return nothing
     * @exception SQLException if a database access error occurs;
     * this method is called on a closed  <code>PreparedStatement</code>
     * or the SQL statement returns a <code>ResultSet</code> object
     * @throws SQLTimeoutException when the driver has determined that the
     * timeout value that was specified by the {@code setQueryTimeout}
     * method has been exceeded and has at least attempted to cancel
     * the currently running {@code Statement}
     * @since 1.8
     */
    default long executeLargeUpdate() throws SQLException {
        throw new UnsupportedOperationException("executeLargeUpdate not implemented");
    }
}

```


# 04、ResultSet

```java

/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.sql;

import java.math.BigDecimal;
import java.util.Calendar;
import java.io.Reader;
import java.io.InputStream;

/**
 * A table of data representing a database result set, which
 * is usually generated by executing a statement that queries the database.
 *
 * <P>A <code>ResultSet</code> object  maintains a cursor pointing
 * to its current row of data.  Initially the cursor is positioned
 * before the first row. The <code>next</code> method moves the
 * cursor to the next row, and because it returns <code>false</code>
 * when there are no more rows in the <code>ResultSet</code> object,
 * it can be used in a <code>while</code> loop to iterate through
 * the result set.
 * <P>
 * A default <code>ResultSet</code> object is not updatable and
 * has a cursor that moves forward only.  Thus, you can
 * iterate through it only once and only from the first row to the
 * last row. It is possible to
 * produce <code>ResultSet</code> objects that are scrollable and/or
 * updatable.  The following code fragment, in which <code>con</code>
 * is a valid <code>Connection</code> object, illustrates how to make
 * a result set that is scrollable and insensitive to updates by others, and
 * that is updatable. See <code>ResultSet</code> fields for other
 * options.
 * <PRE>
 *
 *       Statement stmt = con.createStatement(
 *                                      ResultSet.TYPE_SCROLL_INSENSITIVE,
 *                                      ResultSet.CONCUR_UPDATABLE);
 *       ResultSet rs = stmt.executeQuery("SELECT a, b FROM TABLE2");
 *       // rs will be scrollable, will not show changes made by others,
 *       // and will be updatable
 *
 * </PRE>
 * The <code>ResultSet</code> interface provides
 * <i>getter</i> methods (<code>getBoolean</code>, <code>getLong</code>, and so on)
 * for retrieving column values from the current row.
 * Values can be retrieved using either the index number of the
 * column or the name of the column.  In general, using the
 * column index will be more efficient.  Columns are numbered from 1.
 * For maximum portability, result set columns within each row should be
 * read in left-to-right order, and each column should be read only once.
 *
 * <P>For the getter methods, a JDBC driver attempts
 * to convert the underlying data to the Java type specified in the
 * getter method and returns a suitable Java value.  The JDBC specification
 * has a table showing the allowable mappings from SQL types to Java types
 * that can be used by the <code>ResultSet</code> getter methods.
 *
 * <P>Column names used as input to getter methods are case
 * insensitive.  When a getter method is called  with
 * a column name and several columns have the same name,
 * the value of the first matching column will be returned.
 * The column name option is
 * designed to be used when column names are used in the SQL
 * query that generated the result set.
 * For columns that are NOT explicitly named in the query, it
 * is best to use column numbers. If column names are used, the
 * programmer should take care to guarantee that they uniquely refer to
 * the intended columns, which can be assured with the SQL <i>AS</i> clause.
 * <P>
 * A set of updater methods were added to this interface
 * in the JDBC 2.0 API (Java&trade; 2 SDK,
 * Standard Edition, version 1.2). The comments regarding parameters
 * to the getter methods also apply to parameters to the
 * updater methods.
 *<P>
 * The updater methods may be used in two ways:
 * <ol>
 * <LI>to update a column value in the current row.  In a scrollable
 *     <code>ResultSet</code> object, the cursor can be moved backwards
 *     and forwards, to an absolute position, or to a position
 *     relative to the current row.
 *     The following code fragment updates the <code>NAME</code> column
 *     in the fifth row of the <code>ResultSet</code> object
 *     <code>rs</code> and then uses the method <code>updateRow</code>
 *     to update the data source table from which <code>rs</code> was derived.
 * <PRE>
 *
 *       rs.absolute(5); // moves the cursor to the fifth row of rs
 *       rs.updateString("NAME", "AINSWORTH"); // updates the
 *          // <code>NAME</code> column of row 5 to be <code>AINSWORTH</code>
 *       rs.updateRow(); // updates the row in the data source
 *
 * </PRE>
 * <LI>to insert column values into the insert row.  An updatable
 *     <code>ResultSet</code> object has a special row associated with
 *     it that serves as a staging area for building a row to be inserted.
 *     The following code fragment moves the cursor to the insert row, builds
 *     a three-column row, and inserts it into <code>rs</code> and into
 *     the data source table using the method <code>insertRow</code>.
 * <PRE>
 *
 *       rs.moveToInsertRow(); // moves cursor to the insert row
 *       rs.updateString(1, "AINSWORTH"); // updates the
 *          // first column of the insert row to be <code>AINSWORTH</code>
 *       rs.updateInt(2,35); // updates the second column to be <code>35</code>
 *       rs.updateBoolean(3, true); // updates the third column to <code>true</code>
 *       rs.insertRow();
 *       rs.moveToCurrentRow();
 *
 * </PRE>
 * </ol>
 * <P>A <code>ResultSet</code> object is automatically closed when the
 * <code>Statement</code> object that
 * generated it is closed, re-executed, or used
 * to retrieve the next result from a sequence of multiple results.
 *
 * <P>The number, types and properties of a <code>ResultSet</code>
 * object's columns are provided by the <code>ResultSetMetaData</code>
 * object returned by the <code>ResultSet.getMetaData</code> method.
 *
 * @see Statement#executeQuery
 * @see Statement#getResultSet
 * @see ResultSetMetaData
 */

public interface ResultSet extends Wrapper, AutoCloseable {

    /**
     * Moves the cursor forward one row from its current position.
     * A <code>ResultSet</code> cursor is initially positioned
     * before the first row; the first call to the method
     * <code>next</code> makes the first row the current row; the
     * second call makes the second row the current row, and so on.
     * <p>
     * When a call to the <code>next</code> method returns <code>false</code>,
     * the cursor is positioned after the last row. Any
     * invocation of a <code>ResultSet</code> method which requires a
     * current row will result in a <code>SQLException</code> being thrown.
     *  If the result set type is <code>TYPE_FORWARD_ONLY</code>, it is vendor specified
     * whether their JDBC driver implementation will return <code>false</code> or
     *  throw an <code>SQLException</code> on a
     * subsequent call to <code>next</code>.
     *
     * <P>If an input stream is open for the current row, a call
     * to the method <code>next</code> will
     * implicitly close it. A <code>ResultSet</code> object's
     * warning chain is cleared when a new row is read.
     *
     * @return <code>true</code> if the new current row is valid;
     * <code>false</code> if there are no more rows
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     */
    boolean next() throws SQLException;


    /**
     * Releases this <code>ResultSet</code> object's database and
     * JDBC resources immediately instead of waiting for
     * this to happen when it is automatically closed.
     *
     * <P>The closing of a <code>ResultSet</code> object does <strong>not</strong> close the <code>Blob</code>,
     * <code>Clob</code> or <code>NClob</code> objects created by the <code>ResultSet</code>. <code>Blob</code>,
     * <code>Clob</code> or <code>NClob</code> objects remain valid for at least the duration of the
     * transaction in which they are created, unless their <code>free</code> method is invoked.
     *<p>
     * When a <code>ResultSet</code> is closed, any <code>ResultSetMetaData</code>
     * instances that were created by calling the  <code>getMetaData</code>
     * method remain accessible.
     *
     * <P><B>Note:</B> A <code>ResultSet</code> object
     * is automatically closed by the
     * <code>Statement</code> object that generated it when
     * that <code>Statement</code> object is closed,
     * re-executed, or is used to retrieve the next result from a
     * sequence of multiple results.
     *<p>
     * Calling the method <code>close</code> on a <code>ResultSet</code>
     * object that is already closed is a no-op.
     *
     *
     * @exception SQLException if a database access error occurs
     */
    void close() throws SQLException;

    /**
     * Reports whether
     * the last column read had a value of SQL <code>NULL</code>.
     * Note that you must first call one of the getter methods
     * on a column to try to read its value and then call
     * the method <code>wasNull</code> to see if the value read was
     * SQL <code>NULL</code>.
     *
     * @return <code>true</code> if the last column value read was SQL
     *         <code>NULL</code> and <code>false</code> otherwise
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     */
    boolean wasNull() throws SQLException;

    // Methods for accessing results by column index

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    String getString(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>boolean</code> in the Java programming language.
     *
     * <P>If the designated column has a datatype of CHAR or VARCHAR
     * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
     * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
     * of CHAR or VARCHAR
     * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
     * and contains  a 1, a value of <code>true</code> is returned.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>false</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    boolean getBoolean(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    byte getByte(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>short</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    short getShort(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>int</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    int getInt(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>long</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    long getLong(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>float</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    float getFloat(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>double</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    double getDouble(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.BigDecimal</code> in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param scale the number of digits to the right of the decimal point
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @deprecated Use {@code getBigDecimal(int columnIndex)}
     *             or {@code getBigDecimal(String columnLabel)}
     */
    @Deprecated
    BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> array in the Java programming language.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    byte[] getBytes(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Date</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.sql.Date getDate(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Time</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.sql.Time getTime(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Timestamp</code> object in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a stream of ASCII characters. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARCHAR</code> values.
     * The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a getter method implicitly closes the stream.  Also, a
     * stream may return <code>0</code> when the method
     * <code>InputStream.available</code>
     * is called whether there is data available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of one-byte ASCII characters;
     * if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.io.InputStream getAsciiStream(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * as a stream of two-byte 3 characters. The first byte is
     * the high byte; the second byte is the low byte.
     *
     * The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARCHAR</code>values.  The
     * JDBC driver will do any necessary conversion from the database
     * format into Unicode.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a getter method implicitly closes the stream.
     * Also, a stream may return <code>0</code> when the method
     * <code>InputStream.available</code>
     * is called, whether there is data available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     *         as a stream of two-byte Unicode characters;
     *         if the value is SQL <code>NULL</code>, the value returned is
     *         <code>null</code>
     *
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @deprecated use <code>getCharacterStream</code> in place of
     *              <code>getUnicodeStream</code>
     */
    @Deprecated
    java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a  stream of
     * uninterpreted bytes. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARBINARY</code> values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a getter method implicitly closes the stream.  Also, a
     * stream may return <code>0</code> when the method
     * <code>InputStream.available</code>
     * is called whether there is data available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     *         as a stream of uninterpreted bytes;
     *         if the value is SQL <code>NULL</code>, the value returned is
     *         <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.io.InputStream getBinaryStream(int columnIndex)
        throws SQLException;


    // Methods for accessing results by column label

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    String getString(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>boolean</code> in the Java programming language.
     *
     * <P>If the designated column has a datatype of CHAR or VARCHAR
     * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
     * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
     * of CHAR or VARCHAR
     * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
     * and contains  a 1, a value of <code>true</code> is returned.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>false</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    boolean getBoolean(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    byte getByte(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>short</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    short getShort(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>int</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    int getInt(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>long</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    long getLong(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>float</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    float getFloat(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>double</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>0</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    double getDouble(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.math.BigDecimal</code> in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param scale the number of digits to the right of the decimal point
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @deprecated Use {@code getBigDecimal(int columnIndex)}
     *             or {@code getBigDecimal(String columnLabel)}
     */
    @Deprecated
    BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>byte</code> array in the Java programming language.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    byte[] getBytes(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Date</code> object in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.sql.Date getDate(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Time</code> object in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.sql.Time getTime(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>java.sql.Timestamp</code> object in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.sql.Timestamp getTimestamp(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a stream of
     * ASCII characters. The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARCHAR</code> values.
     * The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a getter method implicitly closes the stream. Also, a
     * stream may return <code>0</code> when the method <code>available</code>
     * is called whether there is data available or not.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of one-byte ASCII characters.
     * If the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code>.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.io.InputStream getAsciiStream(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a stream of two-byte
     * Unicode characters. The first byte is the high byte; the second
     * byte is the low byte.
     *
     * The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARCHAR</code> values.
     * The JDBC technology-enabled driver will
     * do any necessary conversion from the database format into Unicode.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a getter method implicitly closes the stream.
     * Also, a stream may return <code>0</code> when the method
     * <code>InputStream.available</code> is called, whether there
     * is data available or not.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a Java input stream that delivers the database column value
     *         as a stream of two-byte Unicode characters.
     *         If the value is SQL <code>NULL</code>, the value returned
     *         is <code>null</code>.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @deprecated use <code>getCharacterStream</code> instead
     */
    @Deprecated
    java.io.InputStream getUnicodeStream(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a stream of uninterpreted
     * <code>byte</code>s.
     * The value can then be read in chunks from the
     * stream. This method is particularly
     * suitable for retrieving large <code>LONGVARBINARY</code>
     * values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a getter method implicitly closes the stream. Also, a
     * stream may return <code>0</code> when the method <code>available</code>
     * is called whether there is data available or not.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes;
     * if the value is SQL <code>NULL</code>, the result is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    java.io.InputStream getBinaryStream(String columnLabel)
        throws SQLException;


    // Advanced features:

    /**
     * Retrieves the first warning reported by calls on this
     * <code>ResultSet</code> object.
     * Subsequent warnings on this <code>ResultSet</code> object
     * will be chained to the <code>SQLWarning</code> object that
     * this method returns.
     *
     * <P>The warning chain is automatically cleared each time a new
     * row is read.  This method may not be called on a <code>ResultSet</code>
     * object that has been closed; doing so will cause an
     * <code>SQLException</code> to be thrown.
     * <P>
     * <B>Note:</B> This warning chain only covers warnings caused
     * by <code>ResultSet</code> methods.  Any warning caused by
     * <code>Statement</code> methods
     * (such as reading OUT parameters) will be chained on the
     * <code>Statement</code> object.
     *
     * @return the first <code>SQLWarning</code> object reported or
     *         <code>null</code> if there are none
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     */
    SQLWarning getWarnings() throws SQLException;

    /**
     * Clears all warnings reported on this <code>ResultSet</code> object.
     * After this method is called, the method <code>getWarnings</code>
     * returns <code>null</code> until a new warning is
     * reported for this <code>ResultSet</code> object.
     *
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     */
    void clearWarnings() throws SQLException;

    /**
     * Retrieves the name of the SQL cursor used by this <code>ResultSet</code>
     * object.
     *
     * <P>In SQL, a result table is retrieved through a cursor that is
     * named. The current row of a result set can be updated or deleted
     * using a positioned update/delete statement that references the
     * cursor name. To insure that the cursor has the proper isolation
     * level to support update, the cursor's <code>SELECT</code> statement
     * should be of the form <code>SELECT FOR UPDATE</code>. If
     * <code>FOR UPDATE</code> is omitted, the positioned updates may fail.
     *
     * <P>The JDBC API supports this SQL feature by providing the name of the
     * SQL cursor used by a <code>ResultSet</code> object.
     * The current row of a <code>ResultSet</code> object
     * is also the current row of this SQL cursor.
     *
     * @return the SQL name for this <code>ResultSet</code> object's cursor
     * @exception SQLException if a database access error occurs or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     */
    String getCursorName() throws SQLException;

    /**
     * Retrieves the  number, types and properties of
     * this <code>ResultSet</code> object's columns.
     *
     * @return the description of this <code>ResultSet</code> object's columns
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     */
    ResultSetMetaData getMetaData() throws SQLException;

    /**
     * <p>Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>Object</code> in the Java programming language.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java object type corresponding to the column's SQL type,
     * following the mapping for built-in types specified in the JDBC
     * specification. If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     *
     * <p>This method may also be used to read database-specific
     * abstract data types.
     *
     * In the JDBC 2.0 API, the behavior of method
     * <code>getObject</code> is extended to materialize
     * data of SQL user-defined types.
     * <p>
     * If <code>Connection.getTypeMap</code> does not throw a
     * <code>SQLFeatureNotSupportedException</code>,
     * then when a column contains a structured or distinct value,
     * the behavior of this method is as
     * if it were a call to: <code>getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap())</code>.
     *
     * If <code>Connection.getTypeMap</code> does throw a
     * <code>SQLFeatureNotSupportedException</code>,
     * then structured values are not supported, and distinct values
     * are mapped to the default Java class as determined by the
     * underlying SQL type of the DISTINCT type.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>java.lang.Object</code> holding the column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    Object getObject(int columnIndex) throws SQLException;

    /**
     * <p>Gets the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * an <code>Object</code> in the Java programming language.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java object type corresponding to the column's SQL type,
     * following the mapping for built-in types specified in the JDBC
     * specification. If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     * <P>
     * This method may also be used to read database-specific
     * abstract data types.
     * <P>
     * In the JDBC 2.0 API, the behavior of the method
     * <code>getObject</code> is extended to materialize
     * data of SQL user-defined types.  When a column contains
     * a structured or distinct value, the behavior of this method is as
     * if it were a call to: <code>getObject(columnIndex,
     * this.getStatement().getConnection().getTypeMap())</code>.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>java.lang.Object</code> holding the column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     */
    Object getObject(String columnLabel) throws SQLException;

    //----------------------------------------------------------------

    /**
     * Maps the given <code>ResultSet</code> column label to its
     * <code>ResultSet</code> column index.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column index of the given column name
     * @exception SQLException if the <code>ResultSet</code> object
     * does not contain a column labeled <code>columnLabel</code>, a database access error occurs
     *  or this method is called on a closed result set
     */
    int findColumn(String columnLabel) throws SQLException;


    //--------------------------JDBC 2.0-----------------------------------

    //---------------------------------------------------------------------
    // Getters and Setters
    //---------------------------------------------------------------------

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    java.io.Reader getCharacterStream(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    java.io.Reader getCharacterStream(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.math.BigDecimal</code> with full precision.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value (full precision);
     * if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     */
    BigDecimal getBigDecimal(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.math.BigDecimal</code> with full precision.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value (full precision);
     * if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs or this method is
     *            called on a closed result set
     * @since 1.2
     *
     */
    BigDecimal getBigDecimal(String columnLabel) throws SQLException;

    //---------------------------------------------------------------------
    // Traversal/Positioning
    //---------------------------------------------------------------------

    /**
     * Retrieves whether the cursor is before the first row in
     * this <code>ResultSet</code> object.
     * <p>
     * <strong>Note:</strong>Support for the <code>isBeforeFirst</code> method
     * is optional for <code>ResultSet</code>s with a result
     * set type of <code>TYPE_FORWARD_ONLY</code>
     *
     * @return <code>true</code> if the cursor is before the first row;
     * <code>false</code> if the cursor is at any other position or the
     * result set contains no rows
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean isBeforeFirst() throws SQLException;

    /**
     * Retrieves whether the cursor is after the last row in
     * this <code>ResultSet</code> object.
     * <p>
     * <strong>Note:</strong>Support for the <code>isAfterLast</code> method
     * is optional for <code>ResultSet</code>s with a result
     * set type of <code>TYPE_FORWARD_ONLY</code>
     *
     * @return <code>true</code> if the cursor is after the last row;
     * <code>false</code> if the cursor is at any other position or the
     * result set contains no rows
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean isAfterLast() throws SQLException;

    /**
     * Retrieves whether the cursor is on the first row of
     * this <code>ResultSet</code> object.
     * <p>
     * <strong>Note:</strong>Support for the <code>isFirst</code> method
     * is optional for <code>ResultSet</code>s with a result
     * set type of <code>TYPE_FORWARD_ONLY</code>
     *
     * @return <code>true</code> if the cursor is on the first row;
     * <code>false</code> otherwise
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean isFirst() throws SQLException;

    /**
     * Retrieves whether the cursor is on the last row of
     * this <code>ResultSet</code> object.
     *  <strong>Note:</strong> Calling the method <code>isLast</code> may be expensive
     * because the JDBC driver
     * might need to fetch ahead one row in order to determine
     * whether the current row is the last row in the result set.
     * <p>
     * <strong>Note:</strong> Support for the <code>isLast</code> method
     * is optional for <code>ResultSet</code>s with a result
     * set type of <code>TYPE_FORWARD_ONLY</code>
     * @return <code>true</code> if the cursor is on the last row;
     * <code>false</code> otherwise
     * @exception SQLException if a database access error occurs or this method is
     *            called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean isLast() throws SQLException;

    /**
     * Moves the cursor to the front of
     * this <code>ResultSet</code> object, just before the
     * first row. This method has no effect if the result set contains no rows.
     *
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set or the
     * result set type is <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void beforeFirst() throws SQLException;

    /**
     * Moves the cursor to the end of
     * this <code>ResultSet</code> object, just after the
     * last row. This method has no effect if the result set contains no rows.
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set
     * or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void afterLast() throws SQLException;

    /**
     * Moves the cursor to the first row in
     * this <code>ResultSet</code> object.
     *
     * @return <code>true</code> if the cursor is on a valid row;
     * <code>false</code> if there are no rows in the result set
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set
     * or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean first() throws SQLException;

    /**
     * Moves the cursor to the last row in
     * this <code>ResultSet</code> object.
     *
     * @return <code>true</code> if the cursor is on a valid row;
     * <code>false</code> if there are no rows in the result set
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set
     * or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean last() throws SQLException;

    /**
     * Retrieves the current row number.  The first row is number 1, the
     * second number 2, and so on.
     * <p>
     * <strong>Note:</strong>Support for the <code>getRow</code> method
     * is optional for <code>ResultSet</code>s with a result
     * set type of <code>TYPE_FORWARD_ONLY</code>
     *
     * @return the current row number; <code>0</code> if there is no current row
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    int getRow() throws SQLException;

    /**
     * Moves the cursor to the given row number in
     * this <code>ResultSet</code> object.
     *
     * <p>If the row number is positive, the cursor moves to
     * the given row number with respect to the
     * beginning of the result set.  The first row is row 1, the second
     * is row 2, and so on.
     *
     * <p>If the given row number is negative, the cursor moves to
     * an absolute row position with respect to
     * the end of the result set.  For example, calling the method
     * <code>absolute(-1)</code> positions the
     * cursor on the last row; calling the method <code>absolute(-2)</code>
     * moves the cursor to the next-to-last row, and so on.
     *
     * <p>If the row number specified is zero, the cursor is moved to
     * before the first row.
     *
     * <p>An attempt to position the cursor beyond the first/last row in
     * the result set leaves the cursor before the first row or after
     * the last row.
     *
     * <p><B>Note:</B> Calling <code>absolute(1)</code> is the same
     * as calling <code>first()</code>. Calling <code>absolute(-1)</code>
     * is the same as calling <code>last()</code>.
     *
     * @param row the number of the row to which the cursor should move.
     *        A value of zero indicates that the cursor will be positioned
     *        before the first row; a positive number indicates the row number
     *        counting from the beginning of the result set; a negative number
     *        indicates the row number counting from the end of the result set
     * @return <code>true</code> if the cursor is moved to a position in this
     * <code>ResultSet</code> object;
     * <code>false</code> if the cursor is before the first row or after the
     * last row
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set
     * or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean absolute( int row ) throws SQLException;

    /**
     * Moves the cursor a relative number of rows, either positive or negative.
     * Attempting to move beyond the first/last row in the
     * result set positions the cursor before/after the
     * the first/last row. Calling <code>relative(0)</code> is valid, but does
     * not change the cursor position.
     *
     * <p>Note: Calling the method <code>relative(1)</code>
     * is identical to calling the method <code>next()</code> and
     * calling the method <code>relative(-1)</code> is identical
     * to calling the method <code>previous()</code>.
     *
     * @param rows an <code>int</code> specifying the number of rows to
     *        move from the current row; a positive number moves the cursor
     *        forward; a negative number moves the cursor backward
     * @return <code>true</code> if the cursor is on a row;
     *         <code>false</code> otherwise
     * @exception SQLException if a database access error occurs;  this method
     * is called on a closed result set or the result set type is
     *            <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean relative( int rows ) throws SQLException;

    /**
     * Moves the cursor to the previous row in this
     * <code>ResultSet</code> object.
     *<p>
     * When a call to the <code>previous</code> method returns <code>false</code>,
     * the cursor is positioned before the first row.  Any invocation of a
     * <code>ResultSet</code> method which requires a current row will result in a
     * <code>SQLException</code> being thrown.
     *<p>
     * If an input stream is open for the current row, a call to the method
     * <code>previous</code> will implicitly close it.  A <code>ResultSet</code>
     *  object's warning change is cleared when a new row is read.
     *<p>
     *
     * @return <code>true</code> if the cursor is now positioned on a valid row;
     * <code>false</code> if the cursor is positioned before the first row
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set
     * or the result set type is <code>TYPE_FORWARD_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    boolean previous() throws SQLException;

    //---------------------------------------------------------------------
    // Properties
    //---------------------------------------------------------------------

    /**
     * The constant indicating that the rows in a result set will be
     * processed in a forward direction; first-to-last.
     * This constant is used by the method <code>setFetchDirection</code>
     * as a hint to the driver, which the driver may ignore.
     * @since 1.2
     */
    int FETCH_FORWARD = 1000;

    /**
     * The constant indicating that the rows in a result set will be
     * processed in a reverse direction; last-to-first.
     * This constant is used by the method <code>setFetchDirection</code>
     * as a hint to the driver, which the driver may ignore.
     * @since 1.2
     */
    int FETCH_REVERSE = 1001;

    /**
     * The constant indicating that the order in which rows in a
     * result set will be processed is unknown.
     * This constant is used by the method <code>setFetchDirection</code>
     * as a hint to the driver, which the driver may ignore.
     */
    int FETCH_UNKNOWN = 1002;

    /**
     * Gives a hint as to the direction in which the rows in this
     * <code>ResultSet</code> object will be processed.
     * The initial value is determined by the
     * <code>Statement</code> object
     * that produced this <code>ResultSet</code> object.
     * The fetch direction may be changed at any time.
     *
     * @param direction an <code>int</code> specifying the suggested
     *        fetch direction; one of <code>ResultSet.FETCH_FORWARD</code>,
     *        <code>ResultSet.FETCH_REVERSE</code>, or
     *        <code>ResultSet.FETCH_UNKNOWN</code>
     * @exception SQLException if a database access error occurs; this
     * method is called on a closed result set or
     * the result set type is <code>TYPE_FORWARD_ONLY</code> and the fetch
     * direction is not <code>FETCH_FORWARD</code>
     * @since 1.2
     * @see Statement#setFetchDirection
     * @see #getFetchDirection
     */
    void setFetchDirection(int direction) throws SQLException;

    /**
     * Retrieves the fetch direction for this
     * <code>ResultSet</code> object.
     *
     * @return the current fetch direction for this <code>ResultSet</code> object
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     * @see #setFetchDirection
     */
    int getFetchDirection() throws SQLException;

    /**
     * Gives the JDBC driver a hint as to the number of rows that should
     * be fetched from the database when more rows are needed for this
     * <code>ResultSet</code> object.
     * If the fetch size specified is zero, the JDBC driver
     * ignores the value and is free to make its own best guess as to what
     * the fetch size should be.  The default value is set by the
     * <code>Statement</code> object
     * that created the result set.  The fetch size may be changed at any time.
     *
     * @param rows the number of rows to fetch
     * @exception SQLException if a database access error occurs; this method
     * is called on a closed result set or the
     * condition {@code rows >= 0} is not satisfied
     * @since 1.2
     * @see #getFetchSize
     */
    void setFetchSize(int rows) throws SQLException;

    /**
     * Retrieves the fetch size for this
     * <code>ResultSet</code> object.
     *
     * @return the current fetch size for this <code>ResultSet</code> object
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     * @see #setFetchSize
     */
    int getFetchSize() throws SQLException;

    /**
     * The constant indicating the type for a <code>ResultSet</code> object
     * whose cursor may move only forward.
     * @since 1.2
     */
    int TYPE_FORWARD_ONLY = 1003;

    /**
     * The constant indicating the type for a <code>ResultSet</code> object
     * that is scrollable but generally not sensitive to changes to the data
     * that underlies the <code>ResultSet</code>.
     * @since 1.2
     */
    int TYPE_SCROLL_INSENSITIVE = 1004;

    /**
     * The constant indicating the type for a <code>ResultSet</code> object
     * that is scrollable and generally sensitive to changes to the data
     * that underlies the <code>ResultSet</code>.
     * @since 1.2
     */
    int TYPE_SCROLL_SENSITIVE = 1005;

    /**
     * Retrieves the type of this <code>ResultSet</code> object.
     * The type is determined by the <code>Statement</code> object
     * that created the result set.
     *
     * @return <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>,
     *         or <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    int getType() throws SQLException;

    /**
     * The constant indicating the concurrency mode for a
     * <code>ResultSet</code> object that may NOT be updated.
     * @since 1.2
     */
    int CONCUR_READ_ONLY = 1007;

    /**
     * The constant indicating the concurrency mode for a
     * <code>ResultSet</code> object that may be updated.
     * @since 1.2
     */
    int CONCUR_UPDATABLE = 1008;

    /**
     * Retrieves the concurrency mode of this <code>ResultSet</code> object.
     * The concurrency used is determined by the
     * <code>Statement</code> object that created the result set.
     *
     * @return the concurrency type, either
     *         <code>ResultSet.CONCUR_READ_ONLY</code>
     *         or <code>ResultSet.CONCUR_UPDATABLE</code>
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    int getConcurrency() throws SQLException;

    //---------------------------------------------------------------------
    // Updates
    //---------------------------------------------------------------------

    /**
     * Retrieves whether the current row has been updated.  The value returned
     * depends on whether or not the result set can detect updates.
     * <p>
     * <strong>Note:</strong> Support for the <code>rowUpdated</code> method is optional with a result set
     * concurrency of <code>CONCUR_READ_ONLY</code>
     * @return <code>true</code> if the current row is detected to
     * have been visibly updated by the owner or another; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @see DatabaseMetaData#updatesAreDetected
     * @since 1.2
     */
    boolean rowUpdated() throws SQLException;

    /**
     * Retrieves whether the current row has had an insertion.
     * The value returned depends on whether or not this
     * <code>ResultSet</code> object can detect visible inserts.
     * <p>
     * <strong>Note:</strong> Support for the <code>rowInserted</code> method is optional with a result set
     * concurrency of <code>CONCUR_READ_ONLY</code>
     * @return <code>true</code> if the current row is detected to
     * have been inserted; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @see DatabaseMetaData#insertsAreDetected
     * @since 1.2
     */
    boolean rowInserted() throws SQLException;

    /**
     * Retrieves whether a row has been deleted.  A deleted row may leave
     * a visible "hole" in a result set.  This method can be used to
     * detect holes in a result set.  The value returned depends on whether
     * or not this <code>ResultSet</code> object can detect deletions.
     * <p>
     * <strong>Note:</strong> Support for the <code>rowDeleted</code> method is optional with a result set
     * concurrency of <code>CONCUR_READ_ONLY</code>
     * @return <code>true</code> if the current row is detected to
     * have been deleted by the owner or another; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @see DatabaseMetaData#deletesAreDetected
     * @since 1.2
     */
    boolean rowDeleted() throws SQLException;

    /**
     * Updates the designated column with a <code>null</code> value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code>
     * or <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateNull(int columnIndex) throws SQLException;

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBoolean(int columnIndex, boolean x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateByte(int columnIndex, byte x) throws SQLException;

    /**
     * Updates the designated column with a <code>short</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateShort(int columnIndex, short x) throws SQLException;

    /**
     * Updates the designated column with an <code>int</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateInt(int columnIndex, int x) throws SQLException;

    /**
     * Updates the designated column with a <code>long</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateLong(int columnIndex, long x) throws SQLException;

    /**
     * Updates the designated column with a <code>float</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateFloat(int columnIndex, float x) throws SQLException;

    /**
     * Updates the designated column with a <code>double</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateDouble(int columnIndex, double x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.math.BigDecimal</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateString(int columnIndex, String x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> array value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBytes(int columnIndex, byte x[]) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateDate(int columnIndex, java.sql.Date x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateTime(int columnIndex, java.sql.Time x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateTimestamp(int columnIndex, java.sql.Timestamp x)
      throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateAsciiStream(int columnIndex,
                           java.io.InputStream x,
                           int length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBinaryStream(int columnIndex,
                            java.io.InputStream x,
                            int length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateCharacterStream(int columnIndex,
                             java.io.Reader x,
                             int length) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *<p>
     * If the second argument is an <code>InputStream</code> then the stream must contain
     * the number of bytes specified by scaleOrLength.  If the second argument is a
     * <code>Reader</code> then the reader must contain the number of characters specified
     * by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the statement is executed.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> ,
     *          this is the number of digits after the decimal point. For
     *          Java Object types <code>InputStream</code> and <code>Reader</code>,
     *          this is the length
     *          of the data in the stream or reader.  For all other types,
     *          this value will be ignored.
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateObject(int columnIndex, Object x, int scaleOrLength)
      throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateObject(int columnIndex, Object x) throws SQLException;

    /**
     * Updates the designated column with a <code>null</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateNull(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>boolean</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBoolean(String columnLabel, boolean x) throws SQLException;

    /**
     * Updates the designated column with a <code>byte</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateByte(String columnLabel, byte x) throws SQLException;

    /**
     * Updates the designated column with a <code>short</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateShort(String columnLabel, short x) throws SQLException;

    /**
     * Updates the designated column with an <code>int</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateInt(String columnLabel, int x) throws SQLException;

    /**
     * Updates the designated column with a <code>long</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateLong(String columnLabel, long x) throws SQLException;

    /**
     * Updates the designated column with a <code>float </code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateFloat(String columnLabel, float x) throws SQLException;

    /**
     * Updates the designated column with a <code>double</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateDouble(String columnLabel, double x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.BigDecimal</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateString(String columnLabel, String x) throws SQLException;

    /**
     * Updates the designated column with a byte array value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code>
     * or <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBytes(String columnLabel, byte x[]) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Date</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateDate(String columnLabel, java.sql.Date x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Time</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateTime(String columnLabel, java.sql.Time x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Timestamp</code>
     * value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateTimestamp(String columnLabel, java.sql.Timestamp x)
      throws SQLException;

    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateAsciiStream(String columnLabel,
                           java.io.InputStream x,
                           int length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateBinaryStream(String columnLabel,
                            java.io.InputStream x,
                            int length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing
     *        the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateCharacterStream(String columnLabel,
                             java.io.Reader reader,
                             int length) throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *<p>
     * If the second argument is an <code>InputStream</code> then the stream must contain
     * the number of bytes specified by scaleOrLength.  If the second argument is a
     * <code>Reader</code> then the reader must contain the number of characters specified
     * by scaleOrLength. If these conditions are not true the driver will generate a
     * <code>SQLException</code> when the statement is executed.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @param scaleOrLength for an object of <code>java.math.BigDecimal</code> ,
     *          this is the number of digits after the decimal point. For
     *          Java Object types <code>InputStream</code> and <code>Reader</code>,
     *          this is the length
     *          of the data in the stream or reader.  For all other types,
     *          this value will be ignored.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateObject(String columnLabel, Object x, int scaleOrLength)
      throws SQLException;

    /**
     * Updates the designated column with an <code>Object</code> value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateObject(String columnLabel, Object x) throws SQLException;

    /**
     * Inserts the contents of the insert row into this
     * <code>ResultSet</code> object and into the database.
     * The cursor must be on the insert row when this method is called.
     *
     * @exception SQLException if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>,
     * this method is called on a closed result set,
     * if this method is called when the cursor is not on the insert row,
     * or if not all of non-nullable columns in
     * the insert row have been given a non-null value
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void insertRow() throws SQLException;

    /**
     * Updates the underlying database with the new contents of the
     * current row of this <code>ResultSet</code> object.
     * This method cannot be called when the cursor is on the insert row.
     *
     * @exception SQLException if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>;
     *  this method is called on a closed result set or
     * if this method is called when the cursor is on the insert row
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void updateRow() throws SQLException;

    /**
     * Deletes the current row from this <code>ResultSet</code> object
     * and from the underlying database.  This method cannot be called when
     * the cursor is on the insert row.
     *
     * @exception SQLException if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>;
     * this method is called on a closed result set
     * or if this method is called when the cursor is on the insert row
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void deleteRow() throws SQLException;

    /**
     * Refreshes the current row with its most recent value in
     * the database.  This method cannot be called when
     * the cursor is on the insert row.
     *
     * <P>The <code>refreshRow</code> method provides a way for an
     * application to
     * explicitly tell the JDBC driver to refetch a row(s) from the
     * database.  An application may want to call <code>refreshRow</code> when
     * caching or prefetching is being done by the JDBC driver to
     * fetch the latest value of a row from the database.  The JDBC driver
     * may actually refresh multiple rows at once if the fetch size is
     * greater than one.
     *
     * <P> All values are refetched subject to the transaction isolation
     * level and cursor sensitivity.  If <code>refreshRow</code> is called after
     * calling an updater method, but before calling
     * the method <code>updateRow</code>, then the
     * updates made to the row are lost.  Calling the method
     * <code>refreshRow</code> frequently will likely slow performance.
     *
     * @exception SQLException if a database access error
     * occurs; this method is called on a closed result set;
     * the result set type is <code>TYPE_FORWARD_ONLY</code> or if this
     * method is called when the cursor is on the insert row
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method or this method is not supported for the specified result
     * set type and result set concurrency.
     * @since 1.2
     */
    void refreshRow() throws SQLException;

    /**
     * Cancels the updates made to the current row in this
     * <code>ResultSet</code> object.
     * This method may be called after calling an
     * updater method(s) and before calling
     * the method <code>updateRow</code> to roll back
     * the updates made to a row.  If no updates have been made or
     * <code>updateRow</code> has already been called, this method has no
     * effect.
     *
     * @exception SQLException if a database access error
     *            occurs; this method is called on a closed result set;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or if this method is called when the cursor is
     *            on the insert row
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void cancelRowUpdates() throws SQLException;

    /**
     * Moves the cursor to the insert row.  The current cursor position is
     * remembered while the cursor is positioned on the insert row.
     *
     * The insert row is a special row associated with an updatable
     * result set.  It is essentially a buffer where a new row may
     * be constructed by calling the updater methods prior to
     * inserting the row into the result set.
     *
     * Only the updater, getter,
     * and <code>insertRow</code> methods may be
     * called when the cursor is on the insert row.  All of the columns in
     * a result set must be given a value each time this method is
     * called before calling <code>insertRow</code>.
     * An updater method must be called before a
     * getter method can be called on a column value.
     *
     * @exception SQLException if a database access error occurs; this
     * method is called on a closed result set
     * or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void moveToInsertRow() throws SQLException;

    /**
     * Moves the cursor to the remembered cursor position, usually the
     * current row.  This method has no effect if the cursor is not on
     * the insert row.
     *
     * @exception SQLException if a database access error occurs; this
     * method is called on a closed result set
     *  or the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    void moveToCurrentRow() throws SQLException;

    /**
     * Retrieves the <code>Statement</code> object that produced this
     * <code>ResultSet</code> object.
     * If the result set was generated some other way, such as by a
     * <code>DatabaseMetaData</code> method, this method  may return
     * <code>null</code>.
     *
     * @return the <code>Statement</code> object that produced
     * this <code>ResultSet</code> object or <code>null</code>
     * if the result set was produced some other way
     * @exception SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    Statement getStatement() throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Object</code>
     * in the Java programming language.
     * If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     * This method uses the given <code>Map</code> object
     * for the custom mapping of the
     * SQL structured or distinct type that is being retrieved.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param map a <code>java.util.Map</code> object that contains the mapping
     * from SQL type names to classes in the Java programming language
     * @return an <code>Object</code> in the Java programming language
     * representing the SQL value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Object getObject(int columnIndex, java.util.Map<String,Class<?>> map)
        throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Ref</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Ref</code> object representing an SQL <code>REF</code>
     *         value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Ref getRef(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Blob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Blob</code> object representing the SQL
     *         <code>BLOB</code> value in the specified column
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Blob getBlob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Clob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>Clob</code> object representing the SQL
     *         <code>CLOB</code> value in the specified column
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Clob getClob(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Array</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return an <code>Array</code> object representing the SQL
     *         <code>ARRAY</code> value in the specified column
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Array getArray(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Object</code>
     * in the Java programming language.
     * If the value is an SQL <code>NULL</code>,
     * the driver returns a Java <code>null</code>.
     * This method uses the specified <code>Map</code> object for
     * custom mapping if appropriate.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param map a <code>java.util.Map</code> object that contains the mapping
     * from SQL type names to classes in the Java programming language
     * @return an <code>Object</code> representing the SQL value in the
     *         specified column
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Object getObject(String columnLabel, java.util.Map<String,Class<?>> map)
      throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Ref</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>Ref</code> object representing the SQL <code>REF</code>
     *         value in the specified column
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Ref getRef(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Blob</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>Blob</code> object representing the SQL <code>BLOB</code>
     *         value in the specified column
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Blob getBlob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>Clob</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>Clob</code> object representing the SQL <code>CLOB</code>
     * value in the specified column
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Clob getClob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as an <code>Array</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return an <code>Array</code> object representing the SQL <code>ARRAY</code> value in
     *         the specified column
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.2
     */
    Array getArray(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the date if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Date</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the date if the underlying database does not store
     * timezone information.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the date
     * @return the column value as a <code>java.sql.Date</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the time if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    java.sql.Time getTime(int columnIndex, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Time</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the time if the underlying database does not store
     * timezone information.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the time
     * @return the column value as a <code>java.sql.Time</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    java.sql.Time getTime(String columnLabel, Calendar cal) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the timestamp if the underlying database does not store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the timestamp
     * @return the column value as a <code>java.sql.Timestamp</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal)
      throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.sql.Timestamp</code> object
     * in the Java programming language.
     * This method uses the given calendar to construct an appropriate millisecond
     * value for the timestamp if the underlying database does not store
     * timezone information.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param cal the <code>java.util.Calendar</code> object
     * to use in constructing the date
     * @return the column value as a <code>java.sql.Timestamp</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid or
     * if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.2
     */
    java.sql.Timestamp getTimestamp(String columnLabel, Calendar cal)
      throws SQLException;

    //-------------------------- JDBC 3.0 ----------------------------------------

    /**
     * The constant indicating that open <code>ResultSet</code> objects with this
     * holdability will remain open when the current transaction is committed.
     *
     * @since 1.4
     */
    int HOLD_CURSORS_OVER_COMMIT = 1;

    /**
     * The constant indicating that open <code>ResultSet</code> objects with this
     * holdability will be closed when the current transaction is committed.
     *
     * @since 1.4
     */
    int CLOSE_CURSORS_AT_COMMIT = 2;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.net.URL</code>
     * object in the Java programming language.
     *
     * @param columnIndex the index of the column 1 is the first, 2 is the second,...
     * @return the column value as a <code>java.net.URL</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs; this method
     * is called on a closed result set or if a URL is malformed
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    java.net.URL getURL(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>java.net.URL</code>
     * object in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value as a <code>java.net.URL</code> object;
     * if the value is SQL <code>NULL</code>,
     * the value returned is <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs; this method
     * is called on a closed result set or if a URL is malformed
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    java.net.URL getURL(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Ref</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateRef(int columnIndex, java.sql.Ref x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Ref</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateRef(String columnLabel, java.sql.Ref x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Blob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateBlob(String columnLabel, java.sql.Blob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateClob(int columnIndex, java.sql.Clob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Clob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateClob(String columnLabel, java.sql.Clob x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Array</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateArray(int columnIndex, java.sql.Array x) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.Array</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.4
     */
    void updateArray(String columnLabel, java.sql.Array x) throws SQLException;

    //------------------------- JDBC 4.0 -----------------------------------

    /**
     * Retrieves the value of the designated column in the current row of this
     * <code>ResultSet</code> object as a <code>java.sql.RowId</code> object in the Java
     * programming language.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @return the column value; if the value is a SQL <code>NULL</code> the
     *     value returned is <code>null</code>
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    RowId getRowId(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row of this
     * <code>ResultSet</code> object as a <code>java.sql.RowId</code> object in the Java
     * programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value ; if the value is a SQL <code>NULL</code> the
     *     value returned is <code>null</code>
     * @throws SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    RowId getRowId(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a <code>RowId</code> value. The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param x the column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateRowId(int columnIndex, RowId x) throws SQLException;

    /**
     * Updates the designated column with a <code>RowId</code> value. The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateRowId(String columnLabel, RowId x) throws SQLException;

    /**
     * Retrieves the holdability of this <code>ResultSet</code> object
     * @return  either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed result set
     * @since 1.6
     */
    int getHoldability() throws SQLException;

    /**
     * Retrieves whether this <code>ResultSet</code> object has been closed. A <code>ResultSet</code> is closed if the
     * method close has been called on it, or if it is automatically closed.
     *
     * @return true if this <code>ResultSet</code> object is closed; false if it is still open
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    boolean isClosed() throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * It is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param nString the value for the column to be updated
     * @throws SQLException if the columnIndex is not valid;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNString(int columnIndex, String nString) throws SQLException;

    /**
     * Updates the designated column with a <code>String</code> value.
     * It is intended for use when updating <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param nString the value for the column to be updated
     * @throws SQLException if the columnLabel is not valid;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set;
     * the result set concurrency is <CODE>CONCUR_READ_ONLY</code>
     *  or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNString(String columnLabel, String nString) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.NClob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param nClob the value for the column to be updated
     * @throws SQLException if the columnIndex is not valid;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set;
     * if a database access error occurs or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNClob(int columnIndex, NClob nClob) throws SQLException;

    /**
     * Updates the designated column with a <code>java.sql.NClob</code> value.
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param nClob the value for the column to be updated
     * @throws SQLException if the columnLabel is not valid;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set;
     *  if a database access error occurs or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNClob(String columnLabel, NClob nClob) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>NClob</code> object
     * in the Java programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>NClob</code> object representing the SQL
     *         <code>NCLOB</code> value in the specified column
     * @exception SQLException if the columnIndex is not valid;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set
     * or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    NClob getNClob(int columnIndex) throws SQLException;

  /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a <code>NClob</code> object
     * in the Java programming language.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>NClob</code> object representing the SQL <code>NCLOB</code>
     * value in the specified column
     * @exception SQLException if the columnLabel is not valid;
   * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set
     * or if a database access error occurs
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    NClob getNClob(String columnLabel) throws SQLException;

    /**
     * Retrieves the value of the designated column in  the current row of
     *  this <code>ResultSet</code> as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    SQLXML getSQLXML(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in  the current row of
     *  this <code>ResultSet</code> as a
     * <code>java.sql.SQLXML</code> object in the Java programming language.
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
     * @throws SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    SQLXML getSQLXML(String columnLabel) throws SQLException;
    /**
     * Updates the designated column with a <code>java.sql.SQLXML</code> value.
     * The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * <p>
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param xmlObject the value for the column to be updated
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs; this method
     *  is called on a closed result set;
     * the <code>java.xml.transform.Result</code>,
     *  <code>Writer</code> or <code>OutputStream</code> has not been closed
     * for the <code>SQLXML</code> object;
     *  if there is an error processing the XML value or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>.  The <code>getCause</code> method
     *  of the exception may provide a more detailed exception, for example, if the
     *  stream does not contain valid XML.
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException;
    /**
     * Updates the designated column with a <code>java.sql.SQLXML</code> value.
     * The updater
     * methods are used to update column values in the current row or the insert
     * row. The updater methods do not update the underlying database; instead
     * the <code>updateRow</code> or <code>insertRow</code> methods are called
     * to update the database.
     * <p>
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param xmlObject the column value
     * @throws SQLException if the columnLabel is not valid;
     * if a database access error occurs; this method
     *  is called on a closed result set;
     * the <code>java.xml.transform.Result</code>,
     *  <code>Writer</code> or <code>OutputStream</code> has not been closed
     * for the <code>SQLXML</code> object;
     *  if there is an error processing the XML value or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>.  The <code>getCause</code> method
     *  of the exception may provide a more detailed exception, for example, if the
     *  stream does not contain valid XML.
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    String getNString(int columnIndex) throws SQLException;


    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as
     * a <code>String</code> in the Java programming language.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return the column value; if the value is SQL <code>NULL</code>, the
     * value returned is <code>null</code>
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    String getNString(String columnLabel) throws SQLException;


    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language.
     * @param columnIndex the first column is 1, the second is 2, ...
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    java.io.Reader getNCharacterStream(int columnIndex) throws SQLException;

    /**
     * Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object as a
     * <code>java.io.Reader</code> object.
     * It is intended for use when
     * accessing  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @return a <code>java.io.Reader</code> object that contains the column
     * value; if the value is SQL <code>NULL</code>, the value returned is
     * <code>null</code> in the Java programming language
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    java.io.Reader getNCharacterStream(String columnLabel) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.   The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * It is intended for use when
     * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNCharacterStream(int columnIndex,
                             java.io.Reader x,
                             long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.  The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * It is intended for use when
     * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing
     *        the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNCharacterStream(String columnLabel,
                             java.io.Reader reader,
                             long length) throws SQLException;
    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateAsciiStream(int columnIndex,
                           java.io.InputStream x,
                           long length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBinaryStream(int columnIndex,
                            java.io.InputStream x,
                            long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateCharacterStream(int columnIndex,
                             java.io.Reader x,
                             long length) throws SQLException;
    /**
     * Updates the designated column with an ascii stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateAsciiStream(String columnLabel,
                           java.io.InputStream x,
                           long length) throws SQLException;

    /**
     * Updates the designated column with a binary stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBinaryStream(String columnLabel,
                            java.io.InputStream x,
                            long length) throws SQLException;

    /**
     * Updates the designated column with a character stream value, which will have
     * the specified number of bytes.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing
     *        the new column value
     * @param length the length of the stream
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateCharacterStream(String columnLabel,
                             java.io.Reader reader,
                             long length) throws SQLException;
    /**
     * Updates the designated column using the given input stream, which
     * will have the specified number of bytes.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @param length the number of bytes in the parameter data.
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException;

    /**
     * Updates the designated column using the given input stream, which
     * will have the specified number of bytes.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @param length the number of bytes in the parameter data.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateClob(int columnIndex,  Reader reader, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateClob(String columnLabel,  Reader reader, long length) throws SQLException;
   /**
     * Updates the designated column using the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if the columnIndex is not valid;
    * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set,
     * if a database access error occurs or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNClob(int columnIndex,  Reader reader, long length) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * object, which is the given number of characters long.
     * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.Reader</code> object. The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @param length the number of characters in the parameter data.
     * @throws SQLException if the columnLabel is not valid;
     * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set;
     *  if a database access error occurs or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNClob(String columnLabel,  Reader reader, long length) throws SQLException;

    //---

    /**
     * Updates the designated column with a character stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.  The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * It is intended for use when
     * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateNCharacterStream</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNCharacterStream(int columnIndex,
                             java.io.Reader x) throws SQLException;

    /**
     * Updates the designated column with a character stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.  The
     * driver does the necessary conversion from Java character format to
     * the national character set in the database.
     * It is intended for use when
     * updating  <code>NCHAR</code>,<code>NVARCHAR</code>
     * and <code>LONGNVARCHAR</code> columns.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateNCharacterStream</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing
     *        the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code> or this method is called on a closed result set
      * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNCharacterStream(String columnLabel,
                             java.io.Reader reader) throws SQLException;
    /**
     * Updates the designated column with an ascii stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateAsciiStream</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateAsciiStream(int columnIndex,
                           java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a binary stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateBinaryStream</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBinaryStream(int columnIndex,
                            java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a character stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateCharacterStream</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateCharacterStream(int columnIndex,
                             java.io.Reader x) throws SQLException;
    /**
     * Updates the designated column with an ascii stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateAsciiStream</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateAsciiStream(String columnLabel,
                           java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a binary stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateBinaryStream</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param x the new column value
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBinaryStream(String columnLabel,
                            java.io.InputStream x) throws SQLException;

    /**
     * Updates the designated column with a character stream value.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateCharacterStream</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader the <code>java.io.Reader</code> object containing
     *        the new column value
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateCharacterStream(String columnLabel,
                             java.io.Reader reader) throws SQLException;
    /**
     * Updates the designated column using the given input stream. The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateBlob</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @exception SQLException if the columnIndex is not valid; if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBlob(int columnIndex, InputStream inputStream) throws SQLException;

    /**
     * Updates the designated column using the given input stream. The data will be read from the stream
     * as needed until end-of-stream is reached.
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     *   <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateBlob</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateBlob(String columnLabel, InputStream inputStream) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * object.
     *  The data will be read from the stream
     * as needed until end-of-stream is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     *   <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateClob</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateClob(int columnIndex,  Reader reader) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * object.
     *  The data will be read from the stream
     * as needed until end-of-stream is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateClob</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @exception SQLException if the columnLabel is not valid; if a database access error occurs;
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateClob(String columnLabel,  Reader reader) throws SQLException;
   /**
     * Updates the designated column using the given <code>Reader</code>
     *
     * The data will be read from the stream
     * as needed until end-of-stream is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateNClob</code> which takes a length parameter.
     *
     * @param columnIndex the first column is 1, the second 2, ...
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if the columnIndex is not valid;
    * if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set,
     * if a database access error occurs or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNClob(int columnIndex,  Reader reader) throws SQLException;

    /**
     * Updates the designated column using the given <code>Reader</code>
     * object.
     * The data will be read from the stream
     * as needed until end-of-stream is reached.  The JDBC driver will
     * do any necessary conversion from UNICODE to the database char format.
     *
     * <p>
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the <code>updateRow</code> or
     * <code>insertRow</code> methods are called to update the database.
     *
     * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
     * it might be more efficient to use a version of
     * <code>updateNClob</code> which takes a length parameter.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
     * @param reader An object that contains the data to set the parameter value to.
     * @throws SQLException if the columnLabel is not valid; if the driver does not support national
     *         character sets;  if the driver can detect that a data conversion
     *  error could occur; this method is called on a closed result set;
     *  if a database access error occurs or
     * the result set concurrency is <code>CONCUR_READ_ONLY</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.6
     */
    void updateNClob(String columnLabel,  Reader reader) throws SQLException;

    //------------------------- JDBC 4.1 -----------------------------------


    /**
     *<p>Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object and will convert from the
     * SQL type of the column to the requested Java data type, if the
     * conversion is supported. If the conversion is not
     * supported  or null is specified for the type, a
     * <code>SQLException</code> is thrown.
     *<p>
     * At a minimum, an implementation must support the conversions defined in
     * Appendix B, Table B-3 and conversion of appropriate user defined SQL
     * types to a Java type which implements {@code SQLData}, or {@code Struct}.
     * Additional conversions may be supported and are vendor defined.
     * @param <T> the type of the class modeled by this Class object
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param type Class representing the Java data type to convert the designated
     * column to.
     * @return an instance of {@code type} holding the column value
     * @throws SQLException if conversion is not supported, type is null or
     *         another error occurs. The getCause() method of the
     * exception may provide a more detailed exception, for example, if
     * a conversion error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.7
     */
     public <T> T getObject(int columnIndex, Class<T> type) throws SQLException;


    /**
     *<p>Retrieves the value of the designated column in the current row
     * of this <code>ResultSet</code> object and will convert from the
     * SQL type of the column to the requested Java data type, if the
     * conversion is supported. If the conversion is not
     * supported  or null is specified for the type, a
     * <code>SQLException</code> is thrown.
     *<p>
     * At a minimum, an implementation must support the conversions defined in
     * Appendix B, Table B-3 and conversion of appropriate user defined SQL
     * types to a Java type which implements {@code SQLData}, or {@code Struct}.
     * Additional conversions may be supported and are vendor defined.
     *
     * @param columnLabel the label for the column specified with the SQL AS clause.
     * If the SQL AS clause was not specified, then the label is the name
     * of the column
     * @param type Class representing the Java data type to convert the designated
     * column to.
     * @param <T> the type of the class modeled by this Class object
     * @return an instance of {@code type} holding the column value
     * @throws SQLException if conversion is not supported, type is null or
     *         another error occurs. The getCause() method of the
     * exception may provide a more detailed exception, for example, if
     * a conversion error occurs
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     * @since 1.7
     */
     public <T> T getObject(String columnLabel, Class<T> type) throws SQLException;

    //------------------------- JDBC 4.2 -----------------------------------

    /**
     * Updates the designated column with an {@code Object} value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the {@code updateRow} or
     * {@code insertRow} methods are called to update the database.
     *<p>
     * If the second argument is an {@code InputStream} then the stream must contain
     * the number of bytes specified by scaleOrLength.  If the second argument is a
     * {@code Reader} then the reader must contain the number of characters specified
     * by scaleOrLength. If these conditions are not true the driver will generate a
     * {@code SQLException} when the statement is executed.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param targetSqlType the SQL type to be sent to the database
     * @param scaleOrLength for an object of {@code java.math.BigDecimal} ,
     *          this is the number of digits after the decimal point. For
     *          Java Object types {@code InputStream} and {@code Reader},
     *          this is the length
     *          of the data in the stream or reader.  For all other types,
     *          this value will be ignored.
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is {@code CONCUR_READ_ONLY}
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not
     * support this method; if the JDBC driver does not support the specified targetSqlType
     * @see JDBCType
     * @see SQLType
     * @since 1.8
     */
     default void updateObject(int columnIndex, Object x,
             SQLType targetSqlType, int scaleOrLength)  throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }

    /**
     * Updates the designated column with an {@code Object} value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the {@code updateRow} or
     * {@code insertRow} methods are called to update the database.
     *<p>
     * If the second argument is an {@code InputStream} then the stream must
     * contain number of bytes specified by scaleOrLength.  If the second
     * argument is a {@code Reader} then the reader must contain the number
     * of characters specified by scaleOrLength. If these conditions are not
     * true the driver will generate a
     * {@code SQLException} when the statement is executed.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param columnLabel the label for the column specified with the SQL AS
     * clause.  If the SQL AS clause was not specified, then the label is
     * the name of the column
     * @param x the new column value
     * @param targetSqlType the SQL type to be sent to the database
     * @param scaleOrLength for an object of {@code java.math.BigDecimal} ,
     *          this is the number of digits after the decimal point. For
     *          Java Object types {@code InputStream} and {@code Reader},
     *          this is the length
     *          of the data in the stream or reader.  For all other types,
     *          this value will be ignored.
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is {@code CONCUR_READ_ONLY}
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not
     * support this method; if the JDBC driver does not support the specified targetSqlType
     * @see JDBCType
     * @see SQLType
     * @since 1.8
     */
    default void updateObject(String columnLabel, Object x,
            SQLType targetSqlType, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }

    /**
     * Updates the designated column with an {@code Object} value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the {@code updateRow} or
     * {@code insertRow} methods are called to update the database.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param x the new column value
     * @param targetSqlType the SQL type to be sent to the database
     * @exception SQLException if the columnIndex is not valid;
     * if a database access error occurs;
     * the result set concurrency is {@code CONCUR_READ_ONLY}
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not
     * support this method; if the JDBC driver does not support the specified targetSqlType
     * @see JDBCType
     * @see SQLType
     * @since 1.8
     */
    default void updateObject(int columnIndex, Object x, SQLType targetSqlType)
            throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }

    /**
     * Updates the designated column with an {@code Object} value.
     *
     * The updater methods are used to update column values in the
     * current row or the insert row.  The updater methods do not
     * update the underlying database; instead the {@code updateRow} or
     * {@code insertRow} methods are called to update the database.
     *<p>
     * The default implementation will throw {@code SQLFeatureNotSupportedException}
     *
     * @param columnLabel the label for the column specified with the SQL AS
     * clause.  If the SQL AS clause was not specified, then the label is
     * the name of the column
     * @param x the new column value
     * @param targetSqlType the SQL type to be sent to the database
     * @exception SQLException if the columnLabel is not valid;
     * if a database access error occurs;
     * the result set concurrency is {@code CONCUR_READ_ONLY}
     * or this method is called on a closed result set
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not
     * support this method; if the JDBC driver does not support the specified targetSqlType
     * @see JDBCType
     * @see SQLType
     * @since 1.8
     */
    default void updateObject(String columnLabel, Object x,
            SQLType targetSqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject not implemented");
    }
}




```




# 10、ResultSetMetaData

![](../../pic/2020-04-30-17-42-54.png)


```java

package java.sql;

/**
可以从ResultSet中获取返回结果集中columns的列类型和属性信息。

下面的代码片段创建了一个ResultSet对象，然后创建了一个ResultSetMetaData对象，并且用rsmd可以获取rs结果集中包含多少个列，

 whether the first column in rs can be used in a <code>WHERE</code> clause.
 *
 *     ResultSet rs = stmt.executeQuery("SELECT a, b, c FROM TABLE2");
 *     ResultSetMetaData rsmd = rs.getMetaData();
 *     int numberOfColumns = rsmd.getColumnCount();
 *     boolean b = rsmd.isSearchable(1);
 */

public interface ResultSetMetaData extends Wrapper {

    //rs包含多少列
    int getColumnCount() throws SQLException;

    /** 指定的列是否自动递增的
     * Indicates whether the designated column is automatically numbered.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isAutoIncrement(int column) throws SQLException;

    /** 列是否大小写敏感
     * Indicates whether a column's case matters.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isCaseSensitive(int column) throws SQLException;

    /**
     * Indicates whether the designated column can be used in a where clause.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isSearchable(int column) throws SQLException;

    /**
     * Indicates whether the designated column is a cash value.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isCurrency(int column) throws SQLException;

    /**
     * Indicates the nullability of values in the designated column.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the nullability status of the given column; one of <code>columnNoNulls</code>,
     *          <code>columnNullable</code> or <code>columnNullableUnknown</code>
     * @exception SQLException if a database access error occurs
     */
    int isNullable(int column) throws SQLException;

    /**
     * The constant indicating that a
     * column does not allow <code>NULL</code> values.
     */
    int columnNoNulls = 0;

    /**
     * The constant indicating that a
     * column allows <code>NULL</code> values.
     */
    int columnNullable = 1;

    /**
     * The constant indicating that the
     * nullability of a column's values is unknown.
     */
    int columnNullableUnknown = 2;

    /**
     * Indicates whether values in the designated column are signed numbers.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isSigned(int column) throws SQLException;

    /**
     * Indicates the designated column's normal maximum width in characters.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the normal maximum number of characters allowed as the width
     *          of the designated column
     * @exception SQLException if a database access error occurs
     */
    int getColumnDisplaySize(int column) throws SQLException;

    /**
     * Gets the designated column's suggested title for use in printouts and
     * displays. The suggested title is usually specified by the SQL <code>AS</code>
     * clause.  If a SQL <code>AS</code> is not specified, the value returned from
     * <code>getColumnLabel</code> will be the same as the value returned by the
     * <code>getColumnName</code> method.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the suggested column title
     * @exception SQLException if a database access error occurs
     */
    String getColumnLabel(int column) throws SQLException;

    /**
     * Get the designated column's name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return column name
     * @exception SQLException if a database access error occurs
     */
    String getColumnName(int column) throws SQLException;

    /**
     * Get the designated column's table's schema.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return schema name or "" if not applicable
     * @exception SQLException if a database access error occurs
     */
    String getSchemaName(int column) throws SQLException;

    /**
     * Get the designated column's specified column size.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. 0 is returned for data types where the
     * column size is not applicable.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return precision
     * @exception SQLException if a database access error occurs
     */
    int getPrecision(int column) throws SQLException;

    /**
     * Gets the designated column's number of digits to right of the decimal point.
     * 0 is returned for data types where the scale is not applicable.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return scale
     * @exception SQLException if a database access error occurs
     */
    int getScale(int column) throws SQLException;

    /**
     * Gets the designated column's table name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return table name or "" if not applicable
     * @exception SQLException if a database access error occurs
     */
    String getTableName(int column) throws SQLException;

    /**
     * Gets the designated column's table's catalog name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the name of the catalog for the table in which the given column
     *          appears or "" if not applicable
     * @exception SQLException if a database access error occurs
     */
    String getCatalogName(int column) throws SQLException;

    /**
     * Retrieves the designated column's SQL type.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return SQL type from java.sql.Types
     * @exception SQLException if a database access error occurs
     * @see Types
     */
    int getColumnType(int column) throws SQLException;

    /**
     * Retrieves the designated column's database-specific type name.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return type name used by the database. If the column type is
     * a user-defined type, then a fully-qualified type name is returned.
     * @exception SQLException if a database access error occurs
     */
    String getColumnTypeName(int column) throws SQLException;

    /**
     * Indicates whether the designated column is definitely not writable.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isReadOnly(int column) throws SQLException;

    /**
     * Indicates whether it is possible for a write on the designated column to succeed.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isWritable(int column) throws SQLException;

    /**
     * Indicates whether a write on the designated column will definitely succeed.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    boolean isDefinitelyWritable(int column) throws SQLException;

    //--------------------------JDBC 2.0-----------------------------------

    /**
     * <p>Returns the fully-qualified name of the Java class whose instances
     * are manufactured if the method <code>ResultSet.getObject</code>
     * is called to retrieve a value
     * from the column.  <code>ResultSet.getObject</code> may return a subclass of the
     * class returned by this method.
     *
     * @param column the first column is 1, the second is 2, ...
     * @return the fully-qualified name of the class in the Java programming
     *         language that would be used by the method
     * <code>ResultSet.getObject</code> to retrieve the value in the specified
     * column. This is the class name used for custom mapping.
     * @exception SQLException if a database access error occurs
     * @since 1.2
     */
    String getColumnClassName(int column) throws SQLException;
}


```





























