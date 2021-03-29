
<!-- TOC -->

- [01、抽象类可以有构造函数吗](#01抽象类可以有构造函数吗)
- [99、其他](#99其他)
- [参考](#参考)

<!-- /TOC -->


# 01、抽象类可以有构造函数吗

- [java抽象类可以有构造方法](https://www.cnblogs.com/pjcdarker/p/4837014.html)

学过java都知道抽象类不能实例化，会认为它不能够有构造方法，然而并不是这样的。

它的调用是由实现子类构造的时候去调用；这样初始化的时候有用。

```java
abstract class T {
    
    public T(){
        System.out.println("T构造器....");
    }
}

class A extends T {
    
    public A(){
        System.out.println("A.构造器.....");
    }
    
    public static void main(String[] args) {
        new A();
    }
}
//输出 ：
T构造器....
A.构造器.....
```

`子类无参数的构造器会调用父类无参数的构造器`








# 99、其他



1、抽象类继承接口的时候可以不实现接口定义的方法，并且抽象类内部的静态变量在外面可直接引用


# 参考





