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
import com.flowci.core.job.domain.JobReport;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReportService {

    List<JobReport> list(Job job);

    /**
     * Save report to file store
     *
     * @param name   report name
     * @param type   report content type
     * @param zipped is zipped package
     * @param entryFile zipped package entry file
     * @param job    related job
     * @param file   raw file uploaded
     */
    void save(String name, String type, boolean zipped, String entryFile, Job job, MultipartFile file);

    /**
     * Fetch report from file store
     *
     * @param job      target job instance
     * @param reportId report db id
     * @return http access path
     */
    String fetch(Job job, String reportId);
}
