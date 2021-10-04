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

package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobReportDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobOutput;
import com.flowci.core.job.domain.JobReport;
import com.flowci.core.job.service.ReportService;
import com.flowci.core.test.SpringScenario;
import com.flowci.store.FileManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

public class ReportServiceTest extends SpringScenario {

    @MockBean
    private JobReportDao jobReportDao;

    @MockBean
    private FileManager fileManager;

    @Autowired
    private ReportService reportService;

    @Test
    public void should_get_report_url_path() throws IOException {
        Job job = new Job();
        job.setFlowId("12345");
        job.setId("22222");
        job.setBuildNumber(1L);

        JobReport report = new JobReport();
        report.setId("11111");
        report.setJobId(job.getId());
        report.setFileName("hello.html");
        report.setZipped(false);
        report.setContentType(JobOutput.ContentType.HTML);

        Mockito.when(jobReportDao.findById(report.getId())).thenReturn(Optional.of(report));
        Mockito.when(fileManager.read(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        String urlPath = reportService.fetch(job, report.getId());
        Assert.assertNotNull(urlPath);
        Assert.assertEquals("/jobs/22222/reports/11111/hello.html", urlPath);
    }
}
