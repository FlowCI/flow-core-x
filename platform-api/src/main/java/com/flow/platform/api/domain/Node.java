/*
 * *
 *  * Created by yh@fir.im
 *  * Copyright fir.im
 *
 */

package com.flow.platform.api.domain;

import com.flow.platform.domain.Jsonable;

import java.util.*;

public abstract class Node extends Jsonable {

    protected String path;
    protected String name;
    protected Map<Env, String> envs = new HashMap<>();
    protected Node Parent;
    protected List<Node> children = new LinkedList<>();
    protected Node prev;
    protected Node next;
    protected Date createdAt;
    protected Date updatedAt;

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

    public Map<Env, String> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<Env, String> envs) {
        this.envs = envs;
    }

    public Node getParent() {
        return Parent;
    }

    public void setParent(Node parent) {
        Parent = parent;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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

        return name.equals(node.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
            "name='" + name + '\'' +
            '}';
    }
}
