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

package com.flow.platform.plugin.util;

import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yh@firim
 */
public class CmdUtil {

    private final static Logger LOGGER = new Logger(CmdUtil.class);

    private final static int BUFFER_SIZE = 1024;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());

    public static void exeCmd(String shell) {

        LOGGER.debug("Exec cmd is " + shell);
        try {
            Process process;
            ProcessBuilder pb = new ProcessBuilder(Unix.CMD_EXECUTOR, "-c", shell);
            pb.environment();

            // Merge error stream to standard stream
            pb.redirectErrorStream(true);

            // start
            process = pb.start();
            if (process != null) {

                LOGGER.trace("Start debug logs");
                executor.execute(new LoggerRunner(process.getInputStream()));

                // wait process finish
                process.waitFor();
            } else {
                throw new PluginException("Plugin running process is not start");
            }


        } catch (Exception e) {
            LOGGER.error("Exec cmd error", e);
        } finally {
        }
    }

    static class LoggerRunner implements Runnable {

        private InputStream inputStream;

        public LoggerRunner(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream), BUFFER_SIZE);

            String line;
            try {
                while (br != null && (line = br.readLine()) != null) {
                    // show running log
                    LOGGER.debug(line);
                }
            } catch (IOException e) {
                LOGGER.error("Logger running log", e);
            }
        }
    }
}
