package com.flow.platform.agent;

import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.domain.AgentConfig;
import com.flow.platform.domain.Cmd;
import com.flow.platform.util.Logger;
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
                LOGGER.trace("Init socket io successfully : %s", config.getLoggingUrl());
            }

        } catch (URISyntaxException | InterruptedException e) {
            LOGGER.error("Fail to connect socket io server: " + config.getLoggingUrl(), e);
        }
    }

    @Override
    public void onLog(Log log) {
        LOGGER.debug(log.toString());

        writeSocketIo(log);

        // write stdout & stderr
        writeZipStream(stdoutLogZipStream, log.getContent());

        // write stderr only
        if (log.getType() == Log.Type.STDERR) {
            writeZipStream(stderrLogZipStream, log.getContent());
        }
    }

    private void writeSocketIo(Log log) {
        if (socketEnabled) {
            String format = String.format("%s#%s#%s#%s", cmd.getZone(), cmd.getAgent(), cmd.getId(), log.getContent());
            socket.emit(SOCKET_EVENT_TYPE, format);
            LOGGER.debugMarker("SocketIO", "Message sent : %s", format);
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

    private void renameAndUpload(Path logPath, Log.Type logType) {
        // rename xxx.out.tmp to xxx.out.zip and renameAndUpload to server
        if (Files.exists(logPath)) {
            try {
                Path target = Paths.get(DEFAULT_LOG_PATH.toString(), getLogFileName(cmd, logType, false));
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

    private void closeSocketIo() {
        if (socket == null) {
            return;
        }

        try {
            socket.close();
        } catch (Throwable e) {
            LOGGER.error("Exception while close socket io", e);
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

    /**
     * Init {cmd id}.out.tmp, {cmd id}.err.tmp and zip stream
     *
     * @param cmd
     * @throws IOException
     */
    private String getLogFormat(String log) {
        return String.format("%s#%s#%s#%s", cmd.getZone(), cmd.getAgentName(), cmd.getId(), log);
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

        // init zipped log file for tmp
        Path stderrPath = Paths.get(DEFAULT_LOG_PATH.toString(), getLogFileName(cmd, Log.Type.STDERR, true));
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
