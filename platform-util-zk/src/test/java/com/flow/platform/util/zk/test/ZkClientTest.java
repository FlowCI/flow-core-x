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

package com.flow.platform.util.zk.test;

import com.flow.platform.util.zk.ZKClient;
import com.flow.platform.util.zk.ZkException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author yang
 */
public class ZkClientTest {

    private static TestingServer server;

    private ZKClient zkClient;

    // Make thread to Daemon thread, those threads exit while JVM exist
    private final ThreadFactory defaultFactory = r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        t.setName("zk-client-test");
        return t;
    };

    private final Executor executor =
        new ThreadPoolExecutor(10, 10, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), defaultFactory);

    @BeforeClass
    public static void beforeClass() throws Throwable {
        server = new TestingServer();
        server.start();
    }

    @Before
    public void init() {
        zkClient = new ZKClient(server.getConnectString());
        zkClient.setTaskExecutor(executor);
        zkClient.start();
    }

    @Test
    public void should_return_false_if_node_not_exist() throws Throwable {
        Assert.assertFalse(zkClient.exist("/hello/not-exit"));
    }

    @Test
    public void should_create_node_atom() throws Throwable {
        ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(5, 5, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5));

        final AtomicInteger size = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(5);
        final String root = "/flow-agent";

        zkClient.create(root, null);
        String agentNodePath = ZKPaths.makePath(root, "flow-atom");
        zkClient.delete(agentNodePath, false);

        for (int i = 0; i < 5; i++) {
            threadPoolExecutor.execute(() -> {
                try {
                    zkClient.createEphemeral(agentNodePath);
                    size.incrementAndGet();
                } catch (ZkException e) {
                    System.out.println(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        Assert.assertEquals(1, size.get());
        zkClient.delete(root, true);
    }

    @Test
    public void should_create_and_delete_zk_node() throws Throwable {
        // init:
        String path1 = "/flow-test";
        String path2 = "/flow-test-ephemeral";

        // when: create zk node
        String path = zkClient.create(path1, "hello".getBytes());
        Assert.assertEquals(path1, path);

        // then: check node is existed and content data
        byte[] data = zkClient.getData(path1);
        Assert.assertNotNull(data);
        Assert.assertEquals("hello", new String(data));

        // when: create Ephemeral zkNode
        path = zkClient.create(path2, "hello ephemeral".getBytes());
        Assert.assertEquals(path2, path);

        // then: check node is existed and content data
        data = zkClient.getData(path2);
        Assert.assertNotNull(data);
        Assert.assertEquals("hello ephemeral", new String(data));

        // when: crate zk node with same path
        path = zkClient.create(path1, null);

        // then:
        Assert.assertEquals(path1, path);

        // when: delete zk node
        zkClient.delete(path1, false);

        // then:
        Assert.assertTrue(!zkClient.exist(path));
    }

    @Test
    public void should_create_and_delete_zk_node_for_children() throws Throwable {
        // init:
        String rootPath = ZKPaths.makePath("/", "flow-test");
        String childPath = ZKPaths.makePath(rootPath, "child-node");

        // when: create root and child
        zkClient.create(rootPath, null);
        zkClient.create(childPath, null);

        // then:
        Assert.assertEquals(true, zkClient.exist(rootPath));
        Assert.assertEquals(true, zkClient.exist(childPath));

        // when: delete node with children
        zkClient.delete(rootPath, true);

        // then:
        Assert.assertEquals(false, zkClient.exist(rootPath));
        Assert.assertEquals(false, zkClient.exist(childPath));
    }

    @Test
    public void should_listen_node_change_event() throws Throwable {
        // init: create node and watch it
        String path = ZKPaths.makePath("/", "flow-test");
        zkClient.create(path, null);
        Assert.assertEquals(true, zkClient.exist(path));

        final CountDownLatch latch = new CountDownLatch(1);
        zkClient.watchNode(path, latch::countDown);

        // when:
        zkClient.setData(path, "hello".getBytes());

        // then:
        latch.await(10L, TimeUnit.SECONDS);
        Assert.assertEquals("hello", new String(zkClient.getData(path)));
    }

    @Test
    public void should_listen_children_change_event() throws Throwable {
        // init: create node and watch it
        String path = ZKPaths.makePath("/", "flow-children-test");
        zkClient.create(path, null);
        Assert.assertEquals(true, zkClient.exist(path));

        final CountDownLatch latch = new CountDownLatch(3); // should receive 3 events
        final AtomicInteger counterForChildAdded = new AtomicInteger(0);
        final AtomicInteger counterForChildRemoved = new AtomicInteger(0);

        zkClient.watchChildren(path, (client, event) -> {

            if (event.getType() == Type.CHILD_ADDED) {
                counterForChildAdded.getAndAdd(1);
            }

            if (event.getType() == Type.CHILD_REMOVED) {
                counterForChildRemoved.getAndAdd(1);
            }

            ChildData childData = event.getData();
            System.out.println(Thread.currentThread().getName());
            System.out.println(childData.getPath());

            latch.countDown();
        });

        // when:
        String firstChildPath = ZKPaths.makePath(path, "child-1");
        zkClient.create(firstChildPath, "1".getBytes());
        Thread.sleep(5);

        String secondChildPath = ZKPaths.makePath(path, "child-2");
        zkClient.create(secondChildPath, "2".getBytes());
        Thread.sleep(5);

        zkClient.delete(secondChildPath, false);
        Thread.sleep(5);

        // then:
        latch.await(10L, TimeUnit.SECONDS);
        Assert.assertEquals(1, zkClient.getChildren(path).size());

        Assert.assertEquals(2, counterForChildAdded.get());
        Assert.assertEquals(1, counterForChildRemoved.get());
    }

    @Test
    public void should_listen_tree_event() throws Throwable {
        // init:
        String path = ZKPaths.makePath("/", "flow-tree-test");

        final AtomicBoolean isTriggerNodeAddedEvent = new AtomicBoolean(false);
        final AtomicBoolean isTriggerNodeRemovedEvent = new AtomicBoolean(false);
        final AtomicBoolean isTriggerNodeUpdatedEvent = new AtomicBoolean(false);

        zkClient.watchTree(path, (client, event) -> {
            if (event.getType() == TreeCacheEvent.Type.NODE_ADDED) {
                isTriggerNodeAddedEvent.set(true);
            }

            if (event.getType() == TreeCacheEvent.Type.NODE_REMOVED) {
                isTriggerNodeRemovedEvent.set(true);
            }

            if (event.getType() == TreeCacheEvent.Type.NODE_UPDATED) {
                isTriggerNodeUpdatedEvent.set(true);
            }
        });

        // when: create, update and delete node
        zkClient.create(path, null);
        Assert.assertEquals(true, zkClient.exist(path));
        Thread.sleep(100);

        zkClient.setData(path, "hello".getBytes());
        Assert.assertEquals("hello", new String(zkClient.getData(path)));
        Thread.sleep(100);

        zkClient.delete(path, false);
        Assert.assertEquals(false, zkClient.exist(path));
        Thread.sleep(100);

        // then: check listener been triggered
        Assert.assertEquals(true, isTriggerNodeAddedEvent.get());
        Assert.assertEquals(true, isTriggerNodeRemovedEvent.get());
        Assert.assertEquals(true, isTriggerNodeUpdatedEvent.get());
    }

    @After
    public void after() throws Throwable {
        String rootPath = "/";
        for (String nodeName : zkClient.getChildren(rootPath)) {

            if (nodeName.equals("zookeeper")) {
                return;
            }

            String path = ZKPaths.makePath(rootPath, nodeName);
            zkClient.delete(path, false);
        }

        Assert.assertEquals(1, zkClient.getChildren(rootPath).size());

        zkClient.close();
    }

    @AfterClass
    public static void afterClass() throws Throwable {
        server.close();
    }
}
