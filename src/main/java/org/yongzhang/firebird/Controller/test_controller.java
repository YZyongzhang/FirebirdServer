package org.yongzhang.firebird.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 标记为控制器，返回JSON
@RestController
public class test_controller {

    @GetMapping("/hello")
    public String hello() {
        return "成功访问Controller！";
    }

}
