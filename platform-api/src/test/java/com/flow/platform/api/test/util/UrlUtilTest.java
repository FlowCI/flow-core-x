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

import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.UrlUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class UrlUtilTest extends TestBase{

    @Test
    public void should_encode_success(){
        String rawData = "123";
        Assert.assertEquals(rawData, UrlUtil.urlEncoder(rawData));
        rawData = "/a/a";
        Assert.assertEquals("%2Fa%2Fa", UrlUtil.urlEncoder(rawData));
        rawData = null;
        Assert.assertEquals(null, UrlUtil.urlEncoder(rawData));
    }

    @Test
    public void should_decode_success(){
        String rawData = "123";
        Assert.assertEquals(rawData, UrlUtil.urlDecoder(rawData));
        rawData = "%2Fa%2Fa";
        Assert.assertEquals("/a/a", UrlUtil.urlDecoder(rawData));
        rawData = null;
        Assert.assertEquals(null, UrlUtil.urlDecoder(rawData));
    }
}
