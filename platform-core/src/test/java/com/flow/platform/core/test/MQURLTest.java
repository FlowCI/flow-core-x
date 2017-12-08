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

package com.flow.platform.core.test;

import com.flow.platform.core.sysinfo.MQLoader.MQURL;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class MQURLTest {

    @Test
    public void should_parse_mq_url_get_correct_info() throws Throwable {
        MQURL url = new MQURL("amqp://user:pass@host:10000/vhost");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertEquals("user", url.getUser());
        Assert.assertEquals("pass", url.getPass());
        Assert.assertEquals("host", url.getHost());
        Assert.assertEquals(10000, url.getPort().intValue());
        Assert.assertEquals("vhost", url.getvHost());
    }

    @Test
    public void should_parse_mq_url_with_encode() throws Throwable {
        MQURL url = new MQURL("amqp://user%61:%61pass@ho%61st:10000/v%2fhost");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertEquals("usera", url.getUser());
        Assert.assertEquals("apass", url.getPass());
        Assert.assertEquals("hoast", url.getHost());
        Assert.assertEquals(10000, url.getPort().intValue());
        Assert.assertEquals("v/host", url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_protocol() throws Throwable {
        MQURL url = new MQURL("amqp://");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertNull(url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertNull(url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertNull(url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_symbol() throws Throwable {
        MQURL url = new MQURL("amqp://:@/");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertEquals("", url.getUser());
        Assert.assertEquals("", url.getPass());
        Assert.assertNull(url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertEquals("", url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_user() throws Throwable {
        MQURL url = new MQURL("amqp://user@");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertEquals("user", url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertNull(url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertNull(url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_user_and_pass() throws Throwable {
        MQURL url = new MQURL("amqp://user:pass@");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertEquals("user", url.getUser());
        Assert.assertEquals("pass", url.getPass());
        Assert.assertNull(url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertNull(url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_host() throws Throwable {
        MQURL url = new MQURL("amqp://host");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertNull(url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertEquals("host", url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertNull(url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_port() throws Throwable {
        MQURL url = new MQURL("amqp://:10000");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertNull(url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertEquals("", url.getHost());
        Assert.assertEquals(10000, url.getPort().intValue());
        Assert.assertNull(url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_vhost() throws Throwable {
        MQURL url = new MQURL("amqp:///vhost");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertNull(url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertNull(url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertEquals("vhost", url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_host_and_empty_vhost() throws Throwable {
        MQURL url = new MQURL("amqp://host/");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertNull(url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertEquals("host", url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertEquals("", url.getvHost());
    }

    @Test
    public void should_parse_mq_url_only_with_host_and_slash_vhost() throws Throwable {
        MQURL url = new MQURL("amqp://host/%2f");

        Assert.assertEquals("amqp", url.getProtocol());
        Assert.assertNull(url.getUser());
        Assert.assertNull(url.getPass());
        Assert.assertEquals("host", url.getHost());
        Assert.assertNull(url.getPort());
        Assert.assertEquals("/", url.getvHost());
    }
}
