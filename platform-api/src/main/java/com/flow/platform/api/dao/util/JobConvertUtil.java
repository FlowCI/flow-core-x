package com.flow.platform.api.dao.util;/*
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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yh@firim
 */
public class JobConvertUtil {

    public static List<Job> convert(List<Object[]> objects) {
        List<Job> jobs = new ArrayList<>();
        for (Object[] arr : objects) {
            jobs.add(convert(arr));
        }
        return jobs;
    }

    public static Job convert(Object[] objects) {
        if (objects == null) {
            return null;
        }

        if (objects.length == 0) {
            return null;
        }
        
        Job job = (Job) objects[0];
        if (objects[1] == null) {
            return job;
        }
        job.setResult((NodeResult) objects[1]);
        return job;
    }

}