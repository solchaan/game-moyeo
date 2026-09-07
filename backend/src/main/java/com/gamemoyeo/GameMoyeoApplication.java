package com.gamemoyeo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GameMoyeoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameMoyeoApplication.class, args);
    }
}
