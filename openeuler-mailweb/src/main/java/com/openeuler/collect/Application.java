package com.openeuler.collect;

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
        @ComponentScan("com.openeuler.*"),
})
@MapperScan(basePackages = "com.openeuler.collect.mapper")
@SpringBootApplication(scanBasePackages = {"com.openeuler.collect"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
