package com.flowci.core.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.zookeeper.client.ConnectStringParser;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "agent_host")
public class K8sAgentHost extends AgentHost {

    @Getter
    @AllArgsConstructor
    public static class Hosts {

        private final String serverUrl;

        private final String rabbitUrl;

        private final String zkUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class Endpoint {

        private final String name;

        private final String ip;

        private final int port;
    }

    private final static String ServerEp = "flow-ci-server";

    private final static String RabbitEp = "flow-ci-rabbit";

    private final static String ZkEp = "flow-ci-zk-%d";

    @NonNull
    private String namespace;

    @NonNull
    private String url; // k8s master url

    @NonNull
    private String secret; // secret for config file

    public K8sAgentHost() {
        setType(Type.K8s);
    }

    /**
     * Build endpoints list by urls
     *
     * @param serverUrl http|https://192.168.0.103:8080
     * @param rabbitUrl amqp://guest:guest@192.168.0.104:5672
     * @param zkUrl     192.168.0.104:2181
     */
    public static List<Endpoint> buildEndpoints(String serverUrl, String rabbitUrl, String zkUrl) {
        URI server = URI.create(serverUrl);
        URI rabbit = URI.create(rabbitUrl);
        ConnectStringParser zk = new ConnectStringParser(zkUrl);

        List<Endpoint> list = new ArrayList<>(3);
        list.add(new Endpoint(ServerEp, server.getHost(), server.getPort()));
        list.add(new Endpoint(RabbitEp, rabbit.getHost(), rabbit.getPort()));

        int i = 0;
        for (InetSocketAddress address : zk.getServerAddresses()) {
            String name = String.format(ZkEp, i++);
            list.add(new Endpoint(name, address.getHostName(), address.getPort()));
        }

        return list;
    }

    public static Hosts buildHosts(String serverUrl, String rabbitUrl, String zkUrl) {
        String server = UriComponentsBuilder.fromHttpUrl(serverUrl).host(ServerEp).toUriString();
        String rabbit = UriComponentsBuilder.fromUriString(rabbitUrl).host(RabbitEp).toUriString();

        ConnectStringParser zk = new ConnectStringParser(zkUrl);
        StringBuilder zkBuilder = new StringBuilder(zkUrl.length());

        int i = 0;
        for (InetSocketAddress address : zk.getServerAddresses()) {
            String name = String.format(ZkEp, i++);
            zkBuilder.append(String.format("%s:%d,", name, address.getPort()));
        }

        if (zkBuilder.length() > 0) {
            zkBuilder.deleteCharAt(zkBuilder.length() - 1);
        }

        return new Hosts(server, rabbit, zkBuilder.toString());
    }
}
