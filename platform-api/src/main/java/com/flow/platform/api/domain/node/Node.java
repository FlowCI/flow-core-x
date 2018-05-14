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

package com.flow.platform.api.domain.node;

import com.flow.platform.api.domain.EnvObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Gson parse the yml file, @SerializedName("xxx")
 * find the super class, but abstract class cannot be instant,
 * so now modified to class
 */
@EqualsAndHashCode(of = {"path"}, callSuper = false)
@ToString(of = {"path"})
public final class Node extends EnvObject {

    @Expose
    @Getter
    @Setter
    private String path;

    @Expose
    @Getter
    @Setter
    private String name;

    /**
     * Node body which is shell script
     */
    @Expose
    @Getter
    @Setter
    private String script;

    /**
     * Groovy script that to indicate the node can start or not
     */
    @Expose
    @Getter
    @Setter
    private String conditionScript;

    @Expose
    @SerializedName("steps")
    @Getter
    @Setter
    private List<Node> children = new LinkedList<>();

    @Expose
    @Getter
    @Setter
    private Boolean allowFailure = false;

    @Expose
    @Getter
    @Setter
    private Boolean isFinal = false;

    @Expose
    @Getter
    @Setter
    private String plugin;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime createdAt;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime updatedAt;

    /**
     * The parent node reference
     */
    @Getter
    @Setter
    private Node parent;

    /**
     * The previous node reference
     */
    @Getter
    @Setter
    private Node prev;

    /**
     * The next node reference
     */
    @Getter
    @Setter
    private Node next;

    public Node() {
    }

    public Node(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public boolean hasPlugin() {
        return this.plugin != null;
    }
}
