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

import com.flow.platform.exception.IllegalParameterException;
import java.util.HashMap;
import org.springframework.validation.MapBindingResult;

/**
 * @author yang
 */
public class ValidatorUtil {

    public static void invoke(FlowValidator validator, Object obj) {
        if (!validator.supports(obj.getClass())) {
            throw new IllegalParameterException("Unsupported parameter type");
        }

        MapBindingResult errs = new MapBindingResult(new HashMap<>(), validator.getName());
        validator.validate(obj, errs);

        if (errs.hasErrors()) {
            throw new IllegalParameterException(errs.getAllErrors().get(0).getDefaultMessage());
        }
    }
}
