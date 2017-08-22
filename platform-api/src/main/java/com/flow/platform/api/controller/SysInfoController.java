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

package com.flow.platform.api.controller;

import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.service.SysInfoService;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/system")
public class SysInfoController {

    @Autowired
    private SysInfoService sysInfoService;

    @GetMapping(path = "/{system}/info/{type}")
    public SystemInfo getJvmInfo(@PathVariable String system, @PathVariable(required = false) String type) {

        try {
            SystemInfo.System targetSystem = SystemInfo.System.valueOf(system.toUpperCase());
            SystemInfo.Type targetType = null;

            if (!Strings.isNullOrEmpty(type)) {
                targetType = SystemInfo.Type.valueOf(type.toUpperCase());
            }

            return sysInfoService.get(targetSystem, targetType);
        } catch (IllegalArgumentException e) {
            throw new IllegalParameterException(String.format("Cannot load system info of %s - %s", system, type));
        }
    }
}
