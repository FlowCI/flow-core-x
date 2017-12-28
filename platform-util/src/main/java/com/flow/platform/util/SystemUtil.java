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

package com.flow.platform.util;

import com.google.common.base.Strings;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author yang
 */
public class SystemUtil {

    private final static char ENV_VAR_START_CHAR = '$';
    private final static char ENV_VAR_LEFT_BRACKET = '{';
    private final static char ENV_VAR_RIGHT_BRACKET = '}';

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.startsWith("win");
    }

    /**
     * Parse path with ${xxx} variable to absolute path
     *
     * @param pathWithEnv path with folder/${xxx}/folder
     * @return absolute path
     */
    public static Path replacePathWithEnv(String pathWithEnv) {
        String[] paths = pathWithEnv.split(Pattern.quote(File.separator));
        Path path = Paths.get("/");

        for (String pathItem : paths) {
            int index = pathItem.indexOf("$", 0);

            if (index < 0) {
                path = Paths.get(path.toString(), pathItem);
                continue;
            }

            path = Paths.get(path.toString(), parseEnv(pathItem));
        }

        return path;
    }

    /**
     * Parse ${xx} variable to exact value
     */
    public static String parseEnv(String env) {
        if (Objects.isNull(env)) {
            throw new IllegalArgumentException();
        }

        if (env.charAt(0) != ENV_VAR_START_CHAR) {
            throw new IllegalArgumentException();
        }

        boolean hasBracket = env.charAt(1) == ENV_VAR_LEFT_BRACKET;
        env = env.substring(1);

        if (!hasBracket) {
            return getEnvOrProperty(env);
        }

        env = env.substring(1, env.length() - 1);
        return getEnvOrProperty(env);
    }

    private static String getEnvOrProperty(String name) {
        String value = System.getenv(name);

        if (Strings.isNullOrEmpty(value)) {
            return System.getProperty(name);
        }

        return value;
    }
}
