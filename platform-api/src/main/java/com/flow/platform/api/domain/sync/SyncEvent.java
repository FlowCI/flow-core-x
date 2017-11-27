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

package com.flow.platform.api.domain.sync;

import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.Jsonable;

/**
 * @author yang
 */
public class SyncEvent extends Jsonable {

    /**
     * Output env variable for agent repo list
     */
    public final static String FLOW_SYNC_LIST = "FLOW_SYNC_LIST";

    /**
     * Git source url
     */
    private String gitUrl;

    /**
     * Repo name and tag
     */
    private SyncRepo repo;

    /**
     * Sync type
     */
    private SyncType syncType;

    public SyncEvent(String gitUrl, String name, String tag, SyncType syncType) {
        this.gitUrl = gitUrl;
        this.repo = new SyncRepo(name, tag);
        this.syncType = syncType;
    }

    public SyncEvent(String gitUrl, SyncRepo repo, SyncType syncType) {
        this.gitUrl = gitUrl;
        this.repo = repo;
        this.syncType = syncType;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public SyncRepo getRepo() {
        return repo;
    }

    public SyncType getSyncType() {
        return syncType;
    }

    public String toScript() {
        if (syncType == SyncType.LIST) {
            return "export " + FLOW_SYNC_LIST + "=\"$(ls)\"";
        }

        String folder = repo.toString();

        if (syncType == SyncType.DELETE) {
            return "rm -r -f " + folder;
        }

        return "git init " + folder +
            Cmd.NEW_LINE +
            "cd " + folder +
            Cmd.NEW_LINE +
            "git pull " + gitUrl + " --tags" +
            Cmd.NEW_LINE +
            "git checkout " + repo.getTag();
    }

    @Override
    public String toString() {
        return "SyncEvent{" +
            "gitUrl='" + gitUrl + '\'' +
            ", repo='" + repo + '\'' +
            ", syncType=" + syncType +
            "} " + super.toString();
    }
}
