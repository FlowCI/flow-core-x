package com.flowci.zookeeper.test;

import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZookeeperClientTest {

    @Test
    public void should_lock() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ZookeeperClient client = new ZookeeperClient("localhost:2181", 5, 10, executor);
        client.start();

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
        Assert.assertFalse(client.exist(lockPath));
    }

    private void sleep(int second) {
        try {
            Thread.sleep(1000L * second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
