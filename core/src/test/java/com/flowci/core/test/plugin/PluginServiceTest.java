/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.plugin;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.RepoCloneEvent;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.ObjectWrapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author yang
 */
@WireMockTest(httpPort = 8080)
public class PluginServiceTest extends SpringScenario {

    private static final String RepoURL = "http://localhost:8000/plugin/repo.json";

    @Autowired
    private PluginService pluginService;

    @BeforeEach
    void mockPluginRepo() throws IOException {
        InputStream load = load("plugin-repo.json");
        stubFor(get(urlPathEqualTo("/plugin/repo.json"))
                .willReturn(aResponse()
                        .withBody(StringHelper.toString(load))
                        .withHeader("Content-Type", "application/json")));
    }

    @Test
    void should_clone_plugin_repo() throws Throwable {
        // init counter
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Plugin> pluginWrapper = new ObjectWrapper<>();
        addEventListener((ApplicationListener<RepoCloneEvent>) event -> {
            pluginWrapper.setValue(event.getPlugin());
            counter.countDown();
        });

        // when:
        pluginService.load(new UrlResource(RepoURL));
        counter.await(30, TimeUnit.SECONDS);

        // then:
        assertNotNull(pluginWrapper.getValue());
    }
}
