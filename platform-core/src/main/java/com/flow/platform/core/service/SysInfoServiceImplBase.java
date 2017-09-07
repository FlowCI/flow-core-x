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
import com.flow.platform.core.sysinfo.PropertySystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
import com.flow.platform.core.sysinfo.SystemInfo.Type;
import com.flow.platform.core.sysinfo.SystemInfoLoader;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author yang
 */
public abstract class SysInfoServiceImplBase implements SysInfoService {

    protected final String defaultDriverName = "com.mysql.jdbc.Driver";

    @Autowired
    private PropertySystemInfo systemInfo;

    @Value("${jdbc.url}")
    protected String dbUrl;

    @Value("${jdbc.username}")
    protected String dbUsername;

    @Value("${jdbc.password}")
    protected String dbPassword;

    public abstract Map<Category, Map<Type, SystemInfoLoader>> getLoaders();

    @Override
    public PropertySystemInfo system() {
        return systemInfo;
    }

    @Override
    public List<SystemInfo> components(Category sys, Type type) {
        // load all component
        if (type == null) {
            Map<Type, SystemInfoLoader> infoLoader = getLoaders().get(sys);

            List<SystemInfo> infoList = new ArrayList<>(infoLoader.size());

            for (SystemInfoLoader loader : infoLoader.values()) {
                infoList.add(loader.load());
            }

            return infoList;
        }

        // load related components info
        try {
            SystemInfo info = getLoaders().get(sys).get(type).load();
            return Lists.newArrayList(info);
        } catch (NullPointerException e) {
            throw new IllegalParameterException(String.format("Cannot load system info of %s - %s", sys, type));
        }
    }
}
