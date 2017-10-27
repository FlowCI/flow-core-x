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

package com.flow.platform.cc.test.dao;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdLog;
import com.flow.platform.domain.CmdType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class CmdLogDaoTest extends TestBase {

    private String cmdId = "abcdef";

    @Before
    public void before() {
        Cmd cmd = new Cmd("default", "agent", CmdType.RUN_SHELL, "echo a");
        cmd.setId(cmdId);
        cmdDao.save(cmd);
    }

    @Test
    public void should_find_success() {
        CmdLog cmdLog = cmdLogDao.get(cmdId);
        Assert.assertNotNull(cmdLog);
    }

    @Test
    public void should_update_success() {

        String logPath = "/tmp/log.path";
        CmdLog cmdLog = cmdLogDao.get(cmdId);
        cmdLog.setLogPath(logPath);
        cmdLogDao.update(cmdLog);

        Assert.assertEquals(logPath, cmdDao.get(cmdId).getLogPath());
    }

}
