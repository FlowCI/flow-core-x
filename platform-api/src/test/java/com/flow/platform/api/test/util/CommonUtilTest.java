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
import com.flow.platform.api.util.CommonUtil;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class CommonUtilTest extends TestBase {

    @Test
    public void should_generate_success(){
        Map<BigInteger, String> hashMap = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            BigInteger id = CommonUtil.randomId();
            hashMap.put(id, String.valueOf(i));
        }
        Assert.assertEquals(10000, hashMap.size());
    }
}
