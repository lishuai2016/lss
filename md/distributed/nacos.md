










# 源码解析

```java
public class LsConfigExample {

    public static void main(String[] args) throws NacosException, InterruptedException {
        String serverAddr = "localhost";
        String dataId = "ls";
        String group = "DEFAULT_GROUP";
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        ConfigService configService = NacosFactory.createConfigService(properties);
        String content = configService.getConfig(dataId, group, 5000);
        System.out.println("第一次获取配置内容："+content);
        configService.addListener(dataId, group, new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.println("receive:" + configInfo);
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        });

        try {
            int n =System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

```

## 1、入口ConfigService

```java
public interface ConfigService {

    //获取配置信息
    String getConfig(String dataId, String group, long timeoutMs) throws NacosException;

    //获取配置信息同时添加监听器
    String getConfigAndSignListener(String dataId, String group, long timeoutMs, Listener listener) throws NacosException;

    //添加监听器
    void addListener(String dataId, String group, Listener listener) throws NacosException;

    //发布配置信息
    boolean publishConfig(String dataId, String group, String content) throws NacosException;

    //删除配置信息
    boolean removeConfig(String dataId, String group) throws NacosException;

    //移除监听器
    void removeListener(String dataId, String group, Listener listener);

    //服务端的健康检查
    String getServerStatus();

}
```

唯一实现NacosConfigService




# 参考

- [源码](https://github.com/lishuai2016/nacos.git)
- [官网](https://nacos.io/zh-cn/docs/quick-start.html)