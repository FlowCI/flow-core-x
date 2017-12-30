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

package com.flow.platform.agent;

import com.flow.platform.domain.CmdReport;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.base.Charsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

/**
 * For reporting status
 * <p>
 *
 * @author gy@fir.im
 */
public class ReportManager {

    private final static Logger LOGGER = new Logger(ReportManager.class);

    private final static ReportManager INSTANCE = new ReportManager();

    public static ReportManager getInstance() {
        return INSTANCE;
    }

    // Executor to execute report thread
    private final ExecutorService executor = Executors.newFixedThreadPool(100);

    private ReportManager() {

    }

    /**
     * Report cmd status with result in async
     */
    public void cmdReport(final String cmdId, final CmdStatus status, final CmdResult result) {
        executor.execute(() -> {
            cmdReportSync(cmdId, status, result);
        });
    }

    /**
     * Report cmd status in sync
     */
    public boolean cmdReportSync(final String cmdId, final CmdStatus status, final CmdResult result) {
        if (!Config.isReportCmdStatus()) {
            LOGGER.trace("Cmd report toggle is disabled");
            return true;
        }

        // build post body
        final CmdReport postCmd = new CmdReport(cmdId, status, result);
        final String url = Config.agentSettings().getCmdStatusUrl();

        try {
            HttpResponse<String> response = HttpClient.build(url)
                .post(postCmd.toJson())
                .retry(5)
                .withContentType(ContentType.APPLICATION_JSON)
                .bodyAsString();

            if (!response.hasSuccess()) {
                LOGGER.warn("Fail to report cmd status to %s with status %s", url, response.getStatusCode());
                return false;
            }

            LOGGER.trace("Cmd %s report status %s with result %s", cmdId, status, result);
            return true;
        } catch (Throwable e) {
            LOGGER.warn("Fail to report cmd %s status since %s'", cmdId, ExceptionUtil.findRootCause(e).getMessage());
            return false;
        }
    }

    public boolean cmdLogUploadSync(final String cmdId, final Path path) {
        if (!Config.isUploadLog()) {
            LOGGER.trace("Log upload toggle is disabled");
            return true;
        }

        HttpEntity entity = MultipartEntityBuilder.create()
            .addPart("file", new FileBody(path.toFile(), ContentType.create("application/zip")))
            .addPart("cmdId", new StringBody(cmdId, ContentType.create("text/plain", Charsets.UTF_8)))
            .setContentType(ContentType.MULTIPART_FORM_DATA)
            .build();

        String url = Config.agentSettings().getCmdLogUrl();
        HttpResponse<String> response = HttpClient.build(url)
            .post(entity)
            .retry(5)
            .bodyAsString();

        if (!response.hasSuccess()) {
            LOGGER.warn("Fail to upload zipped cmd log to : %s ", url);
            return false;
        }

        LOGGER.trace("Zipped cmd log uploaded %s", path);
        return true;
    }
}
