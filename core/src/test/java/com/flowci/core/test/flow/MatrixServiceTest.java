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

package com.flowci.core.test.flow;

import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.flow.domain.MatrixItem;
import com.flowci.core.flow.domain.MatrixType;
import com.flowci.core.flow.service.MatrixService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.test.SpringScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author yang
 */
public class MatrixServiceTest extends SpringScenario {

    @Autowired
    private MatrixService matrixService;

    @Test
    void should_add_stats_item_when_job_status_changed() {
        Job job = new Job();
        job.setId("1111");
        job.setFlowId("123-456");
        job.setCreatedAt(new Date());
        job.setStatus(Job.Status.SUCCESS);

        multicastEvent(new JobStatusChangeEvent(this, job));
        ThreadHelper.sleep(1000);

        MatrixItem item = matrixService.get(job.getFlowId(), MatrixType.JOB_STATUS, DateHelper.toIntDay(new Date()));
        assertNotNull(item);
        assertEquals(Job.FINISH_STATUS.size(), item.getCounter().size());

        assertEquals(Float.valueOf(1.0F), item.getCounter().get("SUCCESS"));
        assertEquals(Float.valueOf(0.0F), item.getCounter().get("FAILURE"));
        assertEquals(Float.valueOf(0.0F), item.getCounter().get("CANCELLED"));
        assertEquals(Float.valueOf(0.0F), item.getCounter().get("TIMEOUT"));

        assertEquals(Float.valueOf(1.0F), item.getTotal().get("SUCCESS"));
        assertEquals(Float.valueOf(0.0F), item.getTotal().get("FAILURE"));
        assertEquals(Float.valueOf(0.0F), item.getTotal().get("CANCELLED"));
        assertEquals(Float.valueOf(0.0F), item.getTotal().get("TIMEOUT"));
    }

    @Test
    void should_list_stats_by_days() {
        String flowId = "11123123";

        Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Job job1 = new Job();
        job1.setId("1111");
        job1.setFlowId(flowId);
        job1.setCreatedAt(yesterday);
        job1.setStatus(Job.Status.SUCCESS);

        Date today = Date.from(Instant.now());
        Job job2 = new Job();
        job2.setId("2222");
        job2.setFlowId(flowId);
        job2.setCreatedAt(today);
        job2.setStatus(Job.Status.SUCCESS);

        Date tomorrow = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        Job job3 = new Job();
        job3.setId("3333");
        job3.setFlowId(flowId);
        job3.setCreatedAt(tomorrow);
        job3.setStatus(Job.Status.FAILURE);

        multicastEvent(new JobStatusChangeEvent(this, job1));
        multicastEvent(new JobStatusChangeEvent(this, job2));
        multicastEvent(new JobStatusChangeEvent(this, job3));
        ThreadHelper.sleep(1000);

        int fromDay = DateHelper.toIntDay(yesterday);
        int toDay = DateHelper.toIntDay(tomorrow);
        List<MatrixItem> list = matrixService.list(flowId, MatrixType.JOB_STATUS, fromDay, toDay);
        assertNotNull(list);
        assertEquals(3, list.size());

        MatrixItem total = matrixService.get(flowId, MatrixType.JOB_STATUS, MatrixItem.ZERO_DAY);
        assertNotNull(total);
        assertEquals(2.0F, total.getCounter().get("SUCCESS"), 0.0);
        assertEquals(1.0F, total.getCounter().get("FAILURE"), 0.0);
        assertEquals(0.0F, total.getCounter().get("CANCELLED"), 0.0);
        assertEquals(0.0F, total.getCounter().get("TIMEOUT"), 0.0);
    }
}
