/*
 * Copyright 2020 flow.ci
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

package com.flowci.pool.domain;

public abstract class DockerStatus {

    public static final String None = "none";

    public static final String Created = "created";

    public static final String Restarting = "restarting";

    public static final String Running = "running";

    public static final String Removing = "removing";

    public static final String Paused = "paused";

    public static final String Exited = "exited";

    public static final String Dead = "dead";

    /**
     * Convert human readable string to single machine string
     * Ex: Up %s (Paused) -> paused
     * Ref: https://github.com/moby/moby/blob/b44b5bbc8ba48f50343602a21e7d44c017c1e23d/container/state.go#L41
     */
    public static String toStateString(String str) {
        if (str.endsWith("(Paused)")) {
            return Paused;
        }

        if (str.startsWith("Restarting")) {
            return Restarting;
        }

        if (str.startsWith("Up")) {
            return Running;
        }

        if (str.startsWith("Dead")) {
            return Dead;
        }

        if (str.startsWith("Created")) {
            return Created;
        }

        if (str.startsWith("Removal In Progress")) {
            return Removing;
        }

        return Exited;
    }
}