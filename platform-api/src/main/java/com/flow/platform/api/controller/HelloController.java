package com.flow.platform.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by gyfirim on 14/07/2017.
 *
 * @Copyright fir.im
 */
@RestController
@RequestMapping("/")
public class HelloController {
    @GetMapping(path = "/")
    public String hello(){
        return "{\"flow-platform\": \"api\"}";
    }
}
