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
package com.flow.platform.api.domain.credential;

import com.flow.platform.api.domain.CreateUpdateObject;
import com.google.gson.annotations.Expose;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author lhl
 */
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"name"}, callSuper = false)
@ToString(of = {"name", "type"})
public class Credential extends CreateUpdateObject {

    @Expose
    @Getter
    @Setter
    @NonNull
    protected String name;

    @Expose
    @Getter
    @Setter
    protected CredentialType type;

    @Expose
    @Getter
    @Setter
    protected String createdBy;

    /**
     * The credential detail will be saved as raw json
     */
    @Expose
    @Getter
    @Setter
    private CredentialDetail detail;
}
