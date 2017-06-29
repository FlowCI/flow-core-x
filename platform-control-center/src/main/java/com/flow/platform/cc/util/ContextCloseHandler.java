package com.flow.platform.cc.util;

import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Listen spring application context event and shutdown running async tasks
 *
 * Created by gy@fir.im on 29/06/2017.
 * Copyright fir.im
 */
@Component
public class ContextCloseHandler implements ApplicationListener<ContextClosedEvent> {

    private final static Logger LOGGER = new Logger(ContextCloseHandler.class);

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOGGER.trace("Received app context closed event");

        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(60);

        taskExecutor.shutdown();
        LOGGER.trace("Async task shutdown");
    }
}
