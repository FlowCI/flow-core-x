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

package com.flow.platform.api.envs;

import com.google.common.base.Strings;

/**
 * @author yang
 */
public enum FlowEnvs implements EnvKey {

    /**
     * Indicate flow is configured for git
     */
    FLOW_STATUS,

    /**
     * Indicate flow yml loading, ready
     */
    FLOW_YML_STATUS,

    /**
     * For yml error message while loading yml from git
     */
    FLOW_YML_ERROR_MSG,


    /**
     * Defined env variable output prefix
     */
    FLOW_ENV_OUTPUT_PREFIX,

    /**
     * To define crontab task
     */
    FLOW_CRONTAB_TASK;

    /**
     * Value set for FLOW_STATUS
     */
    public enum StatusValue implements EnvValue {

        READY("READY"),

        PENDING("PENDING");

        private String value;

        StatusValue(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Value set for FLOW_YML_STATUS
     */
    public enum YmlStatusValue implements EnvValue {

        NOT_FOUND("NOT_FOUND"), // init status

        GIT_CONNECTING("GIT_CONNECTING"), // on git connection

        GIT_LOADING("GIT_LOADING"), // git clone in progress

        GIT_LOADED("GIT_LOADED"), // git clone is finished

        FOUND("FOUND"), // flow yml is created

        ERROR("ERROR"); // flow yml has error

        private String value;

        YmlStatusValue(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static boolean isLoadingStatus(String value) {
            if (Strings.isNullOrEmpty(value)) {
                return false;
            }

            if (value.equals(GIT_LOADED.value())) {
                return false;
            }

            return value.equals(GIT_CONNECTING.value()) || value.equals(GIT_LOADING.value());
        }
    }

}
