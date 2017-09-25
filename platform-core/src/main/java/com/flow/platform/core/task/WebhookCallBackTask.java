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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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

        if (retry >= MAX_RETRY_TIMES) {
            LOGGER.warn("Webhook fail with max retry time for '%s'", webhook);
            return;
        }

        boolean shouldRetry;
        int nextRetry = retry + 1;
        int nextRetryWaitTime = nextRetry * 20 * 1000; // milliseconds

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpEntity entity = new StringEntity(webhookable.toJson());
            HttpPost post = new HttpPost(webhook);
            post.addHeader("Content-Type", "application/json;charset=utf-8");
            post.setEntity(entity);

            CloseableHttpResponse response = httpclient.execute(post);
            ResponseHandler<String> handler = new BasicResponseHandler();
            LOGGER.trace("Webhook response - %s", handler.handleResponse(response));
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                LOGGER.trace("webhook been reported: '%s'", webhook);
                return;
            }

            int nextRetryForSeconds = nextRetryWaitTime / 1000;
            LOGGER.trace("Webhook response %s, retry %s after %s seconds", statusCode, nextRetry, nextRetryForSeconds);
            shouldRetry = true;

        } catch (UnsupportedEncodingException | ClientProtocolException e) {
            // JSON data or http protocol exception, exit directly
            LOGGER.error("Webhook data or http protocol error, exit ", e);
            shouldRetry = false;
        } catch (IOException e) {
            LOGGER.warn("Webhook request error", e);
            shouldRetry = true;
        }

        if (shouldRetry) {
            try {
                Thread.sleep(nextRetryWaitTime); // 0, 20, 40, 60, 80 in seconds
            } catch (InterruptedException ignored) {

            } finally {
                callWebhook(nextRetry);
            }
        }
    }
}
