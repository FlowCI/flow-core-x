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

package com.flow.platform.core.context;

import com.flow.platform.core.util.SpringContextUtil;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public abstract class AbstractContextInitHandler implements ApplicationListener<ContextRefreshedEvent> {


    public abstract SpringContextUtil getSpringContextUtil();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // init queue consumer
        for (String eventClassName : getSpringContextUtil().getBeanNameByType(ContextEvent.class)) {
            ContextEvent eventClass = (ContextEvent) getSpringContextUtil().getBean(eventClassName);
            eventClass.start();
        }
    }
}