package com.flow.platform.cc.task;

import com.flow.platform.domain.CmdBase;
import com.flow.platform.util.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Send webhook of cmd with max retry times
 *
 * Created by gy@fir.im on 21/06/2017.
 * Copyright fir.im
 */
public final class CmdWebhookTask implements Runnable {

    private final static Logger LOGGER = new Logger(CmdWebhookTask.class);
    private final static int MAX_RETRY_TIMES = 5;

    private final CmdBase cmd;

    public CmdWebhookTask(CmdBase cmd) {
        this.cmd = cmd;
    }

    @Override
    public void run() {
        callWebhook(0);
    }

    private void callWebhook(int retry) {
        if (retry >= MAX_RETRY_TIMES) {
            LOGGER.warn("Webhook fail with max retry time for cmd %s, %s", cmd, cmd.getWebhook());
            return;
        }

        boolean shouldRetry = false;
        int nextRetry = retry + 1;
        int nextRetryWaitTime = nextRetry * 20 * 1000; // milliseconds

        LOGGER.trace("Cmd webhook started %s", cmd);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpEntity entity = new StringEntity(cmd.toJson());
            HttpPost post = new HttpPost(cmd.getWebhook());
            post.setEntity(entity);
            CloseableHttpResponse response = httpclient.execute(post);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                LOGGER.trace("Cmd webhook been reported : %s, %s ", cmd, cmd.getWebhook());
                return;
            }

            LOGGER.trace("Cmd webhook response %s, retry %s after %s seconds", statusCode, nextRetry, nextRetryWaitTime);
            shouldRetry = true;
        } catch (UnsupportedEncodingException | ClientProtocolException e) {
            // JSON data or http protocol exception, exit directly
            LOGGER.error("Webhook data or http protocol error, exit ", e);
            shouldRetry = false;
        } catch (IOException e) {
            LOGGER.warn("Webhook request error", e);
            shouldRetry = true;
        }

        if (shouldRetry) {
            try {
                Thread.sleep(nextRetryWaitTime); // 0, 20, 40, 60, 80 in seconds
            } catch (InterruptedException ignored) {

            } finally {
                callWebhook(nextRetry);
            }
        }
    }
}
