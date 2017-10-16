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

import com.flow.platform.util.git.model.GitCommit;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;

/**
 * @author yang
 */
public interface GitClient {

    /**
     * Git repo dir
     */
    Path targetPath();

    /**
     * Git clone from remote url
     *
     * @param branch branch to clone, can be set to null
     * @param noCheckout git clone xxxx --no-checkout, only clone .git dir
     * @return .git folder path
     */
    File clone(String branch, boolean noCheckout) throws GitException;

    /**
     * Git clone from remote url with specific checkout files
     * Just pull latest if .git folder existed
     *
     * @param branch branch to clone, can be set to null
     * @param checkoutFiles specific checkout file
     * @param monitor git progress monitor, can be null
     * @return .git folder path
     * @throws GitException if git clone fail
     */
    File clone(String branch, Set<String> checkoutFiles, ProgressMonitor monitor) throws GitException;

    /**
     * Fetch file content
     *
     * @param branch branch of target file
     * @param filePath git file path
     * @param monitor git progress monitor, can be null
     */
    String fetch(String branch, String filePath, ProgressMonitor monitor) throws GitException;

    /**
     * Git pull with depth
     *
     * @param branch branch to pull, can be set to null
     * @param monitor git progress monitor, can be null
     * @throws GitException if git pull fail
     */
    void pull(String branch, ProgressMonitor monitor) throws GitException;

    /**
     * Load all branches from git
     */
    Collection<Ref> branches() throws GitException;

    /**
     * Load all tags from git
     */
    Collection<Ref> tags() throws GitException;

    /**
     * Git latest commit from ref
     */
    GitCommit commit(String refName) throws GitException;
}
