package com.flow.platform.util.zk;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ZkException {

    public static abstract class AbstractZkException extends RuntimeException {
        private Exception raw;

        public AbstractZkException(Exception raw, String message) {
            super(message);
            this.raw = raw;
        }

        public Exception getRaw() {
            return raw;
        }
    }

    public static class ZkServerConnectionException extends AbstractZkException {
        public ZkServerConnectionException(Exception raw) {
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
