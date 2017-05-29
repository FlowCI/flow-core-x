package com.flow.platform.agent;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * To report
 * <p>
 * Created by gy@fir.im on 25/05/2017.
 * Copyright fir.im
 */
public class ReportManager {

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
     *
     * @param cmdId
     * @param status
     */
    public void cmdReport(final String cmdId,
                          final Cmd.Status status,
                          final CmdResult result) {
        executor.execute(() -> {
            cmdReportSync(cmdId, status, result);
        });
    }

    /**
     * Report cmd status in sync
     *
     * @param cmdId
     * @param status
     * @return
     */
    public boolean cmdReportSync(final String cmdId,
                                       final Cmd.Status status,
                                       final CmdResult result) {
        try {
            cmdReportSync(cmdId, status, result, 5);
            return true;
        } catch (IOException e) {
            Logger.err(e, "IOException when close http client");
            return false;
        } catch (Throwable e) {
            Logger.err(e, "Fail to report status after 5 times");
            return false;
        }
    }

    private void cmdReportSync(final String cmdId,
                               final Cmd.Status status,
                               final CmdResult result,
                               final int retry) throws IOException {
        // build post body
        Cmd postCmd = new Cmd();
        postCmd.setId(cmdId);
        postCmd.setStatus(status);
        postCmd.setResult(result);

        String url = Config.agentConfig().getCmdStatusUrl();
        HttpPost post = new HttpPost(url);

        StringEntity entity = new StringEntity(postCmd.toJson(), ContentType.APPLICATION_JSON);
        post.setEntity(entity);

        CloseableHttpClient client = HttpClients.createDefault();
        try {
            HttpResponse response = client.execute(post);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpStatus.SC_OK) {
                throw new RuntimeException("Fail to report cmd status to : " + url);
            }
            Logger.info(String.format("Cmd %s report status %s", cmdId, status));
        } catch (Throwable e) {
            if (retry > 0) {
                cmdReportSync(cmdId, status, result, retry - 1);
                return;
            }
            throw new RuntimeException(e);
        } finally {
            client.close();
        }
    }
}
