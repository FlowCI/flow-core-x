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

/**
 * Gson parse the yml file, @SerializedName("xxx")
 * find the super class, but abstract class cannot be instant,
 * so now modified to class
 */
public final class Node extends EnvObject {

    @Expose
    private String path;

    @Expose
    private String name;

    /**
     * Node body which is shell script
     */
    @Expose
    private String script;

    /**
     * Groovy script that to indicate the node can start or not
     */
    @Expose
    private String conditionScript;

    @Expose
    @SerializedName("steps")
    private List<Node> children = new LinkedList<>();

    @Expose
    private Boolean allowFailure = false;

    @Expose
    private Boolean finalNode = false;

    @Expose
    private String plugin;

    @Expose
    private String createdBy;

    @Expose
    private ZonedDateTime createdAt;

    @Expose
    private ZonedDateTime updatedAt;

    /**
     * The parent node reference
     */
    private Node parent;

    /**
     * The previous node reference
     */
    private Node prev;

    /**
     * The next node reference
     */
    private Node next;

    public Node() {
    }

    public Node(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public Node getPrev() {
        return prev;
    }

    public void setPrev(Node prev) {
        this.prev = prev;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getConditionScript() {
        return conditionScript;
    }

    public void setConditionScript(String conditionScript) {
        this.conditionScript = conditionScript;
    }

    public Boolean getAllowFailure() {
        return allowFailure;
    }

    public void setAllowFailure(Boolean allowFailure) {
        this.allowFailure = allowFailure;
    }

    public Boolean getFinalNode() {
        return finalNode;
    }

    public void setFinalNode(Boolean finalNode) {
        this.finalNode = finalNode;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean hasPlugin() {
        return this.plugin != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        return path.equals(node.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
            "path='" + path + '\'' +
            '}';
    }
}
