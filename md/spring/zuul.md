
<!-- TOC -->

- [1、网关的必要性](#1网关的必要性)
    - [1、动态路由](#1动态路由)
    - [2、请求监控](#2请求监控)
    - [3、认证鉴权](#3认证鉴权)
    - [4、压力测试](#4压力测试)
    - [5、灰度发布](#5灰度发布)
- [2、Zuul](#2zuul)
    - [1、过滤器](#1过滤器)
    - [2、请求生命周期](#2请求生命周期)
- [3、Zuul 的使用](#3zuul-的使用)
    - [1、Zuul 的使用](#1zuul-的使用)
    - [2、Zuul 路由方式](#2zuul-路由方式)
    - [3、Zuul 自定义过滤器](#3zuul-自定义过滤器)
    - [4、Zuul 容错与回退](#4zuul-容错与回退)
    - [5、Zuul 小经验分享](#5zuul-小经验分享)
        - [1、内置端点](#1内置端点)
        - [2、文件上传](#2文件上传)
        - [3、请求响应输出](#3请求响应输出)
        - [4、Zuul Debug](#4zuul-debug)
        - [5、跨域配置](#5跨域配置)
        - [6、关闭 zuul 全局路由转发](#6关闭-zuul-全局路由转发)
        - [7、动态过滤器](#7动态过滤器)
    - [6、Zuul 控制路由实例选择](#6zuul-控制路由实例选择)
- [9、zuul应用实例demo](#9zuul应用实例demo)
    - [1、springmvc的HandlerInterceptor拦截请求](#1springmvc的handlerinterceptor拦截请求)
    - [2、对请求参数的改写，下放到后端服务](#2对请求参数的改写下放到后端服务)
    - [3、对返回结果进行改写后返回请求端](#3对返回结果进行改写后返回请求端)

<!-- /TOC -->


首先，我们来了解网关是什么？其实，API 网关是对外提供服务的一个入口，并且隐藏了内部架构的实现，是微服务架构中必不可少的一个组件。API 网关可以为我们管理大量的 API 接口，负责对接客户、协议适配、安全认证、路由转发、流量限制、日志监控、防止爬虫、灰度发布等功能。

 
API 网关也是随着架构演进衍生出的一个框架，是最简单的单体应用架构，所有的逻辑都在一个应用中，应用本身就是请求的入口，根本不需要在应用前面再加一个网关来做转发。当架构从单体应用演进成微服务架构时，网关的重要性就体现出来了。如果没有网关统一请求入口，客户端的请求会直接落到后端的各个服务中，无法集中统一管理。


# 1、网关的必要性

网关是所有请求的入口，承载了所有的流量，始终战斗在最前线，高并发、高可用都是网关需要面对的难题，网关的重要性可想而知。使用网关，还可以扩展出很多实用高级的功能，我总结了几个比较常见的给大家分析下，主要有动态路由、请求监控、认证鉴权、压力测试、灰度发布。


## 1、动态路由

动态路由是动态的将客户端的请求路由到后端不同的服务上，如果没有网关去做统一的路由，那么客户端就需要关注后端 N 个服务。

![](../../pic/2020-11-08/2020-11-08-19-30-01.png)

左边的图没有使用网关，客户端调用服务时就需要访问服务各自的接口，如客户端调用 A 服务的接口就需要请求 a.com，而对需要访问服务的客户端来说访问流程越简单越好，现在需要关注多个 API 提供方，无疑提高了访问的复杂度。


右边的图使用了网关，使用网关后，客户端只需要关注网关的地址，也就是 gateway.com。不再需要关注多个 API 提供方，由网关统一路由到后端的具体服务中，这其实跟我之前讲的集中式负载均衡的概念类似，这样的好处是对客户端来说访问服务的流程简单了，关注的点少了。


另外一个好处就是可以在后端做 API 聚合操作，比如客户端要展示一个商品详情，里面有商品基本信息、库存信息等，如果没有聚合，就需要调用基本信息的接口，然后再调用库存信息的接口，如果做了聚合，客户端只需要调用一个接口，这个接口中包含了所有需要的信息，减少了前后端交互的次数，提升了用户的体验。


## 2、请求监控

请求监控可以对整个系统的请求进行监控，详细地记录请求响应日志，可以实时统计当前系统的访问量及监控状态。

![](../../pic/2020-11-08/2020-11-08-19-31-52.png)

如果没有使用网关的话，记录请求信息需要在各个服务中去做。当网关出现在我们的架构中后，所有客户端的请求都会经过网关来做路由分发，入口统一了，很多事情也就好处理了，我们只需要在网关中统一进行请求信息的记录，就可以基于这些记录做实时的数据分析，比如并发调用量，根据数据分析决定是否要动态限流，分析是否有爬虫请求等多维数据结果。给业务方提供正确实时的决策信息，是非常有价值的。

## 3、认证鉴权

认证鉴权可以对每一个访问请求做认证，拒绝非法请求，保护后端的服务。微服务架构下，如果没有使用网关，那么客户端需要直接跟多个服务进行交互，当请求到达对应的服务时，就必须验证当前的请求有没有登录，有没有权限访问。访问 A 服务需要验证一次，访问 B 服务也需要验证一次，每个服务都要做重复的工作。

当我们使用网关后，就可以在网关中做统一的验证逻辑了，唯一要做的工作就是在网关验证完成后，需要将用户信息传递给后端服务，后端服务默认相信当前的请求已经在网关中通过验证，它不会再去做验证的逻辑，但是当前请求对应的用户信息要告诉后端服务，可以将用户信息通过 HTTP 请求头传递给路由的后端服务。

## 4、压力测试

压力测试是一项很重要的工作，像一些电商公司需要模拟更多真实的用户并发量来保证大促时系统的稳定，通过 Zuul 可以动态地将测试请求转发到后端服务的集群中，还可以识别测试流量和真实流量，用来做一些特殊处理。

![](../../pic/2020-11-08/2020-11-08-19-34-11.png)

对于测试请求，可以在请求头中添加标识，让网关能够识别这是一个测试请求，当识别到测试请求后，根据对应的规则进行路由，这里可以用配置中心存储规则，测试请求路由到测试服务，测试服务会有单独的测试数据库，这样测试的请求就不会影响到正式的服务和数据库了。

## 5、灰度发布

灰度发布可以保障整体系统的稳定性，在初始灰度的时候就可以及时发现、调整问题，以降低影响范围。

![](../../pic/2020-11-08/2020-11-08-19-34-57.png)

当需要发布新版本的时候，不会立即将老的服务停止，去发布新的服务。而是先发布新版本的服务，比如之前的版本是 1.0，那么现在发布的版本就是 1.1，发布后，需要通过测试请求对 1.1 版本的服务进行测试，如果没发现什么问题，就可以将正常的请求转发过来了。如果测试中发现问题，可以直接停掉 1.1 版本的服务，就算不停掉也没关系，不会影响到正常用户的使用。


# 2、Zuul

Zuul 也是 Netflix OSS 中的一员，是一个基于 JVM 路由和服务端的负载均衡器。提供了路由、监控、弹性、安全等服务。Zuul 能够与 Eureka、Ribbon、Hystrix 等组件配合使用。

Zuul 于 2012 年开源，目前在 GitHub 上有超过 8000 多颗星的关注，经过 Netflix 在生产环境中长期的使用和改进，Zuul 的稳定性非常好。


客户端通过负载均衡器把请求路由到 Zuul 网关上，然后 Zuul 网关负责把请求路由到后端具体的服务上。


## 1、过滤器

过滤器是 Zuul 中最核心的内容，过滤器可以对请求或响应结果进行处理，Zuul 还支持动态加载、编译、运行这些过滤器，过滤器的使用方式是采取责任链的方式进行处理，过滤器之间通过 RequestContext 来传递上下文，通过过滤器可以扩展很多高级功能。Zuul 中的过滤器总共有 4 种类型，每种类型都有对应的使用场景。

- pre 过滤器：可以在请求被路由之前调用。适用于身份认证的场景，认证通过后再继续执行下面的流程。

- route 过滤器：在路由请求时被调用。适用于灰度发布的场景，在将要路由的时候可以做一些自定义的逻辑。

- post 过滤器：在 route 和 error 过滤器之后被调用。这种过滤器将请求路由到达具体的服务之后执行。适用于添加响应头，记录响应日志等应用场景。

- error 过滤器：处理请求发生错误时被调用。在执行过程中发送错误时会进入 error 过滤器，可以用来统一记录错误信息。

## 2、请求生命周期

当一个请求进来时，会先进入 pre 过滤器，在 pre 过滤器执行完后，接着就到了 routing 过滤器中，开始路由到具体的服务中，路由完成后，接着就到了 post 过滤器中，然后将请求结果返回给客户端。如果在这个过程中出现异常，则会进入 error 过滤器中，这就是请求在整个 Zuul 中的生命周期。

![](../../pic/2020-11-08/2020-11-08-19-38-30.png)

对应的源码在 ZuulServlet 中，我们可以打开 ZuulServlet 的源码，service 方法中就是执行过滤器的逻辑，首先是 preRoute 方法，也就是执行 pre 过滤器，如果异常了就会执行 error 过滤器和 post 过滤器，接着就是 routing 过滤器，这就是整个过滤器执行流程对应的源码部分。



# 3、Zuul 的使用

## 1、Zuul 的使用

我们快速体验下 Zuul 的路由功能，首先在 pom 中增加 spring-cloud-starter-netflix-zuul 的依赖，加完依赖之后，在启动类上增加 @EnableZuulProxy 注解。然后打开我们的配置文件，配置一个固定的路由 Zuul，.routes 是固定的前缀，yinjihuan 是命名，path 是映射的 URL 地址，URL 是要路由的地址。启动项目在浏览器中访问http://localhost:8087/cxytiandi，可以看到打开了 cxytiandi.com 的主页。这就是一个简单的路由示列。

```
# 固定路由
zuul.routes.yinjihuan.path=/cxytiandi/**
zuul.routes.yinjihuan.url=http://cxytiandi.com
```

## 2、Zuul 路由方式

对于 Zuul 的路由方式，我总结了四种，每种方式都有它的使用场景，下面来介绍下这四种方式：

- 第一种路由方式是 URL 路由，不具备负载均衡。这里的不具备负载均衡是指 Zuul 中转发请求的时候不具备负载均衡，但如果配置的是域名的话，就相当于集中式的负载均衡了。这种方式在 Zuul 的使用中已经讲过了，这边就不再重复讲解了。

- 第二种路由方式是配置多个 URL 负载均衡，本质上还是使用了 Ribbon 来进行负载均衡，所以我们需要通过配置 Ribbon 的 servers 来做负载。将配置的 URL 改成 serviceId，serviceId 也就是 Ribbon 的名称，通过 serviceId.ribbon.listOfServers 配置多个 URL 地址，这样 URL 路由也具备了客户端负载均衡的能力了。

- 第三种路由方式是为所有请求加前缀，这种方式适合需要对 API 有一个统一的前缀的场景，比如我们想统一的前缀为 openapi，那么就需要增加 zuul.prefix=/openapi，这样在访问时，请求地址也需要加上 openapi，比如之前的是 cxytiandi.com/blogs，添加完前缀就变成了cxytiandi.com/openapi/blogs。

- 第四种方式是基于 Eureka 代理服务，我们使用网关，90% 的需求都是转发内部服务，这些服务都会注册到 Eureka 中，虽然可以手动配置 Ribbon 的 servers 列表，但这种方式需要人为干预。所以我们需要在 Zuul 中集成 Eureka，在路由转发时可以转发到 Eureka 中注册的服务上，这样就很方便了，不需要我们去关心服务的上下线。


要使用 Eureka 就必须先将 Eureka 集成到 Zuul 中，还是老套路，第一步在 pom 中增加 Eureka Client 的依赖，然后在配置文件中配置 eureka server 的地址，配置完成后，就可以开始配置路由规则了。

路由规则的 path 还是之前的，不用变化，需要改变的是将 URL 要改成 serviceId，这个 serviceId 是注册到 Eureka 中的服务名，也就是这个路由会转发到你配置的 serviceId 的这个服务上，至于服务实例的选择还是用的 Ribbon。


在 Zuul 中默认会为所有服务都进行路由转发，也就是我们集成了 Eureka 之后，可以不用配置路由规则，使用默认的规则来访问后端的服务，这个规则就是网关地址 + 服务名 + 服务接口地址。


比如我们这边网关是 localhost:8087，然后访问的服务是 feign-user-service，接口地址是user/get。最后完整的地址就是 http://localhost:8087/feign-user-service/user/get?id=1，访问下可以看到返回的结果。

## 3、Zuul 自定义过滤器

过滤器是 Zuul 中的核心内容，很多高级的扩展都需要自定义过滤器来实现，在 Zuul 中自定义一个过滤器只需要继承 ZuulFilter，然后重写 ZuulFilter 的四个方法即可。

![](../../pic/2020-11-08/2020-11-08-19-45-42.png)


首先来重写 shouldFilte 方法，shouldFilter 方法决定了是否执行该过滤器，true 为执行，false 为不执行，这个也可以利用配置中心来做，达到动态的开启或关闭过滤器。


filterType 方法是要返回过滤器的类型，可选值有 pre、route、post、error 四种类型。过滤器有多个，多个过滤器执行肯定有先后顺序，那么我们可以通过 filterOrder 来指定过滤器的执行顺序，数字越小，优先级越高。


最重要的就是 run 方法了，所有的业务逻辑都写在 run 方法中。定义完后，只需要将过滤器交由 Spring 管理即可生效。在第一个过滤器中如果需要传递一些数据给后面的过滤器，我们可以获取 RequestContext，然后调用 set 方法进行值的设置，在后面的过滤器中还是通过 RequestContext 的 get 方法获取对应的值。


还有一个常见的需求就是进行请求的拦截，比如我们在网关中对请求的 Token 进行合法性的验证，如果不合法，通过 RequestContext 的 setSendZuulResponse 告诉 Zuul 不需要将当前请求转发到后端的服务，然后通过 setResponseBody 返回固定的数据给客户端。


如果不想执行后面的过滤器，可以在所有过滤器的 shouldFilter 方法中通过获取一个值来决定是否要执行该过滤器，然后在验证不合法后，往 RequestContext 中设置一个值告诉后面的过滤器不需要执行了。


## 4、Zuul 容错与回退

Spring Cloud 中，Zuul 默认整合了 Hystrix，当后端服务异常时可以为 Zuul 添加回退功能，返回默认的数据给客户端。我们需要在 Zuul 中实现 FallbackProvider 这个类来实现回退逻辑。


需要实现 FallbackProvider 中的 getRoute 方法，告诉 Zuul 它是负责哪个路由的熔断，如果想全局进行处理可以返回 * 号表示所有。而 fallbackResponse 方法则是告诉 Zuul 断路出现时，需要返回给客户端什么数据。可以指定 HttpStatus、HttpHeaders 等信息，最重要的就是返回的数据在 getBody 中进行设置。

 
Zuul 中默认采用信号量隔离机制，如果想要换成线程，需要配置 zuul.ribbon-isolation-strategy=THREAD，配置后所有的路由对应的 Command 都在一个线程池中执行，这样其实达不到隔离的效果，所以我们需要增加一个 zuul.thread-pool.use-separate-thread-pools 的配置，让每个路由都使用独立的线程池，zuul.thread-pool.thread-pool-key-prefix 可以为线程池配置对应的前缀，方便调试。


## 5、Zuul 小经验分享

### 1、内置端点

当 @EnableZuulProxy 与 Spring Boot Actuator 配合使用时，Zuul 会暴露一个路由管理端点 /routes。借助这个端点，可以方便、直观地查看以及管理 Zuul 的路由。

在浏览器中访问 /actuator/routes 端点，可以看到当前网关中的路由信息，映射的 URL 和对应的转发地址或者是转发的服务 ID。

除了 routes 端点可以查看路由信息，还有一个 /filters 端点可以查看 Zuul 中所有过滤器的信息。可以清楚的了解 Zuul 中目前有哪些过滤器，哪些被禁用了等详细信息。

### 2、文件上传

通过 Zuul 上传文件，超过 1M 的文件都会上传失败，我们需要配置最大可以上传文件的大小。

配置 max-file-size和max-request-size，需要注意的是 Zuul 中要配置，然后你最终接收这个文件的服务也要配置。


第二种解决办法是在网关的请求地址前面加上 /zuul，就可以绕过 Spring DispatcherServlet 上传大文件。通过加上 /zuul 前缀可以让 Zuul 服务不用配置文件上传大文件，但是接收文件的服务还是需要配置文件上传大小，否则文件还是会上传失败。


在上传大文件的时候，时间会比较长，这个时候需要设置合理的超时时间来避免超时，可以配置 Ribbon 的 ConnectTimeout 和 ReadTimeout。如果 Zuul 的 Hystrix 隔离模式为线程的话需要设置 Hystrix 的超时时间。

### 3、请求响应输出

系统在生产环境出现问题时，排查问题最好的方式就是查看日志了，日志的记录尽量详细，这样你才能快速定位问题。下面带你学习如何在 Zuul 中输出请求响应的信息来辅助解决一些问题。


熟悉 Zuul 的朋友都知道，Zuul 中有 4 种类型过滤器，每种都有特定的使用场景，要想记录响应数据，必须是在请求路由到了具体的服务之后，返回了才有数据，这种需求就适合用 post 过滤器来实现。在过滤器中获取请求的信息和响应的数据进行输出，这个操作需要有开关可以动态调整，否则在高并发下会影响性能。

具体实现可以参考：

http://cxytiandi.com/blog/detail/20529

http://cxytiandi.com/blog/detail/20343




### 4、Zuul Debug

Zuul 中自带了一个 DebugFilter，会将执行过程中的一些信息记录起来，方便调试和问题排查，我们可以通过配置 zuul.include-debug-header=true 来开启这个 Debug 模式，然后在访问请求的时候，在后面追加一个 debug=true 的参数告诉 Zuul 当前请求的调试信息需要通过响应头进行输出，这样在这个请求的响应头中就有了 Debug 相关的信息。


### 5、跨域配置

![](../../pic/2020-11-08/2020-11-08-20-28-06.png)

网关是负责跟外部对接的一个桥梁，外部有 APP、网页应用等，如果是网页应用需要调用网关的 API，不在同一个域名下会存在跨域的问题，可以在 Zuul 中增加跨域的配置，允许跨域请求。


### 6、关闭 zuul 全局路由转发


Zuul 中会默认为 Eureka 中所有的服务都进行路由转发，这种方式确实很方便，相当于我们不需要配置路由规则就可以直接使用默认的服务名称加 API 的 URI 方式去访问，不好的点在于 Eureka 中的服务是全量的，我们的某个网关对外提供服务，并不需要将所有的 API 都暴露给外部，但是默认的映射会让外部可以访问到，所以我们需要将这个默认的路由转发关闭，通过配置 zuul.ignored-services=* 关闭。

关闭后 Zuul 就只会根据我们配置的路由规则去转发对应的请求了，这样就避免了不想暴露的服务也被外部调用。


再细一点可能会遇到在某个服务中，有的 API 想暴露，有的不想暴露，这种需求有两种方式，一种是增加一个聚合层，通过聚合层来暴露对应的 API，对于多个 API 需要聚合的场景，使用聚合层是非常合理的，但是对于简单的 API，并不需要聚合数据，再加聚合层的话无疑是多了一次转发，影响了性能。

在 Zuul 中有个配置可以忽略指定的 URI 地址，可以通过配置 zuul.ignoredPatterns 来忽略你不想暴露的 API。

### 7、动态过滤器

Zuul 支持过滤器动态修改加载功能，Filter 需要使用 Groovy 编写才可以被动态加载。动态加载的实现原理是定期扫描存放 Groovy Filter 文件的目录，如果发现有新 Groovy Filter 文件或者 Groovy Filter 源码有改动，那么就会对 Groovy 文件进行编译加载。

![](../../pic/2020-11-08/2020-11-08-20-34-35.png)

要实现动态过滤器，首先需要在项目中增加 Groovy 的依赖，然后在项目启动后设置 Groovy 的动态加载任务，这样就会定时的动态加载指定目录的 Groovy 文件了。

然后编写一个简单的 Groovy Filter，在 run 中输出一句话即可，然后访问下网关的接口，可以看到这个动态的过滤器生效了。有了动态过滤器的功能，我们就可以在不用停止服务的情况下，去支持需求的变化。






## 6、Zuul 控制路由实例选择

前面在讲到网关的必要性时，提到了基于网关去做灰度发布，去做压力测试等高级扩展功能，在后面的课时中我会单独介绍使用目前已经开源的组件来实现灰度发布的功能，在这里就不做过多的介绍，只是想让大家了解下如果要实现这些扩展能力，我们需要做哪些工作？最重要的是要了解核心原理，当你了解了核心原理后，也就相当于有了深厚的内力，怎么表现出来就只是表面上的招式而已。


我们来看灰度发布，首先我们需要知道当前请求的目的地是什么。也就是当前请求是正常请求还是一个灰度请求，如果是灰度请求，那么这个请求想要访问的版本是什么？或者想要访问指定的哪个服务实例等。


然后我们需要根据这个请求带来的信息，从 Eureka 中选择一个符合要求的实例信息给 Zuul 进行转发，总体需求就是这两点，那么我们该用什么技术呢？

Zuul 中也是集成了 Ribbon 来做负载均衡的，Ribbon 中又提供了自定义算法策略来让我们控制服务实例的选择，技术方案很明显我们需要自定义 Ribbon 的算法策略来实现这个需求。


创建一个自定义策略类，这边直接采用了 RoundRobinRule 类，目的是为了在选取不出对应的服务实例时，可以直接使用 RoundRobinRule 的策略作为默认值。在 choose 方法中就是我们的主要逻辑了，首先会通过 RequestContext 获取 request 并转换成 HttpServletRequest，因为这样才能拿到请求头的信息，或者可以在 Zuul 的过滤器中获取，然后设置到 RequestContext 中。


这边需要注意的是获取 request 只能在信号量隔离下使用，线程隔离下 ThreadLocal 无法使用，会触发空指针异常。解决方案大家可以参考我的这篇文章，在 Hystrix 课时中也讲到过这个问题。除了文章中介绍的解决方案，还有其他的方案也可以实现这个需求，这个在后面专门讲灰度发布的时候给大家分析如何跨线程池传递数据到 Hystrix 中。

http://cxytiandi.com/blog/detail/18782


接着就是获取请求头 header 的值，获取到后就从 Eureka 中获取服务信息，然后对比 Metadata中的 version 是否一致，如果一致那就选取这个服务返回。Metadata 中的 version 需要我们在服务启动的时候指定，通过 eureka.instance.metadata-map.version=1 配置。


这里只是做了一个简单的示列，让大家明白如何去控制服务实例的选择，明白这个原理后，你就可以根据自己的需求去实现想要的效果了，比如传递进来的是 IP + 端口的参数进行选择，也可以基于配置中心做全局动态配置等。




# 9、zuul应用实例demo

## 1、springmvc的HandlerInterceptor拦截请求

```java
@Configuration
public class ZuulHandlerBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {
    
    private final AuthenticationInterceptor authenticationInterceptor;

    public ZuulHandlerBeanPostProcessor(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
        //在这里设置拦截器
        if (bean instanceof ZuulHandlerMapping) {
            ZuulHandlerMapping zuulHandlerMapping = (ZuulHandlerMapping) bean;
            zuulHandlerMapping.setInterceptors(authenticationInterceptor);
        }
        return super.postProcessAfterInstantiation(bean, beanName);
    }
}
```

备注：zuul留的接口InstantiationAwareBeanPostProcessorAdapter可以把拦截器加进去，在filter执行前执行，比如AuthenticationInterceptor进行请求token合法性的校验等；`其实对token的校验写一个zuul的filter也行，在有些场景下，这个拦截器在服务中也用到，这里可以复用同一个`

## 2、对请求参数的改写，下放到后端服务

流程：zuul服务和另一个服务注册到注册中心上，带有加密过得参数的请求url经过zuul处理参数解密之后发给后续微服务。

首先获取到request，但是在request中只有getParameter（）而没有setParameter（）方法，所以直接修改url参数不可行，另外在request中虽然可以setAttribute（），但是可能由于作用域（request）的不同，一台服务器才能getAttribute（）出来，在这里设置的Attribute在后续的微服务中是获取不到的，因此必须考虑另外的方式：get方法和其他方法处理方式不同，post和put需重写HttpServletRequestWrapper，即获取请求的输入流，重写json参数，传入重写构造上下文中的request中。ctx.setRequest(new HttpServletRequestWrapper(request) {})，这种方式可重新构造上下文中的request


```java
@Component
public class TokenLoginFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(TokenLoginFilter.class);

    @Autowired
    public TokenUtils tokenUtils;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 从token中解析出用户的登录信息
     * @return
     */
    @Override
    public Object run() {

        //获取到request
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        //获取请求头中的token信息
        String token = request.getHeader(Constants.TOKEN_HEADER_STRING);
        //解析token获取用户名
        String username = tokenUtils.getUsername(token);
        try {
            //获取请求方法
            String method = request.getMethod();
            log.info(String.format("请求的方法和url: %s >>> %s", method, request.getRequestURL().toString()));

            // get方法和post、put方法处理方式不同
            if ("GET".equals(method)) {
                // 关键步骤，一定要get一下,下面才能取到值requestQueryParams
                request.getParameterMap();
                Map<String, List<String>> requestQueryParams = ctx.getRequestQueryParams();
                if (requestQueryParams == null) {
                    requestQueryParams = new HashMap<>();
                }
                List<String> arrayList = new ArrayList<>();
                arrayList.add(username);
                requestQueryParams.put("erp", arrayList);
                ctx.setRequestQueryParams(requestQueryParams);

            } else if ("POST".equals(method) || "PUT".equals(method)) {// post和put需重写HttpServletRequestWrapper
                //获取请求的输入流
                InputStream in = request.getInputStream();
                String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
                // 如果body为空初始化为空json
                if (StringUtils.isBlank(body)) {
                    body = "{}";
                }
                log.info("请求体内容body：{}",body);
                // 转化成json
                JSONObject json = JSONObject.parseObject(body);

                json.put("username", username);
                String newBody = json.toString();
                log.info("newBody" + newBody);
                final byte[] reqBodyBytes = newBody.getBytes();
                // 重写上下文的HttpServletRequestWrapper
                ctx.setRequest(new HttpServletRequestWrapper(request) {
                    @Override
                    public ServletInputStream getInputStream() throws IOException {
                        return new ServletInputStreamWrapper(reqBodyBytes);
                    }
                    @Override
                    public int getContentLength() {
                        return reqBodyBytes.length;
                    }

                    @Override
                    public long getContentLengthLong() {
                        return reqBodyBytes.length;
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

```



## 3、对返回结果进行改写后返回请求端

功能：对token的刷新


```java

@Component
@Slf4j
public class ResultRefreshTokenFilter extends ZuulFilter {

    @Autowired
    public TokenUtils tokenUtils;

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = getCurrentContext();

        HttpServletRequest request = context.getRequest();
        String refreshToken = new ResultMap(tokenUtils).refreshToken(request);

        try {
            InputStream stream = context.getResponseDataStream();
            String body = StreamUtils.copyToString(stream, Charset.forName("UTF-8"));
            log.info("改写前的结果：{}",body);
            //对body进行改写
            if (StringUtils.isNotBlank(body)) {
                //Result
                JSONObject jsonObject = JSONObject.parseObject(body);
                jsonObject.put("token",refreshToken);
                body = jsonObject.toJSONString();
            }
            log.info("改写后的结果：{}",body);
            //context.setResponseBody(body); //会出现tokenfilter拦截不通过返回不了结果
            context.setResponseDataStream(new ByteArrayInputStream(body.getBytes()));
        } catch (IOException e) {
            log.error("结果改写异常");
        }
        return null;
    }

    @Override
    public String filterType() {
        return FilterConstants.POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.SEND_RESPONSE_FILTER_ORDER - 2;
    }

}

```



https://www.cnblogs.com/jing99/p/11696192.html