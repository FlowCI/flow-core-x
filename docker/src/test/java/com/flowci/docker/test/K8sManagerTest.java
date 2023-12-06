package com.flowci.docker.test;

import com.flowci.common.helper.StringHelper;
import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.K8sManager;
import com.flowci.docker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class K8sManagerTest {

    private final String podName = "my-test-pod";

    private DockerManager manager;

    @BeforeEach
    void init() throws Exception {
        InputStream is = load("kubeconfig");
        K8sOption option = new KubeConfigOption("test", StringHelper.toString(is));
        manager = new K8sManager(option);
    }

    @Test
    void should_list_pod() throws Exception {
        List<Unit> list = manager.getContainerManager().list(null, "nginx-f89759699-67rcm");
        assertNotNull(list);
    }

    @Test
    void should_create_and_inspect_pod() throws Exception {
        PodStartOption option = new PodStartOption();
        option.setName(podName);
        option.setImage("flowci/agent");
        option.setLabel("ci-agent");

        option.addEnv("FLOWCI_SERVER_URL", "http://192.168.0.104:8080");
        option.addEnv("FLOWCI_AGENT_TOKEN", "315d734d-65f4-45de-a0c4-f6fd40d14369");
        option.addEnv("FLOWCI_AGENT_WORKSPACE", "/ws");

        ContainerManager cm = manager.getContainerManager();

        String pod = cm.start(option);
        assertNotNull(pod);

        // test wildcard
        List<Unit> list = cm.list(null, "ci-agent*");
        assertEquals(1, list.size());

        cm.wait(pod, 1500, output -> System.out.println(new String(output.getData())));

        Unit inspect = cm.inspect(pod);
        assertNotNull(inspect);
        assertFalse(inspect.isRunning());
        assertEquals(PodUnit.Phase.Succeeded, inspect.getStatus());

        assertNotNull(inspect.getExitCode());
        assertEquals(0, inspect.getExitCode().intValue());

        manager.getContainerManager().delete(podName);
    }

    @Test
    void should_create_endpoint() throws Exception {
        K8sManager m = (K8sManager) manager;
        m.createNamespace();
        m.createEndpoint(new K8sCreateEndpointOption("ci-server", "192.168.0.104", 8080));
        m.createEndpoint(new K8sCreateEndpointOption("ci-zk", "192.168.0.104", 2181));
        m.createEndpoint(new K8sCreateEndpointOption("ci-rabbit", "192.168.0.104", 5672));
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
