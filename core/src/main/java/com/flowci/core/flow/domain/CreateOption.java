/*
 * Copyright 2019 fir.im
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

package com.flowci.core.flow.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.flowci.util.StringHelper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * @author yang
 */
@Data
@Accessors(chain = true)
public class CreateOption {

    private static final String TEMPLATE_BLANK = "_blank_";

    private String groupName;

    @JsonProperty("title")
    private String templateTitle;

    /**
     * Yaml in base64 format
     */
    @JsonProperty("yml")
    private String rawYaml;

    public boolean isBlank() {
        return Objects.equals(templateTitle, TEMPLATE_BLANK);
    }

    public boolean hasGroupName() {
        return StringHelper.hasValue(groupName);
    }

    public boolean hasTemplateTitle() {
        return StringHelper.hasValue(templateTitle);
    }

    public boolean hasRawYaml() {
        return StringHelper.hasValue(rawYaml);
    }
}
