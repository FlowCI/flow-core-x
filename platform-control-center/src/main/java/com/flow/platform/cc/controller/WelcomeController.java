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

import com.flow.platform.core.service.SysInfoService;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.util.DateUtil;
import java.io.Serializable;
import java.time.ZonedDateTime;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gy@fir.im
 */
@RestController
@RequestMapping("/")
public class WelcomeController {

    public static class AppStatus implements Serializable {

        private String status;
        private ZonedDateTime startTime;

        AppStatus(String status, ZonedDateTime startTime) {
            this.status = status;
            this.startTime = startTime;
        }

        public String getStatus() {
            return status;
        }

        public ZonedDateTime getStartTime() {
            return startTime;
        }
    }

    private ZonedDateTime startTime;

    @Autowired
    private SysInfoService sysInfoService;

    @PostConstruct
    private void init() {
        startTime = DateUtil.now();
    }

    @GetMapping("/index")
    public AppStatus heartbeat() {
        return new AppStatus("OK", startTime);
    }

    @GetMapping("/sys/info/{type}")
    public SystemInfo systemInfo(@PathVariable(required = false) String type) {
        SystemInfo.Type targetType = SystemInfo.Type.valueOf(type.toUpperCase());
        return sysInfoService.get(SystemInfo.System.CC, targetType);
    }
}
