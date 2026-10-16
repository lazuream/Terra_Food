package com.dayan.user;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@EnableCaching
@MapperScan("com.dayan.food.mapper")
@SpringBootApplication(scanBasePackages = {"com.dayan.food", "com.dayan.user"})
public class UserApplication {
 public static void main(String[] args) { SpringApplication.run(UserApplication.class, args); }
}
