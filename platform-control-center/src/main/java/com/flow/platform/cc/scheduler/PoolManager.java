package com.flow.platform.cc.scheduler;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.ZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agent pool manager, will check idle instance per N seconds
 * <p>
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
@Component
public class PoolManager {

    private final static int MIN_IDLE_AGENT_SIZE = 5;

    private final static int NUM_OF_INSTANCE_TO_START = 5;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ZoneService zoneService;

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 60 * 1000)
    public void check() {
        for (String zone : zoneService.getZones()) {
            int numOfIdle = agentService.findAvailable(zone).size();

            if (numOfIdle <= MIN_IDLE_AGENT_SIZE) {
                startInstance(zone, NUM_OF_INSTANCE_TO_START);
            }
        }
    }

    private void startInstance(String zone, int numOfInstance) {

    }
}
