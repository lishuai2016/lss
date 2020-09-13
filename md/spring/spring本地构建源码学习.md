




http://www.tianxiaobo.com/categories/


https://www.zybuluo.com/dugu9sword/note/382745






# 运行系统自带的单元测试

XmlBeanDefinitionReaderTests

# 遇到的问题

- 问题1

```java
Error:Kotlin: [Internal Error] java.lang.AbstractMethodError: org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar.registerProjectComponents(Lcom/intellij/mock/MockProject;Lorg/jetbrains/kotlin/config/CompilerConfiguration;)V
	at org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.<init>(KotlinCoreEnvironment.kt:172)
	at org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.<init>(KotlinCoreEnvironment.kt:114)
	at org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment$Companion.createForProduction(KotlinCoreEnvironment.kt:382)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.createCoreEnvironment(K2JVMCompiler.kt:281)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.createEnvironmentWithScriptingSupport(K2JVMCompiler.kt:271)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.doExecute(K2JVMCompiler.kt:163)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.doExecute(K2JVMCompiler.kt:58)
	at org.jetbrains.kotlin.cli.common.CLICompiler.execImpl(CLICompiler.java:93)
	at org.jetbrains.kotlin.cli.common.CLICompiler.execImpl(CLICompiler.java:46)
	at org.jetbrains.kotlin.cli.common.CLITool.exec(CLITool.kt:92)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$compile$$inlined$ifAlive$lambda$1.invoke(CompileServiceImpl.kt:381)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$compile$$inlined$ifAlive$lambda$1.invoke(CompileServiceImpl.kt:98)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$doCompile$$inlined$ifAlive$lambda$2.invoke(CompileServiceImpl.kt:832)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$doCompile$$inlined$ifAlive$lambda$2.invoke(CompileServiceImpl.kt:98)
	at org.jetbrains.kotlin.daemon.common.DummyProfiler.withMeasure(PerfUtils.kt:137)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.checkedCompile(CompileServiceImpl.kt:859)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.doCompile(CompileServiceImpl.kt:831)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.compile(CompileServiceImpl.kt:379)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:357)
	at sun.rmi.transport.Transport$1.run(Transport.java:200)
	at sun.rmi.transport.Transport$1.run(Transport.java:197)
	at java.security.AccessController.doPrivileged(Native Method)
	at sun.rmi.transport.Transport.serviceCall(Transport.java:196)
	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:573)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:834)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:687)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)

```

这个问题是Kotlin插件版本的问题，升级一下即可

- [Spring源码分析——调试环境搭建（可能是最省事的构建方法）](https://www.w3xue.com/exp/article/201811/7228.html)

- 问题2

```java
> Task :spring-jms:jar UP-TO-DATE
> Task :api
D:\IdeaProjects\opensource\spring-framework\spring-web\src\main\java\org\springframework\http\codec\multipart\DefaultMultipartMessageReader.java:255: 错误: 编码GBK的不可映射字符
				builder.append("鈵?");
				                 ^
D:\IdeaProjects\opensource\spring-framework\spring-web\src\main\java\org\springframework\http\codec\multipart\DefaultMultipartMessageReader.java:258: 错误: 编码GBK的不可映射字符
				builder.append("鈵?");
				                 ^
2 个错误

> Task :api FAILED
```










- 问题3


```java
Error:Kotlin: [Internal Error] java.lang.LinkageError: loader constraint violation: loader (instance of org/jetbrains/kotlin/cli/jvm/plugins/PluginURLClassLoader$SelfThenParentURLClassLoader) previously initiated loading for a different type with name "kotlin/sequences/Sequence"
	at java.lang.ClassLoader.defineClass1(Native Method)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:763)
	at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
	at java.net.URLClassLoader.defineClass(URLClassLoader.java:467)
	at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
	at org.jetbrains.kotlin.cli.jvm.plugins.PluginURLClassLoader$SelfThenParentURLClassLoader.findClass(PluginURLClassLoader.kt:47)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
	at java.lang.ClassLoader.defineClass1(Native Method)
	at java.lang.ClassLoader.defineClass(ClassLoader.java:763)
	at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
	at java.net.URLClassLoader.defineClass(URLClassLoader.java:467)
	at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:368)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:362)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.net.URLClassLoader.findClass(URLClassLoader.java:361)
	at org.jetbrains.kotlin.cli.jvm.plugins.PluginURLClassLoader$SelfThenParentURLClassLoader.findClass(PluginURLClassLoader.kt:47)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
	at kotlin.coroutines.experimental.SequenceBuilderKt__SequenceBuilderKt.buildSequence(SequenceBuilder.kt:24)
	at org.jetbrains.kotlin.scripting.compiler.plugin.ScriptiDefinitionsFromClasspathDiscoverySourceKt.discoverScriptTemplatesInClasspath(ScriptiDefinitionsFromClasspathDiscoverySource.kt:50)
	at org.jetbrains.kotlin.scripting.compiler.plugin.ScriptDefinitionsFromClasspathDiscoverySource.<init>(ScriptiDefinitionsFromClasspathDiscoverySource.kt:36)
	at org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationExtension.updateConfiguration(ScriptingCompilerConfigurationExtension.kt:56)
	at org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.<init>(KotlinCoreEnvironment.kt:213)
	at org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.<init>(KotlinCoreEnvironment.kt:117)
	at org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment$Companion.createForProduction(KotlinCoreEnvironment.kt:446)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.createCoreEnvironment(K2JVMCompiler.kt:295)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.doExecute(K2JVMCompiler.kt:147)
	at org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.doExecute(K2JVMCompiler.kt:51)
	at org.jetbrains.kotlin.cli.common.CLICompiler.execImpl(CLICompiler.java:94)
	at org.jetbrains.kotlin.cli.common.CLICompiler.execImpl(CLICompiler.java:50)
	at org.jetbrains.kotlin.cli.common.CLITool.exec(CLITool.kt:88)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$compile$$inlined$ifAlive$lambda$1.invoke(CompileServiceImpl.kt:402)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$compile$$inlined$ifAlive$lambda$1.invoke(CompileServiceImpl.kt:101)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$doCompile$$inlined$ifAlive$lambda$2.invoke(CompileServiceImpl.kt:929)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl$doCompile$$inlined$ifAlive$lambda$2.invoke(CompileServiceImpl.kt:101)
	at org.jetbrains.kotlin.daemon.common.DummyProfiler.withMeasure(PerfUtils.kt:137)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.checkedCompile(CompileServiceImpl.kt:969)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.doCompile(CompileServiceImpl.kt:928)
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.compile(CompileServiceImpl.kt:400)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:357)
	at sun.rmi.transport.Transport$1.run(Transport.java:200)
	at sun.rmi.transport.Transport$1.run(Transport.java:197)
	at java.security.AccessController.doPrivileged(Native Method)
	at sun.rmi.transport.Transport.serviceCall(Transport.java:196)
	at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:573)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:834)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:687)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)

```


- [spring学习-01编译spring5.0源码（亲测可用）](https://blog.csdn.net/qq_38412637/article/details/85255625)