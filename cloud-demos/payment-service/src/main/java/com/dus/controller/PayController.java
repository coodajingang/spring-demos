package com.dus.controller;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RefreshScope
@RestController
@RequestMapping("/pay")
public class PayController {
    @Resource
    private RestTemplate restTemplate;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private Integer port;

    @Value("${app.name}")
    private String name;
    @Value("${app.age}")
    private Integer age;

    @Value("${app.application-spec:0}")
    private Integer spec;

    @GetMapping("/info")
    public String info() {
        return "Service of " + appName + " listening at " + port + " " + name + "   -   " + age + "    application-spce:" + spec;
    }

    @GetMapping("/{id}")
    public String payForId(@PathVariable("id") Integer id) {
        System.out.println("Pay for id " + id);

        ResponseEntity<String> forEntity = restTemplate.getForEntity("http://order-service/order/" + id, String.class);

        return "PayService: " + id + " /nResult: " + forEntity.getBody();
    }
}
