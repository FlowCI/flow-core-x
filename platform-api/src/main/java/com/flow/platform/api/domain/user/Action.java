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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author lhl
 */
@EqualsAndHashCode(of = {"name"}, callSuper = false)
@ToString(of = {"name", "alias"})
public class Action extends CreateUpdateObject {

    @Expose
    @Getter
    @Setter
    private String name;

    @Expose
    @Getter
    @Setter
    private String alias;

    @Expose
    @Getter
    @Setter
    private String description;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    @Expose
    @Getter
    @Setter
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
}
