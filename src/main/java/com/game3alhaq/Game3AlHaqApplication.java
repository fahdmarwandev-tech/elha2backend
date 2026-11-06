package com.game3alhaq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Game3AlHaqApplication {

    public static void main(String[] args) {
        SpringApplication.run(Game3AlHaqApplication.class, args);
    }
}
