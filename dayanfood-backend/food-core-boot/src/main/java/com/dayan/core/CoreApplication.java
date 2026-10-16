package com.dayan.core;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@EnableCaching
@MapperScan("com.dayan.food.mapper")
@SpringBootApplication(scanBasePackages = {"com.dayan.food", "com.dayan.core"})
public class CoreApplication {
 public static void main(String[] args) { SpringApplication.run(CoreApplication.class, args); }
}
