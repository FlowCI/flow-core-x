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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * @author yang
 */
public class SystemUtil {

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
            int leftBracket = pathItem.indexOf("{", 0);
            int rightBracket = pathItem.indexOf("}", 0);

            if (index < 0 || leftBracket < 0 || rightBracket < 0) {
                path = Paths.get(path.toString(), pathItem);
                continue;
            }

            String envName = pathItem.substring(leftBracket + 1, rightBracket);
            String envValue = System.getenv(envName);
            path = Paths.get(path.toString(), envValue);
        }

        return path;
    }
}
