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

package com.flow.platform.util.git;

import com.flow.platform.util.StringUtil;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TagOpt;

/**
 * @author yang
 */
public class JGitUtil {

    public final static Comparator<Ref> REF_COMPARATOR = Comparator.comparing(Ref::getObjectId);

    public static String getRepoNameFromGitUrl(String gitUrl) {
        int lastIndexOfSlash = gitUrl.lastIndexOf('/');
        int lastIndexOfDot = gitUrl.lastIndexOf('.');
        return gitUrl.substring(lastIndexOfSlash + 1, lastIndexOfDot);
    }

    public static List<String> simpleRef(Collection<Ref> refs) {
        List<String> refStringList = new ArrayList<>(refs.size());

        for (Ref ref : refs) {
            // convert ref name from refs/heads/master to master
            String refName = ref.getName();
            String simpleName = refName
                .replaceFirst("refs/heads/", StringUtil.EMPTY)
                .replace("refs/tags/", StringUtil.EMPTY);

            // add to result list
            refStringList.add(simpleName);
        }

        return refStringList;
    }

    public static Repository getRepo(Path path) throws GitException {
        try (Git git = Git.open(path.toFile())) {
            return git.getRepository();
        } catch (IOException e) {
            throw new GitException(e.getMessage());
        }
    }

    /**
     * Init git repo with bare
     *
     * @param gitPath xxx.git folder
     * @throws GitException
     */
    public static void init(Path gitPath, boolean bare) throws GitException {
        try {
            if (bare) {
                Git.init().setBare(bare).setGitDir(gitPath.toFile()).call();
            } else {
                Git.init().setDirectory(gitPath.toFile()).call();
            }
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage());
        }
    }

    /**
     * list all tag from local git
     * @param repo
     * @return
     * @throws GitException
     */
    public static List<String> tags(Repository repo) throws GitException {
        Map<String, Ref> tags = repo.getTags();
        List<Ref> refs = Lists.newArrayList(tags.values());
        Collections.reverse(refs);
        return simpleRef(refs);
    }

    /**
     * clone code to folder
     * @param gitUrl
     * @param targetDir
     * @return
     * @throws GitException
     */
    public static Path clone(String gitUrl, Path targetDir) throws GitException {

        CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(gitUrl)
            .setDirectory(targetDir.toFile());

        try (Git ignored = cloneCommand.call()) {
            return targetDir;
        } catch (GitAPIException e) {
            throw new GitException("Fail to clone git repo", e);
        }
    }

    /**
     * push code to remote repo
     * @param path
     * @param remote
     * @param branchOrTag
     * @return
     * @throws GitException
     */
    public static Path push(Path path, String remote, String branchOrTag) throws GitException {
        try (Git git = Git.open(path.toFile())) {
            git.push()
                .setRemote(remote)
                .setRefSpecs(new RefSpec(branchOrTag))
                .call();
        } catch (Throwable throwable) {
            throw new GitException("Fail to Push ", throwable);
        }

        return path;
    }

    /**
     * setting remote url to local repo
     * @param path
     * @param remoteName
     * @param remoteUrl
     * @return
     * @throws GitException
     */
    public static Path remoteSet(Path path, String remoteName, String remoteUrl) throws GitException {
        try (Git git = Git.open(path.toFile())) {
            StoredConfig config = git.getRepository().getConfig();
            config.setString("remote", remoteName, "url", remoteUrl);
            config.save();
        } catch (IOException e) {
            throw new GitException("set remote url exception", e);
        }

        return path;
    }

    public static Path fetchTags(Path path, String remoteName) throws GitException {
        try (Git git = Git.open(path.toFile())) {
            git.pull()
                .setRemote(remoteName)
                .setTagOpt(TagOpt.FETCH_TAGS)
                .call();
        } catch (Throwable throwable) {
            throw new GitException("fetch tags error", throwable);
        }

        return path;
    }
}
