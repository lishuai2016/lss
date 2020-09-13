数据库连接池dbcp

Connection--->PoolableConnection--->ObjectTimestampPair



归还
org.apache.commons.dbcp.PoolableConnection#close
	org.apache.commons.pool.impl.GenericObjectPool#returnObject
		org.apache.commons.pool.impl.GenericObjectPool#addObjectToPool[无论是新增还是归还调用的都是该方法把链接放到池中]




其中的value是数据库链接connection，封装在ObjectTimestampPair中。
static class ObjectTimestampPair implements Comparable {
        Object value;
        long tstamp;}



org.apache.commons.pool.impl.GenericObjectPool#_pool 
private CursorableLinkedList _pool; 通过一个list来保存数据库的连接

在GenericObjectPool中org.apache.commons.pool.impl.GenericObjectPool#addObjectToPool通_lifo参数来决定把新建的连接插入到连接池list的头部还是尾部。
 if (this._lifo) {
    this._pool.addFirst(new ObjectTimestampPair(obj));
} else {
    this._pool.addLast(new ObjectTimestampPair(obj));
}

并且池中存的数据库链接是包装类PoolGuardConnectionWrapper，包含具体的物理连接connection




https://www.cnblogs.com/duanxz/p/3668614.html

[mybatis自己的数据源是实现](https://my.oschina.net/mengyuankan/blog/2664784)

https://blog.csdn.net/tidu2chengfo/article/details/53406890


1，连接池创建
BasicDataSource -> DataSource
    @Override 
    public Connection getConnection()
        【a】createDataSource()
              如果dataSource不为空，则返回数据源对象，否则创建之，如下：
            【1】createConnectionFactory()    
                    (a)通过配置参数<property name="driverClassName" value="${jdbc.driver}" />，加载驱动类Class.forName(driverClassName);
                    (b)通过配置参数<property name="url" value="${jdbc.url}" />，获取驱动DriverManager.getDriver(url)；
                    (c)通过配置参数<property name="username" value="${jdbc.username}" />，<property name="password" value="${jdbc.password}" />，
                    以及driver，url，创建数据库连接工厂new DriverConnectionFactory(driver, url, connectionProperties);
            【2】createConnectionPool()
                    (a)通过配置参数：<property name="maxActive" value="${dbcp.maxActive}" />
                                            <property name="maxIdle" value="${dbcp.maxIdle}" />
                                            <property name="minIdle" value="${dbcp.minIdle}" />
                                            等配置项，创建连接池org.apach.commons.pool.impl.GenericObjectPool connectionPool
                                            commons-dbcp本身不创建连接池，通过commons-pool来管理连接池
                    (b)GenericObjectPool.addObject()中调用下步创建的连接池工厂类，创建连接，并通过addObjectToPool(obj, false);将连接保存在连接池
            【4】createPoolableConnectionFactory(driverConnectionFactory, statementPoolFactory, abandonedConfig)
                    (a)创建连接池工厂类PoolableConnectionFactory，工厂类内部将该工厂设置到上步创建的connectionPool中，这样就可以通过connectionPool中的addObject()调用连接池工厂创建连接
            【5】createDataSourceInstance()
                    (a)根据连接池connectionPool创建池化数据源对象 PoolingDataSource pds = new PoolingDataSource(connectionPool)
            【6】初始化连接
                    for (int i = 0 ; i < initialSize ; i++) {
                        connectionPool.addObject();
                    }
            【7】返回池化数据库连接对象dataSource
        【b】getConnection()
            【1】_pool.borrowObject()；调用【a】-【2】创建的连接池创建连接
                    (a)_factory.makeObject()；调用【a】-【4】创建的连接池工厂类对象，返回new PoolableConnection(conn,_pool,_config);对象
                            其中PoolableConnection持有【a】-【2】创建的连接池_pool，当PoolableConnection.close()时，该连接会被_pool回收，_pool.returnObject(this);

【上面的流程大致正确，具体细节需要补充】




对象池中的几个核心方法borrowObject，returnObject，addObject

public interface ObjectPool {
    Object borrowObject() throws Exception, NoSuchElementException, IllegalStateException;

    void returnObject(Object var1) throws Exception;

    void invalidateObject(Object var1) throws Exception;

    void addObject() throws Exception, IllegalStateException, UnsupportedOperationException;

    int getNumIdle() throws UnsupportedOperationException;

    int getNumActive() throws UnsupportedOperationException;

    void clear() throws Exception, UnsupportedOperationException;

    void close() throws Exception;

    void setFactory(PoolableObjectFactory var1) throws IllegalStateException, UnsupportedOperationException;
}



数据库连接池的公用接口，所有的数据库连接池均是实现这个接口

public interface DataSource  extends CommonDataSource,Wrapper {


  Connection getConnection() throws SQLException;

  Connection getConnection(String username, String password)
    throws SQLException;

}







org.apache.commons.dbcp.BasicDataSource#createDataSource
	org.apache.commons.dbcp.BasicDataSource#createConnectionFactory  -- 1、直接根据驱动的实现类获得链接【驱动类链接工厂】
	org.apache.commons.dbcp.BasicDataSource#createConnectionPool   -- 2、实例化一个org.apache.commons.pool.impl.GenericObjectPool对象
	org.apache.commons.dbcp.BasicDataSource#createPoolableConnectionFactory -- 3、【作用是关联1、2步骤的，并且指定返回的数据库链接对象为PoolableConnection】根据参数1和2构建一个池话的线程池工厂，并把它设置到参数2中，用这个工厂类简介通过驱动连接池工厂产生数据库的链接【简单来说就是把存放线程次的ppool和产生线程的驱动类关联在一起】
	并且这个池话的线程池工厂，在创建以及释放链接的时候，会对原始的驱动类获得的链接进行封装处理。
		org.apache.commons.dbcp.PoolableConnectionFactory#makeObject  -- 具体的创建数据库链接，返回的是数据库链接的包装类org.apache.commons.dbcp.PoolableConnection  ，注意创建的时候并没有放到连接池中

		org.apache.commons.dbcp.PoolableConnection#close数据库链接关闭的时候在这里来把数据库链接放到数据库pool里面
		org.apache.commons.pool.ObjectPool#returnObject
	org.apache.commons.dbcp.BasicDataSource#createDataSourceInstance -- 4、创建具体的池话的数据库 【根据2步骤的pool来操作数据库链接的】
		org.apache.commons.dbcp.PoolingDataSource#getConnection() -- 00用户创建数据库链接的调用
			org.apache.commons.pool.ObjectPool#borrowObject【org.apache.commons.pool.impl.GenericObjectPool#borrowObject】       -- 01从数据库池中获得链接【这里面包含数据库没有会进行创建新的数据库链接makeObject】



0、dbcp线程池获得数据库链接都是原始数据库链接的代理对象【实现相同的接口，并且包含接口的实例对象java.sql.Connection】


1、在哪里做的线程池个数的判断呢？什么时候创建新的线程？什么时候从线程池获取？



org.apache.commons.dbcp.BasicDataSource#getConnection()

    public Connection getConnection() throws SQLException {
        return this.createDataSource().getConnection();
    }



protected synchronized DataSource createDataSource() throws SQLException {
        if (this.closed) {
            throw new SQLException("Data source is closed");
        } else if (this.dataSource != null) {
            return this.dataSource;
        } else {
            ConnectionFactory driverConnectionFactory = this.createConnectionFactory();
            this.createConnectionPool();
            GenericKeyedObjectPoolFactory statementPoolFactory = null;
            if (this.isPoolPreparedStatements()) {
                statementPoolFactory = new GenericKeyedObjectPoolFactory((KeyedPoolableObjectFactory)null, -1, (byte)0, 0L, 1, this.maxOpenPreparedStatements);
            }

            this.createPoolableConnectionFactory(driverConnectionFactory, statementPoolFactory, this.abandonedConfig);
            this.createDataSourceInstance();

            try {
                for(int i = 0; i < this.initialSize; ++i) {
                    this.connectionPool.addObject();
                }
            } catch (Exception var4) {
                throw new SQLNestedException("Error preloading the connection pool", var4);
            }

            return this.dataSource;
        }
    }