package com.flowci.docker.test;

import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSDKManager;
import com.flowci.docker.domain.DockerStartOption;
import com.github.dockerjava.api.exception.NotFoundException;
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
    public void should_pull_image() throws Exception {
        manager.getImageManager().pull("hello-world", 60, (item) -> {
            System.out.println(item.getStatus());
        });
    }

    @Test
    public void should_list_containers() throws Exception {
        List<Container> containers = manager.getContainerManager().list(null, null);
        Assert.assertNotNull(containers);
    }

    @Test
    public void should_create_start_and_delete_container() throws Exception {
        DockerStartOption option = new DockerStartOption();
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        option.addEntryPoint("/bin/bash");
        option.addEntryPoint("-c");
        option.addEntryPoint("echo helloworld\nsleep 10\necho end\necho helloworld");

        ContainerManager cm = manager.getContainerManager();
        String cid = cm.start(option);
        Assert.assertNotNull(cid);

        cm.wait(cid, 60, (frame -> {
            System.out.print(new String(frame.getPayload()));
        }));

        cm.stop(cid);
        cm.delete(cid);
    }

    @Test(expected = NotFoundException.class)
    public void should_throw_exception_when_resume_cid_not_exist() throws Exception {
        manager.getContainerManager().resume("1231231");
    }

    @Test(expected = NotFoundException.class)
    public void should_throw_exception_when_stop_cid_not_exist() throws Exception {
        manager.getContainerManager().stop("1231231");
    }

    @Test(expected = NotFoundException.class)
    public void should_throw_exception_when_delete_cid_not_exist() throws Exception {
        manager.getContainerManager().delete("1231231");
    }
}
