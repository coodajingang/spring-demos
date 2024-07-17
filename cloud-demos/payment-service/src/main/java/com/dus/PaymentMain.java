package com.dus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentMain {
    public static void main(String[] args) {
        new SpringApplication(PaymentMain.class).run(args);
    }


}