package com.flow.platform.agent.test;

import com.flow.platform.agent.LogEventHandler;
import com.flow.platform.cmd.Log;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdType;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

/**
 * Created by gy@fir.im on 24/06/2017.
 * Copyright fir.im
 */
public class LogEventHandlerTest extends TestBase {

    @Test
    public void should_get_correct_format_for_socket_io() throws Throwable {
        // given:
        Cmd cmd = new Cmd("TestZone", "TestAgent", CmdType.RUN_SHELL, "hello");
        cmd.setId(UUID.randomUUID().toString());
        LogEventHandler logEventHandler = new LogEventHandler(cmd);

        // when:
        String mockLogContent = "hello";
        String socketIoData = logEventHandler.socketIoLogFormat(new Log(Log.Type.STDOUT, mockLogContent));

        // then:
        String expect = String.format("%s#%s#%s#%s", cmd.getZoneName(), cmd.getAgentName(), cmd.getId(), mockLogContent);
        Assert.assertEquals(expect, socketIoData);
    }
}
