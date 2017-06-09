package com.flow.platform.agent.test;

import com.flow.platform.agent.Config;
import com.flow.platform.agent.ReportManager;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Created by gy@fir.im on 26/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class ReportManagerTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    private ReportManager reportManager = ReportManager.getInstance();

    @BeforeClass
    public static void init() {
        Config.AGENT_CONFIG = new AgentConfig(
                "http://localhost:3000/agent",
                "http://localhost:8080/cmd/status",
                "http://localhost:8080/cmd/log/upload");
    }

    @Test
    public void should_report_cmd_status() {
        // when:
        stubFor(post(urlEqualTo("/cmd/status"))
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
