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
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;

/**
 * @author yh@firim
 */
public class GitHelperUtil {

    private static final String REF_TAGS = "refs/tags/";
    private static final String GIT_NOTE = ".git";

    /**
     * git clone to path
     * @param gitClient
     * @return
     */
    public static Path clone(GitClient gitClient) {
        File dist;
        try {
            dist = gitClient.clone("master", true);
        } catch (GitException e) {
            throw new PluginException("when clone code, it throw some error", e);
        }
        return dist.toPath().getParent();
    }


    /**
     * push tag to remote
     * @param path
     * @param remote
     * @param tag
     */
    public static void pushTag(Path path, String remote, String tag) {
        checkIsFolderOrNot(path);
        checkIsGitOrNot(path);
        try {
            Git git = Git.open(path.toFile());
            git
                .push()
                .setRemote(remote)
                .setRefSpecs(new RefSpec(tag))
                .call();

        } catch (IOException e) {
            throw new PluginException("Io exception", e);
        } catch (InvalidRemoteException e) {
            throw new PluginException("invalid remote exception", e);
        } catch (TransportException e) {
            throw new PluginException("invalid transport exception", e);
        } catch (GitAPIException e) {
            throw new PluginException("git api exception", e);
        }
    }

    /**
     * init local git repository
     * @param path
     * @return
     */
    public static File initBareGitRepository(Path path) {
        if (path.getFileName().endsWith(GIT_NOTE)) {
            throw new PluginException("path must be end with .git");
        }

        path = Paths.get(path.getParent().toString(), path.getFileName().toString().replace(GIT_NOTE, Strings.EMPTY));

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
     * @return
     */
    public static String getLatestTag(Path gitPath) {
        List<String> tags = tagList(gitPath);
        if (tags.isEmpty()) {
            return null;
        }
        return tags.get(tags.size() - 1);
    }

    /**
     * get tag list from local Thread
     * @param gitPath
     * @return
     */
    public static List<String> tagList(Path gitPath) {
        checkIsFolderOrNot(gitPath);
        checkIsGitOrNot(gitPath);

        List<String> tags = new LinkedList<>();
        try {
            Git git = Git.open(gitPath.toFile());
            List<Ref> refs = git.tagList()
                .call();
            for (Ref ref : refs) {
                tags.add(ref.getName().replace(REF_TAGS, Strings.EMPTY));
            }
        } catch (IOException e) {
            throw new PluginException("io exception ", e);
        } catch (GitAPIException e) {
            throw new PluginException("api exception ", e);
        }
        return tags;
    }

    /**
     * set local remote url to git repo
     * @param gitPath
     * @param localRepoPath
     * @return
     */
    public static Git setLocalRemote(Path gitPath, Path localRepoPath) {
        checkIsFolderOrNot(gitPath);
        Git git;
        try {
            git = Git.open(gitPath.toFile());
            StoredConfig config = git.getRepository().getConfig();
            config.setString("remote", "local", "url", localRepoPath.toString());
            config.save();
        } catch (IOException e) {
            throw new PluginException("set local git remote error", e);
        }

        return git;
    }

    private static File doRenameBareGit(Path path) {
        checkIsGitOrNot(path);

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

        if (!file.exists()) {
            return false;
        }

        // if not directory, not a git project
        if (!file.isDirectory()) {
            return false;
        }

        try {
            Git.open(path.toFile());
        } catch (RepositoryNotFoundException e) {
            return false;
        } catch (Throwable throwable) {
        }
        return true;
    }

    private static void checkIsFolderOrNot(Path gitPath) {
        // gitPath must be folder
        if (!gitPath.toFile().isDirectory()) {
            throw new PluginException("this path must be folder");
        }
    }

    private static void checkIsGitOrNot(Path path) {
        if (!isGitOrNot(path)) {
            throw new PluginException("sorry not a git project");
        }
    }
}
