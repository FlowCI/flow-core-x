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
package com.flow.platform.api.domain.user;

import com.flow.platform.api.domain.CreateUpdateObject;
import com.google.gson.annotations.Expose;

/**
 * @author lhl
 */
public class Action extends CreateUpdateObject {

    @Expose
    private String name;

    @Expose
    private String alias;

    @Expose
    private String description;

    @Expose
    private String createBy;

    @Expose
    private ActionGroup tag = ActionGroup.DEFAULT;

    public Action() {
    }

    public Action(String name) {
        this.name = name;
    }

    public Action(String name, String alias, String description) {
        this.name = name;
        this.alias = alias;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ActionGroup getTag() {
        return tag;
    }

    public void setTag(ActionGroup tag) {
        this.tag = tag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Action action = (Action) o;
        return name.equals(action.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Action{" +
            "name='" + name + '\'' +
            ", alias='" + alias + '\'' +
            "} ";
    }
}
