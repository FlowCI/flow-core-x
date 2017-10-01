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

package com.flow.platform.util.http.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.fail;

import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author yang
 */

@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class HttpClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Test
    public void should_get_with_body_as_string() {
        final String url = "http://127.0.0.1:8080/some/thing";
        final String msg = "Hello world!";

        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withBody(msg)));

        HttpResponse<String> response = HttpClient.build(url).get().bodyAsString();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals(msg, response.getBody());
        Assert.assertEquals(false, response.hasException());
        Assert.assertEquals(0, response.getRetried());
    }

    @Test
    public void should_get_with_body_as_stream() {
        final String url = "http://127.0.0.1:8080/some/thing";
        final String msg = "Hello world!";

        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withBody(msg)));

        HttpClient.build(url).get().bodyAsStream(response -> {
            InputStream stream = response.getBody();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                StringBuilder content = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }

                Assert.assertEquals(msg, content.toString());
            } catch (IOException e) {
                fail();
            }
        });
    }

    @Test
    public void should_get_with_404_error() {
        final String url = "http://127.0.0.1:8080/some/thing";
        final String errorMessage = "Hello world!";

        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withStatus(404)
                .withBody(errorMessage)));

        HttpResponse<String> response = HttpClient.build(url).get().bodyAsString();
        Assert.assertEquals(404, response.getStatusCode());
        Assert.assertEquals(errorMessage, response.getBody());
        Assert.assertEquals(false, response.hasException());
        Assert.assertEquals(0, response.getRetried());
    }

    @Test
    public void should_get_with_retry_when_404_error() {
        final String url = "http://127.0.0.1:8080/some/thing";
        final String errorMessage = "Hello world!";

        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withStatus(404)
                .withBody(errorMessage)));

        HttpResponse<String> response = HttpClient.build(url).get().retry(5).bodyAsString();
        Assert.assertEquals(404, response.getStatusCode());
        Assert.assertEquals(errorMessage, response.getBody());
        Assert.assertEquals(5, response.getRetried());
    }

    @Test
    public void should_post_with_body_as_string() throws Throwable {
        final String url = "http://127.0.0.1:8080/some/aa";
        final String body = "{\"message\": \"ok\"}";

        stubFor(post(urlEqualTo("/some/aa"))
            .withRequestBody(equalToJson(body))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(body)));

        HttpResponse<String> response = HttpClient.build(url).post(body).bodyAsString();
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals(body, response.getBody());
        Assert.assertEquals(false, response.hasException());
        Assert.assertEquals(0, response.getRetried());
    }

    @Test
    public void should_post_with_400_error() throws UnsupportedEncodingException {
        final String url = "http://127.0.0.1:8080/some/bb";
        final String body = "{\"message\": \"error\"}";

        stubFor(post(urlEqualTo("/some/bb"))
            .withRequestBody(equalToJson(body))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody(body)));

        HttpResponse<String> response = HttpClient.build(url).post(body).bodyAsString();
        Assert.assertEquals(400, response.getStatusCode());
        Assert.assertEquals(body, response.getBody());
        Assert.assertEquals(false, response.hasException());
        Assert.assertEquals(0, response.getRetried());
    }
}
