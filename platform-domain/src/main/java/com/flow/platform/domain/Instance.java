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

package com.flow.platform.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Cloud instance base class
 *
 * @author gy@fir.im
 */
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@ToString
public abstract class Instance extends Jsonable {

    /**
     * Cloud provider assigned instance id
     */
    @Getter
    @Setter
    @SerializedName(value = "id", alternate = {"instanceId"})
    protected String id;

    /**
     * Instance name
     */
    @Getter
    @Setter
    @SerializedName(value = "name", alternate = {"instanceName"})
    protected String name;

    /**
     * Instance ip address
     */
    @Getter
    @Setter
    @SerializedName(value = "ip", alternate = {"ipAddresses"})
    protected String ip;

    /**
     * Instance status from provider
     */
    @Getter
    @Setter
    protected String status;

    /**
     * Instance created date
     */
    @Getter
    @Setter
    protected Date createdAt;
}
