package com.liangzhicheng.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestHelloDevopsController {

    @Test
    public void testHelloDevops(){
        assertEquals("Hello Devops ...", new HelloDevopsController().helloDevops());
    }

}
