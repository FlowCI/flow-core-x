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
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
public class ZkLocalBuilder {

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
