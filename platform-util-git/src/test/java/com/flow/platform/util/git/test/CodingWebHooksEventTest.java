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

package com.flow.platform.util.git.test;

import com.flow.platform.util.git.hooks.CodingEvents.Hooks;
import com.flow.platform.util.git.hooks.GitHookEventFactory;
import com.flow.platform.util.git.model.GitEventType;
import com.flow.platform.util.git.model.GitPushTagEvent;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class CodingWebHooksEventTest {

    @Test
    public void should_convert_to_push_event_obj() throws Throwable {
        // given:
        String pushEventContent = loadWebhookSampleJson("coding/webhook_push.json");
        Map<String, String> dummyHeader = new HashMap<>();
        dummyHeader.put(Hooks.HEADER, Hooks.EVENT_TYPE_PUSH);

        // when: build event from header and json content
        GitPushTagEvent pushEvent = (GitPushTagEvent) GitHookEventFactory.build(dummyHeader, pushEventContent);
        Assert.assertNotNull(pushEvent);

        // then:
        Assert.assertEquals(GitEventType.PUSH, pushEvent.getType());
        Assert.assertEquals(GitSource.CODING, pushEvent.getGitSource());

        Assert.assertEquals("4841b089ecf8dd3dd0010f61bd649bfcc21c1fe8", pushEvent.getBefore());
        Assert.assertEquals("b972e2edd91e85ec25ec28c29d7dc3e823f28e8a", pushEvent.getAfter());
        Assert.assertEquals("update .flow.yml test test\n", pushEvent.getMessage());
        Assert.assertEquals(
            "https://coding.net/u/benqyang2006/p/flowclibasic/git/commit/b972e2edd91e85ec25ec28c29d7dc3e823f28e8a",
            pushEvent.getHeadCommitUrl());

        Assert.assertEquals("4841b089ecf8...b972e2edd91e", pushEvent.getCompareId());
        Assert.assertEquals("https://coding.net/u/benqyang2006/p/flowclibasic/git/compare/4841b089ecf8...b972e2edd91e",
            pushEvent.getCompareUrl());

        Assert.assertEquals("benqyang2006", pushEvent.getUserId());
        Assert.assertEquals("benqyang2006", pushEvent.getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", pushEvent.getUserEmail());
    }

    private static String loadWebhookSampleJson(String classPath) throws IOException {
        URL resource = GitLabHooksEventTest.class.getClassLoader().getResource(classPath);
        return Files.toString(new File(resource.getFile()), Charset.forName("UTF-8"));
    }

}
