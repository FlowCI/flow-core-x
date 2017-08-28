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

package com.flow.platform.api.service;

import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.sysinfo.GroupSystemInfo;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.core.service.SysInfoServiceImplBase;
import com.flow.platform.core.sysinfo.AppServerLoader;
import com.flow.platform.core.sysinfo.DBInfoLoader;
import com.flow.platform.core.sysinfo.JvmLoader;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
import com.flow.platform.core.sysinfo.SystemInfo.Type;
import com.flow.platform.core.sysinfo.SystemInfoLoader;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * API system info service to load from api self and cc via http
 *
 * @author yang
 */
@Service("sysInfoService")
public class SysInfoServiceImpl extends SysInfoServiceImplBase {

    private final Map<Category, Map<Type, SystemInfoLoader>> infoLoaders = new HashMap<>(3);

    @Autowired
    private PlatformURL platformURL;

    @PostConstruct
    public void init() {
        // init api system loader
        infoLoaders.put(Category.API, new HashMap<>(3));
        infoLoaders.get(Category.API).put(SystemInfo.Type.JVM, new JvmLoader());
        infoLoaders.get(Category.API)
            .put(SystemInfo.Type.DB, new DBInfoLoader(defaultDriverName, dbUrl, dbUsername, dbPassword));
        infoLoaders.get(Category.API).put(SystemInfo.Type.SERVER, new AppServerLoader());

        // init cc system loader
        infoLoaders.put(Category.CC, new HashMap<>(5));
        infoLoaders.get(Category.CC).put(SystemInfo.Type.JVM, new ControlCenterInfoLoader(SystemInfo.Type.JVM));
        infoLoaders.get(Category.CC).put(SystemInfo.Type.DB, new ControlCenterInfoLoader(SystemInfo.Type.DB));
        infoLoaders.get(Category.CC).put(SystemInfo.Type.SERVER, new ControlCenterInfoLoader(SystemInfo.Type.SERVER));
        infoLoaders.get(Category.CC).put(SystemInfo.Type.ZK, new ControlCenterInfoLoader(SystemInfo.Type.ZK));
        infoLoaders.get(Category.CC).put(SystemInfo.Type.MQ, new ControlCenterInfoLoader(SystemInfo.Type.MQ));
    }

    @Override
    public Map<Category, Map<Type, SystemInfoLoader>> getLoaders() {
        return infoLoaders;
    }

    private class ControlCenterInfoLoader implements SystemInfoLoader {

        private final SystemInfo.Type type;

        public ControlCenterInfoLoader(Type type) {
            this.type = type;
        }

        @Override
        public SystemInfo load() {
            String response = HttpUtil.get(platformURL.getSysinfoUrl() + "/" + type.name().toLowerCase());
            if (response == null) {
                return null;
            }
            return SystemInfo.parse(response, GroupSystemInfo.class);
        }
    }
}
