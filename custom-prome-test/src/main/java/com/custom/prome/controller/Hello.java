package com.custom.prome.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/hello")
public class Hello {
    //用于计数
    private AtomicLong count = new AtomicLong(0);

    @RequestMapping("get")
    public String getCount() {
        return count.intValue()+"";
    }

    @RequestMapping("add")
    public String setCount() {
        return count.incrementAndGet()+"";
    }

    @RequestMapping("reset")
    public String reset() {
        count.set(0);
        int i = 1/0;
        return "重置操作，当前值："+count.intValue();
    }
}
