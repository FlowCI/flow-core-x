package com.flow.platform.cc.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Consume cmd from rabbit mq
 *
 * Created by gy@fir.im on 20/06/2017.
 * Copyright fir.im
 */
@Service
public class CmdQueueConsumer {

    @PostConstruct
    public void init() {

    }
}
