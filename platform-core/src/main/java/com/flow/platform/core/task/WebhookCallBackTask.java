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

package com.flow.platform.core.task;

import com.flow.platform.domain.Webhookable;
import com.flow.platform.util.Logger;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import java.io.UnsupportedEncodingException;
import org.apache.http.entity.ContentType;

/**
 * Send webhook of cmd with max retry times
 *
 * @author gy@fir.im
 */
public final class WebhookCallBackTask implements Runnable {

    private final static Logger LOGGER = new Logger(WebhookCallBackTask.class);

    private final static int MAX_RETRY_TIMES = 5;

    private final Webhookable webhookable;

    public WebhookCallBackTask(Webhookable webhookable) {
        this.webhookable = webhookable;
    }

    @Override
    public void run() {
        callWebhook(0);
    }

    private void callWebhook(int retry) {
        String webhook = webhookable.getWebhook();

        try {
            HttpResponse<String> response = HttpClient
                .build(webhook)
                .post(webhookable.toJson())
                .withContentType(ContentType.APPLICATION_JSON)
                .retry(MAX_RETRY_TIMES)
                .bodyAsString();

            if (!response.hasSuccess()) {
                LOGGER.warn("Webhook fail with max retry time for '%s'", webhook);
                return;
            }

            LOGGER.trace("webhook been reported: '%s'", webhook);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Webhook request error", e);
        }
    }
}
