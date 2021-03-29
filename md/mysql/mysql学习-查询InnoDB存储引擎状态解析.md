

show engine innodb status


- BACKGROUND THREAD:后台Master线程

- SEMAPHORES:信号量信息

- LATEST DETECTED DEADLOCK:最近一次死锁信息，只有产生过死锁才会有

- TRANSACTIONS:事物信息

- FILE I/O:IO Thread信息

- INSERT BUFFER AND ADAPTIVE HASH INDEX: INSERT BUFFER和自适应HASH索引

- LOG:日志

- BUFFER POOL AND MEMORY:BUFFER POOL和内存

- INDIVIDUAL BUFFER POOL INFO:如果设置了多个BUFFER POOL实例，这里显示每个BUFFER POOL信息。可通过innodb_buffer_pool_instances参数设置

- ROW OPERATIONS‍‍:行操作统计信息‍‍








# 参考

- [一条命令解读InnoDB存储引擎—show engine innodb status](https://cloud.tencent.com/developer/article/1424670)

- [mysql之show engine innodb status解读](https://www.cnblogs.com/xiaoboluo768/p/5171425.html)

- [MYSQL show engine innodb status 这么多年，你真的都懂？](https://cloud.tencent.com/developer/article/1507132)