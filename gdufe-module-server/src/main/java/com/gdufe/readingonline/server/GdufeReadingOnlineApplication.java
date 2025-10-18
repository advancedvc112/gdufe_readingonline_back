package com.gdufe.readingonline.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.gdufe.readingonline.dal.mysqlmapper")
@SpringBootApplication
public class GdufeReadingOnlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(GdufeReadingOnlineApplication.class, args);
    }
}
