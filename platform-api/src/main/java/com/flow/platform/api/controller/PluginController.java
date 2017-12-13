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

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.request.PluginListParam;
import com.flow.platform.api.domain.sync.Sync;
import com.flow.platform.api.domain.sync.SyncRepo;
import com.flow.platform.api.domain.sync.SyncTask;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.AgentService;
import com.flow.platform.api.service.SyncService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.plugin.dao.PluginDao;
import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.service.PluginService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */
@RestController
@RequestMapping("/plugins")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    @Autowired
    private PluginDao pluginStoreService;

    @Autowired
    private SyncService syncService;

    @Autowired
    private AgentService agentService;

    /**
     * @api {Get} /plugins List
     * @apiName List
     * @apiParam {String} name plugin name
     * @apiGroup Plugin
     * @apiDescription List all plugins
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         status: ["INSTALLED"],
     *         labels: ["fir"],
     *         keyword: "xxxxx"
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     [
     *          {
     *              "name": fir-cli,
     *              "details": http://github.com/fir/fir-cli,
     *              "labels": ["fir", "plugin"],
     *              "author": xx@fir.im,
     *              "tag": "1.4.9"
     *              "currentTag": "1.5.0",
     *              "platform": ["windows", "mac"],
     *              "status": "INSTALLED" | "PENDING" | "IN_QUEUE" | "INSTALLING"
     *          },
     *
     *          ....
     *     ]
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping
    public Collection<Plugin> index(@RequestBody(required = false) PluginListParam param) {
        if (param == null) {
            param = new PluginListParam();
        }

        return pluginService.list(param.getStatus(), param.getKeyword(), param.getLabels());
    }

    /**
     * @api {Get} /plugins/{name} Get
     * @apiName Get
     * @apiParam {String} name plugin name
     * @apiGroup Plugin
     * @apiDescription Get plugin detail
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *         "name": fir-cli,
     *         "details": http://github.com/fir/fir-cli,
     *         "labels": ["fir", "plugin"],
     *         "author": xx@fir.im,
     *         "platform": ["windows", "mac"],
     *         "status": "INSTALLED" | "PENDING" | "IN_QUEUE" | "INSTALLING"
     *     }
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @GetMapping("/{name}")
    public Plugin get(@PathVariable String name) {
        return pluginService.find(name);
    }

    /**
     * @api {Post} /plugins/refresh Refresh
     * @apiName Refresh
     * @apiGroup Plugin
     * @apiDescription Reload plugin list from main git repo
     *
     * @apiErrorExample {json} Error-Response:
     *   HTTP/1.1 400
     *   {
     *      "message": xxx
     *   }
     */
    @PostMapping("/refresh")
    public void reload() {
        pluginStoreService.refreshCache();
    }

    /**
     * @api {Post} /plugins/install/{name} Install
     * @apiName Install
     * @apiParam {String} name plugin name
     * @apiGroup Plugin
     * @apiDescription Install plugin
     *
     * @apiErrorExample {json} Error-Response:
     *  HTTP/1.1 400
     *  {
     *      "message": xxx
     *  }
     */
    @PostMapping("/install/{name}")
    public void install(@PathVariable String name) {
        if (Objects.isNull(name)) {
            throw new IllegalParameterException("plugin name is null");
        }
        pluginService.install(name);
    }

    /**
     * @api {Delete} /plugins/uninstall/{name} Uninstall
     * @apiName Uninstall
     * @apiParam {String} name plugin name
     * @apiGroup Plugin
     * @apiDescription Uninstall plugin
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @DeleteMapping("/uninstall/{name}")
    public void uninstall(@PathVariable String name) {
        if (Objects.isNull(name)) {
            throw new IllegalParameterException("plugin name is null");
        }
        pluginService.uninstall(name);
    }

    /**
     * @api {Post} /plugins/stop/{name} Stop
     * @apiName Stop
     * @apiParam {String} name plugin name
     * @apiGroup Plugin
     * @apiDescription Stop install plugin
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping("/stop/{name}")
    public void stop(@PathVariable String name) {
        if (Objects.isNull(name)) {
            throw new IllegalParameterException("plugin name is null");
        }
        pluginService.stop(name);
    }

    /**
     * @api {Post} /plugins/sync Sync
     * @apiName Sync
     * @apiGroup Plugin
     * @apiDescription Sync plugin to agent
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         zone: xxx,
     *         name: xxx
     *     }
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping(path = "/sync/start")
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public void startSync(@RequestBody(required = false) AgentPath path) {
        // sync plugin for agent
        if (!Objects.isNull(path) && !path.isEmpty()) {
            syncService.sync(path);
            return;
        }

        // sync plugin for all online agents
        forEachOnlineAgent(agentService.list(), agent -> {
            syncService.sync(agent.getPath());
        });
    }

    /**
     * @api {Post} /plugins/sync/progress In Progress List
     * @apiName In Progress List
     * @apiGroup Plugin
     * @apiDescription Plugin list which been not syned on agent but in progress
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         zone: xxx,
     *         name: xxx
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *         "default#agent-1": {
     *              "total": 2,
     *              "syncQueue": [
     *                  {
     *                      repo: {
     *                          name: "fir-cli"
     *                          tag: "1.4.9"
     *                      },
     *
     *                      syncType: CREATE
     *                  },
     *
     *                  ....
     *              ]
     *         }
     *     }
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping(path = "/sync/progress")
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public Map<String, SyncTask> getProgressList(@RequestBody(required = false) AgentPath path) {
        if (!Objects.isNull(path) && !path.isEmpty()) {
            SyncTask syncTask = syncService.getSyncTask(path);
            if (Objects.isNull(syncTask)) {
                return Collections.emptyMap();
            }
            return ImmutableMap.<String, SyncTask>of(syncTask.getPath().toString(), syncTask);
        }

        List<Agent> list = agentService.list();
        Map<String, SyncTask> tasks = new HashMap<>(list.size());

        forEachOnlineAgent(list, agent -> {
            SyncTask syncTask = syncService.getSyncTask(agent.getPath());

            if (Objects.isNull(syncTask)) {
                tasks.put(agent.getPath().toString(), SyncTask.EMPTY);
                return;
            }

            tasks.put(agent.getPath().toString(), syncTask);
        });

        return tasks;
    }

    /**
     * @api {Post} /plugins/sync/synced Synced List
     * @apiName Synced List
     * @apiGroup Plugin
     * @apiDescription Installed plugin list for agent
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         zone: xxx,
     *         name: xxx
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *     {
     *         "default#agent-1": [
     *              {
     *                  name: "fir-cli",
     *                  tag: "1.4.9"
     *              }
     *        ]
     *     }
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping(path = "/sync/synced")
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public Map<String, Set<SyncRepo>> getSyncedList(@RequestBody(required = false) AgentPath path) {
        if (!Objects.isNull(path) && !path.isEmpty()) {
            Sync sync = syncService.get(path);
            if (Objects.isNull(sync)) {
                return Collections.emptyMap();
            }
            return ImmutableMap.<String, Set<SyncRepo>>of(sync.getPath().toString(), sync.getRepos());
        }

        List<Agent> list = agentService.list();
        Map<String, Set<SyncRepo>> installed = new HashMap<>(list.size());

        forEachOnlineAgent(list, agent -> {
            Sync sync = syncService.get(agent.getPath());
            installed.put(agent.getPath().toString(), sync.getRepos());
        });

        return installed;
    }

    private void forEachOnlineAgent(List<Agent> list, Consumer<Agent> consumer) {
        for (Agent agent : list) {
            if (agent.getStatus() == AgentStatus.OFFLINE) {
                continue;
            }
            consumer.accept(agent);
        }
    }
}
