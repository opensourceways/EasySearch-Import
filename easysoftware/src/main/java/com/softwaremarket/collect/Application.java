package com.softwaremarket.collect;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableAsync
@ComponentScans(value = {
        @ComponentScan("com.softwaremarket.*"),
})
@MapperScan(basePackages = "com.softwaremarket.collect.mapper")
@SpringBootApplication(scanBasePackages = {"com.softwaremarket.collect"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
