package com.dus.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Value("${server.port}")
    private Integer port;

    @GetMapping("/{id}")
    public String order(@PathVariable("id") Integer id) {
        System.out.println("Ordered for id " + id);
        return "Order success:" + id + " Port:" + port;
    }
}
