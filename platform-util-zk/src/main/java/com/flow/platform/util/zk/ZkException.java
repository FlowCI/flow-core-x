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

package com.flow.platform.util.zk;

/**
 * @author gy@fir.im
 */
public class ZkException {

    public static class WatchingException extends AbstractZkException {

        public WatchingException(Exception raw, String path) {
            super(raw, "Cannot watch path: " + path);
        }
    }

    public static class ConnectionException extends AbstractZkException {

        public ConnectionException(Exception raw) {
            super(raw, "Zookeeper server connection error");
        }
    }

    public static class NotExitException extends AbstractZkException {

        public NotExitException(Exception raw, String path) {
            super(raw, String.format("Node not exist: %s", path));
        }
    }

    public static class BadVersion extends AbstractZkException {

        public BadVersion(Exception raw) {
            super(raw, "Bad data version");
        }
    }

}
