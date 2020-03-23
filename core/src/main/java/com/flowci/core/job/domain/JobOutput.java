/*
 * Copyright 2019 flow.ci
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
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * @author yang
 */
@Getter
@Setter
public abstract class JobOutput extends Mongoable {

    public static abstract class ContentType {

        public static final String ZIP = "zip";

        public static final String GZIP = "gzip";

        public static final String JSON = "json";

        public static final String XML = "xml";

        public static final String HTML = "html";
    }

    @JsonIgnore
    @Indexed(name = "index_job_report_jobid")
    private String jobId;

    // path for FileManager
    @JsonIgnore
    protected String path;

    protected String fileName;

    @JsonIgnore
    protected boolean zipped;

    protected String contentType;

    protected Long contentSize = 0L;
}
