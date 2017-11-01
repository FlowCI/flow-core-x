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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.git.GitEventEnvConverter;
import com.flow.platform.api.git.GitWebhookTriggerFinishEvent;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.StringUtil;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.hooks.GitHookEventFactory;
import com.flow.platform.util.git.model.GitEvent;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/hooks/git")
public class GitWebHookController extends NodeController {

    private final static Logger LOGGER = new Logger(GitWebHookController.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostMapping(path = "/{root}")
    public void onEventReceived(@RequestHeader HttpHeaders headers, HttpServletRequest request) {
        final String path = currentNodePath.get();
        Map<String, String> headerAsMap = headers.toSingleValueMap();

        String body;
        try {
            request.setCharacterEncoding(AppConfig.DEFAULT_CHARSET.name());
            body = CharStreams.toString(request.getReader());
        } catch (IOException e) {
            throw new IllegalStatusException("Cannot read raw body");
        }

        try {
            final GitEvent hookEvent = GitHookEventFactory.build(headerAsMap, body);
            LOGGER.trace("Git Webhook received: %s", hookEvent.toString());

            // reset flow yml status to not found otherwise yml cannot start to load
            Flow flow = nodeService.findFlow(path);
            nodeService.addFlowEnv(flow, EnvUtil.build(FlowEnvs.FLOW_YML_STATUS, YmlStatusValue.NOT_FOUND));

            // extract git related env variables from event, and temporary set to node for git loading
            final Map<String, String> gitEnvs = GitEventEnvConverter.convert(hookEvent);

            // get user email from git event
            final User user = new User(hookEvent.getUserEmail(), StringUtil.EMPTY, StringUtil.EMPTY);

            JobCategory jobCategory = GitEventEnvConverter.convert(hookEvent.getType());

            jobService.createWithYmlLoad(path, jobCategory, gitEnvs, user, (job) -> {
                applicationEventPublisher.publishEvent(new GitWebhookTriggerFinishEvent(job));
            });

        } catch (GitException | FlowException e) {
            LOGGER.warn("Cannot process web hook event: %s", e.getMessage());
        }
    }
}