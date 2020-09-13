


实现方法
- 1、自定义需要发布的事件类，需要继承ApplicationEvent类或PayloadApplicationEvent<T>(该类也仅仅是对ApplicationEvent的一层封装)
- 2、使用@EventListener来监听事件
- 3、使用ApplicationEventPublisher来发布自定义事件（@Autowired注入即可）


- [SpringBoot 发布ApplicationEventPublisher和监听ApplicationEvent事件](https://blog.csdn.net/wanping321/article/details/86667216)








