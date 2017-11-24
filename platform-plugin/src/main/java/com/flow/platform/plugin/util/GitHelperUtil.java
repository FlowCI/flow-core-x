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

package com.flow.platform.plugin.util;

import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.util.git.JGitUtil;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;

/**
 * @author yh@firim
 */
public class GitHelperUtil {

    /**
     * get latest tag
     * @return
     */
    public static String getLatestTag(Path gitPath) {
        try {
            List<String> tags = JGitUtil.tags(Git.open(gitPath.toFile()).getRepository());
            if (tags.isEmpty()) {
                return "";
            } else {
                return tags.get(0);
            }
        } catch (Throwable throwable) {
            throw new PluginException("get latest tag is error", throwable);
        }
    }
}
