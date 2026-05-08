package org.yongzhang.firebird.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;

//@Component
public class Test_DB implements CommandLineRunner {

//    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // 执行一条简单SQL测试连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            System.out.println("✅ MySQL 连接成功！");
        } catch (Exception e) {
            System.out.println("❌ MySQL 连接失败：" + e.getMessage());
        }
    }
}
