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

package com.flow.platform.util.zk;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;

/**
 * <p>
 * To create local test zookeeper server
 * </p>
 *
 * @author gy@fir.im
 */
public class ZkLocalServer {

    public static ServerCnxnFactory start() throws IOException, InterruptedException {
        int tickTime = 2000;
        int numConnections = 100;

        Path temp = Paths.get(System.getProperty("java.io.tmpdir"), "zookeeper");
        if (Files.exists(temp)) {
            Files.walk(temp, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }

        ZooKeeperServer zkServer = new ZooKeeperServer(temp.toFile(), temp.toFile(), tickTime);

        ServerCnxnFactory zkFactory = ServerCnxnFactory.createFactory(2181, numConnections);
        zkFactory.getLocalPort();
        zkFactory.startup(zkServer);

        return zkFactory;
    }
}
