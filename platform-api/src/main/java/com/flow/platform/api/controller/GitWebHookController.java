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
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.git.GitEventDataExtractor;
import com.flow.platform.api.git.GitWebhookTriggerFinishEvent;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.node.YmlService;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
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
    private YmlService ymlService;

    @Autowired
    private JobService jobService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ThreadLocal<User> currentUser;

    @PostMapping(path = "/{root}")
    public void onEventReceived(@RequestHeader HttpHeaders headers, HttpServletRequest request) {
        final String path = getNodePathFromUrl();

        Map<String, String> headerAsMap = headers.toSingleValueMap();
        String body;
        try {
            body = CharStreams.toString(request.getReader());
        } catch (IOException e) {
            throw new IllegalStatusException("Cannot read raw body");
        }

        try {
            final GitEvent hookEvent = GitHookEventFactory.build(headerAsMap, body);
            LOGGER.trace("Webhook received: %s", hookEvent.toString());

            // extract git related env variables from event
            final Map<String, String> gitEnvs = GitEventDataExtractor.extract(hookEvent);

            ymlService.loadYmlContent(path, yml -> {
                LOGGER.trace("Yml content has been loaded for path : " + path);

                try {
                    // start job
                    Job job = jobService.createJob(path, hookEvent.getType(), gitEnvs);

                    applicationEventPublisher.publishEvent(new GitWebhookTriggerFinishEvent(job));
                } catch (Throwable e) {
                    LOGGER.warn("Fail to create job for path : " + path);
                }
            });

        } catch (GitException e) {
            LOGGER.warn("Cannot process web hook event: %s", e.getMessage());
        }
    }
}