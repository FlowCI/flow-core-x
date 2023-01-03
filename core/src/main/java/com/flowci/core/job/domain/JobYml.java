/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.job.domain;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.util.StringHelper;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedList;
import java.util.List;

/**
 * JobYml represent set of yml for job
 *
 * @author yang
 */
@Document(collection = "job_yml")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class JobYml extends Mongoable {

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Body {

        private String name;

        private String rawInB64;
    }

    private List<Body> list = new LinkedList<>();

    public JobYml(String jobId) {
        this.id = jobId;
    }

    public void add(String name, String rawInB64) {
        list.add(new Body(name, rawInB64));
    }

    public String[] getRawArray() {
        var array = new String[list.size()];
        int i = 0;
        for (var body : list) {
            array[i++] = StringHelper.fromBase64(body.getRawInB64());
        }
        return array;
    }
}
