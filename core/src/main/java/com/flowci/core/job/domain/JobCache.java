/*
 * Copyright 2020 flow.ci
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.InputStream;
import java.util.List;

@Getter
@Setter
@Document("job_cache")
@CompoundIndex(name = "index_flow_key", def = "{'flowId' : 1, 'key': 1}", unique = true)
public class JobCache extends Mongoable {

    private String flowId;

    private String jobId;

    private String key;

    private String os;

    /**
     * Full path with b64 encoded based on workspace
     * ex: a/b/c.zip --> b64xxx.zip
     */
    private List<String> files;

    @JsonIgnore
    @Transient
    private InputStream src;
}
