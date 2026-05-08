package org.yongzhang.firebird;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.yongzhang.firebird.Mapper")
public class FireBirdApplication {

    public static void main(String[] args) {
        SpringApplication.run(FireBirdApplication.class, args);
    }

}
