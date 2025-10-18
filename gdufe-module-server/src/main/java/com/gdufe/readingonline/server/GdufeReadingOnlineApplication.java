package com.gdufe.readingonline.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan("com.gdufe.readingonline.dal.mysqlmapper")
@SpringBootApplication
@ComponentScan(basePackages = "com.gdufe.readingonline")
public class GdufeReadingOnlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(GdufeReadingOnlineApplication.class, args);
    }
}
