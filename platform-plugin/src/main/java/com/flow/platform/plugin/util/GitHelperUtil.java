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
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * @author yh@firim
 */
public class GitHelperUtil {


    /**
     * git clone to path
     * @param gitClient
     * @return
     */
    public static File clone(GitClient gitClient) {
        File dist;
        try {
            dist = gitClient.clone("master", true);
        } catch (GitException e) {
            throw new PluginException("when clone code, it throw some error", e);
        }
        return dist;
    }


    /**
     * init local git source
     * @param path
     * @return
     */
    public static File initBareGit(Path path) {
        try {
            Git git = Git.init().setDirectory(path.toFile()).call();

            // change bare git location
            doRenameBareGit(path);
        } catch (GitAPIException e) {
            throw new PluginException("when clone code, it throw some error", e);
        }
        return path.toFile();
    }

    /**
     * get latest tag
     * @param gitClient
     * @return
     */
    public static String getLatestTag(GitClient gitClient) {
        String tag;
        try {
            List<String> tags = gitClient.tags();
            if (tags.size() > 0) {
                tag = tags.get(tags.size() - 1);
            } else {
                tag = null;
            }
        } catch (GitException e) {
            throw new PluginException("get tags error", e);
        }
        return tag;
    }

    private static File doRenameBareGit(Path path) {
        if (!isGitOrNot(path)) {
            throw new PluginException("sorry not a git project");
        }

        Path daoGitPath = Paths.get(path.toString(), ".git");
        Path parentPath = daoGitPath.getParent();
        Path destPath = Paths.get(path.getParent().toString(), parentPath.getFileName() + ".git");

        try {
            // move folder
            Files.move(daoGitPath, destPath);

            // delete folder
            FileUtils.deleteDirectory(parentPath.toFile());
        } catch (IOException e) {
            throw new PluginException("move folder error", e);
        }

        return destPath.toFile();
    }

    private static Boolean isGitOrNot(Path path) {
        File file = new File(path.toString());

        // if not directory, not a git project
        if (!file.isDirectory()) {
            return false;
        }

        Path daoGitPath = Paths.get(path.toString(), ".git");
        File daoGitFile = daoGitPath.toFile();

        // if not exist, not git project
        if (!daoGitFile.exists()) {
            return false;
        }

        // if not directory, not a git project
        if (!daoGitFile.isDirectory()) {
            return false;
        }

        return true;
    }

    public static void push() {

    }
}
