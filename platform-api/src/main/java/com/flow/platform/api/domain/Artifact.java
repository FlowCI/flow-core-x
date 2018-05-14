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

package com.flow.platform.api.domain;

import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@firim
 */

@EqualsAndHashCode(of = {"id"}, callSuper = false)
@ToString(of = {"id", "name", "url"})
public class Artifact extends Jsonable {

    @Getter
    @Setter
    private Integer id;

    /**
     * flow name
     */
    @Expose
    @Getter
    @Setter
    private BigInteger jobId;

    /**
     * file name
     */
    @Expose
    @Getter
    @Setter
    private String name;

    /**
     * artifact tag
     */
    @Expose
    @Getter
    @Setter
    private ArtifactType tag;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime createdAt;

    /**
     * url
     */
    @Expose
    @Getter
    @Setter
    private String url;

    public Artifact() {
    }

    public Artifact(BigInteger jobId, String name, String url) {
        this.jobId = jobId;
        this.name = name;
        this.url = url;
    }
}
