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
 * Command helper for windows, and unix liked os
 *
 * Enable parse @{property} for java property
 *
 * @author yang
 */
public class CommandUtil {

    public final static String FLOW_CI_ENV_HOME = "@{user.home}";

    private static CommandHelper commandHelper;

    static {
        if (SystemUtil.isWindows()) {
            commandHelper = new WindowsCommandHelper();
        } else {
            commandHelper = new UnixCommandHelper();
        }
    }

    public static CommandHelper commandHelper(String os) {
        if (SystemUtil.isWindows(os)) {
            return new WindowsCommandHelper();
        }
        return new UnixCommandHelper();
    }

    public static String ls(String dir) {
        return commandHelper.ls(dir);
    }

    /**
     * Delete directory
     */
    public static String rmdir(String dir) {
        return commandHelper.rmdir(dir);
    }

    /**
     * Delete all subdirectories on current path
     */
    public static String rmdir() {
        return commandHelper.rmdir(null);
    }

    public static String exitOnError() {
        return commandHelper.exitOnError();
    }

    public static String pwd() {
        return commandHelper.pwd();
    }

    public static String home() {
        return commandHelper.home();
    }

    public static String environments() {
        return commandHelper.listVariables();
    }

    public static String shellExecutor() {
        return commandHelper.shellExecutor();
    }

    public static String setVariable(String name, String value) {
        return commandHelper.setVariable(name, value);
    }

    public static String getVariable(String name) {
        return commandHelper.getVariable(name);
    }

    public static String parseVariable(String name) {
        return commandHelper.parse(name);
    }

    public static Path absolutePath(String pathWithEnv) {
        return commandHelper.absolutePath(pathWithEnv);
    }

    public static abstract class CommandHelper {

        public abstract String ls(String dir);

        public abstract String rmdir(String dir);

        public abstract String exitOnError();

        public abstract String pwd();

        public abstract String home();

        public abstract String listVariables();

        public abstract String shellExecutor();

        public abstract String setVariable(String name, String value);

        public abstract String getVariable(String name);

        /**
         * Parse variable to actual value
         * @param name variable name
         * @return actual value
         */
        public abstract String parse(String name);

        /**
         * Parse path contain variable to actual value
         *
         * @param pathWithEnv path with env variable
         * @return absolute path
         */
        public abstract Path absolutePath(String pathWithEnv);
    }

    private static class UnixCommandHelper extends CommandHelper {

        private final char ENV_VAR_START_CHAR = '$';

        private final char ENV_VAR_LEFT_BRACKET = '{';

        private final char ENV_VAR_RIGHT_BRACKET = '}';

        private final String PATH_SEPARATOR = "/";

        @Override
        public String ls(String dir) {
            if (Strings.isNullOrEmpty(dir)) {
                return "ls";
            }
            return "ls " + dir;
        }

        @Override
        public String rmdir(String dir) {
            if (Strings.isNullOrEmpty(dir)) {
                return "rm -rf ./*/";
            }
            return "rm -rf " + dir;
        }

        @Override
        public String exitOnError() {
            return "set -e";
        }

        @Override
        public String pwd() {
            return getVariable("PWD");
        }

        @Override
        public String home() {
            return getVariable("HOME");
        }

        @Override
        public String listVariables() {
            return "env";
        }

        @Override
        public String shellExecutor() {
            return "/bin/bash";
        }

        @Override
        public String setVariable(String name, String value) {
            return "export " + name + "=" + value;
        }

        @Override
        public String getVariable(String name) {
            return "${" + name + "}";
        }

        @Override
        public String parse(String name) {
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
        public Path absolutePath(String pathWithEnv) {
            String[] paths = pathWithEnv.split(Pattern.quote(PATH_SEPARATOR));
            StringBuilder pathAsString = new StringBuilder();

            for (String pathItem : paths) {
                int index = pathItem.indexOf(ENV_VAR_START_CHAR, 0);

                if (index < 0) {
                    pathAsString.append(pathItem).append(PATH_SEPARATOR);
                    continue;
                }

                pathAsString.append(parseVariable(pathItem)).append(PATH_SEPARATOR);
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
        public String ls(String dir) {
            if (Strings.isNullOrEmpty(dir)) {
                return "dir /b";
            }
            return "dir " + dir + "/b";
        }

        @Override
        public String rmdir(String dir) {
            if (Strings.isNullOrEmpty(dir)) {
                return "rmdir %CD% /s /q";
            }
            return "rmdir " + dir + " /s /q";
        }

        @Override
        public String exitOnError() {
            return StringUtil.EMPTY;
        }

        @Override
        public String pwd() {
            return getVariable("CD");
        }

        @Override
        public String home() {
            return getVariable("HOMEPATH");
        }

        @Override
        public String listVariables() {
            return "set";
        }

        @Override
        public String shellExecutor() {
            return "cmd.exe";
        }

        @Override
        public String setVariable(String name, String value) {
            return "set " + name + "=" + value;
        }

        @Override
        public String getVariable(String name) {
            return "%" + name + "%";
        }

        @Override
        public String parse(String name) {
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
        public Path absolutePath(String pathWithEnv) {
            String[] paths = pathWithEnv.split(Pattern.quote(SEPARATOR));
            StringBuilder pathAsString = new StringBuilder();

            for (String pathItem : paths) {
                if (pathItem.charAt(0) != ENV_BEGIN || pathItem.charAt(pathItem.length() - 1) != ENV_END) {
                    pathAsString.append(pathItem).append(SEPARATOR);
                    continue;
                }

                pathAsString.append(parseVariable(pathItem)).append(SEPARATOR);
            }

            pathAsString.deleteCharAt(pathAsString.length() - 1);
            return Paths.get(pathAsString.toString());
        }
    }
}
