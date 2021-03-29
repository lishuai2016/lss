

<!-- TOC -->

- [1、netstat](#1netstat)
    - [1、输出常见字段解析](#1输出常见字段解析)
        - [1、recv-Q 表示网络接收队列](#1recv-q-表示网络接收队列)
        - [2、send-Q 表示网路发送队列](#2send-q-表示网路发送队列)
    - [统计tcp链接各个状态的个数](#统计tcp链接各个状态的个数)
- [2、ss](#2ss)
    - [1、查看主机监听的tcp端口](#1查看主机监听的tcp端口)
    - [2、dst/src dport/sport 语法](#2dstsrc-dportsport-语法)
        - [1、匹配远程地址和端口号](#1匹配远程地址和端口号)
        - [2、匹配本地地址和端口号](#2匹配本地地址和端口号)
        - [3、将本地或者远程端口和一个数比较](#3将本地或者远程端口和一个数比较)
    - [3、通过 TCP 的状态进行过滤](#3通过-tcp-的状态进行过滤)
- [参考](#参考)

<!-- /TOC -->



# 1、netstat

```
netstat -h
usage: netstat [-vWeenNcCF] [<Af>] -r         netstat {-V|--version|-h|--help}
       netstat [-vWnNcaeol] [<Socket> ...]
       netstat { [-vWeenNac] -I[<Iface>] | [-veenNac] -i | [-cnNe] -M | -s [-6tuw] } [delay]

        -r, --route              display routing table
        -I, --interfaces=<Iface> display interface table for <Iface>
        -i, --interfaces         display interface table
        -g, --groups             display multicast group memberships
        -s, --statistics         display networking statistics (like SNMP)
        -M, --masquerade         display masqueraded connections

        -v, --verbose            be verbose
        -W, --wide               don't truncate IP addresses
        -n, --numeric            don't resolve names
        --numeric-hosts          don't resolve host names
        --numeric-ports          don't resolve port names
        --numeric-users          don't resolve user names
        -N, --symbolic           resolve hardware names
        -e, --extend             display other/more information
        -p, --programs           display PID/Program name for sockets
        -o, --timers             display timers
        -c, --continuous         continuous listing

        -l, --listening          display listening server sockets
        -a, --all                display all sockets (default: connected)
        -F, --fib                display Forwarding Information Base (default)
        -C, --cache              display routing cache instead of FIB
        -Z, --context            display SELinux security context for sockets

  <Socket>={-t|--tcp} {-u|--udp} {-U|--udplite} {-w|--raw} {-x|--unix}
           --ax25 --ipx --netrom
  <AF>=Use '-6|-4' or '-A <af>' or '--<af>'; default: inet
  List of possible address families (which support routing):
    inet (DARPA Internet) inet6 (IPv6) ax25 (AMPR AX.25)
    netrom (AMPR NET/ROM) ipx (Novell IPX) ddp (Appletalk DDP)
    x25 (CCITT X.25)

```



## 1、输出常见字段解析


```
netstat -ano | head             
Active Internet connections (servers and established)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       Timer
tcp        0      0 127.0.0.1:42222         0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 10.100.70.140:48369     0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 10.100.70.140:13942     0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 10.100.70.140:10586     0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 10.100.70.140:63227     0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 0.0.0.0:8765            0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 10.100.70.140:20126     0.0.0.0:*               LISTEN      off (0.00/0/0)
tcp        0      0 10.100.70.140:23456     0.0.0.0:*               LISTEN      off (0.00/0/0)
```

### 1、recv-Q 表示网络接收队列

表示收到的数据已经在本地接收缓冲，但是还有多少没有被进程取走，如果接收队列Recv-Q一直处于阻塞状态，可能是遭受了拒绝服务 denial-of-service 攻击。



### 2、send-Q 表示网路发送队列

对方没有收到的数据或者说没有Ack的,还是本地缓冲区。如果发送队列Send-Q不能很快的清零，可能是有应用向外发送数据包过快，或者是对方接收数据包不够快。

`这两个值通常应该为0，如果不为0可能是有问题的。packets在两个队列里都不应该有堆积状态。可接受短暂的非0情况。`














## 统计tcp链接各个状态的个数

```
[root@tt ~]# netstat -n | awk '/^tcp/ {++state[$NF]} END {for(key in state) print key,"\t",state[key]}'
CLOSE_WAIT       128
ESTABLISHED      11

```


















# 2、ss


`ss 是 Socket Statistics 的缩写`

ss 命令可以用来获取 socket 统计信息，它显示的内容和 netstat 类似。但 ss 的优势在于它能够显示更多更详细的有关 TCP 和连接状态的信息，而且比 netstat 更快。当服务器的 socket 连接数量变得非常大时，无论是使用 netstat 命令还是直接 `cat /proc/net/tcp`，执行速度都会很慢。ss 命令利用到了 TCP 协议栈中 tcp_diag。tcp_diag 是一个用于分析统计的模块，可以获得 Linux 内核中第一手的信息，因此 ss 命令的性能会好很多。


`由于性能出色且功能丰富，ss 命令可以用来替代 netsate 命令成为我们日常查看 socket 相关信息的利器。其实抛弃 netstate 命令已经是大势所趋，有的 Linux 版本默认已经不再内置 netstate 而是内置了 ss 命令。`



全部参数解析：

```
ss -help
Usage: ss [ OPTIONS ]
       ss [ OPTIONS ] [ FILTER ]
   -h, --help           this message
   -V, --version        output version information      显示版本号
   -n, --numeric        don't resolve service names     不解析服务的名称，如 "22" 端口不会显示成 "ssh"
   -r, --resolve       resolve host names               解析IP为域名
   -a, --all            display all sockets             对 TCP 协议来说，既包含监听的端口，也包含建立的连接
   -l, --listening      display listening sockets       只显示处于监听状态的端口
   -o, --options       show timer information           显示时间信息
   -e, --extended      show detailed socket information
   -m, --memory        show socket memory usage         显示 socket 使用的内存
   -p, --processes      show process using socket       显示监听端口的进程(Ubuntu 上需要 sudo)
   -i, --info           show internal TCP information   显示更多 TCP 内部的信息
   -s, --summary        show socket usage summary       显示概要信息

   -4, --ipv4          display only IP version 4 sockets
   -6, --ipv6          display only IP version 6 sockets
   -0, --packet display PACKET sockets
   -t, --tcp            display only TCP sockets         显示 TCP 协议的 sockets
   -u, --udp            display only UDP sockets         显示 UDP 协议的 sockets
   -d, --dccp           display only DCCP sockets
   -w, --raw            display only RAW sockets
   -x, --unix           display only Unix domain sockets 显示 unix domain sockets，与 -f 选项相同
   -f, --family=FAMILY display sockets of type FAMILY

   -A, --query=QUERY, --socket=QUERY
       QUERY := {all|inet|tcp|udp|raw|unix|packet|netlink}[,QUERY]

   -D, --diag=FILE      Dump raw information about TCP sockets to FILE
   -F, --filter=FILE   read filter information from FILE
       FILTER := [ state TCP-STATE ] [ EXPRESSION ]

```



## 1、查看主机监听的tcp端口

ss -tnl



## 2、dst/src dport/sport 语法

可以通过 dst/src/dport/sprot 语法来过滤连接的来源和目标，来源端口和目标端口。

### 1、匹配远程地址和端口号

```
$ ss dst 192.168.1.5
$ ss dst 192.168.119.113:http
$ ss dst 192.168.119.113:443
```

### 2、匹配本地地址和端口号

```
$ ss src 192.168.119.103
$ ss src 192.168.119.103:http
$ ss src 192.168.119.103:80
```

### 3、将本地或者远程端口和一个数比较

可以使用下面的语法做端口号的过滤：

```
$ ss dport OP PORT
$ ss sport OP PORT
```

OP 可以代表以下任意一个：

```
<=	le	小于或等于某个端口号
>=	ge	大于或等于某个端口号
==	eq	等于某个端口号
!=	ne	不等于某个端口号
>	gt	大于某个端口号
<	lt	小于某个端口号
```


## 3、通过 TCP 的状态进行过滤

ss 命令还可以通过 TCP 连接的状态进程过滤，支持的 TCP 协议中的状态有：

- established
- syn-sent
- syn-recv
- fin-wait-1
- fin-wait-2
- time-wait
- closed
- close-wait
- last-ack
- listening
- closing

除了上面的 TCP 状态，还可以使用下面这些状态：


- all	列出所有的 TCP 状态。
- connected	列出除了 listening 和 closing 之外的所有 TCP 状态。
- synchronized	列出除了 syn-sent 之外的所有 TCP 状态。
- bucket	列出 maintained 的状态，如：time-wait 和 syn-recv。
- big	列出和 bucket 相反的状态。


使用 ipv4 时的过滤语法如下：ss -4 state filter

使用 ipv6 时的过滤语法如下：ss -6 state filter

下面是一个简单的例子：ss -4 state listening




# 参考

- [linux ss 命令](https://www.cnblogs.com/sparkdev/p/8421897.html)
