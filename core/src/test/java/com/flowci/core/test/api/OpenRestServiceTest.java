/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.test.api;

import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobReportDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobOutput.ContentType;
import com.flowci.core.job.domain.JobReport;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

public class OpenRestServiceTest extends SpringScenario {

    @MockBean
    private JobDao jobDao;

    @Autowired
    private JobReportDao jobReportDao;

    @Autowired
    private FlowService flowService;

    @Autowired
    private OpenRestService openRestService;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_list_all_flow_users() {
        Flow flow = flowService.create("user-test");

        List<User> users = openRestService.users(flow.getName());
        Assert.assertEquals(1, users.size());

        User user = users.get(0);
        Assert.assertNotNull(user.getEmail());

        Assert.assertNull(user.getPasswordOnMd5());
        Assert.assertNull(user.getRole());
        Assert.assertNull(user.getId());
        Assert.assertNull(user.getCreatedAt());
        Assert.assertNull(user.getCreatedBy());
        Assert.assertNull(user.getUpdatedAt());
        Assert.assertNull(user.getUpdatedBy());
    }

    @Test
    public void should_save_job_report() {
        // given:
        Flow flow = flowService.create("user-test");

        Job job = new Job();
        job.setId("12345");
        job.setFlowId(flow.getId());
        job.setBuildNumber(1L);
        Mockito.when(jobDao.findByKey(Mockito.any())).thenReturn(Optional.of(job));

        MockMultipartFile uploadFile = new MockMultipartFile("jacoco-report.zip", "content".getBytes());

        CreateJobReport report = new CreateJobReport()
                .setName("jacoco")
                .setZipped(true)
                .setType(ContentType.HTML);

        // when:
        openRestService.saveJobReport(flow.getName(), job.getBuildNumber(), report, uploadFile);

        // then:
        List<JobReport> reports = jobReportDao.findAllByJobId(job.getId());
        Assert.assertNotNull(reports);
        Assert.assertEquals(1, reports.size());
    }
}
