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

import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.StringUtil;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@ToString(of = {"gitUrl", "repo", "syncType"})
public class SyncEvent extends Jsonable {

    /**
     * Output env variable for agent repo list
     */
    public final static String FLOW_SYNC_LIST = "FLOW_SYNC_LIST";

    public final static SyncEvent DELETE_ALL =
        new SyncEvent(null, StringUtil.EMPTY, StringUtil.EMPTY, SyncType.DELETE_ALL);

    public final static SyncEvent LIST =
        new SyncEvent(null, StringUtil.EMPTY, StringUtil.EMPTY, SyncType.LIST);

    /**
     * Git source url
     */
    @Getter
    @Setter
    private String gitUrl;

    /**
     * Repo name and tag
     */
    @Expose
    @Getter
    private SyncRepo repo;

    /**
     * Sync type
     */
    @Expose
    @Getter
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

    public String toScript() {
        if (syncType == SyncType.LIST) {
            return "export " + FLOW_SYNC_LIST + "=\"$(ls)\"";
        }

        if (syncType == SyncType.DELETE_ALL) {
            return "rm -rf ./*/";
        }

        // the sync event type DELETE, CREATE, UPDATE needs folder name
        String folder = repo.toString();

        if (syncType == SyncType.DELETE) {
            return "rm -rf " + folder;
        }

        return "git init " + folder +
            Unix.LINE_SEPARATOR +
            "cd " + folder +
            Unix.LINE_SEPARATOR +
            "git pull " + gitUrl + " --tags" +
            Unix.LINE_SEPARATOR +
            "git checkout " + repo.getTag();
    }
}



