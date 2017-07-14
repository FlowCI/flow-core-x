package com.flow.platform.cc.service;

import com.flow.platform.cc.util.ZkHelper;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

/**
 * Created by gy@fir.im on 28/05/2017.
 * Copyright fir.im
 */
public abstract class ZkServiceBase {

    @Autowired
    protected ZkHelper zkHelper;

    protected ZooKeeper zkClient;

    @PostConstruct
    private void init() {
        zkClient = zkHelper.getClient();
    }
}
