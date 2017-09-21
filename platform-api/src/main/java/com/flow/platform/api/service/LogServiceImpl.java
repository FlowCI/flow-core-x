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

package com.flow.platform.api.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.job.NodeResultService;
import com.flow.platform.api.util.PlatformURL;
import com.flow.platform.api.util.ZipUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.util.HttpUtil;
import com.flow.platform.util.ObjectWrapper;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "logService")
public class LogServiceImpl implements LogService {

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private JobService jobService;

    @Autowired
    private PlatformURL platformURL;

    @Value("${api.workspace}")
    private String workspace;

    @Override
    public String findNodeLog(String path, Integer number, Integer order) {
        Job job = jobService.find(path, number);
        NodeResult nodeResult = nodeResultService.find(job.getId(), order);
        if (nodeResult == null) {
            throw new IllegalParameterException("Illeal job id or step order number");
        }

        return readStepLog(job, nodeResult);
    }

    @Override
    public Resource fullJobLog(String path, Integer buildNumber) {
        Job job = jobService.find(path, buildNumber);

        // only job finish can to download log
        ArrayList<JobStatus> finishStatus = new ArrayList<>(
            Arrays.asList(JobStatus.FAILURE, JobStatus.SUCCESS, JobStatus.STOPPED));
        if (!finishStatus.contains(job.getStatus())) {
            throw new FlowException("job must finish");
        }

        Resource allResource;

        // read zip job log
        File zipFile = readZipLog(job);
        job.setLogPath(zipFile.getPath());
        jobService.update(job);

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(zipFile);
            allResource = new InputStreamResource(inputStream);
        } catch (FileNotFoundException e) {
            throw new FlowException("read job log error");
        }

        return allResource;
    }

    /**
     * save step log to workspace/:flowName/:jobId/
     */
    private String saveStepLog(Job job, InputStream inputStream, NodeResult nodeResult) {
        String jobFolder = workspace + "/" + job.getNodeName() + "/" + job.getId().toString();

        String targetFile = jobFolder + '/' + nodeResult.getName() + ".log";

        File flowFolderFile = new File(jobFolder);
        if (!flowFolderFile.exists()) {
            flowFolderFile.mkdirs();
        }

        byte[] buffers;
        try {
            buffers = new byte[inputStream.available()];
            inputStream.read(buffers);

            File file = new File(targetFile);
            try (OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(buffers);
            }
        } catch (IOException e) {
            return null;
        }

        return targetFile;
    }

    /**
     * read step log from workspace/:flowName/:jobId/
     */
    private String readStepLog(Job job, NodeResult nodeResult) {

        // read log from api storage
        String content = readStepLogFromApi(job, nodeResult);

        if (content != null) {
            return content;
        }

        // read step log from cc
        content = readStepLogFromCC(job, nodeResult);
        if (content != null) {
            saveStepLog(job, new ByteArrayInputStream(
                content.getBytes(AppConfig.DEFAULT_CHARSET)), nodeResult);
        }

        return content;
    }

    /**
     * read log from api storage
     */
    private String readStepLogFromApi(Job job, NodeResult nodeResult) {
        String jobFolder = workspace + "/" + job.getNodeName() + "/" + job.getId().toString();

        String targetFile = jobFolder + '/' + nodeResult.getName() + ".log";

        File file = new File(targetFile);
        if (!file.exists()) {
            return null;
        }

        StringBuilder content = new StringBuilder("");
        try {
            InputStream inputStream = new FileInputStream(file);
            byte[] buffers = new byte[2048];
            int length;
            while ((length = inputStream.read(buffers)) > 0) {
                content.append(new String(buffers, 0, length, AppConfig.DEFAULT_CHARSET));
            }

        } catch (IOException e) {
            return null;
        }

        return content.toString();

    }

    /**
     * read log from cc
     */
    private String readStepLogFromCC(Job job, NodeResult nodeResult) {

        String cmdId = nodeResult.getCmdId();

        if (Strings.isNullOrEmpty(cmdId)) {
            throw new IllegalParameterException("The job node without cmd id");
        }

        final StringBuilder stringBuilder = new StringBuilder(platformURL.getCmdDownloadLogUrl());
        stringBuilder.append("?cmdId=").append(HttpUtil.urlEncode(cmdId)).append("&index=").append(0);

        ObjectWrapper<String> logContent = new ObjectWrapper<>();

        HttpUtil.getResponseEntity(stringBuilder.toString(), entity -> {
            try {
                if (entity != null) {
                    InputStream content = entity.getContent();
                    String log = ZipUtil.readZipFile(content);
                    logContent.setInstance(log);

                    //save file
                    InputStream stream = new ByteArrayInputStream(log.getBytes(AppConfig.DEFAULT_CHARSET));
                    String logPath = saveStepLog(job, stream, nodeResult);
                    if (logPath == null) {
                        throw new FlowException("store log to api error");
                    }

                    nodeResult.setLogPath(logPath);
                    nodeResultService.update(nodeResult);
                } else {
                    logContent.setInstance("");
                }
            } catch (IOException e) {
                throw new FlowException("Cannot unzip log file for " + cmdId, e);
            }
        });

        return logContent.getInstance();
    }

    // save zip log
    private File saveZipLog(Job job) {
        String jobPath = workspace + "/" + job.getNodeName() + "/" + job.getId().toString();
        String zipPath = jobPath + ".zip";
        String destPath = jobPath + "/" + job.getId().toString() + ".zip";
        File folderFile = new File(jobPath);
        File zipFile = new File(zipPath);
        File destFile = new File(destPath);
        try {
            ZipUtil.zipFolder(folderFile, zipFile);
            FileUtils.moveFile(zipFile, destFile);
        } catch (IOException e) {
            throw new FlowException("save zip log error");
        }

        return destFile;
    }


    private File readZipLog(Job job) {
        // read zip log from api
        String jobPath = workspace + "/" + job.getNodeName() + "/" + job.getId().toString();
        String zipPath = jobPath + "/" + job.getId().toString() + ".zip";
        File zipFile = new File(zipPath);
        if (zipFile.exists()) {
            return zipFile;
        }

        // read zip log from cc
        List<NodeResult> list = nodeResultService.list(job, true);
        if (list.isEmpty()) {
            throw new FlowException("node result is empty");
        }
        // download all log from cc
        for (NodeResult nodeResult : list) {
            readStepLog(job, nodeResult);
        }

        saveZipLog(job);
        zipFile = new File(zipPath);

        return zipFile;
    }
}
