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

package com.flow.platform.api.test.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.flow.platform.api.domain.Response;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.HttpUtil;
import com.flow.platform.domain.Jsonable;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.UnsupportedEncodingException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author yh@firim
 */

@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class HttpUtilTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Test
    public void should_get_success() {
        String url = "http://127.0.0.1:8080/some/thing";
        String msg = "Hello world!";
        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withBody(msg)));
        String res = HttpUtil.get(url);
        Assert.assertEquals(msg, res);
    }

    @Test
    public void should_get_error() {
        String url = "http://127.0.0.1:8080/some/thing";
        String msg = "Hello world!";
        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withStatus(404)
                .withBody(msg)));
        String res = HttpUtil.get(url);
        Assert.assertEquals(null, res);
    }

    @Test
    public void should_get_object() {
        String url = "http://127.0.0.1:8080/some/thing";
        String body = "{\"message\": \"ok\"}";
        stubFor(get(urlEqualTo("/some/thing"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(body)));
        String res = HttpUtil.get(url);
        Assert.assertEquals(body, res);
        Response response = Jsonable.parse(res, Response.class);
        Assert.assertNotNull(response);
    }

    @Test
    public void should_post_success() throws UnsupportedEncodingException {
        String url = "http://127.0.0.1:8080/some/aa";
        String body = "{\"message\": \"ok\"}";
        stubFor(post(urlEqualTo("/some/aa"))
            .withRequestBody(equalToJson(body))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(body)));
        String res = HttpUtil.post(url, body);
        Assert.assertEquals(body, res);
    }

    @Test
    public void should_post_error() throws UnsupportedEncodingException {
        String url = "http://127.0.0.1:8080/some/bb";
        String body = "{\"message\": \"ok\"}";
        stubFor(post(urlEqualTo("/some/bb"))
            .withRequestBody(equalToJson(body))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody(body)));
        String res = HttpUtil.post(url, body);
        Assert.assertEquals(null, res);
    }

    @Test
    public void should_put_success() throws UnsupportedEncodingException {
        String url = "http://127.0.0.1:8080/some/put";
        String body = "{\"message\": \"ok\"}";
        stubFor(put(urlEqualTo("/some/put"))
            .withRequestBody(equalToJson(body))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody(body)));
        String res = HttpUtil.put(url, body);
        Assert.assertEquals(body, res);
    }

    @Test
    public void should_put_error() throws UnsupportedEncodingException {
        String url = "http://127.0.0.1:8080/some/error";
        String body = "{\"message\": \"ok\"}";
        stubFor(put(urlEqualTo("/some/error"))
            .withRequestBody(equalToJson(body))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody(body)));
        String res = HttpUtil.put(url, body);
        Assert.assertEquals(null, res);
    }
}
