package com.flowci.docker.test;

import com.flowci.docker.DockerManager;
import com.flowci.docker.K8sManager;
import com.flowci.docker.domain.*;
import com.flowci.util.StringHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class K8sManagerTest {

    private final String podName = "my-test-pod";

    private DockerManager manager;

    @Before
    public void init() throws Exception {
        InputStream is = load("kubeconfig");
        K8sOption option = new KubeConfigOption("test", StringHelper.toString(is));
        manager = new K8sManager(option);
    }

    @After
    public void clean() throws Exception {
        manager.getContainerManager().delete(podName);
    }

    @Test
    public void should_list_pod() throws Exception {
        List<Unit> list = manager.getContainerManager().list(null, "nginx-f89759699-67rcm");
        Assert.assertNotNull(list);
    }

    @Test
    public void should_create_pod() throws Exception {
        PodStartOption option = new PodStartOption();
        option.setName(podName);
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        option.setCommand("/bin/bash");
        option.addArg("-c");
        option.addArg("\"echo helloworld\nsleep 10\necho end\necho helloworld\"");

        String pod = manager.getContainerManager().start(option);
        Assert.assertNotNull(pod);
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
