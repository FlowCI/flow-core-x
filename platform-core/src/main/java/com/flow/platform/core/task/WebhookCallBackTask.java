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
import lombok.extern.log4j.Log4j2;
import org.apache.http.entity.ContentType;

/**
 * Send webhook of cmd with max retry times
 *
 * @author gy@fir.im
 */
@Log4j2
public final class WebhookCallBackTask implements Runnable {

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
                log.warn("Webhook fail with max retry time for '{}'", webhook);
                return;
            }

            log.trace("webhook been reported: '{}'", webhook);
        } catch (UnsupportedEncodingException e) {
            log.warn("Webhook request error", e);
        }
    }
}
