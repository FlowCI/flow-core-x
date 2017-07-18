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
 * @author gy@fir.im
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
