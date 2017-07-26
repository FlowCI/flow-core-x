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

package com.flow.platform.api.util;

import com.flow.platform.util.Logger;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * @author yh@firim
 */
public final class RestClient implements Runnable {

    private final static Logger LOGGER = new Logger(RestClient.class);
    private final static int MAX_RETRY_TIMES = 5;

    private String body;
    private String url;
    private HttpMethod method = HttpMethod.POST;

    public enum HttpMethod implements Serializable {

        POST("POST"),

        PUT("PUT"),

        GET("GET");

        private String name;

        HttpMethod(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public RestClient(HttpMethod method, String body, String url) {
        this.method = method;
        this.body = body;
        this.url = url;
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
            CloseableHttpResponse response = httpResponse(httpclient);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println(String.format("url: %s, body: %s, method: %s send success", url, body, method));
                return;
            }

            shouldRetry = true;
        } catch (UnsupportedEncodingException | ClientProtocolException e) {
            // JSON data or http protocol exception, exit directly
            shouldRetry = false;
            System.out.println(String.format(
                "url: %s, body: %s, method: %s send error, UnsupportedEncodingException | ClientProtocolException: %s",
                url, body, method, e.toString()));
        } catch (IOException e) {
            shouldRetry = true;
            System.out.println(String
                .format("url: %s, body: %s, method: %s send error, IOException: %s", url, body, method, e.toString()));
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

    private CloseableHttpResponse httpResponse(CloseableHttpClient httpClient) throws IOException {
        CloseableHttpResponse response = null;
        switch (method){
            case GET:
                HttpGet httpGet = new HttpGet(url);
                response = httpClient.execute(httpGet);
                break;
            case POST:
                HttpPost httpPost = new HttpPost(url);
                HttpEntity entity = new StringEntity(body);
                httpPost.setEntity(entity);
                response = httpClient.execute(httpPost);
                break;
            case PUT:
                HttpPut httpPut = new HttpPut(url);
                HttpEntity putEntity = new StringEntity(body);
                httpPut.setEntity(putEntity);
                response = httpClient.execute(httpPut);
                break;
        }
        return response;
    }
}





