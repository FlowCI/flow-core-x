package com.flow.platform.cc.controller;

import com.flow.platform.cc.config.AppConfig;
import com.flow.platform.cc.util.ZkHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * For heartbeat checking
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */

@RestController
@RequestMapping("/")
public class WelcomeController {

    public static class AppStatus implements Serializable {
        private String status;
        private Date startTime;
        private ZkHelper.ZkInfo zkInfo;
        private Map<String, List<String>> zkHistory;

        AppStatus(String status,
                  Date startTime,
                  ZkHelper.ZkInfo zkInfo,
                  Map<String, List<String>> zkHistory) {
            this.status = status;
            this.startTime = startTime;
            this.zkInfo = zkInfo;
            this.zkHistory = zkHistory;
        }

        public String getStatus() {
            return status;
        }

        public Date getStartTime() {
            return startTime;
        }

        public ZkHelper.ZkInfo getZkInfo() {
            return zkInfo;
        }

        public Map<String, List<String>> getZkHistory() {
            return zkHistory;
        }
    }

    @Autowired
    private ZkHelper zkHelper;

    private Date startTime;

    @PostConstruct
    private void init() {
        startTime = new Date();
    }

    @RequestMapping("/index")
    public AppStatus heartbeat() {
        return new AppStatus("OK",
                startTime,
                zkHelper.getInfo(),
                zkHelper.getZkHistory());
    }
}
