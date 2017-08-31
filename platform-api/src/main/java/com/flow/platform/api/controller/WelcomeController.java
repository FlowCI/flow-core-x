package com.flow.platform.api.controller;

import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.service.SysInfoService;
import com.flow.platform.core.sysinfo.PropertySystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
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

    @Autowired
    private PropertySystemInfo systemInfo;

    /**
     * @api {get} /index System Information
     * @apiName Get system info
     * @apiGroup Welcome
     * @apiDescription Get flow api system version, status, start time and its properties configuration
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      system: API,
     *      version: 0.0.1,
     *      status: RUNNING,
     *      startTime: 1502301233,
     *      info: {
     *          mq.host: apmq://localhost
     *          api.workspace: ${HOME}/test
     *      }
     *  }
     */
    @GetMapping(path = "/index")
    public PropertySystemInfo index() {
        return systemInfo;
    }

    /**
     * @api {get} /sys/:system/info/:type Component Information
     * @apiParam {String="api","cc"} system System name of components
     * @apiParam {String="jvm","db","server","zk","mq"} type Component type
     *
     * @apiName Get component information
     * @apiGroup Welcome
     * @apiDescription Get flow api or flow control center components info such as jvm version,
     * zookeeper information and so on
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      system: Java SE,
     *      version: 1.8.0
     *      status: RUNNING,
     *      info: {
     *          MEMORY: {
     *              jvm.memory.max: 123123
     *              jvm.memory.used: 123123
     *          },
     *          GENERAL: {
     *              java.home: xxxx
     *          },
     *          OS: {
     *              os.name: OSX,
     *              os.version: 12.3
     *          }
     *      }
     *
     *  }
     */
    @GetMapping(path = "/sys/{system}/info/{type}")
    public SystemInfo getSystemInfo(@PathVariable String system, @PathVariable(required = false) String type) {
        try {
            Category targetSystem = Category.valueOf(system.toUpperCase());
            SystemInfo.Type targetType = null;

            if (!Strings.isNullOrEmpty(type)) {
                targetType = SystemInfo.Type.valueOf(type.toUpperCase());
            }

            return sysInfoService.get(targetSystem, targetType);
        } catch (IllegalArgumentException e) {
            throw new IllegalParameterException(String.format("Cannot load system info of %s - %s", system, type));
        }
    }

    @RequestMapping("/403")
    public String forbidden(){
        return "403";
    }
}
