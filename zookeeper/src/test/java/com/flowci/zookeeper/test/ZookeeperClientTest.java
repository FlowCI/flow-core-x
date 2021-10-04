package com.flowci.zookeeper.test;

import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZookeeperClientTest {

    private ZookeeperClient client;

    @Before
    public void init() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        String host = System.getenv().getOrDefault("FLOWCI_ZK_HOST", "127.0.0.1:2181");
        client = new ZookeeperClient(host, 5, 10, executor);
        client.start();
    }

    @After
    public void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void should_lock() throws InterruptedException {
        String lockPath = client.makePath("/lock-test", "test");

        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            try {
                Optional<InterLock> lock = client.lock(lockPath, 20);
                if (!lock.isPresent()) {
                    System.out.println("thread - 1 not gain the lock");
                    return;
                }

                System.out.println("thread - 1 own the lock");
                sleep(10);
                client.release(lock.get());
                System.out.println("thread - 1 release the lock");
            } finally {
                latch.countDown();
            }

        }).start();

        new Thread(() -> {
            try {
                Optional<InterLock> lock = client.lock(lockPath, 1);
                if (!lock.isPresent()) {
                    System.out.println("thread - 2 not gain the lock");
                    return;
                }
                System.out.println("thread - 2 own the lock");
                sleep(5);
                client.release(lock.get());
                System.out.println("thread - 2 release the lock");
            } finally {
                latch.countDown();
            }
        }).start();

        latch.await();
        Assert.assertTrue(client.exist(lockPath));
    }

    private void sleep(int second) {
        try {
            Thread.sleep(1000L * second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
