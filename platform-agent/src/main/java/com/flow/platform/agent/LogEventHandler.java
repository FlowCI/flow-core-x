package com.flow.platform.agent;

import com.flow.platform.cmd.LogListener;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by gy@fir.im on 29/05/2017.
 * Copyright fir.im
 */
public class LogEventHandler implements LogListener {

    private final static String SOCKET_EVENT_TYPE = "agent-logging";
    private final static int SOCKET_CONN_TIMEOUT = 10; // 10 seconds

    private final Cmd cmd;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    private Socket socket;
    private boolean socketEnabled = false;

    public LogEventHandler(Cmd cmd) {
        this.cmd = cmd;
        AgentConfig config = Config.agentConfig();

        if (config == null || config.getLoggingUrl() == null) {
            return;
        }

        try {
            socket = IO.socket(config.getLoggingUrl());
            socket.on(Socket.EVENT_CONNECT, objects -> initLatch.countDown());
            socket.connect();

            // wait socket connection and set socket enable to true
            initLatch.await(SOCKET_CONN_TIMEOUT, TimeUnit.SECONDS);
            if (initLatch.getCount() <= 0 && socket.connected()) {
                socketEnabled = true;
            }

        } catch (URISyntaxException | InterruptedException e) {
            Logger.err(e, "Fail to connect socket io server: " + config.getLoggingUrl());
        }
    }

    @Override
    public void onLog(String log) {
        if (socketEnabled) {
            socket.emit(SOCKET_EVENT_TYPE, getLogFormat(log));
        }
        Logger.info(log);
    }

    @Override
    public void onFinish() {
        try {
            socket.close();
        } catch (Throwable e) {
            Logger.err(e, "Exception while close socket io");
        }
    }

    /**
     * Send log by format zone#agent#cmdId#log_raw_data
     *
     * @param log
     * @return
     */
    private String getLogFormat(String log) {
        return String.format("%s#%s#%s#%s", cmd.getZone(), cmd.getAgent(), cmd.getId(), log);
    }
}
