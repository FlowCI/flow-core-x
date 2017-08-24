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

package com.flow.platform.api.domain.envs;

/**
 * @author yang
 */
public enum FlowEnvs implements EnvKey {

    /**
     * Indicate flow is ready to run
     */
    FLOW_STATUS,

    /**
     * Indicate flow yml loading, ready
     */
    FLOW_YML_STATUS,

    /**
     * For yml error message while loading yml from git
     */
    FLOW_YML_ERROR_MSG;

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

    public enum YmlStatusValue implements EnvValue {

        NOT_FOUND("NOT_FOUND"),

        GIT_LOADING("GIT_LOADING"),

        GIT_LOADED("GIT_LOADED"),

        FOUND("FOUND"),

        ERROR("ERROR");

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
            return value.startsWith("GIT_");
        }
    }

}
