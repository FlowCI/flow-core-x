package com.flow.platform.cc.context;

import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.util.ZkHelper;
import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Created by gy@fir.im on 14/07/2017.
 * Copyright fir.im
 */
@Component
public class ContextInitHandler implements ApplicationListener<ContextRefreshedEvent> {

    private final static Logger LOGGER = new Logger(ContextInitHandler.class);

    @Autowired
    protected ZkHelper zkHelper;

    @Autowired
    private ZoneService zoneService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // init root node
        String path = zoneService.createRoot();
        LOGGER.trace("Root zookeeper node initialized: %s", path);

        // init zone nodes
        for (Zone zone : zkHelper.getDefaultZones()) {
            path = zoneService.createZone(zone);
            LOGGER.trace("Zone zookeeper node initialized: %s", path);
        }
    }
}
