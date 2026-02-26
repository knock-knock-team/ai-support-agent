package com.mailserver.mailprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MailProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailProcessorApplication.class, args);
    }
}
