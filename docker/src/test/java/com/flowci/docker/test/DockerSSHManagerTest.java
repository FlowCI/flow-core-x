package com.flowci.docker.test;

import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSSHManager;
import com.flowci.docker.domain.DockerStartOption;
import com.flowci.docker.domain.SSHOption;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

@Ignore
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

    @Test
    public void should_pull_image_and_print_log() throws Exception {
        manager.getImageManager().pull("ubuntu:18.04", 60, System.out::println);
    }

    @Test(expected = Exception.class)
    public void should_throw_exception_if_image_not_found() throws Exception {
        manager.getImageManager().pull("ubuntu:notfound", 10, null);
    }

    @Test
    public void should_create_start_and_delete_container() throws Exception {
        DockerStartOption option = new DockerStartOption();
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        option.addEntryPoint("/bin/bash");
        option.addEntryPoint("-c");
        option.addEntryPoint("\"echo helloworld\nsleep 10\necho end\necho helloworld\"");

        ContainerManager cm = manager.getContainerManager();
        String cid = cm.start(option);
        Assert.assertNotNull(cid);

        cm.wait(cid, 60, (frame -> {
            System.out.println(new String(frame.getPayload()));
        }));

        cm.stop(cid);
        cm.delete(cid);
    }

    @Test
    public void should_inspect_container() throws Exception {
        InspectContainerResponse inspect = manager.getContainerManager().inspect("a86fc2720b11");
        Assert.assertNotNull(inspect);
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
