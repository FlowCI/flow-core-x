/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.job.domain;

import com.google.common.collect.ImmutableList;

import java.util.List;

public abstract class JobAction {

    public static final String LIST = "list_job";

    public static final String GET = "get_job";

    public static final String GET_YML = "get_job_yml";

    public static final String LIST_STEPS = "list_job_steps";

    public static final String GET_STEP_LOG = "get_job_step_log";

    public static final String DOWNLOAD_STEP_LOG = "get_job_step_log";

    public static final String CREATE = "create_job";

    public static final String RUN = "run_job";

    public static final String CANCEL = "cancel_job";

    public static final String LIST_REPORTS = "list_job_reports";

    public static final String FETCH_REPORT = "fetch_job_report";

    public static final String LIST_ARTIFACTS = "list_job_artifacts";

    public static final String DOWNLOAD_ARTIFACT = "fetch_job_artifact";

    public static final List<String> ALL = ImmutableList.of(
            LIST,
            GET,
            GET_YML,
            LIST_STEPS,
            GET_STEP_LOG,
            DOWNLOAD_STEP_LOG,
            CREATE,
            RUN,
            CANCEL,
            LIST_REPORTS,
            FETCH_REPORT,
            LIST_ARTIFACTS,
            DOWNLOAD_ARTIFACT
    );
}
