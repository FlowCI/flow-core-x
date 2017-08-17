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
import com.flow.platform.core.sysinfo.DBInfoLoader;
import com.flow.platform.core.sysinfo.JvmLoader;
import com.flow.platform.core.sysinfo.SystemInfo;
import java.util.Objects;
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

    private final JvmLoader jvmLoader = new JvmLoader();

    @Override
    public SystemInfo get(String type) {
        if (!SUPPORT_TYPES.contains(type)) {
            throw new IllegalParameterException("System into type not supported: " + type);
        }

        if (Objects.equals(type, INFO_TYPE_JVM)) {
            return jvmLoader.load();
        }

        if (Objects.equals(type, INFO_TYPE_DB)) {
            return new DBInfoLoader(defaultDriverName, dbUrl, dbUsername, dbPassword).load();
        }

        return null;
    }
}
