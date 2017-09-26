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

package com.flow.platform.core.config;

import com.flow.platform.core.context.SpringContext;
import com.flow.platform.core.sysinfo.PropertySystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.flow.platform.util.DateUtil;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * @author yh@firim
 */
@ComponentScan({"com.flow.platform.core.service"})
public abstract class AppConfigBase {

    @Autowired
    private Environment env;

    @Bean
    public abstract ThreadPoolTaskExecutor taskExecutor();

    @Bean
    public SpringContext springContext() {
        return new SpringContext();
    }

    @Bean
    public PropertySystemInfo systemInfo() {
        PropertySystemInfo info = new PropertySystemInfo(Status.RUNNING);
        info.setName(getName());
        info.setVersion(getVersion());
        info.setStartTime(DateUtil.now());

        if (!(env instanceof StandardServletEnvironment)) {
            return info;
        }

        StandardServletEnvironment env = (StandardServletEnvironment) this.env;
        for (PropertySource<?> next : env.getPropertySources()) {
            if (next instanceof ResourcePropertySource) {
                Map<String, Object> source = ((ResourcePropertySource) next).getSource();
                info.setInfo(source);
            }
        }

        return info;
    }

    abstract protected String getName();

    abstract protected String getVersion();

}
