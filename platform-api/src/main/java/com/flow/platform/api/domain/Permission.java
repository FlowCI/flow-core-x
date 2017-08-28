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
import java.util.Arrays;

/**
 * @author lhl
 */
public class Permission  extends Jsonable{

    private String name;

    private String desc;

    private String subjectName;

    private String action;

    private String subjectClass;

    private Integer[] groupIds;

    public Permission(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSubjectClass() {
        return subjectClass;
    }

    public void setSubjectClass(String subjectClass) {
        this.subjectClass = subjectClass;
    }

    public Integer[] getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(Integer[] groupIds) {
        this.groupIds = groupIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Permission that = (Permission) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Permission{" +
            "name='" + name + '\'' +
            ", desc='" + desc + '\'' +
            ", subjectName='" + subjectName + '\'' +
            ", action='" + action + '\'' +
            ", subjectClass='" + subjectClass + '\'' +
            ", groupIds=" + Arrays.toString(groupIds) +
            '}';
    }
}
