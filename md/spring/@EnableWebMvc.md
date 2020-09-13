在Spring3中已经可以用@Configuration标识一个类代替xml来配置bean容器，该类中所用标识有@Bean注解的方法都会发布成一个bean，在spring mvc框架中也提供了注解的配置的方式，即@EnableWebMvc，这篇文章试图讨论下@EnableWebMvc这个注解背后干了什么。



大家都知道spring mvc容器很灵活，处理请求的各个模块都是隔离的，很方便定制化，当你使用@EnableWebMvc来配置spring mvc时，会把WebMvcConfigurationSupport当成配置文件来用，将其中所有标识有@Bean注解的方法配置成bean，这就成了Spring mvc的默认配置（了解Spring mvc的人应该都知道下面bean的作用）：



HandlerMapping：



Bean: requestMappingHandlerMapping
Bean: viewControllerHandlerMapping
Bean: beanNameHandlerMapping
Bean: resourceHandlerMapping
Bean: defaultServletHandlerMapping
HandlerAdapter：
Bean: requestMappingHandlerAdapter
Bean: httpRequestHandlerAdapter
Bean: simpleControllerHandlerAdapter
ExceptionResolver
Bean: handlerExceptionResolver
其他
Bean: mvcConversionService
Bean: mvcValidator

前面说过各个组件都是可以定制化，在WebMvcConfigurationSupport是通过模板方法模式来实现的，在各个发布成Bean的方法中，都调用了自定义组件的抽象方法，在子类中可以覆盖，如
	对HandlerAdapter组件，有addInterceptors(InterceptorRegistry registry)可以添加自己的拦截器；
	对conversionService组件，有addFormatters(FormatterRegistry registry)可以添加自己的类型转换器；
	等等。。。
从而实现定制化。

上面提到子类，Spring mvc提供的默认实现是DelegatingWebMvcConfiguration，覆盖父类的方法之前，它会寻找容器中所有的WebMvcConfigurer实现类，将所有WebMvcConfigurer实现类中的配置组合起来，组成一个超级配置，这样，WebMvcConfigurationSupport中的bean发布时，就会把这所有配置都带上了。



问题1:假如不使用@EnableWebMvc注解那么DelegatingWebMvcConfiguration难道就不实例化?



- [WebMvcConfigurationSupport与WebMvcConfigurer的关系](https://www.jianshu.com/p/d47a09532de7)

