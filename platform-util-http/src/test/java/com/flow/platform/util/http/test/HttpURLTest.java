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

import com.flow.platform.util.http.HttpURL;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class HttpURLTest {

    @Test
    public void should_convert_url_from_string() throws Throwable {
        HttpURL url = HttpURL.build("http://localhost/");
        Assert.assertEquals("http://localhost", url.toString());
        Assert.assertEquals("http", url.toURL().getProtocol());
        Assert.assertEquals(-1, url.toURL().getPort());

        url.append("/flow-test/").append("test/");
        Assert.assertEquals("http://localhost/flow-test/test", url.toString());

        // verify url parameter
        url.withParam("cmdId", "1").withParam("taskId", "/test");
        Assert.assertEquals("http://localhost/flow-test/test?cmdId=1&taskId=%2Ftest", url.toString());
    }
}
