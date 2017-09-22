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

import com.flow.platform.api.domain.SearchCondition;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.service.LogService;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.job.SearchService;
import com.flow.platform.api.util.I18nUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.StringUtil;
import com.flow.platform.util.git.model.GitEventType;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping(path = "/jobs")
public class JobController extends NodeController {

    private final static Logger LOGGER = new Logger(JobController.class);

    @Autowired
    private JobService jobService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private LogService logService;

    @ModelAttribute
    public void setLocale(@RequestParam(required = false) String locale) {
        if (locale == null) {
            I18nUtil.initLocale("en", "US");
            return;
        }

        if (locale.equals("zh-CN")) {
            I18nUtil.initLocale("zh", "CN");
        }

        if (locale.equals("en-US")) {
            I18nUtil.initLocale("en", "US");
        }
    }

    /**
     * @api {post} /jobs/:root Create
     * @apiParam {String} root flow node path
     * @apiGroup Jobs
     * @apiDescription Create job by flow node path,
     * FLOW_STATUS must be READY and YML contnet must be provided
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      nodePath: flow-integration,
     *      number: 1,
     *      nodeName: flow-integration,
     *      status: CREATED,
     *      envs: {
     *          FLOW_GIT_BRANCH: xxx
     *          FLOW_GIT_: xxx
     *      }
     *      createdAt: 154123211,
     *      updatedAt: 154123211,
     *      result: {
     *          key: {
     *             path: flow-integration
     *          },
     *          outputs: {
     *              FLOW_ENV_OUT_1: xxxx,
     *              FLOW_ENV_OUT_2: xxxx
     *          },
     *          duration: 0,
     *          status: PENDING,
     *          cmdId: xxxx,
     *          nodeTag: FLOW,
     *          order: 5
     *          startTime: 154123211,
     *          finishTime: 154123211,
     *          createdAt: 154123211,
     *          updatedAt: 154123211
     *      },
     *
     *      childrenResult: [
     *          {
     *              key: {
     *                  path: flow-integration/step1
     *              },
     *              duration: 0,
     *              status: PENDING,
     *              cmdId: xxx,
     *              outputs: {
     *                  FLOW_ENV_OUT_1: xx
     *              },
     *              order: 0
     *          },
     *
     *          {
     *              key: {
     *                  path: flow-integration/step2
     *              },
     *              duration: 0,
     *              status: PENDING,
     *              cmdId: xxx,
     *              outputs: {
     *                  FLOW_ENV_OUT_1: xx
     *              },
     *              order: 1
     *          }
     *      ]
     *  }
     */
    @PostMapping(path = "/{root}")
    public Job create() {
        String path = getNodePathFromUrl();
        return jobService.createJob(path, GitEventType.MANUAL);
    }

    /**
     * @api {get} /jobs/:root List
     * @apiParam {String} [root] flow node path, return all jobs if not presented
     * @apiGroup Jobs
     * @apiDescription Get jobs by node path or list all jobs
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          Job response json see Create response
     *      },
     *
     *      {
     *          ...
     *      }
     *  ]
     */
    @GetMapping(path = "/{root}")
    public Collection<Job> index() {
        String path = getNodePathFromUrl();

        List<String> paths = null;
        if (path != null) {
            paths = Lists.newArrayList(path);
        }

        return jobService.list(paths, false);
    }

    /**
     * @api {get} /jobs/:root/:buildNumber Show
     * @apiParam {String} root flow node path
     * @apiParam {String} buildNumber job build number
     * @apiGroup Jobs
     * @apiDescription Get job by path and build number
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      Job response json see Create response
     *  }
     */
    @GetMapping(path = "/{root}/{buildNumber}")
    public Job show(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.find(path, buildNumber);
    }

    /**
     * @api {get} /jobs/:root/:buildNumber/yml Get YML
     * @apiParam {String} root flow node path
     * @apiParam {String} buildNumber job build number
     * @apiGroup Jobs
     * @apiDescription Get job yml content
     *
     * @apiSuccessExample {yml} Success-Response
     *
     *  - flows:
     *      - envs:
     *          FLOW_ENV: xxx,
     *      - steps:
     *          name: xxx
     *          ...
     */
    @GetMapping(path = "/{root}/{buildNumber}/yml")
    public String yml(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.findYml(path, buildNumber);
    }

    /**
     * @api {get} /jobs/:root/:buildNumber/nodes List Nodes
     * @apiParam {String} root flow node path
     * @apiParam {String} buildNumber job build number
     * @apiGroup Jobs
     * @apiDescription Get all sub node results
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          key: {
     *              jobId: xxx,
     *              path: flow-name/step-name
     *          }
     *          outputs: {
     *              FLOW_ENV_OUT_1: xxxx,
     *              FLOW_ENV_OUT_2: xxxx
     *          },
     *          duration: 0,
     *          status: PENDING,
     *          cmdId: xxxx,
     *          nodeTag: FLOW,
     *          startTime: 154123211,
     *          finishTime: 154123211,
     *          createdAt: 154123211,
     *          updatedAt: 154123211
     *      },
     *
     *      {
     *          ...
     *      }
     *  ]
     */
    @GetMapping(path = "/{root}/{buildNumber}/nodes")
    public List<NodeResult> indexNodeResults(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.listNodeResult(path, buildNumber);
    }

    /**
     * @api {get} /jobs/:root/:buildNumber/:stepOrder/log Get log
     * @apiParam {String} root flow node path
     * @apiParam {String} buildNumber job build number
     * @apiParam {String} stepOrder step Order
     * @apiGroup Jobs
     * @apiDescription Get job log
     *
     * @apiSuccessExample {string} Success-Response
     *
     *  log content
     */
    @GetMapping(path = "/{root}/{buildNumber}/{stepOrder}/log")
    public String stepLogs(@PathVariable Integer buildNumber, @PathVariable Integer stepOrder) {
        String path = getNodePathFromUrl();
        try {
            return logService.findNodeLog(path, buildNumber, stepOrder);
        } catch (Throwable e){
            LOGGER.warn("log not found: %s", e.getMessage());
            return StringUtil.EMPTY;
        }
    }


    /**
     * @api {post} /jobs/:root/:buildNumber/stop Stop
     * @apiParam {String} root flow node path
     * @apiParam {String} buildNumber job build number
     * @apiGroup Jobs
     * @apiDescription Stop job by node path and build number
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      value: true
     *  }
     */
    @PostMapping(path = "/{root}/{buildNumber}/stop")
    public Job stopJob(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.stopJob(path, buildNumber);
    }

    /**
     * @api {post} /jobs/status/latest Latest
     * @apiParam {Array} paths List of flow node path
     * @apiGroup Jobs
     * @apiDescription Get latest job for flow nodes
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      {
     *          Job response json see Create response
     *      },
     *
     *      {
     *          ..
     *      }
     *  ]
     */
    @PostMapping(path = "/status/latest")
    public Collection<Job> latestStatus(@RequestBody List<String> paths) {
        return jobService.list(paths, true);
    }

    @PostMapping(path = "/{root}/search")
    public List<Job> search(@RequestBody SearchCondition condition){
        String path = getNodePathFromUrl();

        List<String> paths = null;
        if (path != null) {
            paths = Lists.newArrayList(path);
        }

        return searchService.search(condition, paths);
    }

    /**
     * @api {get} /jobs/:buildNumber/log/download download job full log
     * @apiParam {buildNumber} build number
     * @apiGroup Jobs
     * @apiDescription build number for job
     *
     * @apiSuccessExample {json} Success-Response
     *   job.log file
     */
    @GetMapping(path = "/{root}/{buildNumber}/log/download")
    public org.springframework.core.io.Resource jobLog(@PathVariable Integer buildNumber,
        HttpServletResponse httpResponse) {
        String path = getNodePathFromUrl();
            httpResponse.setHeader("Content-Disposition",
                String.format("attachment; filename=%s", String.format("%s-%s.zip", path, buildNumber)));
        return logService.findJobLog(path, buildNumber);
    }

}