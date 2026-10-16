package com.dayan.community;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@EnableCaching
@MapperScan("com.dayan.food.mapper")
@SpringBootApplication(scanBasePackages = {"com.dayan.food", "com.dayan.community"})
public class CommunityApplication {
 public static void main(String[] args) { SpringApplication.run(CommunityApplication.class, args); }
}
