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

package com.flow.platform.api.task;

import static com.flow.platform.api.service.node.NodeCrontabService.KEY_BRANCH;
import static com.flow.platform.api.service.node.NodeCrontabService.KEY_NODE_PATH;

import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.service.user.UserService;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author yang
 */
@Log4j2
public class NodeCrontabTask implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final String branch = context.getMergedJobDataMap().getString(KEY_BRANCH);
        final String path = context.getMergedJobDataMap().getString(KEY_NODE_PATH);

        final JobService jobService = (JobService) context.getMergedJobDataMap().get("jobService");
        final NodeService nodeService = (NodeService) context.getMergedJobDataMap().get("nodeService");
        final UserService userService = (UserService) context.getMergedJobDataMap().get("userService");

        log.debug("Branch {} with node path {}", branch, path);

        try {
            Node flow = nodeService.find(path).root();
            User owner = userService.findByEmail(flow.getCreatedBy());
            Map<String, String> envs = EnvUtil.build(GitEnvs.FLOW_GIT_BRANCH.name(), branch);
            jobService.createFromFlowYml(path, JobCategory.SCHEDULER, envs, owner);
        } catch (Throwable e) {
            throw new JobExecutionException(e.getMessage());
        }
    }
}
