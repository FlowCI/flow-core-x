/*
 * Copyright 2021 flow.ci
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

package com.flowci.core.job.manager;

import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class LockManagerImpl implements LockManager {

    private static final int DefaultJobLockTimeout = 20; // seconds

    @Autowired
    private ZookeeperClient zk;

    @Override
    public Optional<InterLock> lock(String jobId) {
        String path = zk.makePath("/job-locks", jobId);
        Optional<InterLock> lock = zk.lock(path, DefaultJobLockTimeout);
        lock.ifPresent(interLock -> log.debug("Lock: {}", jobId));
        return lock;
    }

    @Override
    public void unlock(InterLock lock, String jobId) {
        try {
            zk.release(lock);
            log.debug("Unlock: {}", jobId);
        } catch (Exception warn) {
            log.warn("Unable to release lock", warn);
        }
    }
}
