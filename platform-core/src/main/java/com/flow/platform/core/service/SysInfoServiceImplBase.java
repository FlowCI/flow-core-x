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
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
import com.flow.platform.core.sysinfo.SystemInfoLoader;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author yang
 */
public abstract class SysInfoServiceImplBase implements SysInfoService {

    protected final String defaultDriverName = "com.mysql.jdbc.Driver";

    @Value("${jdbc.url}")
    protected String dbUrl;

    @Value("${jdbc.username}")
    protected String dbUsername;

    @Value("${jdbc.password}")
    protected String dbPassword;

    public abstract Map<Category, Map<SystemInfo.Type, SystemInfoLoader>> getLoaders();

    @Override
    public SystemInfo get(Category sys, SystemInfo.Type type) {
        if (type == null) {
            // load system info
        }

        // load related components info
        try {
            return getLoaders().get(sys).get(type).load();
        } catch (NullPointerException e) {
            throw new IllegalParameterException(String.format("Cannot load system info of %s - %s", sys, type));
        }
    }
}
