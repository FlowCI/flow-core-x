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
package com.flow.platform.api.task;

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.Node;
import com.flow.platform.util.Logger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * @author gyfirim
 */
public final class SendCmdTask implements Runnable {

    private final static Logger LOGGER = new Logger(SendCmdTask.class);
    private final static int MAX_RETRY_TIMES = 5;

    private JobNode node;

    public SendCmdTask(Node node) {
        this.node = (JobNode) node;
    }

    @Override
    public void run() {
        sendRequest(0);
    }

    private void sendRequest(int retry) {
        if (retry >= MAX_RETRY_TIMES) {
            return;
        }

        boolean shouldRetry = false;
        int nextRetry = retry + 1;
        int nextRetryWaitTime = nextRetry * 20 * 1000; // milliseconds

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpEntity entity = new StringEntity(node.toJson());
            // TODO: job cmd url
            HttpPost post = new HttpPost(node.getName());
            post.setEntity(entity);
            CloseableHttpResponse response = httpclient.execute(post);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return;
            }

            shouldRetry = true;
        } catch (UnsupportedEncodingException | ClientProtocolException e) {
            // JSON data or http protocol exception, exit directly
            shouldRetry = false;
        } catch (IOException e) {
            shouldRetry = true;
        }

        if (shouldRetry) {
            try {
                Thread.sleep(nextRetryWaitTime); // 0, 20, 40, 60, 80 in seconds
            } catch (InterruptedException ignored) {

            } finally {
                sendRequest(nextRetry);
            }
        }

    }
}
