package com.flow.platform.api.controller;

import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.service.SysInfoService;
import com.flow.platform.core.sysinfo.PropertySystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
import com.flow.platform.util.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
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
    private PlatformURL platformURL;

    /**
     * @api {get} /index API Information
     * @apiName Get API info
     * @apiGroup System Info
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
        return sysInfoService.system();
    }

    /**
     * @api {get} /index/all All System Information
     * @apiName Get All system info
     * @apiGroup System Info
     * @apiDescription Get flow api and control center system version, status, start time and its properties configuration
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          system: API,
     *          version: 0.0.1,
     *          status: RUNNING,
     *          startTime: 1502301233,
     *          info: {
     *              mq.host: apmq://localhost
     *              api.workspace: ${HOME}/test
     *          }
     *      },
     *
     *      {
     *          system: Control Center,
     *          version: 0.0.1,
     *          status: RUNNING,
     *          startTime: 1502301233,
     *          info: {
     *              mq.host: apmq://localhost
     *              api.workspace: ${HOME}/test
     *          }
     *      }
     *  ]
     */
    @GetMapping(path = "/index/all")
    public List<PropertySystemInfo> indexAll() {
        List<PropertySystemInfo> systems = new ArrayList<>(2);
        systems.add(sysInfoService.system());

        try {
            String res = HttpClient.build(platformURL.getSysIndexUrl()).get().bodyAsString().getBody();
            systems.add(PropertySystemInfo.parse(res, PropertySystemInfo.class));
        } catch (Throwable e) {
            throw new IllegalStatusException("Unable to load system info from control center");
        }

        return systems;
    }

    /**
     * @api {get} /sys/:system/info All Components Information
     * @apiParam {String="api","cc"} system System name of components
     *
     * @apiName Get all components information
     * @apiGroup System Info
     * @apiDescription Get flow api or flow control center all components info such as jvm version,
     * zookeeper information and so on
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          system: Java SE,
     *          version: 1.8.0
     *          status: RUNNING,
     *          type: JVM | DB | SERVER | ZK | MQ | CONFIG,
     *          info: {
     *              MEMORY: {
     *                  jvm.memory.max: 123123
     *                  jvm.memory.used: 123123
     *              },
     *              GENERAL: {
     *                  java.home: xxxx
     *              },
     *              OS: {
     *                  os.name: OSX,
     *                  os.version: 12.3
     *              }
     *          }
     *      }
     *  ]
     */
    @GetMapping(path = "/sys/{system}/info")
    public List<SystemInfo> componentInfo(@PathVariable String system) {
        Category targetSystem = Category.valueOf(system.toUpperCase());
        return sysInfoService.components(targetSystem, null);
    }

    /**
     * @api {get} /sys/:system/info/:type Component Information
     * @apiParam {String="api","cc"} system System name of components
     * @apiParam {String="jvm","db","server","zk","mq"} type Component type
     *
     * @apiName Get component information
     * @apiGroup System Info
     * @apiDescription Get flow api or flow control center components info such as jvm version,
     * zookeeper information and so on
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      system: Java SE,
     *      version: 1.8.0
     *      status: RUNNING,
     *      type: JVM | DB | SERVER | ZK | MQ | CONFIG,
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
    public SystemInfo componentInfo(@PathVariable String system, @PathVariable String type) {
        try {
            Category targetSystem = Category.valueOf(system.toUpperCase());
            SystemInfo.Type targetType = SystemInfo.Type.valueOf(type.toUpperCase());
            return sysInfoService.components(targetSystem, targetType).get(0);
        } catch (IllegalArgumentException e) {
            throw new IllegalParameterException(String.format("Cannot load system info of %s - %s", system, type));
        }
    }

    @RequestMapping("/403")
    public String forbidden(){
        return "403";
    }
}
