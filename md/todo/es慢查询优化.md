es慢查询优化


https://www.elastic.co/guide/cn/elasticsearch/guide/current/logging.html


1、慢查询

默认情况，慢日志是不开启的。要开启它，需要定义具体动作（query，fetch 还是 index），你期望的事件记录等级（ WARN 、 DEBUG 等），以及时间阈值。

es有几种搜索模式，比如 query_then_fetch , 表示先从各个节点query到id，然后整合，再去各个节点拿具体数据

这是一个索引级别的设置，也就是说可以独立应用给单个或所有索引：

PUT /_all/_settings
{
    "index.search.slowlog.threshold.query.warn":"5s",
    "index.search.slowlog.threshold.query.info":"2s",
    "index.search.slowlog.threshold.query.debug":"1s",
    "index.search.slowlog.threshold.query.trace":"400ms",
    "index.search.slowlog.threshold.fetch.warn":"1s",
    "index.search.slowlog.threshold.fetch.info":"800ms",
    "index.search.slowlog.threshold.fetch.debug":"500ms",
    "index.search.slowlog.threshold.fetch.trace":"200ms",
    "index.indexing.slowlog.threshold.index.warn":"5s",
    "index.indexing.slowlog.threshold.index.info":"2s",
    "index.indexing.slowlog.threshold.index.debug":"1s",
    "index.indexing.slowlog.threshold.index.trace":"400ms"
}
 

查询慢于 5 秒输出一个 WARN 日志
索引慢于 2 秒输出一个 INFO 日志
获取慢于 1 秒输出一个 DEBUG 日志



https://blog.csdn.net/mingongge/article/details/102512663




2、kibana请求超时可以设置参数，默认是30秒


