package com.flow.platform.cc.controller;

import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cloud.InstanceManager;
import com.flow.platform.domain.Zone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Created by gy@fir.im on 11/06/2017.
 * Copyright fir.im
 */
@RestController
@RequestMapping("/zone")
public class ZoneController {

    @Autowired
    private ZoneService zoneService;

    @GetMapping(path = "/list")
    public Collection<Zone> list() {
        return zoneService.getZones();
    }

    /**
     * Get could instance manager name by zone name
     *
     * @return Instance manager name
     */
    @GetMapping(path = "/cloud/manager")
    public String cloudInstanceManager(@RequestParam String zoneName) {
        Zone zone = zoneService.getZone(zoneName);
        InstanceManager instanceManager = zoneService.findInstanceManager(zone);
        if (instanceManager == null) {
            return "null";
        }
        return instanceManager.getClass().getSimpleName();
    }
}
