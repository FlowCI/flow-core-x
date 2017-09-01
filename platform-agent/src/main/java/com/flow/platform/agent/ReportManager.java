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
import com.flow.platform.util.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        try {
            cmdReportSync(cmdId, status, result, 5);
            return true;
        } catch (IOException e) {
            LOGGER.error("IOException when close http client", e);
            return false;
        } catch (Throwable e) {
            LOGGER.error("Fail to report status after 5 times", e);
            return false;
        }
    }

    public boolean cmdLogUploadSync(final String cmdId, final Path path) {
        try {
            cmdLogUploadSync(cmdId, path, 5);
            return true;
        } catch (IOException e) {
            LOGGER.error("IOException when close http client", e);
            return false;
        } catch (Throwable e) {
            LOGGER.error("Fail to upload cmd log after 5 times", e);
            return false;
        }
    }

    private void cmdReportSync(final String cmdId,
                               final CmdStatus status,
                               final CmdResult result,
                               final int retry) throws IOException {

        if (!Config.isReportCmdStatus()) {
            LOGGER.trace("Cmd report toggle is disabled");
            return;
        }

        // build post body
        CmdReport postCmd = new CmdReport(cmdId, status, result);

        String url = Config.agentSettings().getCmdStatusUrl();
        HttpPost post = new HttpPost(url);

        StringEntity entity = new StringEntity(postCmd.toJson(), ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        String successMsg = String.format("Cmd %s report status %s", cmdId, status);
        String failMsg = String.format("Fail to report cmd status to : %s", url);
        httpSend(post, retry, successMsg, failMsg);
    }

    private void cmdLogUploadSync(final String cmdId, final Path logPath, final int retry) throws IOException {
        if (!Config.isUploadLog()) {
            LOGGER.trace("Log upload toggle is disabled");
            return;
        }

        FileBody zippedFile = new FileBody(logPath.toFile(), ContentType.create("application/zip"));
        HttpEntity entity = MultipartEntityBuilder.create()
            .addPart("file", zippedFile)
            .addPart("cmdId", new StringBody(cmdId, ContentType.TEXT_PLAIN))
            .setContentType(ContentType.MULTIPART_FORM_DATA)
            .build();

        String url = Config.agentSettings().getCmdLogUrl();
        HttpPost post = new HttpPost(url);
        post.setEntity(entity);

        String successMsg = String.format("Zipped cmd log uploaded %s", logPath);
        String failMsg = String.format("Fail to upload zipped cmd log to : %s ", url);
        httpSend(post, retry, successMsg, failMsg);
    }

    private static HttpResponse httpSend(final HttpUriRequest request,
                                         final int retry,
                                         final String successMsg,
                                         final String failMsg) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpStatus.SC_OK) {
                throw new RuntimeException(failMsg);
            }

            LOGGER.trace(successMsg);
            return response;
        } catch (Throwable e) {
            if (retry > 0) {
                return httpSend(request, retry - 1, successMsg, failMsg);
            }
            throw new RuntimeException(e);
        }
    }
}
