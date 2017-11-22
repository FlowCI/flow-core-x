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

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * @author yang
 */
public class JGitUtil {

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
    public static void initBare(Path gitPath) throws GitException {
        try {
            Git.init().setBare(true).setGitDir(gitPath.toFile()).call();
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage());
        }
    }
}
