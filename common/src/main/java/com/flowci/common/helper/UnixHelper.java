/*
 * Copyright 2018 flow.ci
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

package com.flowci.common.helper;

import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author yang
 */
public abstract class UnixHelper {

    private static final char EnvStart = '$';

    private static final char EnvLeftBracket = '{';

    private static final char EnvRightBracket = '}';

    /**
     * Parse path with ${xxx} variable to absolute path
     *
     * @param pathWithEnv path with folder/${xxx}/folder
     * @return absolute path
     */
    public static Path replacePathWithEnv(String pathWithEnv) {
        String[] paths = pathWithEnv.split(Pattern.quote(File.separator));

        StringBuilder builder = new StringBuilder();

        for (String pathItem : paths) {
            int index = pathItem.indexOf("$", 0);

            if (index < 0) {
                builder.append(pathItem).append(File.separator);
                continue;
            }

            builder.append(parseEnv(pathItem)).append(File.separator);
        }

        return Paths.get(builder.toString());
    }

    /**
     * Parse ${xx} variable to exact value
     */
    public static String parseEnv(String env) {
        if (Objects.isNull(env)) {
            throw new IllegalArgumentException();
        }

        if (env.charAt(0) != EnvStart) {
            throw new IllegalArgumentException();
        }

        boolean hasBracket = env.charAt(1) == EnvLeftBracket;
        env = env.substring(1);

        if (!hasBracket) {
            return getEnvOrProperty(env);
        }

        env = env.substring(1, env.length() - 1);
        return getEnvOrProperty(env);
    }

    /**
     * Get value from environment or property
     */
    public static String getEnvOrProperty(String name) {
        String value = System.getenv(name);
        if (StringUtils.hasLength(value)) {
            return value;
        }
        return System.getProperty(name);
    }

}
