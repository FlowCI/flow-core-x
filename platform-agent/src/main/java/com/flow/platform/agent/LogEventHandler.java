package com.flow.platform.agent;

import com.flow.platform.cmd.LogListener;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.logger.Logger;
import io.socket.client.IO;
import io.socket.client.Socket;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Record log to $HOME/agent-log/{cmd id}.out.zip
 * Send log via socket io if socket.io url provided
 * <p>
 * Created by gy@fir.im on 29/05/2017.
 * Copyright fir.im
 */
public class LogEventHandler implements LogListener {

    private final static Logger LOGGER = new Logger(LogEventHandler.class);

    private final static String SOCKET_EVENT_TYPE = "agent-logging";
    private final static int SOCKET_CONN_TIMEOUT = 10; // 10 seconds

    private final static Path DEFAULT_LOG_PATH = Config.logDir();

    private final Cmd cmd;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    private Socket socket;
    private boolean socketEnabled = false;

    private Path logTempPath;
    private Path logFinalPath;
    private FileOutputStream fileOs;
    private ZipOutputStream zipOs;

    public LogEventHandler(Cmd cmd) {
        this.cmd = cmd;

        // init zip log path
        try {
            logTempPath = initZipLogFile(this.cmd);
            logFinalPath = getLogFinalPath(this.cmd);

            fileOs = new FileOutputStream(logTempPath.toFile());
            zipOs = new ZipOutputStream(fileOs);

            ZipEntry ze = new ZipEntry(cmd.getId() + ".out");
            zipOs.putNextEntry(ze);
        } catch (IOException e) {
            LOGGER.error("Fail to init cmd log file", e);
        }

        AgentConfig config = Config.agentConfig();

        if (config == null || config.getLoggingUrl() == null) {
            return;
        }

        // init socket io logging server
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
            LOGGER.error("Fail to connect socket io server: " + config.getLoggingUrl(), e);
        }
    }

    @Override
    public void onLog(String log) {
        if (socketEnabled) {
            socket.emit(SOCKET_EVENT_TYPE, getLogFormat(log));
        }

        if (zipOs != null) {
            try {
                zipOs.write(log.getBytes());
                zipOs.write("\n".getBytes());
            } catch (IOException e) {
                LOGGER.warn("Log cannot write : " + log);
            }
        }

        LOGGER.debug(log);
    }

    @Override
    public void onFinish() {
        // close socket io
        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable e) {
                LOGGER.error("Exception while close socket io", e);
            }
        }

        // close zip logging file
        if (zipOs != null) {
            try {
                zipOs.flush();
                zipOs.closeEntry();
                zipOs.close();
            } catch (IOException e) {
                LOGGER.error("Exception while close zip stream", e);
            } finally {
                try {
                    fileOs.close();
                } catch (IOException e) {
                    LOGGER.error("Exception while close log file", e);
                }
            }

            // rename xxx.out.tmp to xxx.out.zip and upload to server
            if (Files.exists(logTempPath)) {
                try {
                    Files.move(logTempPath, logFinalPath);

                    // delete if uploaded
                    ReportManager reportManager = ReportManager.getInstance();
                    if (reportManager.cmdLogUploadSync(cmd.getId(), logFinalPath) && Config.isDeleteLog()) {
                        Files.deleteIfExists(logFinalPath);
                    }
                } catch (IOException e) {
                    LOGGER.error("Exception while move update log name from temp", e);
                }
            }
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

    private Path initZipLogFile(final Cmd cmd) throws IOException {
        // init log directory
        try {
            Files.createDirectory(DEFAULT_LOG_PATH);
        } catch (FileAlreadyExistsException ignore) {
            LOGGER.warn("Log path %s already exist", DEFAULT_LOG_PATH);
        }

        // init zipped log file for tmp
        Path cmdLogPath = Paths.get(DEFAULT_LOG_PATH.toString(), cmd.getId() + ".out.tmp");
        Files.deleteIfExists(cmdLogPath);

        return Files.createFile(cmdLogPath);
    }

    private Path getLogFinalPath(final Cmd cmd) {
        return Paths.get(DEFAULT_LOG_PATH.toString(), cmd.getId() + ".out.zip");
    }
}
