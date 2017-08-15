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

import com.flow.platform.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.hooks.GitHookEventFactory;
import com.flow.platform.util.git.model.GitEvent;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/hooks/git")
public class GitWebHookController {

    private final static Logger LOGGER = new Logger(GitWebHookController.class);

    @PostMapping(path = "/{flowname}")
    public void onEventReceived(@PathVariable("flowname") String flowName,
                                @RequestHeader HttpHeaders headers,
                                HttpServletRequest request) {

        Map<String, String> headerAsMap = headers.toSingleValueMap();
        String body;
        try {
            body = CharStreams.toString(request.getReader());
        } catch (IOException e) {
            throw new IllegalStatusException("Cannot read raw body");
        }

        try {
            GitEvent hookEvent = GitHookEventFactory.build(headerAsMap, body);
            LOGGER.trace("Webhook received: %s", hookEvent.toString());
        } catch (GitException e) {
            LOGGER.error("Cannot process web hook event", e);
        }
    }
}
