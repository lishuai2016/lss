



https://www.cnblogs.com/EasonJim/p/7841990.html


# static

static可以修饰：属性，方法，代码段，内部类（静态内部类或嵌套内部类）

static修饰的属性的初始化在编译期（类加载的时候），初始化后能改变。

static修饰的属性所有对象都只有一个值。

static修饰的属性强调它们只有一个。

static修饰的属性、方法、代码段跟该类的具体对象无关，不创建对象也能调用static修饰的属性、方法等

static和“this、super”势不两立，static跟具体对象无关，而this、super正好跟具体对象有关。

static不可以修饰局部变量。

static final和final static：

static final和final static没什么区别，一般static写在前面。
