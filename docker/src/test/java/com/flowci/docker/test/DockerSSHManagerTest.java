package com.flowci.docker.test;

import com.flowci.common.helper.StringHelper;
import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSSHManager;
import com.flowci.docker.domain.ContainerStartOption;
import com.flowci.docker.domain.SSHOption;
import com.flowci.docker.domain.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled
public class DockerSSHManagerTest {

    private DockerManager manager;

    @BeforeEach
    void init() throws Exception {
        InputStream pk = load("private.pk");
        SSHOption option = SSHOption.of(StringHelper.toString(pk), "192.168.0.106", "yang", 22);
        manager = new DockerSSHManager(option);
    }

    @Test
    void should_list_containers() throws Exception {
        List<Unit> list = manager.getContainerManager().list(null, null);
        assertNotNull(list);
    }

    @Test
    void should_pull_image_and_print_log() throws Exception {
        manager.getImageManager().pull("ubuntu:18.04", 60, System.out::println);
    }

    @Test
    void should_throw_exception_if_image_not_found() {
        assertThrows(Exception.class, () -> {
            manager.getImageManager().pull("ubuntu:notfound", 10, null);
        });
    }

    @Test
    void should_create_start_and_delete_container() throws Exception {
        ContainerStartOption option = new ContainerStartOption();
        option.setImage("ubuntu:18.04");
        option.addEnv("FLOW_TEST", "hello.world");

        option.addEntryPoint("/bin/bash");
        option.addEntryPoint("-c");
        option.addEntryPoint("\"echo helloworld\nsleep 10\necho end\necho helloworld\"");

        ContainerManager cm = manager.getContainerManager();
        String cid = cm.start(option);
        assertNotNull(cid);

        cm.wait(cid, 60, (frame -> {
            System.out.println(new String(frame.getData()));
        }));

        cm.stop(cid);
        cm.delete(cid);
    }

    @Test
    void should_inspect_container() throws Exception {
        Unit inspect = manager.getContainerManager().inspect("a86fc2720b11");
        assertNotNull(inspect);
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
