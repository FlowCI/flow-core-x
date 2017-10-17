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

package com.flow.platform.api.task;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.Logger;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Task to clone from git repo and create or update yml content of node
 * then write status and error message to
 *
 *   - FLOW_YML_STATUS
 *   - FLOW_YML_ERROR_MSG
 *
 * @author yang
 */
public class UpdateNodeYmlTask implements Runnable {

    private class EmptySuccessConsumer implements Consumer<Yml> {

        @Override
        public void accept(Yml o) {
        }
    }

    private class EmptyErrorConsumer implements Consumer<Throwable> {

        @Override
        public void accept(Throwable throwable) {
        }
    }

    private final static Logger LOGGER = new Logger(UpdateNodeYmlTask.class);

    private final Node root;

    private final NodeService nodeService;

    private final GitService gitService;

    private final Consumer<Yml> onSuccess;

    private final Consumer<Throwable> onError;

    public UpdateNodeYmlTask(Node root,
                             NodeService nodeService,
                             GitService gitService,
                             Consumer<Yml> onSuccess,
                             Consumer<Throwable> onError) {
        this.root = root;
        this.nodeService = nodeService;
        this.gitService = gitService;
        this.onSuccess = onSuccess == null ? new EmptySuccessConsumer() : onSuccess;
        this.onError = onError == null ? new EmptyErrorConsumer() : onError;
    }

    @Override
    public void run() {
        String yml;
        try {
            yml = gitService.fetch(root, AppConfig.DEFAULT_YML_FILE, new GitProgressListener());
            nodeService.updateYmlState(root, YmlStatusValue.GIT_LOADED, null);
        } catch (Throwable e) {
            // check yml status is running since exception will be throw if manual stop the git clone thread
            if (YmlStatusValue.isLoadingStatus(root.getEnv(FlowEnvs.FLOW_YML_STATUS))) {
                Throwable rootCause = ExceptionUtil.findRootCause(e);
                LOGGER.error("Unable to fetch from git repo", rootCause);
                nodeService.updateYmlState(root, YmlStatusValue.ERROR, rootCause.getMessage());
            }

            onError.accept(e);
            return;
        }

        try {
            nodeService.createOrUpdate(root.getPath(), yml);
        } catch (Throwable e) {
            LOGGER.warn("Fail to create or update yml in node: '%s'", ExceptionUtil.findRootCause(e).getMessage());
            onError.accept(e);
        }

        LOGGER.trace("Node %s FLOW_YML_STATUS is: %s", root.getName(), root.getEnv(FlowEnvs.FLOW_YML_STATUS));
        onSuccess.accept(new Yml(root.getPath(), yml));
    }

    private class GitProgressListener implements GitService.ProgressListener {

        @Override
        public void onStart() {

        }

        @Override
        public void onStartTask(String task) {
            LOGGER.debug("Task start: %s", task);
        }

        @Override
        public void onProgressing(String task, int total, int progress) {
            if (!Objects.equals(root.getEnv(FlowEnvs.FLOW_YML_STATUS), YmlStatusValue.GIT_LOADING.value())) {
                nodeService.updateYmlState(root, YmlStatusValue.GIT_LOADING, null);
            }
        }

        @Override
        public void onFinishTask(String task) {
            LOGGER.debug("Task finish: %s", task);
        }
    }
}
