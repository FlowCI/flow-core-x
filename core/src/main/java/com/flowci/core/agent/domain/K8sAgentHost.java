package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.zookeeper.client.ConnectStringParser;
import org.springframework.data.mongodb.core.mapping.Document;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "agent_host")
public class K8sAgentHost extends AgentHost {

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Endpoint {

        private String name;

        private String ip;

        private int port;
    }

    @NonNull
    private String secret; // secret for config file

    private String namespace;

    private List<Endpoint> endpoints;

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
        list.add(new Endpoint().setName("flow-ci-server").setIp(server.getHost()).setPort(server.getPort()));
        list.add(new Endpoint().setName("flow-ci-rabbit").setIp(rabbit.getHost()).setPort(rabbit.getPort()));

        int i = 0;
        for (InetSocketAddress address : zk.getServerAddresses()) {
            String name = String.format("flow-ci-zk-%d", i++);
            list.add(new Endpoint().setName(name).setIp(address.getHostName()).setPort(address.getPort()));
        }

        return list;
    }
}
