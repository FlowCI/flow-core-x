/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.flow.consumer;

import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.common.manager.SocketPushManager;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.flow.event.GitTestEvent.Status;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author yang
 */
@Slf4j
@Component
public class OnGitTestChange implements ApplicationListener<GitTestEvent> {

    @Getter
    public static class GitTestMessage {

        private final List<String> branches;

        private final Status status;

        private final String error;

        public GitTestMessage(GitTestEvent event) {
            this.branches = event.getBranches();
            this.status = event.getStatus();
            this.error = event.getError();
        }
    }

    @Autowired
    private String topicForGitTest;

    @Autowired
    private SocketPushManager socketPushManager;

    @Override
    public void onApplicationEvent(GitTestEvent event) {
        String topic = topicForGitTest + "/" + event.getFlowId();
        socketPushManager.push(topic, PushEvent.STATUS_CHANGE, new GitTestMessage(event));
        log.debug("Git test {} for flow {}", event.getStatus(), event.getFlowId());
    }
}
