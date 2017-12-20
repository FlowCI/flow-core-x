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
import java.util.Objects;
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

    public final static String FLOW_CI_FILE_SEPARATOR = "|";

    private static CommandHelper commandHelper;

    static {
        if (SystemUtil.isWindows()) {
            commandHelper = new WindowsCommandHelper();
        } else {
            commandHelper = new UnixCommandHelper();
        }
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

    public static String[] shellExecutor() {
        return commandHelper.shellExecutor();
    }

    public static String setVariable(String name, String value) {
        return commandHelper.setVariable(name, value);
    }

    public static String setVariableFromCmd(String name, String value) {
        return commandHelper.setVariableFromCmd(name, value);
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

        public abstract String separator();

        public abstract String ls(String dir);

        public abstract String rmdir(String dir);

        public abstract String exitOnError();

        public abstract String pwd();

        public abstract String home();

        public abstract String listVariables();

        public abstract String[] shellExecutor();

        public abstract String setVariable(String name, String value);

        public abstract String setVariableFromCmd(String name, String cmd);

        public abstract String getVariable(String name);

        /**
         * Parse variable to actual value
         * @param var variable name
         * @return actual value
         */
        public abstract String parse(String var);

        /**
         * Parse path contain variable to actual value
         *
         * @param pathWithEnv path with env variable
         * @return absolute path
         */
        public Path absolutePath(String pathWithEnv) {
            String[] paths;
            if (pathWithEnv.contains(FLOW_CI_FILE_SEPARATOR)) {
                paths = pathWithEnv.split(Pattern.quote(FLOW_CI_FILE_SEPARATOR));
            } else {
                paths = pathWithEnv.split(Pattern.quote(separator()));
            }

            StringBuilder pathAsString = new StringBuilder();

            for (String pathItem : paths) {
                pathAsString.append(parse(pathItem)).append(separator());
            }

            pathAsString.deleteCharAt(pathAsString.length() - 1);
            return Paths.get(pathAsString.toString());
        }

        /**
         * Parse java property, and return null if it is not match pattern
         */
        String parseJavaProperty(String var) {
            // at least has 4 chars @{x}
            if (var.length() < 4) {
                return null;
            }

            if (var.startsWith("@{") && var.endsWith("}")) {
                String property = var.substring(2, var.length() - 1);
                return System.getProperty(property);
            }

            return null;
        }
    }

    private static class UnixCommandHelper extends CommandHelper {

        private final char ENV_VAR_START_CHAR = '$';

        private final char ENV_VAR_LEFT_BRACKET = '{';

        private final char ENV_VAR_RIGHT_BRACKET = '}';

        private final String PATH_SEPARATOR = "/";

        @Override
        public String separator() {
            return PATH_SEPARATOR;
        }

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
        public String[] shellExecutor() {
            return new String[]{"/bin/bash"};
        }

        @Override
        public String setVariable(String name, String value) {
            return "export " + name + "=" + value;
        }

        @Override
        public String setVariableFromCmd(String name, String cmd) {
            return setVariable(name, "\"$(" + cmd + ")\"");
        }

        @Override
        public String getVariable(String name) {
            return "${" + name + "}";
        }

        @Override
        public String parse(String var) {
            if (Objects.isNull(var)) {
                throw new IllegalArgumentException();
            }

            if (var.length() < 1) {
                return var;
            }

            // parse by java property
            String value = parseJavaProperty(var);
            if (!Strings.isNullOrEmpty(value)) {
                return value;
            }

            if (var.charAt(0) != ENV_VAR_START_CHAR) {
                return var;
            }

            boolean hasBracket = var.charAt(1) == ENV_VAR_LEFT_BRACKET;
            var = var.substring(1);

            if (!hasBracket) {
                return System.getenv(var);
            }

            var = var.substring(1, var.length() - 1);
            return System.getenv(var);
        }
    }

    private static class WindowsCommandHelper extends CommandHelper {

        private final char ENV_BEGIN = '%';

        private final char ENV_END = '%';

        private final String SEPARATOR = "\\";

        @Override
        public String separator() {
            return SEPARATOR;
        }

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
        public String[] shellExecutor() {
            return new String[]{"cmd.exe", "/v:on"};
        }

        @Override
        public String setVariable(String name, String value) {
            return "set " + name + "=" + value;
        }

        @Override
        public String setVariableFromCmd(String name, String cmd) {
            // assign cmd result to variable, split with \n for multiple line
            return "set " + name + "= && "
                    + "for /f \"delims=\" %f in ('dir /b') "
                        + "do (" +
                            " set " + name + "=!" + name + "!\\n%f" +
                            ")";
        }

        @Override
        public String getVariable(String name) {
            return "%" + name + "%";
        }

        @Override
        public String parse(String var) {
            if (Objects.isNull(var)) {
                throw new IllegalArgumentException();
            }

            if (var.length() < 2) {
                return var;
            }

            // parse by java property
            String value = parseJavaProperty(var);
            if (!Strings.isNullOrEmpty(value)) {
                return value;
            }

            if (var.charAt(0) != ENV_BEGIN || var.charAt(var.length() - 1) != ENV_END) {
                return var;
            }

            String env = var.substring(1, var.length() - 1);
            return System.getenv(env);
        }
    }
}
