package com.example.broken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.example.broken.entity")
@EnableJpaRepositories(basePackages = "com.example.broken.repository")
public class BrokenApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokenApplication.class, args);
    }
}
