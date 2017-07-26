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
import com.flow.platform.util.Logger;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping
public class WebhookController {

    private final Logger LOGGER = new Logger(WebhookController.class);

    @Autowired
    private JobService jobService;

    @PostMapping(path = "/hooks")
    public Response execute(@RequestBody Cmd cmd,
        @RequestParam String identifier) throws UnsupportedEncodingException {
        identifier = UrlUtil.urlDecoder(identifier);
        LOGGER.trace(String
            .format("Webhook Comming Url: %s CmdType: %s CmdStatus: %s", cmd.getWebhook(), cmd.getType().toString(),
                cmd.getStatus().toString()));

        jobService.callback(identifier, cmd);
        return new Response("ok");
    }

}

