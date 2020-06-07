
<!-- TOC -->

- [1、总体设计](#1总体设计)
    - [1、架构图](#1架构图)
    - [2、“配置中心” 设计](#2配置中心-设计)
    - [3、 “客户端” 设计](#3-客户端-设计)
    - [4、表结构设计](#4表结构设计)
- [2、源码分析](#2源码分析)
    - [1、client端](#1client端)
        - [1、XxlConfClient暴露给用户，方便用户使用](#1xxlconfclient暴露给用户方便用户使用)
            - [1、监听器XxlConfListener](#1监听器xxlconflistener)
            - [2、XxlConfListenerFactory](#2xxlconflistenerfactory)
        - [2、XxlConfLocalCacheConf在用户使用API和远程配置中心粘合层](#2xxlconflocalcacheconf在用户使用api和远程配置中心粘合层)
        - [3、XxlConfMirrorConf本地缓存的配置中心文件](#3xxlconfmirrorconf本地缓存的配置中心文件)
        - [4、XxlConfRemoteConf拉取配置中心的配置项](#4xxlconfremoteconf拉取配置中心的配置项)
        - [4、ConfController接收client查询和监控key](#4confcontroller接收client查询和监控key)
            - [1、find查找](#1find查找)
            - [2、monitor长轮询监控](#2monitor长轮询监控)
            - [3、add添加一个key-value](#3add添加一个key-value)
            - [4、delete删除一个key](#4delete删除一个key)
            - [5、update更一个key-value](#5update更一个key-value)
        - [5、服务端数据处理逻辑XxlConfNodeServiceImpl](#5服务端数据处理逻辑xxlconfnodeserviceimpl)
            - [后台线程1：读取xxl_conf_node_msg同步数据到文件](#后台线程1读取xxl_conf_node_msg同步数据到文件)
            - [后台线程2：同步配置主表xxl_conf_node和文件中的数据，以及清理删除key的历史文件](#后台线程2同步配置主表xxl_conf_node和文件中的数据以及清理删除key的历史文件)
        - [普通java项目](#普通java项目)
        - [spring的整合](#spring的整合)
- [参考](#参考)

<!-- /TOC -->


# 1、总体设计

## 1、架构图

![](../../pic/2020-04-12-08-50-57.png)

对比xxl-registry发现，实现原理基本一致，只不过少了服务提供者的注册，可以把用户通过页面更新配置文件当做服务注册和变更，而连接配置中心的客户端当做服务发现和监控（http长轮询）。

![](../../pic/2020-04-11-19-40-49.png)


## 2、“配置中心” 设计


配置中心由以下几个核心部分组成：

- 1、管理平台：提供一个完善强大的配置管理平台，包含：环境管理、用户管理、项目管理、配置管理等功能，全部操作通过Web界面在线完成；

- 2、管理平台DB：存储配置信息备份、配置的版本变更信息等，进一步保证数据的安全性；同时也存储”管理平台”中多个模块的底层数据；

- 3、磁盘配置数据：配置中心在每个配置中心集群节点磁盘中维护一份镜像数据，当配置新增、更新等操作时，将会广播通知并实时刷新每个集群节点磁盘中的配置数据, 最终实时通知接入方客户端；

- 4、客户端：可参考章节 “3 客户端 设计” ；

## 3、 “客户端” 设计

![](../../pic/2020-04-12-08-56-17.png)

客户端基于多层设计，核心四层设计如下:

- 1、API层：提供业务方可直接使用的上层API, 简单易用, 一行代码获取配置信息；同时保证配置的实时性、高性能;

- 2、LocalCache层：客户端的Local Cache，极大提升API层的性能，降低对配置中心集群的压力；首次加载配置、监听配置变更、底层异步周期性同步配置时，将会写入或更新缓存；

- 4、Mirror-File层：配置数据的本地快照文件，会周期性同步 “LocalCache层” 中的配置数据写入到 “Mirror-File” 中；当无法从配置中心获取配置，如配置中心宕机时，将会使用 “Mirror-File” 中的配置数据，提高系统的可用性；

- 3、Remote层：配置中心远程客户端的封装，用于加载远程配置、实时监听配置变更，提高配置时效性；

得益于客户端的多层设计，以及 LocalCache 和 Mirror-File 等特性，因此业务方可以在高QPS、高并发场景下使用XXL-CONF的客户端, 不必担心并发压力或配置中心宕机导致系统问题。


## 4、表结构设计

```sql
-- 环境表  表示系统支持的几种环境
CREATE TABLE `xxl_conf_env` (
  `env` varchar(100) NOT NULL COMMENT 'Env',
  `title` varchar(100) NOT NULL COMMENT '环境名称',
  `order` tinyint(4) NOT NULL DEFAULT '0' COMMENT '显示排序',
  PRIMARY KEY (`env`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 项目表  用户自己创建的项目
CREATE TABLE `xxl_conf_project` (
  `appname` varchar(100) NOT NULL COMMENT 'AppName',
  `title` varchar(100) NOT NULL COMMENT '项目名称',
  PRIMARY KEY (`appname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 配置项表  通过项目appname关联项目中的配置项，这里的配置项均是一个个的key-value键值对，并且每个key均以项目名称标识appname作为前缀
CREATE TABLE `xxl_conf_node` (
  `env` varchar(100) NOT NULL COMMENT 'Env',
  `key` varchar(200) NOT NULL COMMENT '配置Key',
  `appname` varchar(100) NOT NULL COMMENT '所属项目AppName',
  `title` varchar(100) NOT NULL COMMENT '配置描述',
  `value` varchar(2000) DEFAULT NULL COMMENT '配置Value',
  PRIMARY KEY (`env`,`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 配置项变更日志表 对应上面表一个env+key的变更历史记录
CREATE TABLE `xxl_conf_node_log` (
  `env` varchar(255) NOT NULL COMMENT 'Env',
  `key` varchar(200) NOT NULL COMMENT '配置Key',
  `title` varchar(100) NOT NULL COMMENT '配置描述',
  `value` varchar(2000) DEFAULT NULL COMMENT '配置Value',
  `addtime` datetime NOT NULL COMMENT '操作时间',
  `optuser` varchar(100) NOT NULL COMMENT '操作人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



-- 配置信息变更消息
CREATE TABLE `xxl_conf_node_msg` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `addtime` datetime NOT NULL,
  `env` varchar(100) NOT NULL COMMENT 'Env',
  `key` varchar(200) NOT NULL COMMENT '配置Key',
  `value` varchar(2000) DEFAULT NULL COMMENT '配置Value',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;


```


# 2、源码分析

## 1、client端

### 1、XxlConfClient暴露给用户，方便用户使用

![](../../pic/2020-04-12-11-22-48.png)

```java
public static String get(String key, String defaultVal) {//通过指定的key获取配置的value
		return XxlConfLocalCacheConf.get(key, defaultVal);
	}
```

其他的方法都是间接调用这个方法实现的。


添加监听器，针对一个key添加一个监听器
```java
public static boolean addListener(String key, XxlConfListener xxlConfListener){
		return XxlConfListenerFactory.addListener(key, xxlConfListener);
	}
```

#### 1、监听器XxlConfListener


监听器接口：
```java
public interface XxlConfListener {
    public void onChange(String key, String value) throws Exception;

}
```

#### 2、XxlConfListenerFactory

![](../../pic/2020-04-12-16-51-43.png)

通过一个ConcurrentHashMap<String, List<XxlConfListener>> keyListenerRepository = new ConcurrentHashMap<>()保存每个key对应的监听器，然后再对应key的值发送变更的时候就可以通过onChange(String key, String value)触发监听器。



### 2、XxlConfLocalCacheConf在用户使用API和远程配置中心粘合层

![](../../pic/2020-04-12-11-27-12.png)

- 1、init()
    - 1.1、通过XxlConfMirrorConf加载本地之前缓存的配置项到map中；
    - 1.2、通过这些配置项的key集合通过XxlConfRemoteConf拉取最新的值；
    - 1.3、把最新的key-value键值对缓存到本地的内容ConcurrentHashMap<String, CacheNode> localCacheRepository中；
    - 1.4、开启一个后台线程每隔3秒执行一次刷新本地缓存和文件文件；
        - 1.4.1、拿取localCacheRepository中的全部key，发起http长轮询监控；
        - 1.4.2、长轮询结束发起从远端拉取最新的配置XxlConfRemoteConf.find(keySet)；
        - 1.4.3、对比远端的value和缓存中的是否一致，不一致更新本地缓存，然后触发变更监听器；XxlConfListenerFactory.onChange(key, value)



### 3、XxlConfMirrorConf本地缓存的配置中心文件

![](../../pic/2020-04-12-12-27-39.png)

- 1、init(String mirrorfileParam)：初始化本地文件的存储路径；

- 2、Map<String, String> readConfMirror():读取文件中的内容为一个map；

- 3、writeConfMirror(Map<String, String> mirrorConfDataParam)：把数据写入到配置文件；


### 4、XxlConfRemoteConf拉取配置中心的配置项

![](../../pic/2020-04-12-11-27-51.png)

- 1、init(String adminAddress, String env, String accessToken)：初始化链接配置中心的地址；

- 2、Map<String, Object> getAndValid(String url, String requestBody, int timeout)：发送http请求；

- 3、String find(String key)：查找单个key配置的value；

- 4、Map<String, String> find(Set<String> keys)：查找一组key返回一个map；遍历配置服务端地址，通过getAndValid发送请求到/conf/find，查找该key对应的配置项，只要有一个返回值不为null，即返回；[这里会存在一个问题，如歌保证多个服务端数据的同步？？？]

- 5、boolean monitor(Set<String> keys)：基于http长轮询监控这些key的配置项是否发生变化，请求路径：/conf/monitor；




发送http请求的工具类：

```java
package com.xxl.conf.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author xuxueli 2018-11-25 00:55:31
 */
public class BasicHttpUtil {
    private static Logger logger = LoggerFactory.getLogger(BasicHttpUtil.class);

    /**
     * post
     *
     * @param url
     * @param requestBody
     * @param timeout
     * @return
     */
    public static String postBody(String url, String requestBody, int timeout) {
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // connection setting
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // do connection
            connection.connect();

            // write requestBody
            DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
            dataOutputStream.writeBytes(requestBody);
            dataOutputStream.flush();
            dataOutputStream.close();

            /*byte[] requestBodyBytes = requestBody.getBytes("UTF-8");
            connection.setRequestProperty("Content-Length", String.valueOf(requestBodyBytes.length));
            OutputStream outwritestream = connection.getOutputStream();
            outwritestream.write(requestBodyBytes);
            outwritestream.flush();
            outwritestream.close();*/

            // valid StatusCode
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new RuntimeException("http request StatusCode("+ statusCode +") invalid. for url : " + url);
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                logger.error(e2.getMessage(), e2);
            }
        }
        return null;
    }

}

```


### 4、ConfController接收client查询和监控key

备注：在服务端一个key-value会生成一个文件存储，client端查找配置项的时候是直接基于查询配置文件的。


#### 1、find查找

直接查询对应key的配置文件是否存在。路径：D:\data\applogs\xxl-conf\confdata\test\default.key01.properties

base\env\项目名称.key.properties


#### 2、monitor长轮询监控

Map<String, List<DeferredResult>> confDeferredResultMap = new ConcurrentHashMap<>();

这里的key即查找的文件名称，设置一个http长轮询，直到超时或者有变更则结束长轮询；


#### 3、add添加一个key-value

- 1、保存一条数据到xxl_conf_node；

- 2、插入一条日志到xxl_conf_node_log；

- 3、广播消息，插入数据到xxl_conf_node_msg，存在时间戳；

#### 4、delete删除一个key

- 1、删除xxl_conf_node对应key的数据；

- 2、删除一条日志xxl_conf_node_log；

- 3、广播消息，插入数据到xxl_conf_node_msg(此时的value值为null)，存在时间戳；


#### 5、update更一个key-value

- 1、更新key的value，xxl_conf_node；

- 2、插入一条日志到xxl_conf_node_log；

- 3、广播消息，插入数据到xxl_conf_node_msg（更新后的value值），存在时间戳；



### 5、服务端数据处理逻辑XxlConfNodeServiceImpl


#### 后台线程1：读取xxl_conf_node_msg同步数据到文件

brocast conf-data msg, sync to file, for "add、update、delete

- 1、查询消息表xxl_conf_node_msg中的数据；

- 2、基于一条消息判断文件是否存在以及内容值是否一致；

- 3、如果文件不存在进行创建；或者值有更新覆盖文件的值；然后更新http长轮询监控key的状态；

- 4、client拉取心跳间隔清理一次最新心跳时间间隔范围内的消息；

- 5、休眠1秒循环执行；


备注：可以看下它是基于消息表中的数据生成或者对文件进行更新；


#### 后台线程2：同步配置主表xxl_conf_node和文件中的数据，以及清理删除key的历史文件

sync total conf-data, db + file      (1+N/30s)

clean deleted conf-data file

- 1、读取表xxl_conf_node中的数据（分页查询），同步数据到对应的配置文件，有则更新，没有不做操作；

- 2、记录有效的文件名称，循环分页查询，知道表中的数据处理一遍；

- 3、清理无效文件，即db中没有但是磁盘上有的文件；

- 4、休息一个心跳间隔；


> 总结

这里先基于广播key-value变更消息生成配置文件到磁盘，然后还存在一个线程每隔心跳间隔时间同步更新配置主表和配置文件中的数据。这里先基于广播消息构建文件处于性能的考虑？？？其实只要第二个线程即可


### 普通java项目










### spring的整合
















> 1、XxlConfParamVO

对配置项的抽象

环境+配置项【其中配置项=项目名称+配置项name】，这一点感觉作者设计环境的概念的层次更高一些

xxl_conf_env+xxl_conf_project+xxl_conf_node 可以实现从环境的维度或者项目名称的维度查看配置项信息


> XxlConfRemoteConf

实现和配置中心admin端的通信，通过http的方式，主要的方法有

- com.xxl.conf.core.core.XxlConfRemoteConf.find(java.util.Set<java.lang.String>) 查找多个key

- com.xxl.conf.core.core.XxlConfRemoteConf.find(java.lang.String) 查找单个 

- com.xxl.conf.core.core.XxlConfRemoteConf.monitor 返回值为true或者false，监控多个key是否有变化，通过http长轮询实现

简化为两个接口方法/conf/find和/conf/monitor

>> /conf/find[这里读的是磁盘文件中的内容而不是db中的数据,相比直接读取db的好处？？？磁盘文件不会存在并发问题？？？]

对应admin端的接口com.xxl.conf.admin.controller.ConfController.find

处理逻辑为，读取key[项目名+属性]为名称生成的文件，该文件只有该属性的值，即一个属性生成一个磁盘文件；

>> /conf/monitor

对应admin端的接口com.xxl.conf.admin.controller.ConfController.monitor



> db和配置文件的同步

增删改时触发的操作为db+广播消息xxl_conf_node_msg【插入一条数据到数据库】

XxlConfNodeServiceImpl中有两个线程完成磁盘文件和db数据的同步。在项目启动时，运行在一个死循环中。

线程1：

1、根据xxl_conf_node_msg中的消息通过com.xxl.conf.admin.service.impl.XxlConfNodeServiceImpl.setFileConfData完成磁盘文件数的同步+通知客户端http长轮询；

2、心跳间隔删除一次xxl_conf_node_msg中的数据，当前时间>插入时间+心跳间隔之前的数据。

3、一次循环过后睡1秒；

线程2：

1、数据配置表xxl_conf_node的中配置的数据全量更新和替换

2、删除db中已经删除的配置项磁盘文件；




> 初始化流程

XxlConfFactory
    XxlConfBaseFactory.init(adminAddress, env, accessToken, mirrorfile);
        XxlConfRemoteConf.init(adminAddress, env, accessToken);	// init remote util
		XxlConfMirrorConf.init(mirrorfile);			// init mirror util
		XxlConfLocalCacheConf.init();				// init cache + thread, cycle refresh + monitor
		XxlConfListenerFactory.addListener(null, new BeanRefreshXxlConfListener());   



在这里XxlConfLocalCacheConf.init()会启动一个后台进行进行com.xxl.conf.core.core.XxlConfLocalCacheConf.refreshCacheAndMirror
监控管理端的配置，http轮询，有更新时进行更新操作，写本地的磁盘文件



# 参考

- [文档](https://www.xuxueli.com/xxl-conf)































