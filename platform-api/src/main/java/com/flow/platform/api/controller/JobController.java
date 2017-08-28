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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.util.I18nUtil;
import com.flow.platform.api.util.PathUtil;
import java.util.Collection;
import java.util.List;
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
public class JobController {

    @Autowired
    private JobService jobService;

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
     * @api {Post} /jobs/:flowName create
     * @apiName CreateJob
     * @apiParam {String} [flowName] flow name
     * @apiGroup Job
     * @apiDescription run job and create job
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *  {
     *       "nodePath": "Fflow",
     *       "nodeName": "Fflow",
     *       "createdAt": 1503884024,
     *       "updatedAt": 1503884024,
     *       "number": 8,
     *       "result": {
     *          "outputs": {
     *              "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *              "FLOW_VERSION": "1.0.0.0.0",
     *              "FLOW_YML_STATUS": "FOUND",
     *              "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *              "FLOW_STATUS": "READY",
     *              "FLOW_GIT_COMMIT_ID": "1234",
     *              "FLOW_GIT_CHANGELOG": "test",
     *              "FLOW_GIT_COMPARE_ID": "1234..12121",
     *              "FLOW_GIT_BRANCH": "master",
     *              "FLOW_GIT_COMMITER": "WILL"
     *          },
     *          "duration": 9,
     *          "exitCode": 0,
     *          "logPaths": [
     *
     *          ],
     *          "status": "SUCCESS",
     *          "cmdId": "c1ff44c3-f047-4ba6-a39d-9e0c7c0682fd",
     *          "nodeTag": "FLOW",
     *          "startTime": 1503884025,
     *          "finishTime": 1503884034,
     *          "createdAt": 1503884024,
     *          "updatedAt": 1503884024
     *       },
     *       "envs": {
     *          "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *          "FLOW_VERSION": "1.0.0.0.0",
     *          "FLOW_YML_STATUS": "FOUND",
     *          "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *          "FLOW_STATUS": "READY"
     *       }
     *   }
     */
    @PostMapping(path = "/{flowName}")
    public Job create(@PathVariable String flowName) {
        return jobService.createJob(PathUtil.build(flowName));
    }

    /**
     * @api {Get} /jobs/:flowName list
     * @apiName ListJobs
     * @apiParam {String} [flowName] flow name
     * @apiGroup Job
     * @apiDescription get jobs from flowName
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *  [{
     *       "nodePath": "Fflow",
     *       "nodeName": "Fflow",
     *       "createdAt": 1503884024,
     *       "updatedAt": 1503884024,
     *       "number": 8,
     *       "result": {
     *          "outputs": {
     *              "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *              "FLOW_VERSION": "1.0.0.0.0",
     *              "FLOW_YML_STATUS": "FOUND",
     *              "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *              "FLOW_STATUS": "READY",
     *              "FLOW_GIT_COMMIT_ID": "1234",
     *              "FLOW_GIT_CHANGELOG": "test",
     *              "FLOW_GIT_COMPARE_ID": "1234..12121",
     *              "FLOW_GIT_BRANCH": "master",
     *              "FLOW_GIT_COMMITER": "WILL"
     *          },
     *          "duration": 9,
     *          "exitCode": 0,
     *          "logPaths": [
     *
     *          ],
     *          "status": "SUCCESS",
     *          "cmdId": "c1ff44c3-f047-4ba6-a39d-9e0c7c0682fd",
     *          "nodeTag": "FLOW",
     *          "startTime": 1503884025,
     *          "finishTime": 1503884034,
     *          "createdAt": 1503884024,
     *          "updatedAt": 1503884024
     *       },
     *       "envs": {
     *          "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *          "FLOW_VERSION": "1.0.0.0.0",
     *          "FLOW_YML_STATUS": "FOUND",
     *          "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *          "FLOW_STATUS": "READY"
     *       }
     *   }]
     */
    @GetMapping
    public Collection<Job> index(@RequestParam(required = false) String flowName) {
        return jobService.listJobs(flowName, null);
    }

    /**
     * @api {get} /jobs/:flowName/:buildNumber get
     * @apiName GetJob
     * @apiParam {String} [flowName] flow name
     * @apiParam {String} [buildNumber] build number
     * @apiGroup Job
     * @apiDescription get job from flowName and buildNumber
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *  {
     *       "nodePath": "Fflow",
     *       "nodeName": "Fflow",
     *       "createdAt": 1503884024,
     *       "updatedAt": 1503884024,
     *       "number": 8,
     *       "result": {
     *          "outputs": {
     *              "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *              "FLOW_VERSION": "1.0.0.0.0",
     *              "FLOW_YML_STATUS": "FOUND",
     *              "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *              "FLOW_STATUS": "READY",
     *              "FLOW_GIT_COMMIT_ID": "1234",
     *              "FLOW_GIT_CHANGELOG": "test",
     *              "FLOW_GIT_COMPARE_ID": "1234..12121",
     *              "FLOW_GIT_BRANCH": "master",
     *              "FLOW_GIT_COMMITER": "WILL"
     *          },
     *          "duration": 9,
     *          "exitCode": 0,
     *          "logPaths": [
     *
     *          ],
     *          "status": "SUCCESS",
     *          "cmdId": "c1ff44c3-f047-4ba6-a39d-9e0c7c0682fd",
     *          "nodeTag": "FLOW",
     *          "startTime": 1503884025,
     *          "finishTime": 1503884034,
     *          "createdAt": 1503884024,
     *          "updatedAt": 1503884024
     *       },
     *       "envs": {
     *          "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *          "FLOW_VERSION": "1.0.0.0.0",
     *          "FLOW_YML_STATUS": "FOUND",
     *          "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *          "FLOW_STATUS": "READY"
     *       }
     *   }
     */
    @GetMapping(path = "/{flowName}/{buildNumber}")
    public Job show(@PathVariable String flowName, @PathVariable Integer buildNumber) {
        return jobService.find(flowName, buildNumber);
    }

    @GetMapping(path = "/{flowName}/{buildNumber}/nodes")
    public List<NodeResult> indexNodeResults(@PathVariable String flowName, @PathVariable Integer buildNumber) {
        return jobService.listNodeResult(flowName, buildNumber);
    }

    /**
     * @api {Get} /jobs/status/latest latestJob
     * @apiName GetLatestJob
     * @apiParam {String} [flowPaths] flow paths
     * @apiGroup Job
     * @apiDescription get latest jobs with flowPaths
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *  [{
     *       "nodePath": "Fflow",
     *       "nodeName": "Fflow",
     *       "createdAt": 1503884024,
     *       "updatedAt": 1503884024,
     *       "number": 8,
     *       "result": {
     *          "outputs": {
     *              "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *              "FLOW_VERSION": "1.0.0.0.0",
     *              "FLOW_YML_STATUS": "FOUND",
     *              "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *              "FLOW_STATUS": "READY",
     *              "FLOW_GIT_COMMIT_ID": "1234",
     *              "FLOW_GIT_CHANGELOG": "test",
     *              "FLOW_GIT_COMPARE_ID": "1234..12121",
     *              "FLOW_GIT_BRANCH": "master",
     *              "FLOW_GIT_COMMITER": "WILL"
     *          },
     *          "duration": 9,
     *          "exitCode": 0,
     *          "logPaths": [
     *
     *          ],
     *          "status": "SUCCESS",
     *          "cmdId": "c1ff44c3-f047-4ba6-a39d-9e0c7c0682fd",
     *          "nodeTag": "FLOW",
     *          "startTime": 1503884025,
     *          "finishTime": 1503884034,
     *          "createdAt": 1503884024,
     *          "updatedAt": 1503884024
     *       },
     *       "envs": {
     *          "FLOW_WORKSPACE": "~/flow-platform/test/id/1/1/3",
     *          "FLOW_VERSION": "1.0.0.0.0",
     *          "FLOW_YML_STATUS": "FOUND",
     *          "FLOW_GIT_WEBHOOK": "http://localhost:8088/hooks/git/Fflow",
     *          "FLOW_STATUS": "READY"
     *       }
     *   }]
     */
    @PostMapping(path = "/status/latest")
    public Collection<Job> latestStatus(@RequestBody List<String> flowPaths) {
        return jobService.listJobs(null, flowPaths);
    }

    /**
     * @api {Post} /jobs/:flowName/:buildNumber/stop stop
     * @apiName StopJob
     * @apiParam {String} [flowName] flow name
     * @apiParam {String} [buildNumber] build number
     * @apiGroup Job
     * @apiDescription stop job from flowName and buildNumber
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *     true or false
     */
    @PostMapping(path = "/{flowName}/{buildNumber}/stop")
    public Boolean stopJob(@PathVariable String flowName, @PathVariable Integer buildNumber) {
        return jobService.stopJob(flowName, buildNumber);
    }
}
