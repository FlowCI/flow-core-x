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

package com.flow.platform.agent.test;

import com.flow.platform.agent.Config;
import com.flow.platform.domain.AgentSettings;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author gy@fir.im
 */
public abstract class TestBase {

    protected static Path TEMP_LOG_DIR;

    static {
        if (System.getenv("TMPDIR") != null) {
            TEMP_LOG_DIR = Paths.get(System.getenv("TMPDIR"), "flow-agent-log");
        } else {
            TEMP_LOG_DIR = Paths.get(System.getenv("HOME"), "flow-agent-log-ut");
        }

        System.setProperty(Config.PROP_LOG_DIR, TEMP_LOG_DIR.toString());
    }

    @BeforeClass
    public static void beforeClassBase() {
        Config.AGENT_SETTINGS = new AgentSettings(
                "amqp://127.0.0.1:5672",
                "flow-logging-queue-ut",
                "http://localhost:8080/cmd/report",
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
