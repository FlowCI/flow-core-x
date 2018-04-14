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

package com.flow.platform.core.sysinfo;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.flow.platform.core.sysinfo.SystemInfo.Type;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;

/**
 * @author yang
 */
@Log4j2
public class AppServerLoader implements SystemInfoLoader {

    private final static String CATALINA_HOME = "catalina.home";

    private final static String CMD = "java -cp lib/catalina.jar org.apache.catalina.util.ServerInfo";

    private final static String TOMCAT_SERVER_NAME_KEY = "Server version";

    private final static String TOMCAT_SERVER_VERSION_KEY = "Server number";

    private final Map<String, String> keyMapping = new HashMap<>();

    public enum AppServerGroup implements GroupName {
        TOMCAT
    }

    public AppServerLoader() {
        keyMapping.put("Server built", "tomcat.built");
        keyMapping.put("JVM Version", "tomcat.jvm.version");
        keyMapping.put("JVM Vendor", "tomcat.jvm.vendor");
        keyMapping.put("OS Name", "tomcat.os.name");
        keyMapping.put("OS Version", "tomcat.os.version");
        keyMapping.put("Architecture", "tomcat.os.arch");
    }

    @Override
    public SystemInfo load() {
        Properties properties = System.getProperties();
        String catalinaBase = properties.getProperty(CATALINA_HOME);
        if (Strings.isNullOrEmpty(catalinaBase)) {
            return new SystemInfo(Status.OFFLINE, Type.SERVER);
        }

        try {
            GroupSystemInfo info = new GroupSystemInfo(Status.RUNNING, Type.SERVER);
            Map<String, String> infoContent = new HashMap<>();
            infoContent.put("tomcat.home", catalinaBase);
            info.put(AppServerGroup.TOMCAT, infoContent);

            CmdExecutor exec = new CmdExecutor(catalinaBase, Lists.newArrayList(CMD));
            exec.setLogListener(new LogListener() {
                @Override
                public void onLog(Log log) {
                    // the content format like: Server version: Apache Tomcat/8.5.14
                    String content = log.getContent();
                    int splitterIndex = content.indexOf(':');
                    String key = content.substring(0, splitterIndex).trim();
                    String value = content.substring(splitterIndex + 1).trim();

                    if (key.equals(TOMCAT_SERVER_NAME_KEY)) {
                        info.setName(value);
                        return;
                    }

                    if (key.equals(TOMCAT_SERVER_VERSION_KEY)) {
                        info.setVersion(value);
                        return;
                    }

                    infoContent.put(keyMapping.get(key), value);
                }

                @Override
                public void onFinish() {

                }
            });

            exec.run();
            return info;

        } catch (Throwable warn) {
            log.warn("Unable to get tomcat server config: {}", warn.getMessage());
        }

        return new SystemInfo(Status.OFFLINE, Type.SERVER);
    }
}
