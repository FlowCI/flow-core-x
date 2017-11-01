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

import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.google.gson.annotations.Expose;
import java.time.ZonedDateTime;
import java.util.List;

public class Flow extends Node<Step> {

    @Expose
    private String createdBy;

    @Expose
    protected ZonedDateTime createdAt;

    @Expose
    protected ZonedDateTime updatedAt;

    @Expose
    protected List<String> branchFilter;

    @Expose
    protected List<String> tagFilter;

    @Expose
    protected boolean tagEnable = true;

    @Expose
    protected boolean pushEnable = true;

    @Expose
    protected boolean prEnable = true;


    public Flow(String path, String name) {
        super(path, name);
    }

    public Flow() {
    }

    @Override
    public void setChildren(List<Step> children) {
        super.setChildren(children);
    }

    @Override
    public List<Step> getChildren() {
        return super.getChildren();
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

    public List<String> getBranchFilter() {
        return branchFilter;
    }

    public void setBranchFilter(List<String> branchFilter) {
        this.branchFilter = branchFilter;
    }

    public List<String> getTagFilter() {
        return tagFilter;
    }

    public void setTagFilter(List<String> tagFilter) {
        this.tagFilter = tagFilter;
    }

    public boolean isTagEnable() {
        return tagEnable;
    }

    public void setTagEnable(boolean tagEnable) {
        this.tagEnable = tagEnable;
    }

    public boolean isPushEnable() {
        return pushEnable;
    }

    public void setPushEnable(boolean pushEnable) {
        this.pushEnable = pushEnable;
    }

    public boolean isPrEnable() {
        return prEnable;
    }

    public void setPrEnable(boolean prEnable) {
        this.prEnable = prEnable;
    }

    @YmlSerializer(required = false)
    @Override
    public String getName() {
        return super.getName();
    }
}
