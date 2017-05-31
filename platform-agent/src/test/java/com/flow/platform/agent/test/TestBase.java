package com.flow.platform.agent.test;

import com.flow.platform.agent.Config;
import org.junit.AfterClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by gy@fir.im on 31/05/2017.
 * Copyright fir.im
 */
public abstract class TestBase {

    private final static Path tmp = Paths.get(System.getenv("TMPDIR"), "flow-agent-log");

    static {
        System.setProperty(Config.PROP_LOG_DIR, tmp.toString());
        System.out.println("Setting flow-agent-log in path: " + tmp.toString());
    }

    @AfterClass
    public static void afterClass() {
        try {
            Files.list(tmp).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {

                }
            });

            Files.deleteIfExists(tmp);
        } catch (IOException e) { }
    }
}
