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

package com.flow.platform.cc.service;

import com.flow.platform.core.service.SysInfoServiceImplBase;
import com.flow.platform.core.sysinfo.AppServerLoader;
import com.flow.platform.core.sysinfo.DBInfoLoader;
import com.flow.platform.core.sysinfo.JvmLoader;
import com.flow.platform.core.sysinfo.MQLoader;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
import com.flow.platform.core.sysinfo.SystemInfo.Type;
import com.flow.platform.core.sysinfo.SystemInfoLoader;
import com.flow.platform.core.sysinfo.ZooKeeperLoader;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service("sysInfoService")
public class SysInfoServiceImpl extends SysInfoServiceImplBase {

    private final Map<Category, Map<Type, SystemInfoLoader>> infoLoaders = new HashMap<>(3);

    @Value("${zk.host}")
    private String zkHost;

    @Value("${mq.host}")
    private String mqHost;

    @Value("${mq.management.host}")
    private String mqManagementHost;

    @PostConstruct
    public void init() {
        infoLoaders.put(Category.CC, new HashMap<>(5));

        infoLoaders.get(Category.CC).put(SystemInfo.Type.JVM, new JvmLoader());

        infoLoaders.get(Category.CC)
            .put(SystemInfo.Type.DB, new DBInfoLoader(defaultDriverName, dbUrl, dbUsername, dbPassword));

        infoLoaders.get(Category.CC).put(SystemInfo.Type.SERVER, new AppServerLoader());

        infoLoaders.get(Category.CC).put(SystemInfo.Type.ZK, new ZooKeeperLoader(zkHost));

        MQLoader.MQURL mqUrl = new MQLoader.MQURL(mqHost);
        infoLoaders.get(Category.CC).put(SystemInfo.Type.MQ, new MQLoader(mqManagementHost, mqUrl.getUser(), mqUrl.getPass()));
    }

    @Override
    public Map<Category, Map<SystemInfo.Type, SystemInfoLoader>> getLoaders() {
        return infoLoaders;
    }
}
