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
import com.flowci.util.StringHelper;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yang
 */
@Data
@Accessors(chain = true)
public class ConfirmOption {

    private static final String BlankTitle = "_blank_";

    private String gitUrl;

    private String secret;

    /**
     * Yaml in base64
     */
    private String yaml = StringHelper.EMPTY;

    /**
     * Template name (desc), yaml property will be ignored if it is defined
     */
    @JsonProperty("title")
    private String templateTitle;

    public boolean hasGitUrl() {
        return StringHelper.hasValue(gitUrl);
    }

    public boolean hasSecret() {
        return StringHelper.hasValue(secret);
    }

    public boolean hasYml() {
        return StringHelper.hasValue(yaml);
    }

    public boolean hasTemplateTitle() {
        return StringHelper.hasValue(templateTitle);
    }

    public boolean hasBlankTemplate() {
        return BlankTitle.equalsIgnoreCase(templateTitle);
    }
}
