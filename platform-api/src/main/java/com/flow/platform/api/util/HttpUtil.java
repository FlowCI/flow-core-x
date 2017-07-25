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
import java.io.UnsupportedEncodingException;
import java.util.function.Consumer;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * @author yh@firim
 */
public class HttpUtil {

    private static Integer TRY_TIMES = 5;

    private static Logger LOGGER = new Logger(HttpUtil.class);

    /**
     * http post
     *
     * @param url url
     * @param body body
     * @return String null or other
     */
    public static String post(String url, String body) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = new StringEntity(body);
        httpPost.setEntity(entity);
        final String[] res = {null};
        exec(httpPost, 1, (String item) -> {
            res[0] = item;
        });
        return res[0];
    }

    /**
     * http get
     *
     * @return string null or other
     */
    public static String get(String url) {
        HttpGet httpGet = new HttpGet(url);
        final String[] res = {null};
        exec(httpGet, 1, (String item) -> {
            res[0] = item;
        });
        return res[0];
    }

    /**
     * http put
     *
     * @return string null or other
     */
    public static String put(String url, String body) throws UnsupportedEncodingException {
        HttpPut httpPut = new HttpPut(url);
        HttpEntity entity = new StringEntity(body);
        httpPut.setEntity(entity);
        final String[] res = {null};
        exec(httpPut, 1, (String item) -> {
            res[0] = item;
        });
        return res[0];
    }

    private static void exec(HttpUriRequest httpUriRequest, Integer tryTimes, Consumer<String> consumer) {
        if (tryTimes > TRY_TIMES) {
            consumer.accept(null);
        } else {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                CloseableHttpResponse response = httpClient.execute(httpUriRequest);
                int statusCode = response.getStatusLine().getStatusCode();
                ResponseHandler<String> handler = new BasicResponseHandler();
                if (statusCode == 200) {
                    consumer.accept(handler.handleResponse(response));
                } else {
                    tryTimes += 1;
                    exec(httpUriRequest, tryTimes, consumer);
                }
            } catch (UnsupportedEncodingException | ClientProtocolException e) {
                // JSON data or http protocol exception, exit directly
                LOGGER.warn(String
                    .format("url: %s, method: %s, UnsupportedEncodingException | ClientProtocolException e: %s",
                        httpUriRequest.getURI().toString(), httpUriRequest.getMethod().toString(), e.toString()), e);
                tryTimes += 1;
                exec(httpUriRequest, tryTimes, consumer);
            } catch (IOException e) {
                LOGGER.warn(String
                    .format("url: %s, method: %s, IOException e: %s",
                        httpUriRequest.getURI().toString(), httpUriRequest.getMethod().toString(), e.toString()), e);
                tryTimes += 1;
                exec(httpUriRequest, tryTimes, consumer);
            }
        }
    }
}
