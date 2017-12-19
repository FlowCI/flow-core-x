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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * @author yang
 */
public class CommandUtil {

    private static CommandHelper commandHelper;

    static {
        if (SystemUtil.isWindows()) {
            commandHelper = new WindowsCommandHelper();
        } else {
            commandHelper = new UnixCommandHelper();
        }
    }

    public static String exitOnError() {
        return commandHelper.exitOnError();
    }

    public static String pwd() {
        return commandHelper.pwd();
    }

    public static String homeVariable() {
        return commandHelper.home();
    }

    public static String listVariable() {
        return commandHelper.listVariables();
    }

    public static String shellExecutor() {
        return commandHelper.shellExecutor();
    }

    public static String setVariableScript(String name, String value) {
        return commandHelper.setVariable(name, value);
    }

    public static String getVariableScript(String name) {
        return commandHelper.getVariable(name);
    }

    public static String parseEnv(String name) {
        return commandHelper.parse(name);
    }

    public static Path absolutePath(String pathWithEnv) {
        return commandHelper.absolutePath(pathWithEnv);
    }

    static abstract class CommandHelper {

        abstract String exitOnError();

        abstract String pwd();

        abstract String home();

        abstract String listVariables();

        abstract String shellExecutor();

        abstract String setVariable(String name, String value);

        abstract String getVariable(String name);

        /**
         * Parse variable to actual value
         * @param name variable name
         * @return actual value
         */
        abstract String parse(String name);

        /**
         * Parse path contain variable to actual value
         *
         * @param pathWithEnv path with env variable
         * @return absolute path
         */
        abstract Path absolutePath(String pathWithEnv);
    }

    private static class UnixCommandHelper extends CommandHelper {

        private final char ENV_VAR_START_CHAR = '$';

        private final char ENV_VAR_LEFT_BRACKET = '{';

        private final char ENV_VAR_RIGHT_BRACKET = '}';

        private final String SEPARATOR = "/";

        @Override
        String exitOnError() {
            return "set -e";
        }

        @Override
        String pwd() {
            return getVariable("PWD");
        }

        @Override
        String home() {
            return getVariable("HOME");
        }

        @Override
        String listVariables() {
            return "env";
        }

        @Override
        String shellExecutor() {
            return "/bin/bash";
        }

        @Override
        String setVariable(String name, String value) {
            return "export " + name + "=" + value;
        }

        @Override
        String getVariable(String name) {
            return "${" + name + "}";
        }

        @Override
        String parse(String name) {
            if (Strings.isNullOrEmpty(name)) {
                throw new IllegalArgumentException();
            }

            if (name.charAt(0) != ENV_VAR_START_CHAR) {
                throw new IllegalArgumentException();
            }

            boolean hasBracket = name.charAt(1) == ENV_VAR_LEFT_BRACKET;
            name = name.substring(1);

            if (!hasBracket) {
                return System.getenv(name);
            }

            name = name.substring(1, name.length() - 1);
            return System.getenv(name);
        }

        @Override
        Path absolutePath(String pathWithEnv) {
            String[] paths = pathWithEnv.split(Pattern.quote(SEPARATOR));
            StringBuilder pathAsString = new StringBuilder();

            for (String pathItem : paths) {
                int index = pathItem.indexOf(ENV_VAR_START_CHAR, 0);

                if (index < 0) {
                    pathAsString.append(pathItem).append(SEPARATOR);
                    continue;
                }

                pathAsString.append(parseEnv(pathItem)).append(SEPARATOR);
            }

            pathAsString.deleteCharAt(pathAsString.length() - 1);
            return Paths.get(pathAsString.toString());
        }
    }

    private static class WindowsCommandHelper extends CommandHelper {

        private final char ENV_BEGIN = '%';

        private final char ENV_END = '%';

        private final String SEPARATOR = "\\";

        @Override
        String exitOnError() {
            return StringUtil.EMPTY;
        }

        @Override
        String pwd() {
            return getVariable("CD");
        }

        @Override
        String home() {
            return getVariable("HOMEPATH");
        }

        @Override
        String listVariables() {
            return "set";
        }

        @Override
        String shellExecutor() {
            return "cmd.exe";
        }

        @Override
        String setVariable(String name, String value) {
            return "set " + name + "=" + value;
        }

        @Override
        String getVariable(String name) {
            return "%" + name + "%";
        }

        @Override
        String parse(String name) {
            if (Strings.isNullOrEmpty(name)) {
                throw new IllegalArgumentException();
            }

            if (name.charAt(0) != ENV_BEGIN || name.charAt(name.length() - 1) != ENV_END) {
                throw new IllegalArgumentException();
            }

            String env = name.substring(1, name.length() - 1);
            return System.getenv(env);
        }

        @Override
        Path absolutePath(String pathWithEnv) {
            String[] paths = pathWithEnv.split(Pattern.quote(SEPARATOR));
            StringBuilder pathAsString = new StringBuilder();

            for (String pathItem : paths) {
                if (pathItem.charAt(0) != ENV_BEGIN || pathItem.charAt(pathItem.length() - 1) != ENV_END) {
                    pathAsString.append(pathItem).append(SEPARATOR);
                    continue;
                }

                pathAsString.append(parseEnv(pathItem)).append(SEPARATOR);
            }

            pathAsString.deleteCharAt(pathAsString.length() - 1);
            return Paths.get(pathAsString.toString());
        }
    }
}
