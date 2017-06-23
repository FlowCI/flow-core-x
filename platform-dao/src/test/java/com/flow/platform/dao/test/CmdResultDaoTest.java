package com.flow.platform.dao.test;

import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.DateUtil;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by gy@fir.im on 23/06/2017.
 * Copyright fir.im
 */
public class CmdResultDaoTest extends TestBase {

    @Test
    public void should_save_cmd_with_jsonable_type() throws Throwable {
        // given:
        Map<String, String> output = new HashMap<>();
        output.put("FLOW_DAO_TEST", "hello");
        output.put("FLOW_DAO_TEST_1", "aa");

        CmdResult result = new CmdResult();
        result.setCmdId(UUID.randomUUID().toString());
        result.setExitValue(1);
        result.setDuration(10L);
        result.setStartTime(new Date());
        result.setFinishTime(new Date());
        result.setExecutedTime(new Date());
        result.setProcessId(1013);
        result.setTotalDuration(10L);
        result.setOutput(output);
        result.setExceptions(Lists.newArrayList(new RuntimeException("Dummy Exception")));

        // when: save
        cmdResultDao.save(result);

        // then:
        CmdResult loaded = cmdResultDao.findByCmdId(result.getCmdId());
        Assert.assertNotNull(loaded);
        Assert.assertEquals(result.getExitValue(), loaded.getExitValue());
        Assert.assertEquals(result.getDuration(), loaded.getDuration());

        Assert.assertEquals(result.getOutput().size(), loaded.getOutput().size());
        Assert.assertEquals(result.getExceptions().size(), loaded.getExceptions().size());
    }
}
