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

package com.flow.platform.api.test.dao;

import com.flow.platform.api.dao.JobDao;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.test.TestBase;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobDaoTest extends TestBase{

    @Autowired
    private JobDao jobDao;

    @Test
    public void should_save_success(){
        Job job = new Job();
        job.setStatus(NodeStatus.PENDING);
        String simpleDateFormat ;
        for (int i = 0; i < 100; i++) {
            Date date = new Date();
            simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS").format(date);
            UUID uuid = UUID.randomUUID();
            String res = simpleDateFormat ;//+ Math.abs(uuid.hashCode());
            Long dou = Long.valueOf(res);
//            System.out.println(dou);
            System.out.println(uuid.hashCode());
        }
    }
}
