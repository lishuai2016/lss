

1、maven跳过单元测试

-Dmaven.test.skip=true，不执行测试用例，也不编译测试用例类。


2、pom.xml文件中dependency中optional属性的作用

参考：https://blog.csdn.net/zyf_balance/article/details/43937405

当project-A 依赖project-B,  project-B 依赖project-D时

```
What if we dont want project D and its dependencies to be added to Project A's classpath because we know some of Project-D's dependencies (maybe Project-E for example) was missing from the repository, and you don't need/want the functionality in Project-B that depends on Project-D anyway. In this case, Project-B's developers could provide a dependency on Project-D that is <optional>true</optional>, like this:
```

```xml
<dependency>
  <groupId>sample.ProjectD</groupId>
  <artifactId>ProjectD</artifactId>
  <version>1.0-SNAPSHOT</version>
  <optional>true</optional>
</dependency>
```




所以当project-B的<optional>true</optional>时, project-A中如果没有显式的引入project-D, 则project-A不依赖project-D, 即project-A可以自己选择是否依赖project-D

默认<optional>的值为false, 及子项目必须依赖




