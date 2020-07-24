package com.flowci.docker.test;

import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSDKManager;
import com.flowci.docker.domain.DockerStartOption;
import com.github.dockerjava.api.model.Container;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DockerSDKManagerTest {

    private DockerManager manager;

    @Before
    public void init() {
        manager = new DockerSDKManager(DockerManager.DockerLocalHost);
    }

    @After
    public void clean() {
        manager = null;
    }

    @Test
    public void should_list_container() throws Exception {
        List<Container> containers = manager.getContainerManager().list(null, null);
        Assert.assertNotNull(containers);
    }

    @Test
    public void should_create_and_start_container() throws Exception {
        DockerStartOption option = new DockerStartOption();
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        ContainerManager cm = manager.getContainerManager();
        String cid = cm.start(option);
        Assert.assertNotNull(cid);

        cm.stop(cid);
        cm.delete(cid);
    }
}
