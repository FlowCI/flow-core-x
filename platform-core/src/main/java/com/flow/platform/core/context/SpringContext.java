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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * @author yang
 */
public class SpringContext implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private ApplicationEventMulticaster eventMulticaster;

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void registerApplicationListener(ApplicationListener<?> listener) {
        eventMulticaster.addApplicationListener(listener);
    }

    public void removeApplicationListener(ApplicationListener<?> listener) {
        eventMulticaster.removeApplicationListener(listener);
    }

    public void cleanApplictionListener() {
        eventMulticaster.removeAllListeners();
    }

    public Object getBean(String name) {
        try {
            return applicationContext.getBean(name);
        } catch (BeansException e) {
            return null;
        }
    }

    public String[] getBeanNameByType(Class<?> aClass) {
        return applicationContext.getBeanNamesForType(aClass);
    }

    public boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return applicationContext.isSingleton(name);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.eventMulticaster = (ApplicationEventMulticaster) applicationContext
            .getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
    }
}
