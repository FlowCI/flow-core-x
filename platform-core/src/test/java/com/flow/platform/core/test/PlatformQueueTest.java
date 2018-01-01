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

package com.flow.platform.core.test;

import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import com.flow.platform.util.ObjectWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
@FixMethodOrder(MethodSorters.JVM)
public class PlatformQueueTest {

    @Autowired
    private PlatformQueue<PriorityMessage> inMemoryQueue;

    @Autowired
    private PlatformQueue<PriorityMessage> rabbitQueue;

    @Before
    public void init() {
        inMemoryQueue.clean();
        inMemoryQueue.cleanListener();
        rabbitQueue.cleanListener();
    }

    @Test
    public void should_enqueue_for_rabbit_queue() throws Throwable {
        // given: queue listener
        CountDownLatch latch = new CountDownLatch(1);
        ObjectWrapper<PriorityMessage> result = new ObjectWrapper<>();
        QueueListener<PriorityMessage> listener = item -> {
            result.setInstance(item);
            latch.countDown();
        };

        rabbitQueue.register(listener);
        rabbitQueue.start();

        // when: enqueue
        rabbitQueue.enqueue(PriorityMessage.create("hello".getBytes(), 1));

        // then:
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertNotNull(result.getInstance());
        Assert.assertEquals("hello", new String(result.getInstance().getBody(), "UTF-8"));
    }

    @Test
    public void should_enqueue_with_priority_in_memory_queue() throws Throwable {
        // given: queue listener
        final int size = 4;
        CountDownLatch latch = new CountDownLatch(size);
        List<String> prioritizedList = new ArrayList<>(size);

        QueueListener<PriorityMessage> listener = item -> {
            prioritizedList.add(new String(item.getBody()));
            latch.countDown();
        };

        // when:
        inMemoryQueue.register(listener);
        inMemoryQueue.enqueue(PriorityMessage.create("1".getBytes(), 1));
        ThreadUtil.sleep(1);
        inMemoryQueue.enqueue(PriorityMessage.create("2".getBytes(), 1));
        ThreadUtil.sleep(1);
        inMemoryQueue.enqueue(PriorityMessage.create("3".getBytes(), 10));
        ThreadUtil.sleep(1);
        inMemoryQueue.enqueue(PriorityMessage.create("4".getBytes(), 10));
        inMemoryQueue.start();

        // then:
        boolean await = latch.await(60, TimeUnit.SECONDS);
        Assert.assertTrue(await);
        Assert.assertEquals(size, prioritizedList.size());

        Assert.assertEquals("4", prioritizedList.get(0));
        Assert.assertEquals("3", prioritizedList.get(1));
        Assert.assertEquals("2", prioritizedList.get(2));
        Assert.assertEquals("1", prioritizedList.get(3));
    }

    @Test
    public void should_enqueue_for_in_memory_queue() throws Throwable {
        // given: queue listener
        CountDownLatch latch = new CountDownLatch(1);
        ObjectWrapper<PriorityMessage> result = new ObjectWrapper<>();
        QueueListener<PriorityMessage> listener = item -> {
            result.setInstance(item);
            latch.countDown();
        };

        inMemoryQueue.register(listener);
        inMemoryQueue.start();

        // when: enqueue
        inMemoryQueue.enqueue(PriorityMessage.create("Hello".getBytes(), 1));

        // then:
        latch.await(30, TimeUnit.SECONDS);
        Assert.assertEquals(0, inMemoryQueue.size());
        Assert.assertEquals("Hello", new String(result.getInstance().getBody()));

        // when: pause and enqueue again
        inMemoryQueue.pause();
        inMemoryQueue.enqueue(PriorityMessage.create("Pause".getBytes(), 1));
        Assert.assertEquals(1, inMemoryQueue.size());

        // then: resume
        inMemoryQueue.resume();
        Thread.sleep(1000);
        Assert.assertEquals(0, inMemoryQueue.size());
    }

    @After
    public void stop() {
        inMemoryQueue.stop();
        rabbitQueue.stop();
    }

}
