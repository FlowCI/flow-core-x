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

import com.flowci.common.helper.StringHelper;
import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobReportDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobOutput.ContentType;
import com.flowci.core.job.domain.JobReport;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class OpenRestServiceTest extends MockLoggedInScenario {

    @MockBean
    private JobDao jobDao;

    @Autowired
    private JobReportDao jobReportDao;

    @Autowired
    private FlowService flowService;

    @Autowired
    private OpenRestService openRestService;

    private Flow flow;

    @BeforeEach
    void init() throws IOException {
        var yaml = StringHelper.toString(load("flow.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(yaml));
        flow = flowService.create("user-test", option);
    }

    @Test
    void should_list_all_flow_users() {
        List<User> users = openRestService.users(flow.getName());
        assertEquals(1, users.size());

        User user = users.get(0);
        assertNotNull(user.getEmail());

        assertNull(user.getPasswordOnMd5());
        assertNull(user.getRole());
        assertNull(user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getCreatedBy());
        assertNull(user.getUpdatedAt());
        assertNull(user.getUpdatedBy());
    }

    @Test
    void should_save_job_report() throws IOException {
        // given:
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
        assertNotNull(reports);
        assertEquals(1, reports.size());
    }
}
