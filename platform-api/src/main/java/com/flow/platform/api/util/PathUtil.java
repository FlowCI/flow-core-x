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

import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.base.Strings;
import com.google.common.collect.Range;

/**
 * Node path builder
 * Path format ex : {root name}/{child name}/{child name}
 *
 * @author yang
 */
public class PathUtil {

    private final static String PATH_SEPARATOR = "/";

    private final static char PATH_SEPARATOR_CHAR = PATH_SEPARATOR.charAt(0);

    private final static int MAX_DEPTH = 10;

    private final static Range<Integer> NAME_LENGTH_RANGE = Range.closed(2, 100);

    private PathUtil() {

    }

    /**
     * Check parameter is path or name
     */
    public static boolean isRootName(String pathOrName) {
        return !pathOrName.contains(PATH_SEPARATOR);
    }

    /**
     * Build flow node path
     *
     * @param nameOrPaths node name or path, can be null or empty string
     * @return node path or null
     */
    public static String build(String... nameOrPaths) {
        StringBuilder builder = new StringBuilder();
        for (String nameOrPath : nameOrPaths) {
            if (nameOrPath == null) {
                continue;
            }
            nameOrPath = nameOrPath.trim();

            if (nameOrPath.startsWith(PATH_SEPARATOR)) {
                nameOrPath = nameOrPath.substring(1);
            }

            // name include path separator
            String[] names = nameOrPath.split(PATH_SEPARATOR);
            if (names.length > 0) {
                for (String name : names) {
                    if (Strings.isNullOrEmpty(name.trim())) {
                        continue;
                    }
                    validateName(name);
                    builder.append(PATH_SEPARATOR).append(name);
                }
                continue;
            }

            String name = nameOrPath;
            if (Strings.isNullOrEmpty(name)) {
                continue;
            }
            validateName(name);
            builder.append(PATH_SEPARATOR).append(name);
        }

        // remove first slash
        if (builder.length() > 0) {
            if (builder.charAt(0) == PATH_SEPARATOR_CHAR) {
                builder.deleteCharAt(0);
            }

            // remove last slash
            if (builder.charAt(builder.length() - 1) == PATH_SEPARATOR_CHAR) {
                builder.deleteCharAt(builder.length() - 1);
            }

            return builder.toString();
        }

        return null;
    }

    public static String rootName(String path) {
        return path.split(PATH_SEPARATOR)[0];
    }

    public static String rootPath(String path) {
        path = build(path); // format path
        return rootName(path);
    }

    /**
     * Get parent name
     *
     * @return parent name or empty string "" if in root level
     */
    public static String parentName(String path) {
        String[] names = path.split(PATH_SEPARATOR);
        try {
            return names[names.length - 2];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Get parent path
     *
     * @return parent path or empty string "" if in root level
     */
    public static String parentPath(String path) {
        int lastSeparatorIndex = path.lastIndexOf(PATH_SEPARATOR);
        if (lastSeparatorIndex <= 0) {
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

        name = name.trim();
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
