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
public class ZkException extends RuntimeException {

    public ZkException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class WatchingException extends ZkException {

        public WatchingException(String path, Throwable raw) {
            super("Cannot watch path: " + path, raw);
        }
    }

    public static class ConnectionException extends ZkException {

        public ConnectionException(Throwable raw) {
            super("Zookeeper server connection error", raw);
        }
    }

    public static class NotExitException extends ZkException {

        public NotExitException(String path, Throwable raw) {
            super(String.format("Node not exist: %s", path), raw);
        }
    }

    public static class BadVersion extends ZkException {

        public BadVersion(Throwable raw) {
            super("Bad data version", raw);
        }
    }
}
