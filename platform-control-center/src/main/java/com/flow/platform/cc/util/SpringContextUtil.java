package com.flow.platform.cc.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Created by gy@fir.im on 06/06/2017.
 * Copyright fir.im
 */
@Component(value = "springContextUtil")
public class SpringContextUtil implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public Object getBean(String name) {
        try {
            return applicationContext.getBean(name);
        } catch (BeansException e) {
            return null;
        }
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
    }
}
