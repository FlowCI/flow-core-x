package com.flowci.docker.test;

import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSDKManager;
import com.flowci.docker.domain.ContainerStartOption;
import com.flowci.docker.domain.Unit;
import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
public class DockerSDKManagerTest {

    private DockerManager manager;

    @BeforeEach
    void init() {
        manager = new DockerSDKManager(DockerManager.DockerLocalHost);
    }

    @AfterEach
    void clean() {
        manager = null;
    }

    @Test
    void should_pull_image() throws Exception {
        manager.getImageManager().pull("ubuntu:18.04", 60, System.out::println);
    }

    @Test
    void should_throw_exception_if_image_not_found() {
        assertThrows(Exception.class, () -> {
            manager.getImageManager().pull("ubuntu:notfound", 120, null);
        });
    }

    @Test
    void should_list_containers() throws Exception {
        List<Unit> containers = manager.getContainerManager().list(null, null);
        assertNotNull(containers);
    }

    @Test
    void should_create_start_and_delete_container() throws Exception {
        ContainerStartOption option = new ContainerStartOption();
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        option.addEntryPoint("/bin/bash");
        option.addEntryPoint("-c");
        option.addEntryPoint("echo helloworld\nsleep 10\necho end\necho helloworld");

        ContainerManager cm = manager.getContainerManager();
        String cid = cm.start(option);
        assertNotNull(cid);

        cm.wait(cid, 60, (frame -> {
            System.out.print(new String(frame.getData()));
        }));

        cm.stop(cid);
        cm.delete(cid);
    }

    @Test
    void should_throw_exception_when_resume_cid_not_exist() {
        assertThrows(NotFoundException.class, () -> {
            manager.getContainerManager().resume("1231231");
        });
    }

    @Test
    void should_throw_exception_when_stop_cid_not_exist() {
        assertThrows(NotFoundException.class, () -> {
            manager.getContainerManager().stop("1231231");
        });
    }

    @Test
    void should_throw_exception_when_delete_cid_not_exist() {
        assertThrows(NotFoundException.class, () -> {
            manager.getContainerManager().delete("1231231");
        });
    }
}
