---
title: 服务器从request中解析真实访问IP地址
categories: 
- web
tags:
---

#服务器从request中解析真实访问IP地址


解析IP的工具类：[com.ls.util.IpUtil]


## 1、背景知识
###1.1、非公网IP的域名范围
tcp/ip协议中，专门保留了三个IP地址区域作为私有地址，其地址范围如下：
10.0.0.0/8：10.0.0.0～10.255.255.255 
172.16.0.0/12：172.16.0.0～172.31.255.255 
192.168.0.0/16：192.168.0.0～192.168.255.255



在服务器获取客户端的IP地址的方法是：request.getRemoteAddr()，这种方法在大部分情况下都是有效的。
但是在通过了 Apache，Nginx等反向代理软件就不能获取到客户端的真实IP地址了。
如果使用了反向代理软件，用 request.getRemoteAddr()方法获取的IP地址是：127.0.0.1或 192.168.1.110，而并不是客户端的真实IP。
经过代理以后，由于在客户端和服务之间增加了中间层，因此服务器无法直接拿到客户端的 IP，服务器端应用也无法直接通过转发请求的地址返回给客户端。
但是在转发请求的HTTP头信息中，增加了X-FORWARDED-FOR信息。用以跟踪原有的客户端 IP地址和原来客户端请求的服务器地址。

举例来说，当我们访问口碑网首页hangzhou.jsp时，其实并不是我们浏览器真正访问到了服务器上的hangzhou.jsp 文件，而是先由代理服务器Nagix去访问hagnzhou.jsp ，
代理服务器再将访问到的结果返回给我们的浏览器，因为是代理服务器去访问hangzhou.jsp的，
所以hangzhou.jsp中通过 request.getRemoteAddr()的方法获取的IP实际上是代理服务器的地址，并不是客户端的IP地址。
可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串Ｉｐ值，究竟哪个才是真正的用户端的真实IP呢？
答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
如：X-Forwarded-For：192.168.1.110， 192.168.1.120， 192.168.1.130， 192.168.1.100用户真实IP为： 192.168.1.110
除了上面的简单获取IP之外，一般的公司还会进行内网外网判断。