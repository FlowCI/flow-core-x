package com.flow.platform.agent.test;

import com.flow.platform.agent.Config;
import com.flow.platform.domain.AgentConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by gy@fir.im on 31/05/2017.
 * Copyright fir.im
 */
public abstract class TestBase {

    protected final static Path TEMP_LOG_DIR = Paths.get(System.getenv("TMPDIR"), "flow-agent-log");

    static {
        if (TEMP_LOG_DIR != null) {
            System.setProperty(Config.PROP_LOG_DIR, TEMP_LOG_DIR.toString());
        } else {
            System.setProperty(Config.PROP_LOG_DIR, Paths.get(System.getenv("HOME"), "flow-agent-log-ut").toString());
        }
        System.out.println("Setting flow-agent-log in path: " + TEMP_LOG_DIR.toString());
    }

    @BeforeClass
    public static void beforeClassBase() {
        Config.AGENT_CONFIG = new AgentConfig(
                "http://localhost:3000/agent",
                "http://localhost:8080/cmd/status",
                "http://localhost:8080/cmd/log/upload");
    }

    @AfterClass
    public static void afterClassBase() {
        try {
            Files.list(TEMP_LOG_DIR).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {

                }
            });

            Files.deleteIfExists(TEMP_LOG_DIR);
        } catch (IOException e) { }
    }
}
