package com.flow.platform.agent;

import com.flow.platform.domain.Agent;
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
 *
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

    public void reportStatus(final Agent.Status status) {
        executor.execute(() -> {
            reportStatusSync(status);
        });
    }

    public boolean reportStatusSync(final Agent.Status status) {
        try {
            reportStatus(status, 5);
            return true;
        } catch (IOException e) {
            Logger.err(e, "IOException when close http client");
            return false;
        } catch (Throwable e) {
            Logger.err(e, "Fail to report status after 5 times");
            return false;
        }
    }

    private void reportStatus(Agent.Status status, int retry) throws IOException {
        // build post body
        Agent agent = new Agent(Config.ZONE, Config.NAME);
        agent.setStatus(status);

        String url = Config.agentConfig().getCmdStatusUrl();
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(agent.toJson(), ContentType.APPLICATION_JSON));

        CloseableHttpClient client = HttpClients.createDefault();
        try {
            HttpResponse response = client.execute(post);
            int code = response.getStatusLine().getStatusCode();
            if (code != HttpStatus.SC_OK) {
                throw new RuntimeException("Fail to report status to : " + url);
            }
        } catch (Throwable e) {
            if (retry > 0) {
                reportStatus(status, retry - 1);
                return;
            }
            throw new RuntimeException(e);
        } finally {
            client.close();
        }
    }
}
