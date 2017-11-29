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

package com.flow.platform.plugin.domain;

import com.google.common.collect.ImmutableSet;
import com.google.gson.annotations.Expose;
import java.util.List;
import java.util.Set;

/**
 * @author yh@firim
 */
public class Plugin implements Comparable {

    public final static Set<PluginStatus> RUNNING_AND_FINISH_STATUS =
        ImmutableSet.of(PluginStatus.INSTALLING, PluginStatus.INSTALLED);

    public final static Set<PluginStatus> FINISH_STATUSES = ImmutableSet.of(PluginStatus.INSTALLED);

    private volatile Boolean isStopped = false;

    //plugin name
    @Expose
    private String name;

    // plugin git url
    @Expose
    private String details;

    // plugin labels
    @Expose
    private List<String> labels;

    // plugin author
    @Expose
    private String author;

    // plugin support platform
    @Expose
    private List<String> platform;

    // plugin status
    @Expose
    private PluginStatus status = PluginStatus.PENDING;

    @Expose
    private String tag;

    // if install error, the reason is error trace
    @Expose
    private String reason;

    @Expose
    private PluginDetail pluginDetail;

    public Plugin(String name, String details, List<String> label, String author, List<String> platform) {
        this.name = name;
        this.details = details;
        this.labels = label;
        this.author = author;
        this.platform = platform;
    }

    public Plugin(String name, String details, List<String> labels, String author,
                  List<String> platform, PluginStatus status, String tag) {
        this.name = name;
        this.details = details;
        this.labels = labels;
        this.author = author;
        this.platform = platform;
        this.status = status;
        this.tag = tag;
    }

    public Plugin() {
        this.isStopped = false;
        this.status = PluginStatus.PENDING;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getPlatform() {
        return platform;
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public void setPlatform(List<String> platform) {
        this.platform = platform;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }

    public PluginDetail getPluginDetail() {
        return pluginDetail;
    }

    public void setPluginDetail(PluginDetail pluginDetail) {
        this.pluginDetail = pluginDetail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Plugin plugin = (Plugin) o;

        return name != null ? name.equals(plugin.name) : plugin.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Plugin{" +
            "name='" + name + '\'' +
            ", details='" + details + '\'' +
            ", labels=" + labels +
            ", author='" + author + '\'' +
            ", platform='" + platform + '\'' +
            '}';
    }
}
