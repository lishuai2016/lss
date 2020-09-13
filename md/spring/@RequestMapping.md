<!-- TOC -->

- [1、@RequestMapping](#1requestmapping)
    - [1、 value\path， method；](#1-value\path-method)
    - [2、 consumes，produces；](#2-consumesproduces)
    - [3、 params，headers；](#3-paramsheaders)
- [2、示例](#2示例)
    - [1：value、method 示例](#1valuemethod-示例)
    - [2：consumes、produces 示例](#2consumesproduces-示例)
    - [3：params、headers 示例](#3paramsheaders-示例)
- [参考](#参考)

<!-- /TOC -->


# 1、@RequestMapping


```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {
    String name() default "";

    @AliasFor("path")
    String[] value() default {};

    @AliasFor("value")
    String[] path() default {};

    RequestMethod[] method() default {};

    String[] params() default {};

    String[] headers() default {};

    String[] consumes() default {};

    String[] produces() default {};
}
```


RequestMapping是一个用来处理请求地址映射的注解，可用于类或方法上。用于类上，表示类中的所有响应请求的方法都是以该地址作为父路径。

RequestMapping注解有六个属性，下面我们把她分成三类进行说明。

## 1、 value\path， method；

- value\path：指定请求的实际地址，指定的地址可以是URI Template 模式（后面将会说明）；

- method：指定请求的method类型， GET、POST、PUT、DELETE等；



## 2、 consumes，produces；

- consumes：指定处理请求的提交内容类型（Content-Type），例如application/json, text/html;

- produces：指定返回的内容类型，仅当request请求头中的(Accept)类型中包含该指定类型才返回；



## 3、 params，headers；

- params： 指定request中必须包含某些参数值是，才让该方法处理。

- headers： 指定request中必须包含某些指定的header值，才能让该方法处理请求。



# 2、示例

## 1：value、method 示例


默认RequestMapping("....str...")即为value的值；

```java
@Controller  
    @RequestMapping("/appointments")  
    public class AppointmentsController {  
      
        private final AppointmentBook appointmentBook;  
          
        @Autowired  
        public AppointmentsController(AppointmentBook appointmentBook) {  
            this.appointmentBook = appointmentBook;  
        }  
      
        @RequestMapping(method = RequestMethod.GET)  
        public Map<String, Appointment> get() {  
            return appointmentBook.getAppointmentsForToday();  
        }  
      
        @RequestMapping(value="/{day}", method = RequestMethod.GET)  
        public Map<String, Appointment> getForDay(@PathVariable @DateTimeFormat(iso=ISO.DATE) Date day, Model model) {  
            return appointmentBook.getAppointmentsForDay(day);  
        }  
      
        @RequestMapping(value="/new", method = RequestMethod.GET)  
        public AppointmentForm getNewForm() {  
            return new AppointmentForm();  
        }  
      
        @RequestMapping(method = RequestMethod.POST)  
        public String add(@Valid AppointmentForm appointment, BindingResult result) {  
            if (result.hasErrors()) {  
                return "appointments/new";  
            }  
            appointmentBook.addAppointment(appointment);  
            return "redirect:/appointments";  
        }  
    }  
```

value的uri值为以下三类：

A） 可以指定为普通的具体值；

B)  可以指定为含有某变量的一类值(URI Template Patterns with Path Variables)；

```java
@RequestMapping(value="/owners/{ownerId}", method=RequestMethod.GET)  
    public String findOwner(@PathVariable String ownerId, Model model) {  
      Owner owner = ownerService.findOwner(ownerId);    
      model.addAttribute("owner", owner);    
      return "displayOwner";   
    }  
```

C) 可以指定为含正则表达式的一类值( URI Template Patterns with Regular Expressions);

```java
@RequestMapping("/spring-web/{symbolicName:[a-z-]+}-{version:\d\.\d\.\d}.{extension:\.[a-z]}")  
      public void handle(@PathVariable String version, @PathVariable String extension) {      
        // ...  
      }  
    }  
```


## 2：consumes、produces 示例

cousumes的样例：方法仅处理request Content-Type为“application/json”类型的请求。

```java
@Controller  
@RequestMapping(value = "/pets", method = RequestMethod.POST, consumes="application/json")  
public void addPet(@RequestBody Pet pet, Model model) {      
    // implementation omitted  
}  
````




produces的样例：方法仅处理request请求中Accept头中包含了"application/json"的请求，同时暗示了返回的内容类型为application/json;

```java
@Controller  
@RequestMapping(value = "/pets/{petId}", method = RequestMethod.GET, produces="application/json")  
@ResponseBody  
public Pet getPet(@PathVariable String petId, Model model) {      
    // implementation omitted  
}  

```



## 3：params、headers 示例

params的样例：仅处理请求中包含了名为“myParam”，值为“myValue”的请求；

```java
 @Controller  
    @RequestMapping("/owners/{ownerId}")  
    public class RelativePathUriTemplateController {  
      
      @RequestMapping(value = "/pets/{petId}", method = RequestMethod.GET, params="myParam=myValue")  
      public void findPet(@PathVariable String ownerId, @PathVariable String petId, Model model) {      
        // implementation omitted  
      }  
    }  
```
 

headers的样例：

```java
@Controller  
@RequestMapping("/owners/{ownerId}")  
public class RelativePathUriTemplateController {  
    
    @RequestMapping(value = "/pets", method = RequestMethod.GET, headers="Referer=http://www.ifeng.com/")  
    public void findPet(@PathVariable String ownerId, @PathVariable String petId, Model model) {      
    // implementation omitted  
    }  
}  
```

仅处理request的header中包含了指定“Refer”请求头和对应值为“http://www.ifeng.com/”的请求；




# 参考

https://blog.csdn.net/kobejayandy/article/details/12690041
