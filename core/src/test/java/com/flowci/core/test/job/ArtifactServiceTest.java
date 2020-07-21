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

import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobArtifact;
import com.flowci.core.job.service.ArtifactService;
import com.flowci.core.test.SpringScenario;
import com.flowci.store.FileManager;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ArtifactServiceTest extends SpringScenario {

    @Autowired
    private JobDao jobDao;

    @Autowired
    private ArtifactService artifactService;

    @MockBean
    private FileManager fileManager;

    @Test
    public void should_get_artifact_with_src_stream() throws IOException {
        Job job = new Job();
        job.setFlowId("1111");
        job.setBuildNumber(1L);
        jobDao.save(job);

        ByteArrayInputStream content = new ByteArrayInputStream("content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", "test.jar", null, content);

        Mockito.when(fileManager.save(eq(file.getOriginalFilename()), any(), any()))
                .thenReturn("artifact/file/path");

        Mockito.when(fileManager.read(eq(file.getOriginalFilename()), any()))
                .thenReturn(content);

        // when: save artifact
        artifactService.save(job, "foo/boo", "md5..", file);

        // then: job artifact num should increased
        Assert.assertEquals(1, jobDao.findById(job.getId()).get().getNumOfArtifact());

        // then: fetch
        List<JobArtifact> list = artifactService.list(job);
        Assert.assertEquals(1, list.size());

        JobArtifact fetched = artifactService.fetch(job, list.get(0).getId());
        Assert.assertNotNull(fetched);
        Assert.assertNotNull(fetched.getSrc());
        Assert.assertEquals("test.jar", fetched.getFileName());
        Assert.assertEquals("artifact/file/path", fetched.getPath());
    }
}
