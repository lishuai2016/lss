
<!-- TOC -->

- [1、总体设计](#1总体设计)
    - [1.1 基础模型](#11-基础模型)
    - [1.2 架构模块](#12-架构模块)
    - [1.3 各模块概要介绍](#13-各模块概要介绍)
        - [1.3.1 Config Service](#131-config-service)
        - [1.3.2 Admin Service](#132-admin-service)
        - [1.3.3 Meta Server](#133-meta-server)
        - [1.3.4 Eureka](#134-eureka)
        - [1.3.5 Portal](#135-portal)
        - [1.3.6 Client](#136-client)
    - [1.4 E-R Diagram](#14-e-r-diagram)
        - [1.4.1 主体E-R Diagram](#141-主体e-r-diagram)
        - [1.4.2 权限相关E-R Diagram](#142-权限相关e-r-diagram)
- [2、服务端设计](#2服务端设计)
    - [2.1 配置发布后的实时推送设计](#21-配置发布后的实时推送设计)
        - [2.1.1 发送ReleaseMessage的实现方式](#211-发送releasemessage的实现方式)
        - [2.1.2 Config Service通知客户端的实现方式](#212-config-service通知客户端的实现方式)
- [3、客户端设计](#3客户端设计)
- [4、可用性考虑](#4可用性考虑)
- [参考](#参考)

<!-- /TOC -->

# 1、总体设计

![](../../pic/2020-04-08-18-01-01.png)


部署的时候需要启动三个服务：Config Service（包含meta service服务和eureka服务）、Admin Service、Portal服务。




## 1.1 基础模型

如下即是Apollo的基础模型：

![](../../pic/2020-04-08-17-23-51.png)

- 1、用户在配置中心对配置进行修改并发布；

- 2、配置中心通知Apollo客户端有配置更新；

- 3、Apollo客户端从配置中心拉取最新的配置、更新本地配置并通知到应用
basic-architecture；

## 1.2 架构模块

下图是Apollo架构模块的概览，详细说明可以参考Apollo配置中心架构剖析。 

![](../../pic/2020-04-08-17-25-05.png)

上图简要描述了Apollo的总体设计，我们可以从下往上看：

- Config Service提供配置的读取、推送等功能，服务对象是Apollo客户端

- Admin Service提供配置的修改、发布等功能，服务对象是Apollo Portal（管理界面）

- Config Service和Admin Service都是多实例、无状态部署，所以需要将自己注册到Eureka中并保持心跳

- 在Eureka之上我们架了一层Meta Server用于封装Eureka的服务发现接口

- Client通过域名访问Meta Server获取Config Service服务列表（IP+Port），而后直接通过IP+Port访问服务，同时在Client侧会做load balance、错误重试

- Portal通过域名访问Meta Server获取Admin Service服务列表（IP+Port），而后直接通过IP+Port访问服务，同时在Portal侧会做load balance、错误重试

- 为了简化部署，我们实际上会把Config Service、Eureka和Meta Server三个逻辑角色部署在同一个JVM进程中



> Why Eureka

为什么我们采用Eureka作为服务注册中心，而不是使用传统的zk、etcd呢？我大致总结了一下，有以下几方面的原因：

- 1、它提供了完整的Service Registry和Service Discovery实现
    - 首先是提供了完整的实现，并且也经受住了Netflix自己的生产环境考验，相对使用起来会比较省心。

- 2、和Spring Cloud无缝集成
    - 我们的项目本身就使用了Spring Cloud和Spring Boot，同时Spring Cloud还有一套非常完善的开源代码来整合Eureka，所以使用起来非常方便。
    - 另外，Eureka还支持在我们应用自身的容器中启动，也就是说我们的应用启动完之后，既充当了Eureka的角色，同时也是服务的提供者。这样就极大的提高了服务的可用性。
    - 这一点是我们选择Eureka而不是zk、etcd等的主要原因，为了提高配置中心的可用性和降低部署复杂度，我们需要尽可能地减少外部依赖。

- 3、Open Source
    - 最后一点是开源，由于代码是开源的，所以非常便于我们了解它的实现原理和排查问题。


## 1.3 各模块概要介绍

### 1.3.1 Config Service

- 提供配置获取接口
- 提供配置更新推送接口（基于Http long polling）
- 服务端使用Spring DeferredResult实现异步化，从而大大增加长连接数量
- 目前使用的tomcat embed默认配置是最多10000个连接（可以调整），使用了4C8G的虚拟机- 实测可以支撑10000个连接，所以满足需求（一个应用实例只会发起一个长连接）。
- 接口服务对象为Apollo客户端


### 1.3.2 Admin Service

- 提供配置管理接口
- 提供配置修改、发布等接口
- 接口服务对象为Portal


### 1.3.3 Meta Server

- Portal通过域名访问Meta Server获取Admin Service服务列表（IP+Port）

- Client通过域名访问Meta Server获取Config Service服务列表（IP+Port）

- Meta Server从Eureka获取Config Service和Admin Service的服务信息，相当于是一个Eureka Client

- 增设一个Meta Server的角色主要是为了封装服务发现的细节，对Portal和Client而言，永远通过一个Http接口获取Admin Service和Config Service的服务信息，而不需要关心背后实际的服务注册和发现组件

- Meta Server只是一个逻辑角色，在部署时和Config Service是在一个JVM进程中的，所以IP、端口和Config Service一致


### 1.3.4 Eureka

- 基于Eureka和Spring Cloud Netflix提供服务注册和发现

- Config Service和Admin Service会向Eureka注册服务，并保持心跳

- 为了简单起见，目前Eureka在部署时和Config Service是在一个JVM进程中的（通过Spring Cloud Netflix）


### 1.3.5 Portal

- 提供Web界面供用户管理配置

- 通过Meta Server获取Admin Service服务列表（IP+Port），通过IP+Port访问服务

- 在Portal侧做load balance、错误重试


### 1.3.6 Client

- Apollo提供的客户端程序，为应用提供配置获取、实时更新等功能

- 通过Meta Server获取Config Service服务列表（IP+Port），通过IP+Port访问服务

- 在Client侧做load balance、错误重试


## 1.4 E-R Diagram


### 1.4.1 主体E-R Diagram

- App：App信息

- AppNamespace：App下Namespace的元信息

- Cluster：集群信息

- Namespace：集群下的namespace

- Item：Namespace的配置，每个Item是一个key, value组合

- Release：Namespace发布的配置，每个发布包含发布时该Namespace的所有配置

- Commit：Namespace下的配置更改记录

- Audit：审计信息，记录用户在何时使用何种方式操作了哪个实体。


### 1.4.2 权限相关E-R Diagram

- User：Apollo portal用户

- UserRole：用户和角色的关系

- Role：角色

- RolePermission：角色和权限的关系

- Permission：权限；对应到具体的实体资源和操作，如修改NamespaceA的配置，发布NamespaceB的配置等。

- Consumer：第三方应用

- ConsumerToken：发给第三方应用的token

- ConsumerRole：第三方应用和角色的关系

- ConsumerAudit：第三方应用访问审计

# 2、服务端设计


## 2.1 配置发布后的实时推送设计

在配置中心中，一个重要的功能就是配置发布后实时推送到客户端。下面我们简要看一下这块是怎么设计实现的。

![](../../pic/2020-04-08-17-50-20.png)

上图简要描述了配置发布的大致过程：

- 1、用户在Portal操作配置发布
- 2、Portal调用Admin Service的接口操作发布
- 3、Admin Service发布配置后，发送ReleaseMessage给各个Config Service
- 4、Config Service收到ReleaseMessage后，通知对应的客户端


### 2.1.1 发送ReleaseMessage的实现方式

Admin Service在配置发布后，需要通知所有的Config Service有配置发布，从而Config Service可以通知对应的客户端来拉取最新的配置。

从概念上来看，这是一个典型的消息使用场景，Admin Service作为producer发出消息，各个Config Service作为consumer消费消息。通过一个消息组件（Message Queue）就能很好的实现Admin Service和Config Service的解耦。

在实现上，考虑到Apollo的实际使用场景，以及为了尽可能减少外部依赖，我们没有采用外部的消息中间件，而是通过数据库实现了一个简单的消息队列。

实现方式如下：

- 1、Admin Service在配置发布后会往ReleaseMessage表插入一条消息记录，消息内容就是配置发布的AppId+Cluster+Namespace，参见DatabaseMessageSender

- 2、Config Service有一个线程会每秒扫描一次ReleaseMessage表，看看是否有新的消息记录，参见ReleaseMessageScanner

- 3、Config Service如果发现有新的消息记录，那么就会通知到所有的消息监听器（ReleaseMessageListener），如NotificationControllerV2，消息监听器的注册过程参见ConfigServiceAutoConfiguration

- 4、NotificationControllerV2得到配置发布的AppId+Cluster+Namespace后，会通知对应的客户端

示意图如下：

![](../../pic/2020-04-08-17-53-14.png)


[问题：如果启动多个configservice如何保证只有一个实例在处理？？？]


### 2.1.2 Config Service通知客户端的实现方式

上一节中简要描述了NotificationControllerV2是如何得知有配置发布的，那NotificationControllerV2在得知有配置发布后是如何通知到客户端的呢？

实现方式如下：

- 1、客户端会发起一个Http请求到Config Service的notifications/v2接口，也就是NotificationControllerV2，参见RemoteConfigLongPollService

- 2、NotificationControllerV2不会立即返回结果，而是通过Spring DeferredResult把请求挂起

- 3、如果在60秒内没有该客户端关心的配置发布，那么会返回Http状态码304给客户端

- 4、如果有该客户端关心的配置发布，NotificationControllerV2会调用DeferredResult的setResult方法，传入有配置变化的namespace信息，同时该请求会立即返回。客户端从返回的结果中获取到配置变化的namespace后，会立即请求Config Service获取该namespace的最新配置。


# 3、客户端设计

![](../../pic/2020-04-08-17-56-15.png)

上图简要描述了Apollo客户端的实现原理：

- 1、客户端和服务端保持了一个长连接，从而能第一时间获得配置更新的推送。（通过Http Long Polling实现）

- 2、客户端还会定时从Apollo配置中心服务端拉取应用的最新配置。
    - 1、这是一个fallback机制，为了防止推送机制失效导致配置不更新
    - 2、客户端定时拉取会上报本地版本，所以一般情况下，对于定时拉取的操作，服务端都会返回304 - Not Modified
    - 3、定时频率默认为每5分钟拉取一次，客户端也可以通过在运行时指定System Property: apollo.refreshInterval来覆盖，单位为分钟。

- 3、客户端从Apollo配置中心服务端获取到应用的最新配置后，会保存在内存中

- 4、客户端会把从服务端获取到的配置在本地文件系统缓存一份，在遇到服务不可用，或网络不通的时候，依然能从本地恢复配置

- 5、应用程序可以从Apollo客户端获取最新的配置、订阅配置更新通知



> 3.1 和Spring集成的原理

Apollo除了支持API方式获取配置，也支持和Spring/Spring Boot集成，集成原理简述如下。

Spring从3.1版本开始增加了ConfigurableEnvironment和PropertySource：

- ConfigurableEnvironment:Spring的ApplicationContext会包含一个Environment（实现ConfigurableEnvironment接口）;ConfigurableEnvironment自身包含了很多个PropertySource

- PropertySource:属性源。可以理解为很多个Key - Value的属性配置


需要注意的是，PropertySource之间是有优先级顺序的，如果有一个Key在多个property source中都存在，那么在前面的property source优先。

所以对上图的例子：

- env.getProperty(“key1”) -> value1
- env.getProperty(“key2”) -> value2
- env.getProperty(“key3”) -> value4

在理解了上述原理后，Apollo和Spring/Spring Boot集成的手段就呼之欲出了：在应用启动阶段，Apollo从远端获取配置，然后组装成PropertySource并插入到第一个即可，如下图所示：


相关代码可以参考PropertySourcesProcessor






# 4、可用性考虑

![](../../pic/2020-04-08-18-03-31.png)
















# 参考

