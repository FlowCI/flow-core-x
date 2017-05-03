package com.flow.platform.client;

import java.io.IOException;

/**
 * Created by gy@fir.im on 03/05/2017.
 *
 * @copyright fir.im
 */
public class Client {

    private static final String ZK_HOME = "54.222.129.38:2181";
    private static final int ZK_TIMEOUT = 2000;

    private static final String NODE_ZONE = "ali";
    private static final String NODE_MACHINE = "test-001";

    public static void main(String args[]) throws IOException {
        System.out.println("========= Run client =========");

        ClientNode client = new ClientNode(ZK_HOME, ZK_TIMEOUT, NODE_ZONE, NODE_MACHINE);
        new Thread(client).run();

        System.out.println("========= Client end =========");
    }
}
