package com.flowci.docker.test;

import com.flowci.docker.ContainerManager;
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
    public void should_create_and_inspect_pod() throws Exception {
        PodStartOption option = new PodStartOption();
        option.setName(podName);
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        option.setCommand("/bin/bash");
        option.addArg("-c");
        option.addArg("echo helloworld\nsleep 5\necho end\necho helloworld");

        ContainerManager cm = manager.getContainerManager();

        String pod = cm.start(option);
        Assert.assertNotNull(pod);

        cm.wait(pod, 1500, null);

        Unit inspect = cm.inspect(pod);
        Assert.assertNotNull(inspect);
        Assert.assertFalse(inspect.isRunning());
        Assert.assertEquals(PodUnit.Phase.Succeeded, inspect.getStatus());

        Assert.assertNotNull(inspect.getExitCode());
        Assert.assertEquals(0, inspect.getExitCode().intValue());
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
