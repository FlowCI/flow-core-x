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

package com.flowci.core.job.service;

import com.flowci.core.job.dao.JobArtifactDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobArtifact;
import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.NotAvailableException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.flowci.store.StringPath;
import com.flowci.util.StringHelper;
import com.google.api.client.util.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ArtifactServiceImpl implements ArtifactService {

    private static final String Separator = "/";

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobArtifactDao jobArtifactDao;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Override
    public List<JobArtifact> list(Job job) {
        return jobArtifactDao.findAllByJobId(job.getId());
    }

    @Override
    public void save(Job job, String srcDir, String md5, MultipartFile file) {
        srcDir = formatSrcDir(srcDir);
        Pathable[] artifactPath = getArtifactPath(job, srcDir);

        try (InputStream reportRaw = file.getInputStream()) {
            // save to file manager by file name
            String path = fileManager.save(file.getOriginalFilename(), reportRaw, artifactPath);

            JobArtifact artifact = new JobArtifact();
            artifact.setJobId(job.getId());
            artifact.setFileName(file.getOriginalFilename());
            artifact.setContentType(file.getContentType());
            artifact.setContentSize(file.getSize());
            artifact.setPath(path);
            artifact.setSrcDir(srcDir);
            artifact.setMd5(md5);

            jobArtifactDao.save(artifact);
            jobDao.increaseNumOfArtifact(job.getId());
        } catch (IOException e) {
            throw new NotAvailableException("Invalid artifact data");
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Duplicate job artifact");
        }
    }

    @Override
    public JobArtifact fetch(Job job, String artifactId) {
        Optional<JobArtifact> optional = jobArtifactDao.findById(artifactId);
        if (!optional.isPresent()) {
            throw new NotFoundException("The job artifact not available");
        }

        try {
            JobArtifact artifact = optional.get();
            Pathable[] artifactPath = getArtifactPath(job, artifact.getSrcDir());
            InputStream stream = fileManager.read(artifact.getFileName(), artifactPath);
            artifact.setSrc(stream);
            return artifact;
        } catch (IOException e) {
            throw new NotAvailableException("Invalid job artifact");
        }
    }

    private static Pathable[] getArtifactPath(Job job, String srcDir) {
        String[] split = srcDir.split(Separator);
        List<Pathable> list = Lists.newArrayListWithCapacity(split.length + 3);

        list.add(job::getFlowId);
        list.add(job);
        list.add(JobArtifact.ArtifactPath);

        for (String dir : split) {
            list.add(new StringPath(dir));
        }

        return list.toArray(new Pathable[0]);
    }

    private static String formatSrcDir(String dir) {
        if (!StringHelper.hasValue(dir)) {
            return StringHelper.EMPTY;
        }

        if (dir.startsWith(Separator)) {
            dir = dir.substring(1);
        }

        if (dir.endsWith(Separator)) {
            dir = dir.substring(0, dir.length() - 2);
        }

        return dir;
    }
}
