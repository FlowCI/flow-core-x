package com.flow.platform.util.zk;

/**
 * Created by gy@fir.im on 03/05/2017.
 * Copyright fir.im
 */
public class ZkException {

    public static class ZkWatchingException extends AbstractZkException {
        public ZkWatchingException(Exception raw, String path) {
            super(raw, "Cannot watch path: " + path);
        }
    }

    public static class ConnectionException extends AbstractZkException {
        public ConnectionException(Exception raw) {
            super(raw, "Zookeeper server connection error");
        }
    }

    public static class ZkNoNodeException extends AbstractZkException {
        public ZkNoNodeException(Exception raw, String path) {
            super(raw, String.format("Node not exist: %s", path));
        }
    }

    public static class ZkBadVersion extends AbstractZkException {
        public ZkBadVersion(Exception raw) {
            super(raw, "Bad data version");
        }
    }

}
