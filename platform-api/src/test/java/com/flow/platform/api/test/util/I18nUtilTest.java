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
import com.flow.platform.api.util.I18nUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class I18nUtilTest extends TestBase {

    @Test
    public void should_get_message_success() {
        I18nUtil.initLocale("en", "US");
        Assert.assertEquals("hello", I18nUtil.translate("hello"));
        I18nUtil.initLocale("zh", "CN");
        Assert.assertEquals("您好", I18nUtil.translate("hello"));
    }

    @Test
    public void should_get_message_null() {
        I18nUtil.initLocale("en", "US");
        Assert.assertNull(I18nUtil.translate("AAAA"));
        I18nUtil.initLocale("zh", "CN");
        Assert.assertNull(I18nUtil.translate("AAAA"));
    }
}
