package com.flow.platform.dao.test;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Created by gy@fir.im on 27/06/2017.
 * Copyright fir.im
 */
@Transactional
public class CmdDaoTest extends TestBase {

    @Test
    public void should_save_all_fields() throws Throwable {
        // given:
        Cmd cmd = new Cmd("zone", "agent", CmdType.SHUTDOWN, "123");
        cmd.setId(UUID.randomUUID().toString());
        cmd.setStatus(CmdStatus.KILLED);
        cmd.setOutputEnvFilter("FLOW_VAR");
        cmd.setCreatedDate(ZonedDateTime.now());
        cmd.setUpdatedDate(ZonedDateTime.now());
        cmd.setFinishedDate(ZonedDateTime.now());
        cmd.setLogPaths(Lists.newArrayList("/test/log/path"));
        cmd.setPriority(1);
        cmd.setTimeout(10L);
        cmd.setWebhook("http://webhook.com");
        cmd.getInputs().put("VAR_1", "1");
        cmd.getInputs().put("VAR_2", "2");
        cmd.setWorkingDir("/");
        cmd.setSessionId("session-id");

        // when:
        cmdDao.save(cmd);

        // then:
        Cmd loaded = cmdDao.get(cmd.getId());
        Assert.assertEquals(cmd.getZoneName(), loaded.getZoneName());
        Assert.assertEquals(cmd.getAgentName(), loaded.getAgentName());
        Assert.assertEquals(cmd.getType(), loaded.getType());
        Assert.assertEquals(cmd.getCmd(), loaded.getCmd());
        Assert.assertEquals(cmd.getStatus(), loaded.getStatus());
        Assert.assertEquals(cmd.getOutputEnvFilter(), loaded.getOutputEnvFilter());
        Assert.assertEquals(cmd.getWebhook(), loaded.getWebhook());
    }

    @Test
    public void should_get_cmd_by_agent_path() throws Throwable {
        // given:
        final String zoneName = "zone-1";

        Cmd cmd0 = new Cmd(zoneName, "agent-1", CmdType.CREATE_SESSION, "hello");
        cmd0.setStatus(CmdStatus.KILLED);
        cmd0.setId(UUID.randomUUID().toString());
        cmdDao.save(cmd0);

        Cmd cmd1 = new Cmd(zoneName, "agent-2", CmdType.SHUTDOWN, "hello");
        cmd1.setStatus(CmdStatus.RUNNING);
        cmd1.setId(UUID.randomUUID().toString());
        cmdDao.save(cmd1);

        // when: get all cmd for zone
        List<Cmd> result = cmdDao.list(null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        // when: get all cmd for zone
        result = cmdDao.list(new AgentPath(zoneName, null), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());

        // when: get all cmd for zone and agent
        result = cmdDao.list(new AgentPath(zoneName, "agent-2"), null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(cmd1, result.get(0));

        // when: get cmd for agent by type and status
        result = cmdDao.list(new AgentPath(zoneName, "agent-2"),
                Sets.newHashSet(CmdType.SHUTDOWN), Sets.newHashSet(CmdStatus.RUNNING));
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(cmd1, result.get(0));
    }
}
