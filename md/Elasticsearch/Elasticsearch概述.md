


- [Elasticsearch 6.x 倒排索引与分词](https://juejin.im/post/5b799cf551882542f676daba)
- [[转]23个最有用的Elasticsearch检索技巧](https://juejin.im/post/5b7fe4a46fb9a019d92469a9)





<!-- TOC -->

- [elasticsearch基本接口使用](#elasticsearch基本接口使用)
    - [查看当前es上的所有索引](#查看当前es上的所有索引)
    - [查看elasticsearch集群状态](#查看elasticsearch集群状态)
    - [查看集群节点信息](#查看集群节点信息)
    - [查看节点进程信息](#查看节点进程信息)
    - [查看索引统计信息](#查看索引统计信息)
    - [查看指定索引统计信息](#查看指定索引统计信息)
    - [查看热点线程](#查看热点线程)
    - [查看指定索引信息](#查看指定索引信息)
    - [查看所有节点jvm信息](#查看所有节点jvm信息)
    - [查看指定节点jvm信息](#查看指定节点jvm信息)
    - [删除全部索引](#删除全部索引)
    - [删除指定索引](#删除指定索引)
    - [查看线程池配置](#查看线程池配置)

<!-- /TOC -->




先说Elasticsearch的文件存储，Elasticsearch是面向文档型数据库，一条数据在这里就是一个文档，用JSON作为文档序列化的格式，比如下面这条用户数据：

{
    "name" :    "John",
    "sex" :      "Male",
    "age" :      25,
    "birthDate": "1990/05/01",
    "about" :    "I love to go rock climbing",
    "interests": [ "sports", "music" ]
}

一个 Elasticsearch 集群可以包含多个索引(数据库)，也就是说其中包含了很多类型(表)。这些类型中包含了很多的文档(行)，然后每个文档中又包含了很多的字段(列)。Elasticsearch的交互，可以使用Java API，也可以直接使用HTTP的Restful API方式，比如我们打算插入一条记录，可以简单发送一个HTTP的请求：

PUT /megacorp/employee/1 
{
    "name" :    "John",
    "sex" :      "Male",
    "age" :      25,
    "about" :    "I love to go rock climbing",
    "interests": [ "sports", "music" ]
}


更新，查询也是类似这 样的操作






# elasticsearch基本接口使用
elasticsearch 2.3.4-常用命令

| 功能 | 命令 |
| ------------- |:-------------------:|
| 查看集群状态 |	curl -XGET ‘http://localhost:9200/_cluster/health?pretty’|
| 列出节点信息 |	curl -XGET ‘http://localhost:9200/_cat/nodes?v’|
| 列出索引信息 |	curl -XGET ‘http://localhost:9200/_cat/indices?v’|
| 列出分片信息 |	curl -XGET ‘http://localhost:9200/_cat/shards?v’|
| 查看进程信息 |	curl -XGET ‘http://localhost:9200/_nodes/process?pretty’|
| 获取统计信息 |	curl -XGET ‘http://localhost:9200/_stats?pretty’|
| 获取统计信息-执行索引 |	curl -XGET ‘http://localhost:9200/productindex/_stats?pretty’|
| 热点线程 |	curl -XGET ‘http://localhost:9200/_nodes/hot_threads?pretty’|
| 每10sdump热点线程 |	curl -XGET ‘http://localhost:9200/_nodes/hot_threads?type=cpu&interval=10s’|
| 获取索引信息 |	curl -XGET ‘http://localhost:9200/productindex/_mapping?pretty’|
| 获取JVM信息 |	curl -XGET “http://localhost:9200/_nodes/stats/jvm?pretty”|
| 获取JVM信息-指定节点 |	curl -XGET “http://localhost:9200/_nodes/yoho.node.114/stats/jvm?pretty”|
| 删除全部索引 |	curl -XDELETE ‘http://localhost:9200/*?pretty’|
| 删除指定索引 |	curl -XDELETE ‘http://localhost:9200/storagesku?pretty’|
| 查看段信息 |	curl -XGET ‘http://localhost:9200/productindex/_segments’|
| 执行段合并 |	curl -XPOST ‘http://localhost:9200/productindex/_forcemerge?max_num_segments=1’|
| 查看线程池配置 |	curl -XGET “http://localhost:9200/_nodes/thread_pool/”|


## 查看当前es上的所有索引  
- curl -XGET "http://127.0.0.1:9200/_cat/indices"   　　 # 查看索引缩略信息
- curl -XGET "http://127.0.0.1:9200/_cat/indices?v"      　　# 查看索引详细信息

## 查看elasticsearch集群状态  
- curl -sXGET "http://127.0.0.1:9200/_cluster/health?pretty"

## 查看集群节点信息  
- curl -XGET "http://127.0.0.1:9200/_cat/nodes?v"

## 查看节点进程信息
- curl -XGET "http://127.0.0.1:9200/_nodes/process?pretty"

## 查看索引统计信息
- curl -XGET "http://127.0.0.1:9200/_stats?pretty"

## 查看指定索引统计信息
- curl -XGET "http://127.0.0.1:9200/索引名/_stats?pretty"
- curl -XGET "http://127.0.0.1:9200/logstash-2018.10.21/_stats?pretty"

## 查看热点线程
- curl -XGET "http://127.0.0.1:9200/_nodes/hot_threads?pretty"

## 查看指定索引信息
- curl -XGET "http://127.0.0.1:9200/索引名/_mapping?pretty"
- curl -XGET "http://127.0.0.1:9200/logstash-2018.10.21/_mapping?pretty"

## 查看所有节点jvm信息
- curl -XGET "http://127.0.0.1:9200/_nodes/stats/jvm?pretty" 

```java
C:\Users\lishuai29>curl -XGET "http://127.0.0.1:9200/_nodes/stats/jvm?pretty"
{
  "_nodes" : {
    "total" : 1,
    "successful" : 1,
    "failed" : 0
  },
  "cluster_name" : "my-application",
  "nodes" : {
    "5ySlLg46SWS-yMst7ap-gA" : {
      "timestamp" : 1545573783696,
      "name" : "node-1",
      "transport_address" : "100.124.17.33:9300",
      "host" : "100.124.17.33",
      "ip" : "100.124.17.33:9300",
      "roles" : [
        "master",
        "data",
        "ingest"
      ],
      "attributes" : {
        "ml.machine_memory" : "8096264192",
        "xpack.installed" : "true",
        "ml.max_open_jobs" : "20",
        "ml.enabled" : "true"
      },
      "jvm" : {
        "timestamp" : 1545573783698,
        "uptime_in_millis" : 588079,
        "mem" : {
          "heap_used_in_bytes" : 327901808,
          "heap_used_percent" : 31,
          "heap_committed_in_bytes" : 1038876672,
          "heap_max_in_bytes" : 1038876672,
          "non_heap_used_in_bytes" : 105728136,
          "non_heap_committed_in_bytes" : 114343936,
          "pools" : {
            "young" : {
              "used_in_bytes" : 130427712,
              "max_in_bytes" : 279183360,
              "peak_used_in_bytes" : 279183360,
              "peak_max_in_bytes" : 279183360
            },
            "survivor" : {
              "used_in_bytes" : 34865152,
              "max_in_bytes" : 34865152,
              "peak_used_in_bytes" : 34865152,
              "peak_max_in_bytes" : 34865152
            },
            "old" : {
              "used_in_bytes" : 162608944,
              "max_in_bytes" : 724828160,
              "peak_used_in_bytes" : 162608944,
              "peak_max_in_bytes" : 724828160
            }
          }
        },
        "threads" : {
          "count" : 46,
          "peak_count" : 57
        },
        "gc" : {
          "collectors" : {
            "young" : {
              "collection_count" : 8,
              "collection_time_in_millis" : 1282
            },
            "old" : {
              "collection_count" : 2,
              "collection_time_in_millis" : 168
            }
          }
        },
        "buffer_pools" : {
          "direct" : {
            "count" : 38,
            "used_in_bytes" : 135036121,
            "total_capacity_in_bytes" : 135036120
          },
          "mapped" : {
            "count" : 39,
            "used_in_bytes" : 89694091,
            "total_capacity_in_bytes" : 89694091
          }
        },
        "classes" : {
          "current_loaded_count" : 15250,
          "total_loaded_count" : 15250,
          "total_unloaded_count" : 0
        }
      }
    }
  }
}
```
 

## 查看指定节点jvm信息
- curl -XGET "http://127.0.0.1:9200/_nodes/节点名/stats/jvm?pretty"
- curl -XGET "http://127.0.0.1:9200/_nodes/searchnode-01/stats/jvm?pretty"


## 删除全部索引
- curl -XDELETE "http://127.0.0.1:9200/*?pretty" 

## 删除指定索引
- curl -XDELETE "http://127.0.0.1:9200/索引名?pretty"
- curl -XDELETE "http://127.0.0.1:9200/logstash-2018.10.19?pretty"
-  curl -XDELETE "http://127.0.0.1:9200/logstash-2018.10.19?pretty"
{
  "acknowledged" : true
}

删除历史索引思路 
1、取出要删除的索引名 
- curl -sXGET "http://127.0.0.1:9200/_cat/indices"|awk '{print $3}'|grep 2018.10    # 2018.10月的所有索引

2、 使用for循环调用接口删除之 
- for i in `curl -sXGET "http://127.0.0.1:9200/_cat/indices"|awk '{print $3}'|grep 2018.10`;do curl -XDELETE "http://127.0.0.1:9200/$i?pretty";done 

## 查看线程池配置
c- url -XGET "http://127.0.0.1:9200/_nodes/thread_pool/"  



