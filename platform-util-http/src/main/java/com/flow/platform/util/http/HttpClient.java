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

package com.flow.platform.util.http;

import static com.flow.platform.util.http.HttpResponse.EXCEPTION_STATUS_CODE;

import com.flow.platform.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * @author yang
 */
public class HttpClient {

    /**
     * Build basic http authorization header by user and pass
     */
    public static Map<String, String> buildHttpBasicAuthHeader(final String user, final String pass) {
        byte[] encodedBytes = Base64.encodeBase64((user + ":" + pass).getBytes());
        String userPass = new String(encodedBytes);

        Map<String, String> header = new HashMap<>(1);
        header.put("Authorization", "Basic " + userPass);
        return header;
    }

    /**
     * Create http client instance
     */
    public static HttpClient build(String url) {
        return new HttpClient(url);
    }

    private final static int HTTP_TIMEOUT = 5 * 1000;

    private final RequestConfig config = RequestConfig.custom()
        .setConnectTimeout(HTTP_TIMEOUT)
        .setConnectionRequestTimeout(HTTP_TIMEOUT)
        .setSocketTimeout(HTTP_TIMEOUT)
        .build();

    private final String url;

    private HttpRequestBase httpRequest;

    private int numOfRetry = 0;

    private int retried = 0;

    private CloseableHttpResponse failureResponse;

    private List<Throwable> exceptions = new LinkedList<>();

    private HttpClient(String url) {
        this.url = url;
    }

    public HttpClient retry(int numOfRetry) {
        this.numOfRetry = numOfRetry;
        return this;
    }

    public HttpClient post(String body) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        HttpEntity entity = new StringEntity(body);
        httpPost.setEntity(entity);
        httpRequest = httpPost;
        return this;
    }

    public HttpClient post(HttpEntity entity) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        httpRequest = httpPost;
        return this;
    }

    public HttpClient post() {
        httpRequest = new HttpPost(url);
        return this;
    }

    public HttpClient get() {
        httpRequest = new HttpGet(url);
        return this;
    }

    public HttpClient withContentType(final ContentType contentType) {
        withHeader("Content-Type", contentType.toString());
        return this;
    }

    public HttpClient withHeader(String name, String value) {
        requireHttpRequestInstance();
        httpRequest.addHeader(name, value);
        return this;
    }

    public HttpClient withHeader(Map<String, String> headers) {
        requireHttpRequestInstance();

        if (headers == null || headers.isEmpty()) {
            return this;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            withHeader(entry.getKey(), entry.getValue());
        }

        return this;
    }

    /**
     * Execute http request and process on consumer, and then close http connection
     */
    public HttpResponse<String> bodyAsString() {
        final List<HttpResponse<String>> wrapper = new ArrayList<>(1);

        exec(httpResponse -> {
            if (httpResponse == null) {
                wrapper.add(new HttpResponse<>(retried, EXCEPTION_STATUS_CODE, exceptions, StringUtil.EMPTY));
                return;
            }

            try {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                wrapper.add(new HttpResponse<>(retried, statusCode, exceptions, body));
            } catch (IOException e) {
                exceptions.add(e);
                wrapper.add(new HttpResponse<>(retried, EXCEPTION_STATUS_CODE, exceptions, StringUtil.EMPTY));
            }
        });

        return wrapper.get(0);
    }

    public void bodyAsStream(Consumer<HttpResponse<InputStream>> response) {
        exec(httpResponse -> {
            if (httpResponse == null) {
                response.accept(new HttpResponse<>(retried, EXCEPTION_STATUS_CODE, exceptions, null));
                return;
            }

            try {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                InputStream content = httpResponse.getEntity().getContent();
                response.accept(new HttpResponse<>(retried, statusCode, exceptions, content));
            } catch (IOException e) {
                exceptions.add(e);
                response.accept(new HttpResponse<>(retried, EXCEPTION_STATUS_CODE, exceptions, null));
            }
        });
    }

    private void exec(Consumer<CloseableHttpResponse> consumer) {
        requireHttpRequestInstance();

        if (retried > numOfRetry) {
            retried--;
            consumer.accept(failureResponse);
            return;
        }

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    consumer.accept(response);
                    return;
                }

                failureResponse = response;
                retried++;
                exec(consumer);
            }
        } catch (IOException e) {
            exceptions.add(e);

            retried++;
            exec(consumer);
        }
    }

    private void requireHttpRequestInstance() {
        if (httpRequest == null) {
            throw new IllegalArgumentException("The http GET POST PUT DELETE method must be set");
        }
    }
}
