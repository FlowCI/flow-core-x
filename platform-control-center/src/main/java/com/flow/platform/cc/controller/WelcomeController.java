package com.flow.platform.cc.controller;

import com.flow.platform.cc.util.ZkHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
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
        private ZkHelper.ZkStatus zkStatus;
        private Map<String, List<String>> zkHistory;

        AppStatus(String status, ZkHelper.ZkStatus zkStatus, Map<String, List<String>> zkHistory) {
            this.status = status;
            this.zkStatus = zkStatus;
            this.zkHistory = zkHistory;
        }

        public String getStatus() {
            return status;
        }

        public ZkHelper.ZkStatus getZkStatus() {
            return zkStatus;
        }

        public Map<String, List<String>> getZkHistory() {
            return zkHistory;
        }
    }

    @Autowired
    private ZkHelper zkHelper;

    @RequestMapping("/index")
    public AppStatus heartbeat() {
        return new AppStatus("OK",
                zkHelper.getStatus(),
                zkHelper.getZkHistory());
    }
}
