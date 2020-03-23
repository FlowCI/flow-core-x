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

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobArtifact;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ArtifactService {

    List<JobArtifact> list(Job job);

    /**
     * Save report to file store
     *
     * @param job  related job
     * @param srcDir source relevant directory path
     * @param file raw file uploaded
     */
    void save(Job job, String srcDir, String md5, MultipartFile file);

    /**
     * Fetch report from file store
     *
     * @param job        target job instance
     * @param artifactId artifact db id
     * @return http access path
     */
    JobArtifact fetch(Job job, String artifactId);
}
