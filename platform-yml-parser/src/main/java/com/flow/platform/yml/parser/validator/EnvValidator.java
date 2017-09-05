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

package com.flow.platform.yml.parser.validator;


import java.util.Map;

/**
 * @author gyfirim
 */
public class EnvValidator extends BaseValidator<Map<String, String>> {

    @Override
    public Boolean ReadValidator(Map<String, String> o) {
        if(o == null){
            return false;
        }
        o.forEach((k, v) -> {
        });
        return true;
    }
}
