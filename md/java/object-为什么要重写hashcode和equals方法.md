
<!-- TOC -->

- [1、为什么要重写hashcode和equals方法](#1为什么要重写hashcode和equals方法)
- [参考](#参考)

<!-- /TOC -->


- 1、Object类的hashCode()方法返回这个对象存储的内存地址的编号; equals默认比较的是在内存的地址是否为同一个；

- 2、两个对象的equals相同，那么他们的hashCode一定相同；两个对象的equals不相同那么他们的hash也一样不相同（用户自己定义，不是强制的）；hash相同则，equals不一定相同；

- 3、如果重写equals()方法必须要重写hashCode()方法，来保证第二条的成立；

```java
public class Object {

    //两个对象的equals相同，那么他们的hashCode一定相同；两个对象的equals不相同那么他们的hash也一样不相同（用户自己定义，不是强制的）；hash相同则，equals不一定相同；
 	public native int hashCode();

    //默认比较的是在内存的地址是否为同一个
 	public boolean equals(Object obj) {
        return (this == obj);
    }
    ...
    }
```

> 问题

- 1、你有没有重写过hashCode方法？
- 2、你在使用HashMap时有没有重写hashCode和equals方法？你是怎么写的？
- 3、一个对象的hashcode可以改变么？



# 1、为什么要重写hashcode和equals方法

`如果你不重写这两个方法，将几乎不遇到任何问题，但是有的时候程序要求我们必须改变一些对象的默认实现`

来看看这个例子，让我们创建一个简单的类Employee

```java
public class Employee {
    private Integer id;
    private String firstname;
    private String lastName;
    private String department;

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getFirstname() {
        return firstname;
    }
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getDepartment() {
        return department;
    }
    public void setDepartment(String department) {
        this.department = department;
    }
}

public class EqualsTest {
    public static void main(String[] args) {
        Employee e1 = new Employee();
        Employee e2 = new Employee();

        e1.setId(100);
        e2.setId(100);
        //Prints false in console
        System.out.println(e1.equals(e2));
    }
}
```

毫无疑问，上面的程序将输出false，但是，事实上上面两个对象代表的是通过一个employee。真正的商业逻辑希望我们返回true。为了达到这个目的，我们需要重写equals方法。

```java
public boolean equals(Object o) {
        if(o == null)
        {
            return false;
        }
        if (o == this)
        {
           return true;
        }
        if (getClass() != o.getClass())
        {
            return false;
        }
        Employee e = (Employee) o;
        return (this.getId() == e.getId());
}
```

在上面的类中添加这个方法，EauqlsTest将会输出true。

So are we done?没有，让我们换一种测试方法来看看。


```java
import java.util.HashSet;
import java.util.Set;

public class EqualsTest
{
    public static void main(String[] args)
    {
        Employee e1 = new Employee();
        Employee e2 = new Employee();

        e1.setId(100);
        e2.setId(100);

        //Prints 'true'
        System.out.println(e1.equals(e2));

        Set<Employee> employees = new HashSet<Employee>();
        employees.add(e1);
        employees.add(e2);
        //Prints two objects
        System.out.println(employees);
    }
```
上面的程序输出的结果是两个。如果两个employee对象equals返回true，Set中应该只存储一个对象才对，问题在哪里呢？我们忘掉了第二个重要的方法hashCode()。`就像JDK的Javadoc中所说的一样，如果重写equals()方法必须要重写hashCode()方法。`我们加上下面这个方法，程序将执行正确。


```java
@Override
 public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + getId();
    return result;
 }
```


# 参考

- [Java 中正确使用 hashCode 和 equals 方法](https://www.oschina.net/question/82993_75533)
