

<!-- TOC -->

- [1、jdk提供的标准DataSource接口](#1jdk提供的标准datasource接口)
- [2、UnpooledDataSource](#2unpooleddatasource)
- [3、PooledDataSource](#3pooleddatasource)
    - [1、popConnection(String username, String password)](#1popconnectionstring-username-string-password)
    - [2、pushConnection(PooledConnection conn)链接归还到连接池](#2pushconnectionpooledconnection-conn链接归还到连接池)
- [4、PoolState维护线程池的一些状态信息，是真正保存线程的地方](#4poolstate维护线程池的一些状态信息是真正保存线程的地方)
- [5、PooledConnection基于jdk动态代理连接](#5pooledconnection基于jdk动态代理连接)

<!-- /TOC -->



mybatis内部自己的数据库连接池实现PooledDataSource.

> 总结

其思路是使用两个list分别维护当前真正被使用的活跃链接activeConnections和空闲链接idleConnections，每个池中均有最大个数限制poolMaximumActiveConnections和poolMaximumIdleConnections。（同时存在的物理链接数最大poolMaximumActiveConnections）当通过getconnection获取链接时，没有空闲链接就新建（控制在最大链接数范围内，否则调用state.wait(poolTimeToWait)阻塞等待），并把当前链接放到活跃连接池。当调用close归还的时候，会判断空闲链接数是否已经达到最大了，达到最大关闭物理连接，否则把链接放回到空闲链接池，并通知阻塞获取链接的线程state.notifyAll()。另外一个核心点是，通过DataSource获取的链接对象为PooledConnection，通过动态代理的方式来进行数据库池化链接的管理。


# 1、jdk提供的标准DataSource接口

```java
Connection getConnection() throws SQLException;

Connection getConnection(String username, String password)
    throws SQLException;
```

可见连接池只定义了两个获取数据库链接的方法，具体的链接获取逻辑以及数据库链接的池化管理由具体的实现类自己处理。


# 2、UnpooledDataSource

通过代码可以看出，当前类并没有提供数据库连接管理的功能方法。这个类就是基于驱动直接创建链接，PooledDataSource使用当前类创建具体的链接。

```java
public class UnpooledDataSource implements DataSource {

  private ClassLoader driverClassLoader;//驱动类加载器
  private Properties driverProperties;//驱动类属性？
  private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<>();//缓存驱动类的名称和驱动类实例

  private String driver;
  private String url;
  private String username;
  private String password;

  private Boolean autoCommit;//自动提交设置
  private Integer defaultTransactionIsolationLevel;//默认的事务隔离级别

  static {
    Enumeration<Driver> drivers = DriverManager.getDrivers();
    while (drivers.hasMoreElements()) {
      Driver driver = drivers.nextElement();
      registeredDrivers.put(driver.getClass().getName(), driver);
    }
  }

  public UnpooledDataSource() {
  }

  public UnpooledDataSource(String driver, String url, String username, String password) {
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public UnpooledDataSource(String driver, String url, Properties driverProperties) {
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }

  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    this.driverClassLoader = driverClassLoader;
    this.driver = driver;
    this.url = url;
    this.driverProperties = driverProperties;
  }
```

获取连接的流程：

- getConnection()\ getConnection(String username, String password)
    - doGetConnection(String username, String password)
        - doGetConnection(Properties properties)
            - DriverManager.getConnection(url, properties)基于驱动直接创建连接



# 3、PooledDataSource


```java
public class PooledDataSource implements DataSource {

  private final PoolState state = new PoolState(this);//封装池化链接

  private final UnpooledDataSource dataSource;//用于创建链接connection

  // OPTIONAL CONFIGURATION FIELDS
  protected int poolMaximumActiveConnections = 10;//在任意时间可以存在的活动（也就是正在使用）连接数量，默认值：10
  protected int poolMaximumIdleConnections = 5;//任意时间可能存在的空闲连接数
  protected int poolMaximumCheckoutTime = 20000;//在被强制返回之前，池中连接被检出（checked out）时间，默认值：20000 毫秒（即 20 秒）
  //在连接数已经达到最大的时候，调用wait等待的时间
  protected int poolTimeToWait = 20000;//这是一个底层设置，如果获取连接花费了相当长的时间，连接池会打印状态日志并重新尝试获取一个连接（避免在误配置的情况下一直安静的失败），默认值：20000 毫秒（即 20 秒）。
  protected int poolMaximumLocalBadConnectionTolerance = 3;//这是一个关于坏连接容忍度的底层设置， 作用于每一个尝试从缓存池获取连接的线程。 如果这个线程获取到的是一个坏的连接，那么这个数据源允许这个线程尝试重新获取一个新的连接，但是这个重新尝试的次数不应该超过 poolMaximumIdleConnections 与 poolMaximumLocalBadConnectionTolerance 之和。 默认值：3 （新增于 3.4.5）
  protected String poolPingQuery = "NO PING QUERY SET";//发送到数据库的侦测查询，用来检验连接是否正常工作并准备接受请求。默认是“NO PING QUERY SET”，这会导致多数数据库驱动失败时带有一个恰当的错误消息
  protected boolean poolPingEnabled;//是否启用侦测查询。若开启，需要设置 poolPingQuery 属性为一个可执行的 SQL 语句（最好是一个速度非常快的 SQL 语句），默认值：false。
  protected int poolPingConnectionsNotUsedFor;//配置 poolPingQuery 的频率。可以被设置为和数据库连接超时时间一样，来避免不必要的侦测，默认值：0（即所有连接每一时刻都被侦测 — 当然仅当 poolPingEnabled 为 true 时适用）。

  private int expectedConnectionTypeCode;//根据URL+username+password生成的唯一码，可以看成是一个数据源的唯一标识

  public PooledDataSource() {
    dataSource = new UnpooledDataSource();
  }

  public PooledDataSource(UnpooledDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public PooledDataSource(String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, username, password);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }

  public PooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
    dataSource = new UnpooledDataSource(driverClassLoader, driver, url, driverProperties);
    expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
  }
```




获取连接的流程：

- getConnection()\ getConnection(String username, String password)
    - PooledConnection popConnection(String username, String password)

## 1、popConnection(String username, String password)

```java
/**
   * 数据源获得数据库链接的入口
   *
   * 无论是从活跃的连接池获得链接还是归还链接的时候都是从旧的链接中获得真实的链接，然后包装成新的PooledConnection，
   *
   现在让我们看一下popConnection()方法到底做了什么：
   1.先看空闲列表idleConnections是否有空闲的链接，如果有，就直接返回一个可用的PooledConnection对象；否则进行第2步。
   2.查看活动列表activeConnections是否已满poolMaximumActiveConnections；如果没有满，则创建一个新的PooledConnection对象，然后放到activeConnections池中，然后返回此PooledConnection对象；否则进行第三步；
   3.看最早进入activeConnections池中的PooledConnection对象是否已经过期：如果已经过期，从activeConnections池中移除此对象，使用该代理连接的物理连接然后创建一个新的PooledConnection对象，添加到activeConnections中，然后将此对象返回；否则进行第4步。
   4.线程等待state.wait(poolTimeToWait)，循环；
   * @param username
   * @param password
   * @return
   * @throws SQLException
   */
  private PooledConnection popConnection(String username, String password) throws SQLException {
    boolean countedWait = false;
    PooledConnection conn = null;
    long t = System.currentTimeMillis();
    int localBadConnectionCount = 0;

    while (conn == null) {   //循环等待
      synchronized (state) {
        if (!state.idleConnections.isEmpty()) {//1、说明空闲连接池有空闲的链接
          // Pool has available connection
          conn = state.idleConnections.remove(0);//首先从空闲的连接池中移除最前面的那个
          if (log.isDebugEnabled()) {
            log.debug("Checked out connection " + conn.getRealHashCode() + " from pool.");
          }
        } else {
          //2、空闲的连接池中没有链接，判断活跃连接池小于最大活跃链接数，创建一个新的链接
          if (state.activeConnections.size() < poolMaximumActiveConnections) {
            // Can create new connection
            conn = new PooledConnection(dataSource.getConnection(), this);
            if (log.isDebugEnabled()) {
              log.debug("Created connection " + conn.getRealHashCode() + ".");
            }
          } else {//3、活跃链接数大于等于最大活跃数，不能直接新建
            // Cannot create new connection
            PooledConnection oldestActiveConnection = state.activeConnections.get(0);
            long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
            // 计算它的校验时间，如果校验时间大于连接池规定的最大校验时间，则认为它已经过期了，利用这个PoolConnection内部的realConnection重新生成一个PooledConnection
            if (longestCheckoutTime > poolMaximumCheckoutTime) {//4、处于活跃状态的连接数已经达到最大的连接数，从活跃线程池中拿取进入最早的，即第一个，判断一下是否过期
              // Can claim overdue connection
              state.claimedOverdueConnectionCount++;
              state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
              state.accumulatedCheckoutTime += longestCheckoutTime;
              state.activeConnections.remove(oldestActiveConnection);
              if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {//非自动提交模式，之前的事务进行回滚
                try {
                  oldestActiveConnection.getRealConnection().rollback();
                } catch (SQLException e) {
                  /*
                     Just log a message for debug and continue to execute the following
                     statement like nothing happened.
                     Wrap the bad connection with a new PooledConnection, this will help
                     to not interrupt current executing thread and give current thread a
                     chance to join the next competition for another valid/good database
                     connection. At the end of this loop, bad {@link @conn} will be set as null.
                   */
                  log.debug("Bad connection. Could not roll back");
                }
              }
              conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
              conn.setCreatedTimestamp(oldestActiveConnection.getCreatedTimestamp());
              conn.setLastUsedTimestamp(oldestActiveConnection.getLastUsedTimestamp());
              oldestActiveConnection.invalidate();
              if (log.isDebugEnabled()) {
                log.debug("Claimed overdue connection " + conn.getRealHashCode() + ".");
              }
            } else {// 4、线程等待
              // Must wait
              try {
                if (!countedWait) {//一个线程获取链接会等待多次？
                  state.hadToWaitCount++;//
                  countedWait = true;
                }
                if (log.isDebugEnabled()) {
                  log.debug("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                }
                long wt = System.currentTimeMillis();
                state.wait(poolTimeToWait);//阻塞等待20秒。可能中途被唤醒
                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
              } catch (InterruptedException e) {
                break;
              }
            }
          }
        }
        if (conn != null) {//获取到链接
          // ping to server and check the connection is valid or not
          if (conn.isValid()) {//链接有效
            if (!conn.getRealConnection().getAutoCommit()) {//非自动提交事务模式，把上一个事务进行的操作进行回滚
              conn.getRealConnection().rollback();
            }
            conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));
            conn.setCheckoutTimestamp(System.currentTimeMillis());
            conn.setLastUsedTimestamp(System.currentTimeMillis());
            state.activeConnections.add(conn);//链接有效添加到活跃的线程池中
            state.requestCount++;
            state.accumulatedRequestTime += System.currentTimeMillis() - t;
          } else {
            if (log.isDebugEnabled()) {
              log.debug("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
            }
            state.badConnectionCount++;
            localBadConnectionCount++;
            conn = null;
            if (localBadConnectionCount > (poolMaximumIdleConnections + poolMaximumLocalBadConnectionTolerance)) {
              if (log.isDebugEnabled()) {
                log.debug("PooledDataSource: Could not get a good connection to the database.");
              }
              throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
            }
          }
        }
      }

    }
    //循环获取链接结束 如果还是null 抛出异常
    if (conn == null) {
      if (log.isDebugEnabled()) {
        log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
      }
      throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
    }

    return conn;
  }
```



## 2、pushConnection(PooledConnection conn)链接归还到连接池

PooledConnection调用close的时候会间接调用这个函数，并且有链接归还到空闲连接池时，通知阻塞获取链接的线程state.notifyAll()，简单来说这里做了三件事：

- 1、把要归还的链接从活跃链接列表移除，并检查当前链接是否有效，无效的话直接结束；
- 2、判断空闲链接列表数量是否达到最大poolMaximumIdleConnections，没有达到最大时，先判断是否需要对事务进行回滚，然后从连接中获取物理连接，新建一个PooledConnection对象放到空闲连接池列表idleConnections，最后，state.notifyAll()通知那些阻塞等待链接的线程。
- 3、当空闲链接达到最大时，先判断是否需要对事务进行回滚，然后关闭物理连接，标记当前链接无效；

```java
protected void pushConnection(PooledConnection conn) throws SQLException {

    synchronized (state) {
      state.activeConnections.remove(conn);//1、首先从活动的线程池中移除
      if (conn.isValid()) {
        //1、空闲链接没有达到最大，允许放入
        if (state.idleConnections.size() < poolMaximumIdleConnections && conn.getConnectionTypeCode() == expectedConnectionTypeCode) {
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {//清理上个使用者没有提交的事务
            conn.getRealConnection().rollback();
          }
          PooledConnection newConn = new PooledConnection(conn.getRealConnection(), this);
          state.idleConnections.add(newConn);//根据真实的链接来构建一个新的连接放到空闲池中
          newConn.setCreatedTimestamp(conn.getCreatedTimestamp());//更新这个链接的创建时间为真实链接第一次被创建的时间
          newConn.setLastUsedTimestamp(conn.getLastUsedTimestamp());
          conn.invalidate();//这里只是把链接设置为失效，为什么没有设置为null？？？
          if (log.isDebugEnabled()) {
            log.debug("Returned connection " + newConn.getRealHashCode() + " to pool.");
          }
          state.notifyAll();//通知那些阻塞等待链接的线程
        } else {//2、空闲链接达到最大或者链接串不匹配，关闭真实的链接
          state.accumulatedCheckoutTime += conn.getCheckoutTime();
          if (!conn.getRealConnection().getAutoCommit()) {
            conn.getRealConnection().rollback();
          }
          conn.getRealConnection().close();//关闭真实的链接
          if (log.isDebugEnabled()) {
            log.debug("Closed connection " + conn.getRealHashCode() + ".");
          }
          conn.invalidate();//当前链接标记位无效
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug("A bad connection (" + conn.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
        }
        state.badConnectionCount++;
      }
    }
  }

```



# 4、PoolState维护线程池的一些状态信息，是真正保存线程的地方

```java
public class PoolState {

  protected PooledDataSource dataSource;//数据源对象

  protected final List<PooledConnection> idleConnections = new ArrayList<>();//空闲链接
  protected final List<PooledConnection> activeConnections = new ArrayList<>();//活跃链接池，正在被使用的连接放在这里
  protected long requestCount = 0;//添加到活跃线程池+1，应该是用来统计成功获取连接的次数
  protected long accumulatedRequestTime = 0;//累计获取连接花费的总时间
  protected long accumulatedCheckoutTime = 0;//和accumulatedCheckoutTimeOfOverdueConnections区别？
  protected long claimedOverdueConnectionCount = 0;//过期链接数统计
  protected long accumulatedCheckoutTimeOfOverdueConnections = 0;//全部过期链接数的过期时间累计
  protected long accumulatedWaitTime = 0;//获取不到链接等待时间累计
  protected long hadToWaitCount = 0;//获取不到链接等待次数统计
  protected long badConnectionCount = 0;//获取到链接发现链接check不通过累计\或者check链接发现无效累计+1

  public PoolState(PooledDataSource dataSource) {
    this.dataSource = dataSource;
  }
```





# 5、PooledConnection基于jdk动态代理连接


```java
class PooledConnection implements InvocationHandler {

  private static final String CLOSE = "close";
  private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };

  private final int hashCode;
  private final PooledDataSource dataSource;
  private final Connection realConnection;//真正通过驱动类创建的原始链接
  private final Connection proxyConnection;//代理的数据库链接
  private long checkoutTimestamp;//被选中添加到活跃线程池时间
  private long createdTimestamp;//当前代理连接对象的创建世界
  private long lastUsedTimestamp;//最后使用时间，包含创建、添加到活跃线程池、
  private int connectionTypeCode;//生成规则是基于URL+username+password的hash码功能？
  private boolean valid;//把当前代理连接标记为无效

  
  public PooledConnection(Connection connection, PooledDataSource dataSource) {
    this.hashCode = connection.hashCode();
    this.realConnection = connection;
    this.dataSource = dataSource;
    this.createdTimestamp = System.currentTimeMillis();
    this.lastUsedTimestamp = System.currentTimeMillis();
    this.valid = true;
    //这里使用了jdk的动态代理，用户使用的是这个生成的动态代理对象，然后对connection方法的调用，间接调用该对象的invoke方法，最后由真正的链接realConnection来执行
    this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
  }


 @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {//在关闭线程池的时候会先判断是否有必要放到线程池中
      dataSource.pushConnection(this);//调用关闭链接时，把当前链接归还到线程池
      return null;
    }
    try {
      if (!Object.class.equals(method.getDeclaringClass())) {//非object类的基础方法的调用均执行下面的方法
        // issue #579 toString() should never fail
        // throw an SQLException instead of a Runtime
        checkConnection();
      }
      return method.invoke(realConnection, args);//调用被代理对象的方法
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }

  }

}

```


