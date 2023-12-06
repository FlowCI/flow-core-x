/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.job.controller;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.auth.controller.WebAuth;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.core.job.service.TtyService;
import com.flowci.core.user.domain.User;
import com.flowci.common.exception.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class TtyController {

    private final WebAuth webAuth;

    private final SpringEventManager eventManager;

    private final TtyService ttyService;

    public TtyController(WebAuth webAuth, SpringEventManager eventManager, TtyService ttyService) {
        this.webAuth = webAuth;
        this.eventManager = eventManager;
        this.ttyService = ttyService;
    }

    @MessageExceptionHandler(AuthenticationException.class)
    public void onAuthException(AuthenticationException e) {
        TtyCmd.In in = (TtyCmd.In) e.getExtra();

        TtyCmd.Out out = new TtyCmd.Out()
                .setId(in.getId())
                .setAction(in.getAction())
                .setSuccess(false)
                .setError(e.getMessage());

        eventManager.publish(new TtyStatusUpdateEvent(this, out));
    }

    @MessageMapping("/tty/{jobId}/{b64Path}/open")
    public void open(@DestinationVariable String jobId, @DestinationVariable String b64Path, MessageHeaders headers) {
        TtyCmd.In in = new TtyCmd.In()
                .setId(jobId)
                .setNodePath(StringHelper.fromBase64(b64Path))
                .setAction(TtyCmd.Action.OPEN);

        validate(in, headers);
        ttyService.execute(in);
    }

    @MessageMapping("/tty/{jobId}/{b64Path}/shell")
    public void shell(@DestinationVariable String jobId,
                      @DestinationVariable String b64Path,
                      @Payload String script,
                      MessageHeaders headers) {
        TtyCmd.In in = new TtyCmd.In()
                .setId(jobId)
                .setNodePath(StringHelper.fromBase64(b64Path))
                .setAction(TtyCmd.Action.SHELL)
                .setInput(script);

        validate(in, headers);
        ttyService.execute(in);
    }

    @MessageMapping("/tty/{jobId}/{b64Path}/close")
    public void close(@DestinationVariable String jobId, @DestinationVariable String b64Path, MessageHeaders headers) {
        TtyCmd.In in = new TtyCmd.In()
                .setId(jobId)
                .setNodePath(StringHelper.fromBase64(b64Path))
                .setAction(TtyCmd.Action.CLOSE);

        validate(in, headers);
        ttyService.execute(in);
    }

    private void validate(TtyCmd.In in, MessageHeaders headers) {
        try {
            User user = webAuth.validate(headers);
            if (!user.isAdmin()) {
                throw new AuthenticationException("Admin permission is required");
            }
        } catch (AuthenticationException e) {
            e.setExtra(in);
            throw e;
        }
    }
}
