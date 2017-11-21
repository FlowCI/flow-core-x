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

import com.flow.platform.plugin.util.GitHttpUtil;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        List<String> tags = GitHttpUtil.listTags(fullName);

        // then: tags is not 0
        Assert.assertNotEquals(0, tags.size());

        // then: tags should equal 14
        Assert.assertEquals(14, tags.size());
    }

    @Test
    public void should_fetch_latest_tag_success() {
        String fullName = "yunheli/info";

        // when: fetch latest tags
        String tag = GitHttpUtil.fetchLatestTag(fullName);

        // then: tag is not null
        Assert.assertNotNull(tag);

        // then: tag is 2.3
        Assert.assertEquals("2.3", tag);
    }

    @Test
    public void should_download_tag_success() {
        String fullName = "yunheli/info";
        String tag = "2.3";
        Path path = Paths.get("/tmp");

        // when: download tag zip
        GitHttpUtil.downloadTagZip(path, fullName, tag);
        File file = new File(Paths.get(path.toString(), tag + ".zip").toString());

        // then: file should exist is true
        Assert.assertEquals(true, file.exists());
        file.delete();

        // then: file should exist is false
        Assert.assertEquals(false, file.exists());
    }
}
