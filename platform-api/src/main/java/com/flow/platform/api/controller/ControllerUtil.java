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

package com.flow.platform.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author yang
 */
public class ControllerUtil {

    private final static String PARAM_DELIMITE = ",";

    public final static Function<String, String> STRING_CONVERTER = s -> s;

    public static <R> List<R> extractParam(String param, Function<String, R> converter) {
        String[] params = param.split(PARAM_DELIMITE);
        List<R> list = new ArrayList<>(params.length);
        for (String item : params) {
            R apply = converter.apply(item);

            if (apply != null) {
                list.add(apply);
            }
        }
        return list;
    }

}
