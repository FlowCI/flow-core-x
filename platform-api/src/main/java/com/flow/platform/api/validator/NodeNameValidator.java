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

package com.flow.platform.api.validator;

import com.flow.platform.api.util.NodeUtil;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 * @author yang
 */
@Component
public class NodeNameValidator implements FlowValidator {

    private final static String ERR_CODE_NODE_NAME = "err.node.name";

    private final static String NAME = "node.name";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return String.class.isAssignableFrom(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        if (!NodeUtil.isValidName(o.toString())) {
            errors.reject(ERR_CODE_NODE_NAME, "Invalid node name");
        }
    }
}
