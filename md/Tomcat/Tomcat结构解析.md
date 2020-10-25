Tomcat结构解析

<!-- TOC -->

- [4、Tomcat通用组件](#4tomcat通用组件)
    - [1、Logger组件：Tomcat的日志框架及实战](#1logger组件tomcat的日志框架及实战)
    - [2、Manager组件：Tomcat的Session管理机制解析](#2manager组件tomcat的session管理机制解析)
    - [3、Cluster组件：Tomcat的集群通信原理](#3cluster组件tomcat的集群通信原理)
- [6、源码解析](#6源码解析)
- [参考](#参考)

<!-- /TOC -->



![](../../pic/2020-10-25/2020-10-25-15-24-13.png)

![](../../pic/2020-10-25/2020-10-25-15-24-56.png)


# 4、Tomcat通用组件

## 1、Logger组件：Tomcat的日志框架及实战
## 2、Manager组件：Tomcat的Session管理机制解析
## 3、Cluster组件：Tomcat的集群通信原理


# 6、源码解析


![](../../pic/2020-09-05/2020-09-05-09-44-29.png)

Tomcat的功能：

- http服务器：socket通信（tcp/ip），解析http报文
- servlet容器：多个servlet实例进行业务的处理

![](../../pic/2020-09-05/2020-09-05-09-57-22.png)


配置文件的结构：

![](../../pic/2020-09-05/2020-09-05-10-04-58.png)

备注：servlet和wrapper一一对应。


Tomcat套娃设计模式的好处：

- 一层套一层的方式，组件之间的关系清晰、便于组件生命周期的管理；
- 架构设计和xml配置文件标签结构类似；
- 便于子容器继承父容器的属性；


![](../../pic/2020-09-05/2020-09-05-10-59-05.png)


Tomcat启动过程

![](../../pic/2020-09-05/2020-09-05-11-27-55.png)

初始化阶段：

![](../../pic/2020-09-05/2020-09-05-11-58-08.png)

启动阶段：

![](../../pic/2020-09-05/2020-09-05-12-24-05.png)


![Tomcat的NIO模型](../../pic/2020-09-05/2020-09-05-12-26-39.png)

备注：acceptor线程接收请求，poller线程用来判断是否有channel数据到来。


servlet处理链路

![](../../pic/2020-09-05/2020-09-05-12-32-50.png)

备注：poller线程是处理逻辑的入口

![](../../pic/2020-09-05/2020-09-05-12-57-28.png)


mapper组件结构封装（基于请求路径找到对应的处理servlet）

![](../../pic/2020-09-05/2020-09-05-12-47-02.png)

疑问：mapper什么时候初始化的？standardservice中进行初始化的。


![架构师成长路径](../../pic/2020-09-05/2020-09-05-13-15-50.png)


# 参考

- [java-nio系列文章](http://ifeve.com/java-nio-all/)

- [devcenter-embedded-tomcat](https://github.com/heroku/devcenter-embedded-tomcat)
