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

package com.flow.platform.core.sysinfo;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.flow.platform.core.exception.UnsupportedException;
import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author yang
 */
public class ZooKeeperLoader implements SystemInfoLoader {

    public enum ZooKeeperGroup implements GroupName {
        CONF, // print details about serving configuration.
        SRVR, // lists full details for the server
        RUOK; // Tests if server is running in a non-error state
    }

    private static final Map<GroupName, Class<? extends FourLetterCmdConsumer>> cmdConsumer = new HashMap<>();

    private static final String ZK_VERSION_KEY = "Zookeeper version";

    static {
        cmdConsumer.put(ZooKeeperGroup.CONF, ConfConsumer.class);
        cmdConsumer.put(ZooKeeperGroup.SRVR, SrvrConsumer.class);
        cmdConsumer.put(ZooKeeperGroup.RUOK, RuokConsumer.class);
    }

    private final String zkUrl; // should xx.xx.xx.xx:2181

    public ZooKeeperLoader(String zkUrl) {
        this.zkUrl = zkUrl;
    }

    @Override
    public SystemInfo load() {
        Map<String, String> srvr = execFourLetterCmd(ZooKeeperGroup.SRVR);
        Map<String, String> ruok = execFourLetterCmd(ZooKeeperGroup.RUOK);
        Map<String, String> conf = execFourLetterCmd(ZooKeeperGroup.CONF);

        GroupSystemInfo info = new GroupSystemInfo();
        info.setVersion(srvr.remove(ZK_VERSION_KEY));
        info.setName("ZooKeeper");
        info.setStatus(ruok.containsKey(RuokConsumer.STATES) ? Status.RUNNING : Status.OFFLINE);

        info.put(ZooKeeperGroup.SRVR, srvr);
        info.put(ZooKeeperGroup.CONF, conf);

        return info;
    }

    private Map<String, String> execFourLetterCmd(final ZooKeeperGroup group) {
        String fourLetterCmd = group.name().toLowerCase();

        Class<? extends FourLetterCmdConsumer> aClass = cmdConsumer.get(group);
        if (aClass == null) {
            throw new UnsupportedException("The cmd " + fourLetterCmd + " is unsupported");
        }

        FourLetterCmdConsumer consumer;
        try {
            consumer = aClass.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new UnsupportedException("Cannot instance cmd consumer");
        }

        // build four letter cmd
        String[] zkHostTokens = zkUrl.split(":");
        String cmd = String.format("echo %s | nc %s %s", fourLetterCmd, zkHostTokens[0], zkHostTokens[1]);

        // exec cmd
        CmdExecutor exec = new CmdExecutor(null, Lists.newArrayList(cmd));
        exec.setLogListener(consumer);
        exec.run();

        return consumer.rawData();
    }

    /**
     * Interface for four letter cmd consumer
     */
    public static abstract class FourLetterCmdConsumer implements LogListener {

        Map<String, String> data = new HashMap<>(9);

        Map<String, String> rawData() {
            return data;
        }

        @Override
        public void onFinish() {

        }
    }

    public static abstract class SplitterConsumer extends FourLetterCmdConsumer {

        @Override
        public void onLog(Log log) {
            String content = log.getContent();
            int colonIndex = content.indexOf(getSplitter());
            if (colonIndex == -1) {
                return;
            }

            String key = content.substring(0, colonIndex).trim();
            String value = content.substring(colonIndex + 1).trim();
            data.put(key, value);
        }

        abstract int getSplitter();
    }

    /**
     * Consumer for 'srvr' cmd,
     * Print out format is 'Received: 49255011', split with ':'
     */
    public static class SrvrConsumer extends SplitterConsumer {

        @Override
        int getSplitter() {
            return ':';
        }
    }

    /**
     * Consumer 'conf' cmd
     * Print out format is 'clientPort=2181', split with '='
     */
    public static class ConfConsumer extends SplitterConsumer {

        @Override
        int getSplitter() {
            return '=';
        }
    }

    /**
     * Consumer for 'ruok' cmd, it means running status if cmd print 'imok'
     */
    public static class RuokConsumer extends FourLetterCmdConsumer {

        final static String IMOK = "imok";
        final static String STATES = "status";

        @Override
        public void onLog(Log log) {
            if (log.getContent().contains(IMOK)) {
                data.put(STATES, IMOK);
            }
        }
    }
}