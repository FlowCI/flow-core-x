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

package com.flow.platform.agent.test;

import com.flow.platform.agent.ReportManager;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * @author gy@fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class ReportManagerTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    private ReportManager reportManager = ReportManager.getInstance();

    @Test
    public void should_report_cmd_status() {
        // when:
        stubFor(post(urlEqualTo("/cmd/report"))
                .withRequestBody(matchingJsonPath("$.id"))
                .withRequestBody(matchingJsonPath("$.status"))
                .withRequestBody(matchingJsonPath("$.result"))
                .willReturn(aResponse()
                        .withStatus(200)));

        // then:
        CmdResult mockResult = new CmdResult();
        boolean result = reportManager.cmdReportSync("cmdId-001", CmdStatus.RUNNING, mockResult);
        Assert.assertTrue(result);
    }

    @Test
    public void should_upload_zipped_cmd_log() {
        // given:
        stubFor(post(urlEqualTo("/cmd/log/upload"))
                .willReturn(aResponse().withStatus(200)));

        ClassLoader classLoader = ReportManagerTest.class.getClassLoader();
        URL resource = classLoader.getResource("test-cmd-id.out.zip");
        Path path = Paths.get(resource.getFile());

        // when:
        boolean result = reportManager.cmdLogUploadSync("cmdId-001", path);

        // then:
        Assert.assertTrue(result);
    }
}
