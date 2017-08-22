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

package com.flow.platform.core.service;

import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.sysinfo.AppServerLoader;
import com.flow.platform.core.sysinfo.DBInfoLoader;
import com.flow.platform.core.sysinfo.JvmLoader;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfoLoader;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class SysInfoServiceImpl implements SysInfoService {

    private final String defaultDriverName = "com.mysql.jdbc.Driver";

    @Value("${jdbc.url}")
    private String dbUrl;

    @Value("${jdbc.username}")
    private String dbUsername;

    @Value("${jdbc.password}")
    private String dbPassword;

    private final Map<SystemInfo.System, Map<SystemInfo.Type, SystemInfoLoader>> infoLoaders = new HashMap<>(3);

    @PostConstruct
    public void init() {
        infoLoaders.put(SystemInfo.System.CC, new HashMap<>(5));
        infoLoaders.get(SystemInfo.System.CC)
            .put(SystemInfo.Type.JVM, new JvmLoader());
        infoLoaders.get(SystemInfo.System.CC)
            .put(SystemInfo.Type.DB, new DBInfoLoader(defaultDriverName, dbUrl, dbUsername, dbPassword));
        infoLoaders.get(SystemInfo.System.CC)
            .put(SystemInfo.Type.TOMCAT, new AppServerLoader());
    }

    @Override
    public SystemInfo get(SystemInfo.System sys, SystemInfo.Type type) {
        if (type == null) {
            // load system info
        }

        // load related components info
        try {
            return infoLoaders.get(sys).get(type).load();
        } catch (NullPointerException e) {
            throw new IllegalParameterException(String.format("Cannot load system info of %s - %s", sys, type));
        }
    }
}
