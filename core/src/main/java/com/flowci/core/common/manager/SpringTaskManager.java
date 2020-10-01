package com.flowci.core.common.manager;

import com.flowci.core.common.config.AppProperties;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Run task among all duplications
 */
@Log4j2
@Component
public class SpringTaskManager {

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    public void run(String name, Runnable task) {
        try {
            if (!lock(name)) {
                return;
            }

            log.info("task {} started", name);
            task.run();
            log.info("task {} finished", name);
        } finally {
            release(name);
        }
    }

    private boolean lock(String name) {
        try {
            String path = ZKPaths.makePath(zkProperties.getCronRoot(), name);
            zk.create(CreateMode.EPHEMERAL, path, null);
            return true;
        } catch (ZookeeperException e) {
            return false;
        }
    }

    private void release(String name) {
        try {
            String path = ZKPaths.makePath(zkProperties.getCronRoot(), name);
            zk.delete(path, false);
        } catch (ZookeeperException ignore) {

        }
    }
}
