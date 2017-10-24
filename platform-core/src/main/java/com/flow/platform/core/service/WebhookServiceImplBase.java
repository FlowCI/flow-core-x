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

package com.flow.platform.core.service;

import com.flow.platform.core.task.WebhookCallBackTask;
import com.flow.platform.domain.Webhookable;
import com.google.common.base.Strings;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public abstract class WebhookServiceImplBase implements WebhookService {

    @Autowired
    private Executor taskExecutor;

    @Override
    public void webhookCallback(Webhookable webhookable) {
        if (webhookable == null) {
            return;
        }

        if (Strings.isNullOrEmpty(webhookable.getWebhook())) {
            return;
        }

        taskExecutor.execute(new WebhookCallBackTask(webhookable));
    }
}
