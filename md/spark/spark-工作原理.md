


- rdd
- dag
- 转化和动作
- 宽依赖和窄依赖
- 内存+迭代式






Spark 运算比 Hadoop 的 MapReduce 框架快的原因是因为 Hadoop 在一次 MapReduce 运算之后,会将数据的运算结果从内存写入到磁盘中,第二次 Mapredue 运算时在从磁盘中读取数据,所以其瓶颈在2次运算间的多余 IO 消耗. Spark 则是将数据一直缓存在内存中,直到计算得到最后的结果,再将结果写入到磁盘,所以多次运算的情况下, Spark 是比较快的. 其优化了`迭代式`工作负载.








# 参考

- [Spark 学习: spark 原理简述](https://zhuanlan.zhihu.com/p/34436165)