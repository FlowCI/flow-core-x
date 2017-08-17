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

import com.flow.platform.core.exception.NotImplementedException;
import java.util.Properties;

/**
 * @author yang
 */
public class AppServerLoader implements SystemInfoLoader {

    private final String tomcatBase = "catalina.home";

    @Override
    public SystemInfo load() {
        Properties properties = System.getProperties();
        if (properties.getProperty(tomcatBase) == null) {
            return new SystemInfo();
        }

        String s = "java -cp lib/catalina.jar org.apache.catalina.util.ServerInfo";
        throw new NotImplementedException();
    }
}
