package com.flow.platform.agent.test;

import com.flow.platform.agent.Config;
import com.flow.platform.agent.ReportManager;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.*;
import org.junit.runners.MethodSorters;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Created by gy@fir.im on 26/05/2017.
 * Copyright fir.im
 */
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class ReportManagerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    private ReportManager reportManager = ReportManager.getInstance();

    @BeforeClass
    public static void init() {
        Config.AGENT_CONFIG = new AgentConfig(
                "http://localhost:3000/agent",
                "http://localhost:8080/cmd/status");
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
        boolean result = reportManager.cmdReportSync("cmdId-001", Cmd.Status.RUNNING, mockResult);
        Assert.assertTrue(result);
    }
}
