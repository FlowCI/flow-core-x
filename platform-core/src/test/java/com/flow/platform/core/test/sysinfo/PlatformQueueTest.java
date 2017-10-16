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

package com.flow.platform.core.test.sysinfo;

import com.flow.platform.core.queue.PlatformQueue;
import com.flow.platform.core.queue.QueueListener;
import com.flow.platform.core.queue.RabbitQueue;
import com.flow.platform.util.ObjectWrapper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfig.class})
public class PlatformQueueTest {

    @Autowired
    private PlatformQueue<Object> inMemoryQueue;

    @Autowired
    private PlatformQueue<Message> rabbitQueue;

    @Test
    public void should_enqueue_for_in_memory_queue() throws Throwable {
        // given: queue listener
        CountDownLatch latch = new CountDownLatch(1);
        ObjectWrapper<Object> result = new ObjectWrapper<>();
        QueueListener<Object> listener = item -> {
            latch.countDown();
            result.setInstance(item);
        };
        inMemoryQueue.register(listener);
        inMemoryQueue.start();

        // when: enqueue
        inMemoryQueue.enqueue("Hello");

        // then:
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, inMemoryQueue.size());
        Assert.assertTrue(result.getInstance() instanceof String);
        Assert.assertEquals("Hello", result.getInstance().toString());

        // when: pause and enqueue again
        inMemoryQueue.pause();
        inMemoryQueue.enqueue("Pause");
        Assert.assertEquals(1, inMemoryQueue.size());

        // then: resume
        inMemoryQueue.resume();
        Thread.sleep(1000);
        Assert.assertEquals(0, inMemoryQueue.size());
    }

    @Test
    public void should_enqueue_for_rabbit_queue() throws Throwable {
        // given: queue listener
        CountDownLatch latch = new CountDownLatch(1);
        ObjectWrapper<Message> result = new ObjectWrapper<>();
        QueueListener<Message> listener = item -> {
            latch.countDown();
            result.setInstance(item);
        };
        rabbitQueue.register(listener);
        rabbitQueue.start();

        // when: enqueue
        rabbitQueue.enqueue(RabbitQueue.createMessage("hello".getBytes()));

        // then:
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, rabbitQueue.size());
        Assert.assertNotNull(result.getInstance());
        Assert.assertEquals("hello", new String(result.getInstance().getBody(), "UTF-8"));

        // when: pause and enqueue again
        rabbitQueue.pause();
        rabbitQueue.enqueue(RabbitQueue.createMessage("pause".getBytes()));
        Assert.assertEquals(1, rabbitQueue.size());

        // then: resume
        rabbitQueue.resume();
        Thread.sleep(1000);
        Assert.assertEquals(0, rabbitQueue.size());
    }

}
