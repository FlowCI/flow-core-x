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

package com.flow.platform.api.test.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.service.SysInfoService;
import com.flow.platform.core.sysinfo.DBInfoLoader;
import com.flow.platform.core.sysinfo.DBInfoLoader.DBGroupName;
import com.flow.platform.core.sysinfo.GroupSystemInfo;
import com.flow.platform.core.sysinfo.JvmLoader;
import com.flow.platform.core.sysinfo.JvmLoader.JvmGroup;
import com.flow.platform.core.sysinfo.SystemInfo;
import com.flow.platform.core.sysinfo.SystemInfo.Category;
import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.flow.platform.core.sysinfo.SystemInfo.Type;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author yang
 */
public class SysInfoServiceTest extends TestBase {

    @Autowired
    private SysInfoService sysInfoService;

    @Value("${jdbc.url}")
    private String jdbcUrl;

    @Value("${jdbc.username}")
    private String jdbcUsername;

    @Value("${jdbc.password}")
    private String jdbcPassword;

    @Test
    public void should_get_api_settings_info() throws Throwable {

    }

    @Test
    public void should_get_api_jvm_info() throws Throwable {
        // when:
        GroupSystemInfo jvmInfo = (GroupSystemInfo) sysInfoService.components(Category.API, Type.JVM).get(0);
        Assert.assertNotNull(jvmInfo);

        // then:
        Assert.assertEquals(3, jvmInfo.size());

        Map<String, String> jvmOsInfo = jvmInfo.get(JvmGroup.OS);
        Assert.assertEquals(6, jvmOsInfo.size());

        Map<String, String> jvmGeneralInfo = jvmInfo.get(JvmGroup.GENERAL);
        Assert.assertEquals(5, jvmGeneralInfo.size());

        Map<String, String> jvmMemInfo = jvmInfo.get(JvmGroup.MEMORY);
        Assert.assertEquals(3, jvmMemInfo.size());
    }

    @Test
    public void should_get_api_db_info() throws Throwable {
        // when:
        GroupSystemInfo dbInfo = (GroupSystemInfo) sysInfoService.components(Category.API, Type.DB).get(0);
        Assert.assertNotNull(dbInfo);

        // then:
        Map<String, String> mysqlInfo = dbInfo.get(DBGroupName.MYSQL);
        Assert.assertEquals(5, mysqlInfo.size());
    }

    @Test
    public void should_get_api_tomcat_info() throws Throwable {
        // when: get tomcat info should be offline since unit test not in tomcat container
        SystemInfo serverInfo = sysInfoService.components(Category.API, Type.SERVER).get(0);
        Assert.assertNotNull(serverInfo);
        Assert.assertEquals(Status.OFFLINE, serverInfo.getStatus());
    }

    @Test
    public void should_get_cc_jvm_info() throws Throwable {
        JvmLoader jvmLoader = new JvmLoader();
        stubFor(get("/sys/info/jvm").willReturn(aResponse().withBody(jvmLoader.load().toJson())));

        GroupSystemInfo jvmInfo = (GroupSystemInfo) sysInfoService.components(Category.CC, Type.JVM).get(0);
        Assert.assertNotNull(jvmInfo);

        Assert.assertEquals(3, jvmInfo.size());

        Map<String, String> jvmOsInfo = jvmInfo.get(JvmGroup.OS);
        Assert.assertEquals(6, jvmOsInfo.size());

        Map<String, String> jvmGeneralInfo = jvmInfo.get(JvmGroup.GENERAL);
        Assert.assertEquals(5, jvmGeneralInfo.size());

        Map<String, String> jvmMemInfo = jvmInfo.get(JvmGroup.MEMORY);
        Assert.assertEquals(3, jvmMemInfo.size());
    }

    @Test
    public void should_get_cc_db_info() throws Throwable {
        DBInfoLoader dbInfoLoader = new DBInfoLoader("com.mysql.jdbc.Driver", jdbcUrl, jdbcUsername, jdbcPassword);
        stubFor(get("/sys/info/db").willReturn(aResponse().withBody(dbInfoLoader.load().toJson())));

        GroupSystemInfo dbInfo = (GroupSystemInfo) sysInfoService.components(Category.CC, Type.DB).get(0);
        Assert.assertNotNull(dbInfo);

        Map<String, String> mysqlInfo = dbInfo.get(DBGroupName.MYSQL);
        Assert.assertEquals(5, mysqlInfo.size());
    }

    @Test
    public void should_get_cc_db_offline_status() throws Throwable {
        DBInfoLoader dbInfoLoader = new DBInfoLoader("com.mysql.jdbc.Driver",
            "jdbc:mysql://localhost:8888/test_db", jdbcUsername, jdbcPassword);
        stubFor(get("/sys/info/db").willReturn(aResponse().withBody(dbInfoLoader.load().toJson())));

        GroupSystemInfo dbInfo = (GroupSystemInfo) sysInfoService.components(Category.CC, Type.DB).get(0);
        Assert.assertNotNull(dbInfo);
        Assert.assertEquals(Status.OFFLINE, dbInfo.getStatus());
    }
}
