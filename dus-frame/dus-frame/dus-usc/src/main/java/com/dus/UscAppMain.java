package com.dus;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UscAppMain {
    public static void main(String[] args) {
        new SpringApplication(UscAppMain.class).run(args);
    }
}
