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

import com.flowci.core.job.dao.JobReportDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobReport;
import com.flowci.domain.ObjectWrapper;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.flowci.util.FileHelper;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private Path staticResourceDir;

    @Autowired
    private JobReportDao jobReportDao;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Override
    public List<JobReport> list(Job job) {
        return jobReportDao.findAllByJobId(job.getId());
    }

    @Override
    public void save(String name, String type, boolean zipped, String entryFile, Job job, MultipartFile file) {
        Pathable[] reportPath = getReportPath(job);
        ObjectWrapper<String> path = new ObjectWrapper<>();

        try (InputStream reportRaw = file.getInputStream()) {

            // save to file manager by unique report name
            path.setValue(fileManager.save(name, reportRaw, reportPath));

            // save to job report db
            JobReport r = new JobReport();
            r.setName(name);
            r.setJobId(job.getId());
            r.setFileName(file.getOriginalFilename());
            r.setZipped(zipped);
            r.setEntryFile(entryFile);
            r.setContentType(type);
            r.setContentSize(file.getSize());
            r.setPath(path.getValue());

            jobReportDao.save(r);

        } catch (DuplicateKeyException e) {
            if (path.hasValue()) {
                try {
                    fileManager.remove(path.getValue());
                } catch (IOException ignore) {
                }
            }

            log.warn("The job report duplicated");
            throw new DuplicateException("The job report duplicated");
        } catch (IOException e) {
            throw new NotAvailableException("Invalid report data");
        }
    }

    @Override
    public String fetch(Job job, String reportId) {
        Optional<JobReport> optional = jobReportDao.findById(reportId);
        if (!optional.isPresent()) {
            throw new NotFoundException("The job report not available");
        }

        JobReport report = optional.get();

        Path destDir;
        try {
            destDir = getStaticResourcePath(report);
        } catch (IOException e) {
            throw new NotAvailableException("Unable to create static resource path");
        }

        // write to static site folder
        try (InputStream stream = fileManager.read(report.getName(), getReportPath(job))) {
            // unzip to static resource dir
            if (report.isZipped()) {
                Path destFile = Paths.get(destDir.toString(), report.getName());
                if (!Files.exists(destFile)) {
                    FileHelper.unzip(stream, destFile);
                }

                Path entry = Paths.get(destFile.toString(), report.getEntryFile());
                return entry.toString().replace(staticResourceDir.toString(), StringHelper.EMPTY);
            }

            Path destFile = Paths.get(destDir.toString(), report.getFileName());

            if (!Files.exists(destFile)) {
                Files.createFile(destFile);
                FileHelper.writeToFile(stream, destFile);
            }
            return destFile.toString().replace(staticResourceDir.toString(), StringHelper.EMPTY);

        } catch (IOException e) {
            throw new NotAvailableException("Invalid report");
        }
    }

    private Path getStaticResourcePath(JobReport report) throws IOException {
        Path path = Paths.get(staticResourceDir.toString(), "jobs", report.getJobId(), "reports", report.getId());
        FileHelper.createDirectory(path);
        return path;
    }

    private static Pathable[] getReportPath(Job job) {
        Pathable flow = job::getFlowId;
        return new Pathable[]{flow, job, JobReport.ReportPath};
    }
}
