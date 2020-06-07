
# JAX-RS


The full name of JAX-RS is Jakarta RESTful Web Services that provides a Java API for RESTful Web Services over the HTTP protocol。

JAX-RS API有两个主要实现。它们分别如下：

- 1、Jersey

- 2、RESTEasy

注意这里关注点是RESTful Web Services。



## 1、注解说明

AX-RS提供了一些标注将一个资源类，一个POJOJava类，封装为Web资源。标注包括：
- @Path，JAX-RS有“根”资源（标记为@Path）和子资源的概念。@Path注释被用来描述根资源、子资源方法或子资源的位置。value 值可以包含文本字符、变量或具有定制正则表达式的变量。


- @GET，@PUT，@POST，@DELETE，标注方法是用的HTTP请求的类型.您可以使用它们来绑定根资源或子资源内的 Java 方法与 HTTP 请求方法。HTTP GET 请求被映射到由 @GET 注释的方法；HTTP POST 请求被映射到由 @POST 注释的方法，以此类推。用户可能还需要通过使用 @HttpMethod 注释定义其自己的定制 HTTP 请求方法指示符。


- @Produces，注释代表的是一个资源可以返回的 MIME 类型。这些注释均可在资源、资源方法、子资源方法、子资源定位器或子资源内找到。


- @Consumes，注释代表的是一个资源可以接受的 MIME 类型。


- @PathParam，@QueryParam，@HeaderParam，@CookieParam，@MatrixParam，@FormParam,分别标注方法的参数来自于HTTP请求的不同位置，例如@PathParam来自于URL的路径，@QueryParam来自于URL的查询参数，@HeaderParam来自于HTTP请求的头信息，@CookieParam来自于HTTP请求的Cookie。


- @Component将AccountResource声明为Spring bean。

- @Scope声明了一个prototype Spring bean，这样每次使用时都会实例化（比如每次请求时）。

- @Autowired指定了一个AccountRepository引用，Spring会提供该引用。

- @Context也是一个JAX-RS注解，要求注入特定于请求的UriInfo对象。


![](../../pic/2020-02-04-10-45-24.png)



## 2、和springmvc的区别

- 不同点

JAX-RS的目标是Web Services开发（这与HTML Web应用不同）而Spring MVC的目 标则是Web应用开发。

REST与Spring

REST特性是Spring Framework的一部分，也是现有的Spring MVC编 程模型的延续，因此，并没有所谓的“Spring REST framework”这种概念，有的只是Spring和Spring MVC。这意味着如果你有一个Spring应用的话，你既可以使用Spring MVC创建HTML Web层，也可以创建 RESTful Web Services层。

