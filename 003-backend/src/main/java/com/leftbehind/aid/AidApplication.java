package com.leftbehind.aid;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.leftbehind.aid.mapper")
public class AidApplication {
    public static void main(String[] args) {
        SpringApplication.run(AidApplication.class, args);
    }
}
