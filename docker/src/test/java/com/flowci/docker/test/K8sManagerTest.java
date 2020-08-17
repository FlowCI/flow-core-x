package com.flowci.docker.test;

import com.flowci.docker.DockerManager;
import com.flowci.docker.K8sManager;
import com.flowci.docker.domain.K8sOption;
import com.flowci.docker.domain.KubeConfigOption;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

public class K8sManagerTest {

    private DockerManager manager;

    @Before
    public void init() throws Exception {
        InputStream kubeconfig = load("kubeconfig");
        K8sOption option = new KubeConfigOption("test", new InputStreamReader(kubeconfig));
        manager = new K8sManager(option);
    }

    @Test
    public void should_namespace_created() throws Exception {
        manager.getContainerManager().list(null, null);
    }

    protected InputStream load(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }
}
