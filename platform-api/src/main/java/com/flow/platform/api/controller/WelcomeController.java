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
 * @author gy@fir.im
 */
@RestController
@RequestMapping("/")
public class WelcomeController {

    @Autowired
    private SysInfoService sysInfoService;

    @GetMapping(path = "/index")
    public String hello() {
        return "{\"flow-platform\": \"api\"}";
    }

    @GetMapping(path = "/sys/{system}/info/{type}")
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
