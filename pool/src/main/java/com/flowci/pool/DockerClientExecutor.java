package com.flowci.pool;

import com.flowci.pool.exception.DockerPoolException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public final class DockerClientExecutor {

    private static final String localHost = "unix:///var/run/docker.sock";

    private final Object lock = new Object();

    @Getter
    private DockerClient client;

    @Setter
    @Getter
    private int numOfRetry = 2; // retry on create new docker client instance

    public DockerClientExecutor() {
        this.client = newClient();
    }

    /**
     * Execute SyncDockerCmd
     */
    public <T> T exec(SyncDockerCmd<T> cmd) throws DockerPoolException {
        return execDockerCmd(numOfRetry, cmd);
    }

    /**
     * Execute AsyncDockerCmd
     */
    public <CMD_T extends AsyncDockerCmd<CMD_T, A_RES_T>, A_RES_T, T extends ResultCallback<A_RES_T>> T exec(CMD_T cmd, T callback) throws DockerPoolException {
        return execDockerCmd(numOfRetry, cmd, callback);
    }

    private <CMD_T extends AsyncDockerCmd<CMD_T, A_RES_T>, A_RES_T, T extends ResultCallback<A_RES_T>> T execDockerCmd(int numOfRetry, CMD_T cmd, T callback) throws DockerPoolException {
        try {
            return cmd.exec(callback);
        } catch (Exception e) {
            if (isClientClosedException(e)) {
                log.warn("Docker client instance closed, trying to create new instance...");

                if (numOfRetry <= 0) {
                    log.warn(e);
                    throw new DockerPoolException(e.getMessage());
                }

                synchronized (lock) {
                    close();
                    this.client = newClient();
                }

                return execDockerCmd(--numOfRetry, cmd, callback);
            }

            log.warn(e);
            throw new DockerPoolException(e.getMessage());
        }
    }

    private <T> T execDockerCmd(int numOfRetry, SyncDockerCmd<T> cmd) throws DockerPoolException {
        try {
            return cmd.exec();
        } catch (Exception e) {
            if (isClientClosedException(e)) {
                log.warn("Docker client instance closed, trying to create new instance...");

                if (numOfRetry <= 0) {
                    log.warn(e);
                    throw new DockerPoolException(e.getMessage());
                }

                synchronized (lock) {
                    close();
                    this.client = newClient();
                }

                return execDockerCmd(--numOfRetry, cmd);
            }

            log.warn(e);
            throw new DockerPoolException(e.getMessage());
        }
    }

    public void close() {
        if (this.client != null) {
            try {
                this.client.close();
            } catch (IOException e) {
                log.warn(e);
            }
        }
    }

    private static DockerClient newClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(localHost).build();
        return DockerClientBuilder.getInstance(config).build();
    }


    private static boolean isClientClosedException(Exception e) {
        return e instanceof IllegalStateException && e.getMessage().startsWith("Client instance has been closed");
    }
}
