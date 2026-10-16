package com.dayan.media;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@EnableCaching
@MapperScan("com.dayan.food.mapper")
@SpringBootApplication(scanBasePackages = {"com.dayan.food", "com.dayan.media"})
public class MediaApplication {
 public static void main(String[] args) { SpringApplication.run(MediaApplication.class, args); }
}
