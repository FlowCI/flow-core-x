package com.flow.platform.dao.test;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/**
 * Created by gy@fir.im on 27/06/2017.
 * Copyright fir.im
 */
public class CmdDaoTest extends TestBase {

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
