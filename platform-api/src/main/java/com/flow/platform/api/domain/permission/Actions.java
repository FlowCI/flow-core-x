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

package com.flow.platform.api.domain.permission;

/**
 * @author yang
 */
public enum Actions {

    ADMIN_CREATE,

    ADMIN_DELETE,

    ADMIN_UPDATE,

    ADMIN_SHOW,

    FLOW_CREATE,

    FLOW_DELETE,

    FLOW_SHOW,

    FLOW_SET_ENV,

    FLOW_YML,

    FLOW_AUTH,

    GENERATE_KEY,

    JOB_CREATE,

    JOB_SHOW,

    JOB_YML,

    JOB_LOG,

    JOB_STOP,

    CREDENTIAL_SHOW,

    USER_SHOW

}
