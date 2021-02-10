/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.api.service;

import com.flowci.core.api.domain.CreateJobArtifact;
import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.StatsService;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobKey;
import com.flowci.core.job.service.ArtifactService;
import com.flowci.core.job.service.ReportService;
import com.flowci.core.secret.domain.AndroidSign;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.user.domain.User;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class OpenRestServiceImpl implements OpenRestService {

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private FlowService flowService;

    @Autowired
    private SecretService credentialService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ArtifactService artifactService;

    @Autowired
    private ConfigService configService;

    @Override
    public Secret getSecret(String name) {
        Secret secret = credentialService.get(name);
        secret.cleanDBInfo();
        return secret;
    }

    @Override
    public Config getConfig(String name) {
        Config config = configService.get(name);
        config.cleanDBInfo();
        return config;
    }

    @Override
    public Resource getResource(Secret secret, String file) {
        if (!(secret instanceof AndroidSign)) {
            throw new ArgumentException("Unsupported secret type");
        }

        AndroidSign signSecret = (AndroidSign) secret;

        if (!Objects.equals(file, signSecret.getKeyStoreFileName())) {
            throw new ArgumentException("File not existed in config");
        }

        try {
            InputStream stream = fileManager.read(signSecret.getKeyStoreFileName(), signSecret.getPath());
            return new InputStreamResource(stream);
        } catch (IOException e) {
            throw new StatusException("Failed to get resource: {}", e.getMessage());
        }
    }

    @Override
    public void saveStatsForFlow(String flowName, String statsType, StatsCounter counter) {
        Flow flow = flowService.get(flowName);
        int today = DateHelper.toIntDay(new Date());
        statsService.add(flow.getId(), today, statsType, counter);
    }

    @Override
    public void saveJobReport(String flowName, long buildNumber, CreateJobReport meta, MultipartFile file) {
        Job job = getJob(flowName, buildNumber);
        reportService.save(meta.getName(), meta.getType(), meta.getZipped(), meta.getEntryFile(), job, file);
    }

    @Override
    public void saveJobArtifact(String flowName, long buildNumber, CreateJobArtifact meta, MultipartFile file) {
        Job job = getJob(flowName, buildNumber);
        artifactService.save(job, meta.getSrcDir(), meta.getMd5(), file);
    }

    @Override
    public void addToJobContext(String flowName, long buildNumber, Map<String, String> vars) {
        Job job = getJob(flowName, buildNumber);

        // TODO: verify key value string

        job.getContext().putAll(vars);
        jobDao.save(job);
    }

    @Override
    public List<User> users(String flowName) {
        Flow flow = flowService.get(flowName);
        List<String> emails = flowUserDao.findAllUsers(flow.getId());
        List<User> users = new ArrayList<>(emails.size());
        for (String email : emails) {
            User user = new User();
            user.setEmail(email);
            users.add(user);
        }
        return users;
    }

    private Job getJob(String name, long number) {
        Flow flow = flowService.get(name);

        String key = JobKey.of(flow.getId(), number).toString();
        Optional<Job> optional = jobDao.findByKey(key);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Job for flow {0} with build number {1} not found", name, Long.toString(number));
    }
}
