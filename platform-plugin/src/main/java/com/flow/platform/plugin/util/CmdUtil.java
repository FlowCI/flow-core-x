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
import java.io.InputStreamReader;

/**
 * @author yh@firim
 */
public class CmdUtil {

    private final static Logger LOGGER = new Logger(CmdUtil.class);

    public static void exeCmd(String shell) {
        BufferedReader br;
        try {
            Process process;
            ProcessBuilder pb = new ProcessBuilder(Unix.CMD_EXECUTOR, "-c", shell);
            pb.environment();
            pb.redirectErrorStream(true); // merge error stream into standard stream
            process = pb.start();
            if (process != null) {
                br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()), 1024);
                process.waitFor();
            } else {
                throw new PluginException("Plugin running process is not start");
            }
            String line;
            while (br != null && (line = br.readLine()) != null) {
                LOGGER.debug(line);
            }
        } catch (Exception e) {
            LOGGER.error("Exec cmd error", e);
        } finally {
        }
    }
}
