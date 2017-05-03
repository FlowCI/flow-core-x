package com.flow.platform.client;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class ZkException {

    public static class ZkServerConnectionException extends RuntimeException {

        private Exception raw;

        public ZkServerConnectionException(Exception raw) {
            super("Zookeeper server connection error");
            this.raw = raw;
        }

        public Exception getRaw() {
            return raw;
        }
    }

}
