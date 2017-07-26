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
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Record log to $HOME/agent-log/{cmd id}.out.zip
 * Send log via socket io if socket.io url provided
 * <p>
 * @author gy@fir.im
 */
public class LogEventHandler implements LogListener {

    private final static Logger LOGGER = new Logger(LogEventHandler.class);

    private final static Path DEFAULT_LOG_PATH = Config.logDir();

    private final Cmd cmd;

    private Connection loggingQueueConn;
    private Channel loggingQueueChannel;
    private String loggingQueueName;

    private Path stdoutLogPath;
    private FileOutputStream stdoutLogStream;
    private ZipOutputStream stdoutLogZipStream;

    private Path stderrLogPath;
    private FileOutputStream stderrLogStream;
    private ZipOutputStream stderrLogZipStream;

    public LogEventHandler(Cmd cmd) {
        this.cmd = cmd;

        // init zip log path
        try {
            initZipLogFile(this.cmd);
        } catch (IOException e) {
            LOGGER.error("Fail to init cmd log file", e);
        }

        AgentSettings config = Config.agentSettings();

        if (config == null
            || Strings.isNullOrEmpty(config.getLoggingQueueHost())
            || Strings.isNullOrEmpty(config.getLoggingQueueName())) {
            return;
        }

        // init rabbit queue
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(config.getLoggingQueueHost());
            loggingQueueConn = factory.newConnection();
            loggingQueueChannel = loggingQueueConn.createChannel();
            loggingQueueName = config.getLoggingQueueName();
        } catch (Throwable e) {
            LOGGER.error("Fail to connect rabbitmq: " + config.getLoggingQueueHost(), e);
        }
    }

    @Override
    public void onLog(Log log) {
        LOGGER.debug(log.toString());

        sendRealtimeLog(log);

        // write stdout & stderr
        writeZipStream(stdoutLogZipStream, log.getContent());

        // write stderr only
        if (log.getType() == Log.Type.STDERR) {
            writeZipStream(stderrLogZipStream, log.getContent());
        }
    }

    private void sendRealtimeLog(Log log) {
        if (loggingQueueChannel == null || !loggingQueueChannel.isOpen()) {
            return;
        }

        try {
            String format = websocketLogFormat(log);
            loggingQueueChannel.basicPublish("", loggingQueueName, null, format.getBytes());
            LOGGER.debugMarker("Logging", "Message sent : %s", format);
        } catch (Throwable e) {
            LOGGER.warn("Fail to send real time log to queue");
        }
    }

    @Override
    public void onFinish() {
        // close socket io
        closeSocketIo();

        if (closeZipAndFileStream(stdoutLogZipStream, stdoutLogStream)) {
            renameAndUpload(stdoutLogPath, Log.Type.STDOUT);
        }

        if (closeZipAndFileStream(stderrLogZipStream, stderrLogStream)) {
            renameAndUpload(stderrLogPath, Log.Type.STDERR);
        }
    }

    public String websocketLogFormat(Log log) {
        return String.format("%s#%s#%s#%s", cmd.getZoneName(), cmd.getAgentName(), cmd.getId(),
            log.getContent());
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
            } catch (IOException e) {
                LOGGER.error("Exception while move update log name from temp", e);
            }
        }
    }

    private boolean closeZipAndFileStream(final ZipOutputStream zipStream,
        final FileOutputStream fileStream) {
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

    private void closeSocketIo() {
        if (loggingQueueChannel != null) {
            try {
                loggingQueueChannel.close();
            } catch (Throwable e) {
                LOGGER.error("Exception while close logging queue channel", e);
            }
        }

        if (loggingQueueConn != null) {
            try {
                loggingQueueConn.close();
            } catch (Throwable e) {
                LOGGER.error("Exception while close logging queue connection", e);
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
            stream.write("\n".getBytes());
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
        Path stdoutPath = Paths
            .get(DEFAULT_LOG_PATH.toString(), getLogFileName(cmd, Log.Type.STDOUT, true));
        Files.deleteIfExists(stdoutPath);
        stdoutLogPath = Files.createFile(stdoutPath);

        // init zip stream for stdout log
        stdoutLogStream = new FileOutputStream(stdoutLogPath.toFile());
        stdoutLogZipStream = new ZipOutputStream(stdoutLogStream);
        ZipEntry outEntry = new ZipEntry(cmd.getId() + ".out");
        stdoutLogZipStream.putNextEntry(outEntry);

        // init zipped log file for tmp
        Path stderrPath = Paths
            .get(DEFAULT_LOG_PATH.toString(), getLogFileName(cmd, Log.Type.STDERR, true));
        Files.deleteIfExists(stderrPath);
        stderrLogPath = Files.createFile(stderrPath);

        // init zip stream for stderr log
        stderrLogStream = new FileOutputStream(stderrLogPath.toFile());
        stderrLogZipStream = new ZipOutputStream(stderrLogStream);
        ZipEntry errEntry = new ZipEntry(cmd.getId() + ".err");
        stderrLogZipStream.putNextEntry(errEntry);
    }

    private String getLogFileName(Cmd cmd, Log.Type logType, boolean isTemp) {
        String logTypeSuffix = logType == Log.Type.STDERR ? ".err" : ".out";
        String tempSuffix = isTemp ? ".tmp" : ".zip";
        return cmd.getId() + logTypeSuffix + tempSuffix;
    }
}
