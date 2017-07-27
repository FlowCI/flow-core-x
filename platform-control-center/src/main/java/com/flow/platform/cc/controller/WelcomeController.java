/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.cc.controller;

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
 * @author gy@fir.im
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
