package com.ls;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: weixin-java-mp-demo-springboot
 * @author: lishuai
 * @create: 2020-05-06 21:55
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public String index(){
        String current = "current:" + System.currentTimeMillis();
        System.out.println(current);
        return current;
    }
}
