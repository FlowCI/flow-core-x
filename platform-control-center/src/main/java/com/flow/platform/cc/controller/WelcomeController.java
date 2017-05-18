package com.flow.platform.cc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * For heartbeat checking
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@RestController
@RequestMapping("/")
public class WelcomeController {

    @RequestMapping("/index")
    public String heartbeat() {
        return "ok";
    }
}
