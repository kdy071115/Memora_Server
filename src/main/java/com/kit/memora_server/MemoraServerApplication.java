package com.kit.memora_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MemoraServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoraServerApplication.class, args);
    }

}
