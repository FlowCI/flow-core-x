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

import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.domain.AgentSettings;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;

/**
 * Record log to $HOME/agent-log/{cmd id}.out.zip
 * Send log via web socket if real time log enabled and ws url provided
 * <p>
 *
 * @author gy@fir.im
 */
public class LogEventHandler implements LogListener {

    private final static Logger LOGGER = new Logger(LogEventHandler.class);

    private final static Path DEFAULT_LOG_PATH = Config.logDir();

    private final Cmd cmd;

    private Path stdoutLogPath;
    private FileOutputStream stdoutLogStream;
    private ZipOutputStream stdoutLogZipStream;

    private Session wsSession;

    public LogEventHandler(Cmd cmd) {
        this.cmd = cmd;

        // init zip log path
        try {
            initZipLogFile(this.cmd);
        } catch (IOException e) {
            LOGGER.error("Fail to init cmd log file", e);
        }

        AgentSettings config = Config.agentSettings();

        if (config == null || !Config.enableRealtimeLog() || Strings.isNullOrEmpty(config.getWebSocketUrl())) {
            return;
        }

        // init rabbit queue
        try {
            initWebSocketSession(config.getWebSocketUrl(), 10);
        } catch (Throwable warn) {
            wsSession = null;
            LOGGER.warn("Fail to web socket: " + config.getWebSocketUrl() + ": " + warn.getMessage());
        }
    }

    @Override
    public void onLog(Log log) {
        LOGGER.debug(log.toString());

        sendRealTimeLog(log);

        // write stdout & stderr
        writeZipStream(stdoutLogZipStream, log.getContent());
    }

    private void sendRealTimeLog(Log log) {
        if (wsSession == null) {
            return;
        }

        try {
            String format = websocketLogFormat(log);
            wsSession.getBasicRemote().sendText(format);
            LOGGER.debugMarker("Logging", "Message sent : %s", format);
        } catch (Throwable e) {
            LOGGER.warn("Fail to send real time log to queue");
        }
    }

    @Override
    public void onFinish() {
        // close socket io
        closeWebSocket();

        if (closeZipAndFileStream(stdoutLogZipStream, stdoutLogStream)) {
            renameAndUpload(stdoutLogPath, Log.Type.STDOUT);
        }
    }

    public String websocketLogFormat(Log log) {
        return String
            .format("%s#%s#%s#%s#%s#%s", cmd.getType(), log.getNumber(), cmd.getZoneName(), cmd.getAgentName(),
                cmd.getId(),
                log.getContent());
    }

    private void initWebSocketSession(String url, int wsConnectionTimeout) throws Exception {
        CountDownLatch wsLatch = new CountDownLatch(1);
        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        ClientManager client = ClientManager.createClient();

        client.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                wsSession = session;
                wsLatch.countDown();
            }
        }, cec, new URI(url));

        if (!wsLatch.await(wsConnectionTimeout, TimeUnit.SECONDS)) {
            throw new TimeoutException("Web socket connection timeout");
        }
    }


    private void renameAndUpload(Path logPath, Log.Type logType) {
        // rename xxx.out.tmp to xxx.out.zip and renameAndUpload to server
        if (Files.exists(logPath)) {
            try {
                Path target = Paths
                    .get(DEFAULT_LOG_PATH.toString(), getLogFileName(cmd, logType, false));
                Files.move(logPath, target);

                // delete if uploaded
                ReportManager reportManager = ReportManager.getInstance();
                if (reportManager.cmdLogUploadSync(cmd.getId(), target) && Config.isDeleteLog()) {
                    Files.deleteIfExists(target);
                }
            } catch (IOException warn) {
                LOGGER.warn("Exception while move update log name from temp: %s", warn.getMessage());
            }
        }
    }

    private boolean closeZipAndFileStream(final ZipOutputStream zipStream, final FileOutputStream fileStream) {
        try {
            if (zipStream != null) {
                zipStream.flush();
                zipStream.closeEntry();
                zipStream.close();
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Exception while close zip stream", e);
        } finally {
            try {
                if (fileStream != null) {
                    fileStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("Exception while close log file", e);
            }
        }
        return false;
    }

    private void closeWebSocket() {
        if (wsSession != null) {
            try {
                wsSession.close();
            } catch (IOException e) {
                LOGGER.warn("Exception while close web socket session");
            }
        }
    }

    private void writeZipStream(final ZipOutputStream stream, final String log) {
        if (stream == null) {
            return;
        }

        // write to zip output stream
        try {
            stream.write(log.getBytes());
            stream.write(Unix.LINE_SEPARATOR.getBytes());
        } catch (IOException e) {
            LOGGER.warn("Log cannot write : " + log);
        }
    }

    private void initZipLogFile(final Cmd cmd) throws IOException {
        // init log directory
        try {
            Files.createDirectory(DEFAULT_LOG_PATH);
        } catch (FileAlreadyExistsException ignore) {
            LOGGER.warn("Log path %s already exist", DEFAULT_LOG_PATH);
        }

        // init zipped log file for tmp
        Path stdoutPath = Paths.get(DEFAULT_LOG_PATH.toString(), getLogFileName(cmd, Log.Type.STDOUT, true));
        Files.deleteIfExists(stdoutPath);

        stdoutLogPath = Files.createFile(stdoutPath);

        // init zip stream for stdout log
        stdoutLogStream = new FileOutputStream(stdoutLogPath.toFile());
        stdoutLogZipStream = new ZipOutputStream(stdoutLogStream);
        ZipEntry outEntry = new ZipEntry(cmd.getId() + ".out");
        stdoutLogZipStream.putNextEntry(outEntry);
    }

    private String getLogFileName(Cmd cmd, Log.Type logType, boolean isTemp) {
        String logTypeSuffix = logType == Log.Type.STDERR ? ".err" : ".out";
        String tempSuffix = isTemp ? ".tmp" : ".zip";

        // replace / with - since cmd id may includes slash which the same as dir path
        return cmd.getId().replace(Unix.PATH_SEPARATOR.charAt(0), '-') + logTypeSuffix + tempSuffix;
    }
}
