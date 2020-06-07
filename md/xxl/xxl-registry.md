
<!-- TOC -->

- [1、服务中心系统设计](#1服务中心系统设计)
    - [1、系统架构图](#1系统架构图)
    - [2、原理解析[http方式]](#2原理解析http方式)
    - [3、跨机房（异地多活）](#3跨机房异地多活)
    - [4、一致性](#4一致性)
    - [5、数据库表设计](#5数据库表设计)
- [2、源码分析](#2源码分析)
    - [1、服务注册和发现[服务提供者和消费者]](#1服务注册和发现服务提供者和消费者)
        - [1、XxlRegistryClient](#1xxlregistryclient)
        - [2、XxlRegistryBaseClient](#2xxlregistrybaseclient)
    - [2、注册中心admin服务端](#2注册中心admin服务端)
        - [1、接收客户端的请求ApiController](#1接收客户端的请求apicontroller)
            - [1、registry服务注册](#1registry服务注册)
            - [2、remove服务移除](#2remove服务移除)
            - [3、discovery服务发现](#3discovery服务发现)
            - [4、monitor服务监控基于http长轮询](#4monitor服务监控基于http长轮询)
        - [2、XxlRpcRegistryServiceImpl核心处理逻辑](#2xxlrpcregistryserviceimpl核心处理逻辑)
            - [1、处理服务注册信息后台线程（10个）](#1处理服务注册信息后台线程10个)
            - [2、处理服务移除信息后台线程（10个）](#2处理服务移除信息后台线程10个)
            - [3、数据同步到文件并更新对应文件的http长轮询的状态](#3数据同步到文件并更新对应文件的http长轮询的状态)
            - [4、清理旧的注册数据、旧的注册文件以及通过注册表和注册文件](#4清理旧的注册数据旧的注册文件以及通过注册表和注册文件)
- [参考](#参考)

<!-- /TOC -->


# 1、服务中心系统设计

## 1、系统架构图

![](../../pic/2020-04-11-19-40-49.png)

## 2、原理解析[http方式]

内部通过广播机制，集群节点实时同步服务注册信息，确保一致。客户端借助 long pollong 实时感知服务注册信息，简洁、高效；

## 3、跨机房（异地多活）

得益于服务注册中心集群关系对等特性，集群各节点提供幂等的服务注册服务；因此，异地跨机房部署时，只需要请求本机房服务注册中心即可，实现异地多活；

举个例子：比如机房A、B 内分别部署服务注册中心集群节点。即机房A部署 a1、a2 两个服务注册中心服务节点，机房B部署 b1、b2 两个服务注册中心服务节点；

那么各机房内应用只需要请求本机房内部署的服务注册中心节点即可，不需要跨机房调用。即机房A内业务应用请求 a1、a2 获取配置、机房B内业务应用 b1、b2 获取配置。

这种跨机房部署方式实现了配置服务的 “异地多活”，拥有以下几点好处：

- 1、注册服务响应更快：注册请求本机房内搞定；
- 2、注册服务更稳定：注册请求不需要跨机房，不需要考虑复杂的网络情况，更加稳定；
- 3、容灾性：即使一个机房内服务注册中心全部宕机，仅会影响到本机房内应用加载服务，其他机房不会受到影响。


## 4、一致性

类似 Raft 方案，更轻量级、稳定；

Raft：Leader统一处理变更操作请求，一致性协议的作用具化为保证节点间操作日志副本(log replication)一致，以term作为逻辑时钟(logical clock)保证时序，节点运行相同状态机(state machine)得到一致结果。

本项目：

- Leader（统一处理分发变更请求）：DB消息表（仅变更时产生消息，消息量较小，而且消息轮训存在间隔，因此消息表压力不会太大；）；

- state machine（顺序操作日志副本并保证结果一直）：顺序消费消息，保证本地数据一致，并通过周期全量同步进一步保证一致性；


## 5、数据库表设计

```sql
CREATE database if NOT EXISTS `xxl_rpc` default character set utf8 collate utf8_general_ci;
use `xxl_rpc`;


CREATE TABLE `xxl_rpc_registry` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `biz` varchar(255) NOT NULL COMMENT '业务标识',
  `env` varchar(255) NOT NULL COMMENT '环境标识',
  `key` varchar(255) NOT NULL COMMENT '注册Key',
  `data` text NOT NULL COMMENT '注册Value有效数据',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态：0-正常、1-锁定',
  PRIMARY KEY (`id`),
  UNIQUE KEY `I_b_e_k` (`biz`,`env`,`key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `xxl_rpc_registry_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `biz` varchar(255) NOT NULL COMMENT '业务标识',
  `env` varchar(255) NOT NULL COMMENT '环境标识',
  `key` varchar(255) NOT NULL COMMENT '注册Key',
  `value` varchar(255) NOT NULL COMMENT '注册Value',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `I_b_e_k_v` (`biz`,`env`,`key`,`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `xxl_rpc_registry_message` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息类型：0-注册更新',
  `data` text NOT NULL COMMENT '消息内容',
  `addTime` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


```




# 2、源码分析

## 1、服务注册和发现[服务提供者和消费者]

### 1、XxlRegistryClient

XxlRegistryClient和XxlRegistryBaseClient区别：

前者是后者的增强，内部包含两个后台线程，服务注册和服务消费者通过http长轮询更新服务列表。功能是维持服务提供者时，不断上报自己注册信息；服务消费者的话，不断拉取自己感兴趣的服务信息；


```java

//功能是维持服务提供者时，不断上报自己注册信息；服务消费者的话，不断拉取自己感兴趣的服务信息；
public class XxlRegistryClient {
    private static Logger logger = LoggerFactory.getLogger(XxlRegistryClient.class);

    //那些需要进行注册的参数
    private volatile Set<XxlRegistryDataParamVO> registryData = new HashSet<>();
    //服务发现本地缓存
    private volatile ConcurrentMap<String, TreeSet<String>> discoveryData = new ConcurrentHashMap<>();

    private Thread registryThread;//服务注册线程
    private Thread discoveryThread;//服务发现线程
    private volatile boolean registryThreadStop = false;//注册服务后台线程运行标记


    private XxlRegistryBaseClient registryBaseClient;//具体发送注册请求的封装

    public XxlRegistryClient(String adminAddress, String accessToken, String biz, String env) {
        registryBaseClient = new XxlRegistryBaseClient(adminAddress, accessToken, biz, env);//初始化服务注册
        logger.info(">>>>>>>>>>> xxl-registry, XxlRegistryClient init .... [adminAddress={}, accessToken={}, biz={}, env={}]", adminAddress, accessToken, biz, env);

        // registry thread 服务注册后台线程。【通过间隔来不断刷新服务】
        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!registryThreadStop) {
                    try {
                        if (registryData.size() > 0) {//说明有需要进行数据

                            boolean ret = registryBaseClient.registry(new ArrayList<XxlRegistryDataParamVO>(registryData));
                            logger.debug(">>>>>>>>>>> xxl-registry, refresh registry data {}, registryData = {}", ret?"success":"fail",registryData);
                        }
                    } catch (Exception e) {
                        if (!registryThreadStop) {
                            logger.error(">>>>>>>>>>> xxl-registry, registryThread error.", e);
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(10);//心跳间隔10s
                    } catch (Exception e) {
                        if (!registryThreadStop) {
                            logger.error(">>>>>>>>>>> xxl-registry, registryThread error.", e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-registry, registryThread stoped.");
            }
        });
        registryThread.setName("xxl-registry, XxlRegistryClient registryThread.");
        registryThread.setDaemon(true);
        registryThread.start();

        // discovery thread   服务监控后台线程
        discoveryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!registryThreadStop) {

                    if (discoveryData.size() == 0) {
                        try {
                            TimeUnit.SECONDS.sleep(3);//没有需要拉取的休眠3秒
                        } catch (Exception e) {
                            if (!registryThreadStop) {
                                logger.error(">>>>>>>>>>> xxl-registry, discoveryThread error.", e);
                            }
                        }
                    } else {
                        try {
                            // monitor
                            boolean monitorRet = registryBaseClient.monitor(discoveryData.keySet());

                            // avoid fail-retry request too quick
                            if (!monitorRet){//监控到没有数据变化，休息10秒再刷新，避免拉取请求太快
                                TimeUnit.SECONDS.sleep(10);
                            }

                            // refreshDiscoveryData, all   其实他这里会一直刷新服务列表
                            refreshDiscoveryData(discoveryData.keySet());
                        } catch (Exception e) {
                            if (!registryThreadStop) {
                                logger.error(">>>>>>>>>>> xxl-registry, discoveryThread error.", e);
                            }
                        }
                    }

                }
                logger.info(">>>>>>>>>>> xxl-registry, discoveryThread stoped.");
            }
        });
        discoveryThread.setName("xxl-registry, XxlRegistryClient discoveryThread.");
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        logger.info(">>>>>>>>>>> xxl-registry, XxlRegistryClient init success.");
    }


    public void stop() {
        registryThreadStop = true;
        if (registryThread != null) {
            registryThread.interrupt();
        }
        if (discoveryThread != null) {
            discoveryThread.interrupt();
        }
    }


    /**
     * registry
     *
     * @param registryDataList
     * @return
     */
    public boolean registry(List<XxlRegistryDataParamVO> registryDataList){

        // valid
        if (registryDataList==null || registryDataList.size()==0) {
            throw new RuntimeException("xxl-registry registryDataList empty");
        }
        for (XxlRegistryDataParamVO registryParam: registryDataList) {
            if (registryParam.getKey()==null || registryParam.getKey().trim().length()<4 || registryParam.getKey().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue()==null || registryParam.getValue().trim().length()<4 || registryParam.getValue().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#value Invalid[4~255]");
            }
        }

        // cache
        registryData.addAll(registryDataList);

        // remote
        registryBaseClient.registry(registryDataList);

        return true;
    }



    /**
     * remove
     *
     * @param registryDataList
     * @return
     */
    public boolean remove(List<XxlRegistryDataParamVO> registryDataList) {
        // valid
        if (registryDataList==null || registryDataList.size()==0) {
            throw new RuntimeException("xxl-registry registryDataList empty");
        }
        for (XxlRegistryDataParamVO registryParam: registryDataList) {
            if (registryParam.getKey()==null || registryParam.getKey().trim().length()<4 || registryParam.getKey().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue()==null || registryParam.getValue().trim().length()<4 || registryParam.getValue().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#value Invalid[4~255]");
            }
        }

        // cache
        registryData.removeAll(registryDataList);

        // remote
        registryBaseClient.remove(registryDataList);

        return true;
    }


    /**
     * discovery 查找一组服务
     *
     * @param keys
     * @return
     */
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        if (keys==null || keys.size() == 0) {
            return null;
        }

        // find from local 先看本地内存缓存中是否存在
        Map<String, TreeSet<String>> registryDataTmp = new HashMap<String, TreeSet<String>>();//封装本次查询的结果
        for (String key : keys) {
            TreeSet<String> valueSet = discoveryData.get(key);
            if (valueSet != null) {
                registryDataTmp.put(key, valueSet);
            }
        }

        // not find all, find from remote 没有找到全部要找的，去远端拉取
        if (keys.size() != registryDataTmp.size()) {

            // refreshDiscoveryData, some, first use 全部刷新一遍
            refreshDiscoveryData(keys);

            // find from local
            for (String key : keys) {
                TreeSet<String> valueSet = discoveryData.get(key);
                if (valueSet != null) {
                    registryDataTmp.put(key, valueSet);
                }
            }

        }

        return registryDataTmp;
    }

    /** 请求远端的注册中心的服务列表
     * refreshDiscoveryData, some or all
     */
    private void refreshDiscoveryData(Set<String> keys){
        if (keys==null || keys.size() == 0) {
            return;
        }

        // discovery mult 用来标记是否有更新
        Map<String, TreeSet<String>> updatedData = new HashMap<>();

        Map<String, TreeSet<String>> keyValueListData = registryBaseClient.discovery(keys);
        if (keyValueListData!=null) {
            for (String keyItem: keyValueListData.keySet()) {

                // list > set
                TreeSet<String> valueSet = new TreeSet<>();
                valueSet.addAll(keyValueListData.get(keyItem));

                // valid if updated
                boolean updated = true;
                TreeSet<String> oldValSet = discoveryData.get(keyItem);
                if (oldValSet!=null && BasicJson.toJson(oldValSet).equals(BasicJson.toJson(valueSet))) {
                    updated = false;
                }

                // set
                if (updated) {
                    discoveryData.put(keyItem, valueSet);
                    updatedData.put(keyItem, valueSet);
                }

            }
        }

        if (updatedData.size() > 0) {
            logger.info(">>>>>>>>>>> xxl-registry, refresh discovery data finish, discoveryData(updated) = {}", updatedData);
        }
        logger.debug(">>>>>>>>>>> xxl-registry, refresh discovery data finish, discoveryData = {}", discoveryData);
    }

    //去注册中心查找服务列表
    public TreeSet<String> discovery(String key) {
        if (key==null) {
            return null;
        }

        Map<String, TreeSet<String>> keyValueSetTmp = discovery(new HashSet<String>(Arrays.asList(key)));
        if (keyValueSetTmp != null) {
            return keyValueSetTmp.get(key);
        }
        return null;
    }


}

```


### 2、XxlRegistryBaseClient

![](../../pic/2020-04-11-21-01-34.png)

主要方法：

- public boolean registry(List<XxlRegistryDataParamVO> registryDataList) 注册服务

- public boolean remove(List<XxlRegistryDataParamVO> registryDataList) 服务移除

- public Map<String, TreeSet<String>> discovery(Set<String> keys) 服务发现

- public boolean monitor(Set<String> keys) 服务的变更监控，基于http的长轮询实现

原理是使用http发送信息到admin，在ApiController中接收请求，把数据同步到db中。主要的逻辑XxlRegistryServiceImpl中实现。

```java
public class XxlRegistryBaseClient {//服务注册客户端
    private static Logger logger = LoggerFactory.getLogger(XxlRegistryBaseClient.class);


    private String adminAddress;
    private String accessToken;
    private String biz;
    private String env;

    private List<String> adminAddressArr;//多个服务注册列表


    public XxlRegistryBaseClient(String adminAddress, String accessToken, String biz, String env) {
        this.adminAddress = adminAddress;//服务地址列表
        this.accessToken = accessToken;
        this.biz = biz;
        this.env = env;

        // valid
        if (adminAddress==null || adminAddress.trim().length()==0) {
            throw new RuntimeException("xxl-registry adminAddress empty");
        }
        if (biz==null || biz.trim().length()<4 || biz.trim().length()>255) {
            throw new RuntimeException("xxl-registry biz empty Invalid[4~255]");
        }
        if (env==null || env.trim().length()<2 || env.trim().length()>255) {
            throw new RuntimeException("xxl-registry biz env Invalid[2~255]");
        }

        // parse
        adminAddressArr = new ArrayList<>();
        if (adminAddress.contains(",")) {
            adminAddressArr.addAll(Arrays.asList(adminAddress.split(",")));
        } else {
            adminAddressArr.add(adminAddress);
        }

    }

    /**
     * registry
     *
     * @param registryDataList
     * @return
     */
    public boolean registry(List<XxlRegistryDataParamVO> registryDataList){

        // valid
        if (registryDataList==null || registryDataList.size()==0) {
            throw new RuntimeException("xxl-registry registryDataList empty");
        }
        for (XxlRegistryDataParamVO registryParam: registryDataList) {
            if (registryParam.getKey()==null || registryParam.getKey().trim().length()<4 || registryParam.getKey().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue()==null || registryParam.getValue().trim().length()<4 || registryParam.getValue().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#value Invalid[4~255]");
            }
        }

        // pathUrl
        String pathUrl = "/api/registry";

        // param
        XxlRegistryParamVO registryParamVO = new XxlRegistryParamVO();
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setBiz(this.biz);
        registryParamVO.setEnv(this.env);
        registryParamVO.setRegistryDataList(registryDataList);

        String paramsJson = BasicJson.toJson(registryParamVO);

        // result
        Map<String, Object> respObj = requestAndValid(pathUrl, paramsJson, 5);
        return respObj!=null?true:false;
    }
    //发送请求
    private Map<String, Object> requestAndValid(String pathUrl, String requestBody, int timeout){

        for (String adminAddressUrl: adminAddressArr) {
            String finalUrl = adminAddressUrl + pathUrl;

            // request
            String responseData = BasicHttpUtil.postBody(finalUrl, requestBody, timeout);
            if (responseData == null) {
                return null;
            }

            // parse resopnse
            Map<String, Object> resopnseMap = null;
            try {
                resopnseMap = BasicJson.parseMap(responseData);
            } catch (Exception e) { }


            // valid resopnse
            if (resopnseMap==null
                    || !resopnseMap.containsKey("code")
                    || !"200".equals(String.valueOf(resopnseMap.get("code")))
                    ) {
                logger.warn("XxlRegistryBaseClient response fail, responseData={}", responseData);
                return null;
            }

            return resopnseMap;
        }


        return null;
    }

    /**
     * remove
     *
     * @param registryDataList
     * @return
     */
    public boolean remove(List<XxlRegistryDataParamVO> registryDataList) {
        // valid
        if (registryDataList==null || registryDataList.size()==0) {
            throw new RuntimeException("xxl-registry registryDataList empty");
        }
        for (XxlRegistryDataParamVO registryParam: registryDataList) {
            if (registryParam.getKey()==null || registryParam.getKey().trim().length()<4 || registryParam.getKey().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#key Invalid[4~255]");
            }
            if (registryParam.getValue()==null || registryParam.getValue().trim().length()<4 || registryParam.getValue().trim().length()>255) {
                throw new RuntimeException("xxl-registry registryDataList#value Invalid[4~255]");
            }
        }

        // pathUrl
        String pathUrl = "/api/remove";

        // param
        XxlRegistryParamVO registryParamVO = new XxlRegistryParamVO();
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setBiz(this.biz);
        registryParamVO.setEnv(this.env);
        registryParamVO.setRegistryDataList(registryDataList);

        String paramsJson = BasicJson.toJson(registryParamVO);

        // result
        Map<String, Object> respObj = requestAndValid(pathUrl, paramsJson, 5);
        return respObj!=null?true:false;
    }

    /**
     * discovery
     *
     * @param keys
     * @return
     */
    public Map<String, TreeSet<String>> discovery(Set<String> keys) {
        // valid
        if (keys==null || keys.size()==0) {
            throw new RuntimeException("xxl-registry keys empty");
        }

        // pathUrl
        String pathUrl = "/api/discovery";

        // param
        XxlRegistryParamVO registryParamVO = new XxlRegistryParamVO();
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setBiz(this.biz);
        registryParamVO.setEnv(this.env);
        registryParamVO.setKeys(new ArrayList<String>(keys));

        String paramsJson = BasicJson.toJson(registryParamVO);

        // result
        Map<String, Object> respObj = requestAndValid(pathUrl, paramsJson, 5);

        // parse
        if (respObj!=null && respObj.containsKey("data")) {
            Map<String, TreeSet<String>> data = (Map<String, TreeSet<String>>) respObj.get("data");
            return data;
        }

        return null;
    }

    /**
     * discovery
     *
     * @param keys
     * @return
     */
    public boolean monitor(Set<String> keys) {
        // valid
        if (keys==null || keys.size()==0) {
            throw new RuntimeException("xxl-registry keys empty");
        }

        // pathUrl
        String pathUrl = "/api/monitor";

        // param
        XxlRegistryParamVO registryParamVO = new XxlRegistryParamVO();
        registryParamVO.setAccessToken(this.accessToken);
        registryParamVO.setBiz(this.biz);
        registryParamVO.setEnv(this.env);
        registryParamVO.setKeys(new ArrayList<String>(keys));

        String paramsJson = BasicJson.toJson(registryParamVO);

        // result
        Map<String, Object> respObj = requestAndValid(pathUrl, paramsJson, 60);
        return respObj!=null?true:false;
    }

}

```

```java

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

            // result 获取返回结果
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




## 2、注册中心admin服务端

### 1、接收客户端的请求ApiController

#### 1、registry服务注册

把注册信息添加到XxlRpcRegistryServiceImpl内部的registryQueue即返回；


#### 2、remove服务移除

把注册信息添加到XxlRpcRegistryServiceImpl内部的removeQueue即返回；

#### 3、discovery服务发现

读取磁盘上的文件，格式如：[xxx\biz\env\key.properties]\data\applogs\xxl-rpc\registrydata\default\default\com.xxl.rpc.sample.api.DemoService.properties



#### 4、monitor服务监控基于http长轮询

初始化一个长轮询对象，3倍心跳间隔

DeferredResult deferredResult = new DeferredResult(30 * 1000L, new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor timeout, no key updated."));

Map<String, List<DeferredResult>> registryDeferredResultMap = new ConcurrentHashMap<>();//长轮询map


磁盘注册文件有变更的时候更新长轮询的状态；


### 2、XxlRpcRegistryServiceImpl核心处理逻辑

#### 1、处理服务注册信息后台线程（10个）

**从表xxl_rpc_registry_data更新数据到xxl_rpc_registry,并可能插入数到xxl_rpc_registry_message**

- 1.1、从阻塞队列registryQueue<XxlRpcRegistryData>中拉取；
- 1.2、把数据存到表xxl_rpc_registry_data中（存在即更新，不存在插入）[唯一索引：biz+env+key+value]；
- 1.3、从阻塞队列拉取的数据XxlRpcRegistryData去查询磁盘文件的数据，存在以下几种情况：
    - 1.3.1、文件存在：数据文件的状态表示禁用或者当前注册value在文件中已经存在，跳过本条信息，继续从阻塞队列拉取；
    - 1.3.2、文件不存在：进行1.4；
- 1.4、通过biz+env+key去表xxl_rpc_registry_data查询一组数据，然后去表xxl_rpc_registry查询一条数据，比较value值是否一致，不一致更新表xxl_rpc_registry中的data。并且如果第一次服务注册或者服务提供者列表有变更，会发送保存一条消息到表xxl_rpc_registry_message中，里面的data即表xxl_rpc_registry中一条记录的data。



#### 2、处理服务移除信息后台线程（10个）

**从表xxl_rpc_registry_data更新数据到xxl_rpc_registry,并可能插入数到xxl_rpc_registry_message**

- 1.1、从阻塞队列removeQueue<XxlRpcRegistryData>中拉取；
- 1.2、把数从表xxl_rpc_registry_data中（删除）[唯一索引：biz+env+key+value]；
- 1.3、从阻塞队列拉取的数据XxlRpcRegistryData去查询磁盘文件的数据，存在以下几种情况：
    - 1.3.1、文件存在：数据文件的状态表示禁用或者当前注册value在文件中已经存在，跳过本条信息，继续从阻塞队列拉取；
    - 1.3.2、文件不存在：进行1.4；
- 1.4、通过biz+env+key去表xxl_rpc_registry_data查询一组数据，然后去表xxl_rpc_registry查询一条数据，比较value值是否一致，不一致更新表xxl_rpc_registry中的data。并且如果第一次服务注册或者服务提供者列表有变更，会发送保存一条消息到表xxl_rpc_registry_message中，里面的data即表xxl_rpc_registry中一条记录的data。

对比发现，服务注册队列和服务移除队列的处理逻辑基本一致。把服务注册和移除通过阻塞队列异步处理保存或者更新到表xxl_rpc_registry_data中


#### 3、数据同步到文件并更新对应文件的http长轮询的状态

**把表xxl_rpc_registry_message中的数据持久化到磁盘文件并定期清理该表的文件**

基于消息，把数据同步到文件中，并且同时设置监控该文件的长轮询结束

- 1、读取消息表xxl_rpc_registry_message中的数据;
- 2、基于读取对象的data（即对象XxlRpcRegistry）尝试写文件，如果之前文件内容和要写的内容一样返回；不一样覆盖重新写入新数据并且更新长轮询的状态；
- 3、判断是否到了一个心跳间隔时间点（心跳间隔10秒：00,10,20,30,40,50）的时候会执行一次清理当前心跳间隔时间范围内的数据xxl_rpc_registry_message；
- 4、休眠1秒；

备注：每隔1秒进行一次同步文件判断操作；每隔10秒清理一次消息表；

broadcase new one registry-data-file     (1/1s)

clean old message   (1/10s)

#### 4、清理旧的注册数据、旧的注册文件以及通过注册表和注册文件


- 1、时间对齐，保证一个心跳间隔时间点执行一次；
- 2、清理表xxl_rpc_registry_data中更新时间超过3倍心跳间隔的数据；
- 3、对比表xxl_rpc_registry和xxl_rpc_registry_data中的注册数据是否一致，不一致更新表xxl_rpc_registry中的数据；
- 4、基于最新的xxl_rpc_registry数据尝试写文件，如果文件内容和要写的内容一样返回；不一样覆盖重新写入新数据并且更新长轮询的状态；并记录有效的注册文件
- 5、清理无效的文件；
- 6、休眠一个心跳间隔；

clean old registry-data     (1/10s)

sync total registry-data db + file      (1+N/10s)

clean old registry-data file



# 参考













