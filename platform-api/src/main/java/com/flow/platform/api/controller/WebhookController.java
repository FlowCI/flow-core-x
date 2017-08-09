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

import com.flow.platform.api.domain.Response;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.util.UrlUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */
@RestController
@RequestMapping
public class WebhookController {

    private final Logger LOGGER = new Logger(WebhookController.class);

    @Autowired
    private JobService jobService;

    @PostMapping(path = "/hooks/cmd")
    public Response execute(@RequestBody Cmd cmd,
        @RequestParam String identifier) {
        identifier = UrlUtil.urlDecoder(identifier);
        if (identifier == null || cmd == null) {
            throw new IllegalParameterException(
                String.format("Mission Param Identifier - %s Or Cmd - %s", identifier, cmd.toJson()));
        }

        LOGGER.trace(String
            .format("Webhook Comming Url: %s CmdType: %s CmdStatus: %s", cmd.getWebhook(), cmd.getType(),
                cmd.getStatus()));

        jobService.callback(identifier, cmd);
        return new Response("ok");
    }

}

