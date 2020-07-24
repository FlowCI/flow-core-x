package com.flowci.docker.test;

import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSSHManager;
import com.flowci.docker.domain.SSHOption;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.model.Container;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

public class DockerSSHManagerTest {

    private DockerManager manager;

    @Before
    public void init() throws Exception {
        InputStream pk = load("private.pk");
        SSHOption option = SSHOption.of(StringHelper.toString(pk), "192.168.0.106", "yang", 22);
        manager = new DockerSSHManager(option);
    }

    @Test
    public void should_list_containers() throws Exception {
        List<Container> list = manager.getContainerManager().list(null, null);
        Assert.assertNotNull(list);
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
