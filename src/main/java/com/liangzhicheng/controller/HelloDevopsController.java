package com.liangzhicheng.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloDevopsController {

    @GetMapping(value = "/")
    public String helloDevops(){
        return "Hello Devops ...";
    }

}
