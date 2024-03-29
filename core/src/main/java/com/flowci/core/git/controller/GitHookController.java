/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.git.controller;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.git.converter.*;
import com.flowci.core.git.domain.GitTrigger;
import com.flowci.core.git.event.GitHookEvent;
import com.flowci.common.exception.ArgumentException;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller to handle callback from git provider
 *
 * @author yang
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
public class GitHookController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private TriggerConverter gitHubConverter;

    @Autowired
    private TriggerConverter gitLabConverter;

    @Autowired
    private TriggerConverter gogsConverter;

    @Autowired
    private TriggerConverter giteeConverter;

    @Autowired
    private TriggerConverter gerritConverter;

    @Autowired
    private SpringEventManager eventManager;

    private final Map<GitSource, TriggerConverter> converterMap = new HashMap<>(5);

    @PostConstruct
    public void createMapping() {
        converterMap.put(GitSource.GITHUB, gitHubConverter);
        converterMap.put(GitSource.GITLAB, gitLabConverter);
        converterMap.put(GitSource.GOGS, gogsConverter);
        converterMap.put(GitSource.GITEE, giteeConverter);
        converterMap.put(GitSource.GERRIT, gerritConverter);
    }

    /**
     * Gerrit:
     *   - patch-created - wall be called for each patch push
     * @param name
     * @throws IOException
     */
    @PostMapping("/{name}")
    public void onGitTrigger(@PathVariable String name) throws IOException {
        GitSourceWithEvent data = findGitSourceByHeader(request);
        Optional<GitTrigger> trigger = converterMap.get(data.source).convert(data.event, request.getInputStream());

        if (trigger.isEmpty()) {
            throw new ArgumentException("Unsupported git event {0}", data.event);
        }

        log.info("{} trigger received: {}", data.source, trigger.get());
        eventManager.publish(new GitHookEvent(this, name, trigger.get()));
    }

    private GitSourceWithEvent findGitSourceByHeader(HttpServletRequest request) {
        GitSourceWithEvent obj = new GitSourceWithEvent();

        // gogs, on the first place since it has github header..
        String event = request.getHeader(GogsConverter.Header);
        if (StringHelper.hasValue(event)) {
            obj.source = GitSource.GOGS;
            obj.event = event;
            return obj;
        }

        // github
        event = request.getHeader(GitHubConverter.Header);
        if (StringHelper.hasValue(event)) {
            obj.source = GitSource.GITHUB;
            obj.event = event;
            return obj;
        }

        // gitlab
        event = request.getHeader(GitLabConverter.Header);
        if (StringHelper.hasValue(event)) {
            obj.source = GitSource.GITLAB;
            obj.event = event;
            return obj;
        }

        // gitee
        event = request.getHeader(GiteeConverter.Header);
        if (StringHelper.hasValue(event)) {
            obj.source = GitSource.GITEE;
            obj.event = event;

            String pingInd = request.getHeader(GiteeConverter.HeaderForPing);
            if (Boolean.parseBoolean(pingInd)) {
                obj.event = GiteeConverter.Ping;
            }
            return obj;
        }

        // gerrit
        String gerritUrl = request.getHeader(GerritConverter.Header);
        // TODO: verify gerrit url
        if (StringHelper.hasValue(gerritUrl)) {
            obj.source = GitSource.GERRIT;
            obj.event =  GerritConverter.AllEvent;
            return obj;
        }

        throw new ArgumentException("Unsupported git event");
    }

    private static class GitSourceWithEvent {

        private GitSource source;

        private String event;

    }
}
