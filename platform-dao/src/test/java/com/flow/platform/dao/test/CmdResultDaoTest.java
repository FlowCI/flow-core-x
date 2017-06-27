package com.flow.platform.dao.test;

import com.flow.platform.domain.CmdResult;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by gy@fir.im on 23/06/2017.
 * Copyright fir.im
 */
public class CmdResultDaoTest extends TestBase {

    private CmdResult cmdResult;

    @Before
    public void before() {
        Map<String, String> output = new HashMap<>();
        output.put("FLOW_DAO_TEST", "hello");
        output.put("FLOW_DAO_TEST_1", "aa");

        cmdResult = new CmdResult();
        cmdResult.setCmdId(UUID.randomUUID().toString());
        cmdResult.setExitValue(1);
        cmdResult.setDuration(10L);
        cmdResult.setStartTime(ZonedDateTime.now());
        cmdResult.setFinishTime(ZonedDateTime.now());
        cmdResult.setExecutedTime(ZonedDateTime.now());
        cmdResult.setProcessId(1013);
        cmdResult.setTotalDuration(10L);
        cmdResult.setOutput(output);
        cmdResult.setExceptions(Lists.newArrayList(new RuntimeException("Dummy Exception")));
    }

    @Test
    public void should_save_cmd_with_jsonable_type() throws Throwable {
        // when: save
        cmdResultDao.save(cmdResult);

        // then:
        CmdResult loaded = cmdResultDao.get(cmdResult.getCmdId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(cmdResult.getExitValue(), loaded.getExitValue());
        Assert.assertEquals(cmdResult.getDuration(), loaded.getDuration());

        Assert.assertEquals(cmdResult.getOutput().size(), loaded.getOutput().size());
        Assert.assertEquals(cmdResult.getExceptions().size(), loaded.getExceptions().size());
    }

    @Ignore(value = "not finished yet")
    @Test
    public void should_update_only_for_not_null_field() throws Throwable {
        // given:
        cmdResultDao.save(cmdResult);

        // when:
        cmdResult.setExitValue(null);
        cmdResult.setOutput(null);
        cmdResult.setProcessId(null);
        cmdResultDao.update(cmdResult);

        // then:
        CmdResult loaded = cmdResultDao.get(cmdResult.getCmdId());
        Assert.assertNotNull(loaded.getExitValue());
        Assert.assertNotNull(loaded.getOutput());
        Assert.assertNotNull(loaded.getProcessId());
    }
}
