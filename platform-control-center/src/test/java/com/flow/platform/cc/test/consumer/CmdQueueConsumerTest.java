package com.flow.platform.cc.test.consumer;

import com.flow.platform.cc.service.CmdService;
import com.flow.platform.cc.service.ZoneService;
import com.flow.platform.cc.test.TestBase;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.CmdBase;
import com.flow.platform.domain.CmdType;
import com.flow.platform.domain.Zone;
import com.rabbitmq.client.Channel;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * Created by gy@fir.im on 20/06/2017.
 * Copyright fir.im
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class CmdQueueConsumerTest extends TestBase {

    private final static String ZONE = "ut-test-zone-for-queue";

    @Autowired
    private Channel cmdSendChannel;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private ZoneService zoneService;

    @Value("${mq.exchange.name}")
    private String cmdExchangeName;

    @Autowired
    private String cmdConsumeQueue;

    @Before
    public void before() {
        zoneService.createZone(new Zone(ZONE, "mock-cloud-provider"));
    }

    @Test
    public void should_receive_cmd_via_queue() throws Throwable {
        // given:
        String agentName = "mock-agent-1";
        AgentPath agentPath = createMockAgent(ZONE, agentName);
        Thread.sleep(1000);

        // when: send cmd by rabbit mq with cmd exchange name
        CmdBase mockCmd = new CmdBase(ZONE, agentName, CmdType.RUN_SHELL, "echo hello");
        cmdSendChannel.basicPublish(cmdExchangeName, "", null, mockCmd.toBytes());
        Thread.sleep(1000);

        // then: cmd should received in zookeeper agent node
        byte[] raw = zkClient.getData(zkHelper.getZkPath(agentPath), false, null);
        CmdBase received = CmdBase.parse(raw, CmdBase.class);
        Assert.assertEquals(mockCmd, received);
    }

    @Test
    public void should_re_enqueue_if_no_agent() throws Throwable {
        // when: send cmd without available agent
        CmdBase mockCmd = new CmdBase(ZONE, null, CmdType.RUN_SHELL, "echo hello");
        cmdSendChannel.basicPublish(cmdExchangeName, "", null, mockCmd.toBytes());
        Thread.sleep(3000); // wait for re_enqueue
    }

    private static HttpResponse httpSend(final HttpUriRequest request) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse response = client.execute(request);
            return response;
        }
    }
}
