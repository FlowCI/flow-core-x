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

package com.flow.platform.api.util;

import com.flow.platform.exception.IllegalParameterException;
import com.google.common.base.Strings;
import com.google.common.collect.Range;

/**
 * Node path builder
 *
 * @author yang
 */
public class PathUtil {

    private final static String PATH_SEPARATOR = "/";

    private final static int MAX_DEPTH = 10;

    private final static Range<Integer> NAME_LENGTH_RANGE = Range.closed(2, 20);

    private PathUtil() {

    }

    /**
     * Build flow node path
     *
     * @param nameOrPaths node name or path
     * @return node path
     */
    public static String build(String... nameOrPaths) {
        StringBuilder builder = new StringBuilder();
        for (String name : nameOrPaths) {
            if (name.startsWith(PATH_SEPARATOR)) {
                name = name.substring(1);
            }
            validateName(name);
            builder.append(PATH_SEPARATOR).append(name);
        }
        return builder.toString();
    }

    public static String rootName(String path) {
        return path.split(PATH_SEPARATOR)[1]; // 0 is empty string
    }

    public static String rootPath(String path) {
        return build(rootName(path)); // 0 is empty string
    }

    /**
     * Get parent name
     *
     * @return parent name or empty string "" if in root level
     */
    public static String parentName(String path) {
        String[] names = path.split(PATH_SEPARATOR);
        return names[names.length - 2];
    }

    /**
     * Get parent path
     *
     * @return parent path or empty string "" if in root level
     */
    public static String parentPath(String path) {
        int lastSeparatorIndex = path.lastIndexOf(PATH_SEPARATOR);
        if (lastSeparatorIndex == 0) {
            return "";
        }
        return path.substring(0, lastSeparatorIndex);
    }

    /**
     * Get current level of node name from node path
     */
    public static String currentName(String path) {
        int lastSeparatorIndex = path.lastIndexOf(PATH_SEPARATOR);
        if (lastSeparatorIndex == 0) {
            return path.substring(1);
        }
        return path.substring(lastSeparatorIndex + 1);
    }

    public static void validateName(String name) throws IllegalParameterException {
        String errMsg = "Illegal node name: " + name;

        if (Strings.isNullOrEmpty(name) || name.startsWith(PATH_SEPARATOR)) {
            throw new IllegalParameterException(errMsg);
        }

        if (!NAME_LENGTH_RANGE.contains(name.length())) {
            throw new IllegalParameterException(errMsg);
        }

        if (name.contains("*")) {
            throw new IllegalParameterException(errMsg);
        }
    }

    public static void validatePath(String path) throws IllegalParameterException {
        String errMsg = "Illegal node path";

        if (Strings.isNullOrEmpty(path)) {
            throw new IllegalParameterException(errMsg);
        }

        // path must start with / and cannot end with /
        if (!path.startsWith(PATH_SEPARATOR) || path.endsWith(PATH_SEPARATOR)) {
            throw new IllegalParameterException(errMsg);
        }

        String[] names = path.split(PATH_SEPARATOR);
        int depth = names.length - 1;

        if (depth < 1 || depth > MAX_DEPTH) {
            throw new IllegalParameterException(errMsg);
        }

        for (int i = 1; i < names.length; i++) {
            validateName(names[i]);
        }
    }
}
