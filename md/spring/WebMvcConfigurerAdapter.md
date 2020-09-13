WebMvcConfigurerAdapter的用法


以前写Spring MVC的时候，要添加一个新页面访问总是要新增一个Controller或者在已有的一个Controller中新增一个方法，然后再跳转到设置的页面上去。考虑到大部分应用场景中View和后台都会有数据交互，这样的处理也无可厚非，不过我们肯定也有只是想通过一个URL Mapping然后不经过Controller处理直接跳转到页面上的需求！通过 WebMvcConfigurerAdapter 即可实现，代码：

@Configuration
public class MyAdapter extends WebMvcConfigurerAdapter{
    @Override
    public void addViewControllers( ViewControllerRegistry registry ) {
//        registry.addViewController( "/" ).setViewName( "forward:/index.shtml" );
//        registry.setOrder( Ordered.HIGHEST_PRECEDENCE );
//        super.addViewControllers( registry );
        registry.addViewController( "/index" ).setViewName( "index" );  //可以直接设置跳转
    } 
}

那么通过上面的配置，不用再Controller中添加处理index的处理逻辑，就可以直接通过“http://localhost:8080/index”访问到index.html页面了