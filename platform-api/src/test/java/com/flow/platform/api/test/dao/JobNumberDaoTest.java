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

package com.flow.platform.api.test.dao;

import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class JobNumberDaoTest extends TestBase {

    @Test
    public void should_save_job_number() {
        // when:
        String nodePath = "flow/path";
        jobNumberDao.save(new JobNumber(nodePath));

        // then:
        JobNumber number = jobNumberDao.get(nodePath);
        Assert.assertNotNull(number);
        Assert.assertEquals(0L, number.getNumber().longValue());
    }

    @Test
    public void should_increase_job_number() {
        // given:
        String nodePath = "flow/path-increase";
        JobNumber number = jobNumberDao.save(new JobNumber(nodePath));
        Assert.assertEquals(0L, number.getNumber().longValue());

        // when:
        JobNumber increased = jobNumberDao.increase(nodePath);
        Assert.assertNotNull(increased);
        Assert.assertEquals(1L, increased.getNumber().longValue());

        // when:
        increased = jobNumberDao.increase(nodePath);
        Assert.assertNotNull(increased);
        Assert.assertEquals(2L, increased.getNumber().longValue());
    }
}
