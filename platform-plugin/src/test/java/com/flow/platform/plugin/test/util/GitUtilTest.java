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

package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.util.GitUtil;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class GitUtilTest {

    @Test
    public void should_fetch_tags_success() {
        String fullName = "yunheli/info";

        // when: fetch tags
        List<String> tags = GitUtil.listTags(fullName);

        // then: tags is not 0
        Assert.assertNotEquals(0, tags.size());

        // then: tags should equal 14
        Assert.assertEquals(14, tags.size());
    }
}
