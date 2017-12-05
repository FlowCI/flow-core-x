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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobYml;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author lhl
 */
public class JobYmlDaoTest extends TestBase {

    @Test
    public void should_create_job_yml_storage() {
        Job job = new Job(CommonUtil.randomId());
        JobYml jobYmlStorage = new JobYml(job.getId(), "file");
        jobYmlDao.save(jobYmlStorage);
        Assert.assertEquals(job.getId(), jobYmlStorage.getJobId());
        JobYml job_yml = jobYmlDao.get(jobYmlStorage.getJobId());
        Assert.assertEquals("file", job_yml.getFile());
    }

    @Test
    public void should_save_and_get_yml_success() throws IOException {
        Job job = new Job(CommonUtil.randomId());
        ClassLoader classLoader = JobYmlDaoTest.class.getClassLoader();
        URL resource = classLoader.getResource("yml/flow.yaml");
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, AppConfig.DEFAULT_CHARSET);
        JobYml jys = new JobYml(job.getId(), ymlString);
        jobYmlDao.save(jys);
        JobYml storage = jobYmlDao.get(jys.getJobId());
        Assert.assertNotNull(storage);
        Assert.assertEquals(ymlString, storage.getFile());
    }

    @Test
    public void should_update_job_yml_storage_success() {
        Job job = new Job(CommonUtil.randomId());
        JobYml jobYmlStorage = new JobYml(job.getId(), "file1");
        jobYmlDao.save(jobYmlStorage);
        JobYml job_yml = jobYmlDao.get(jobYmlStorage.getJobId());
        job_yml.setFile("update_file");
        jobYmlDao.update(job_yml);
        Assert.assertEquals("update_file", job_yml.getFile());
    }

    @Test
    public void should_delete_job_yml_storge_success() {
        Job job = new Job(CommonUtil.randomId());
        JobYml jobYmlStorage = new JobYml(job.getId(), "file2");
        jobYmlDao.save(jobYmlStorage);
        jobYmlDao.delete(jobYmlStorage);
        Assert.assertEquals(null, jobYmlDao.get(jobYmlStorage.getJobId()));
    }
}
