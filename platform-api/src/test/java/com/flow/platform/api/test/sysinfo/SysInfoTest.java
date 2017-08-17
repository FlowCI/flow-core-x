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

package com.flow.platform.api.test.sysinfo;

import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.sysinfo.DBInfoLoader;
import com.flow.platform.core.sysinfo.DBInfoLoader.DBGroupName;
import com.flow.platform.core.sysinfo.JvmLoader;
import com.flow.platform.core.sysinfo.JvmLoader.JvmGroup;
import com.flow.platform.core.sysinfo.SystemInfo;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author yang
 */
public class SysInfoTest extends TestBase {

    @Value("${jdbc.url}")
    private String dbUrl;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    final String driverName = "com.mysql.jdbc.Driver";

    @Test
    public void should_load_jvm_properties() throws Throwable {
        SystemInfo jvmInfo = new JvmLoader().load();
        Assert.assertEquals(JvmGroup.values().length, jvmInfo.size());
    }

    @Test
    public void should_load_mysql_properties() throws Throwable {
        SystemInfo dbInfo = new DBInfoLoader(driverName, dbUrl, username, password).load();
        Assert.assertNotNull(dbInfo);

        Map<String, String> map = dbInfo.get(DBGroupName.MYSQL);
        Assert.assertEquals(5, map.size());
    }

}
