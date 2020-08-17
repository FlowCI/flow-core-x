package com.flowci.docker;

import com.flowci.docker.domain.*;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class K8sManager implements DockerManager {

    private final static String Label = "flow-ci";

    private final static String LabelValueAgent = "agent";

    private CoreV1Api api;

    private final K8sOption option;

    private final ContainerManager cm = new ContainerManagerImpl();

    public K8sManager(K8sOption option) throws IOException {
        this.option = option;

        if (option instanceof KubeConfigOption) {
            this.init((KubeConfigOption) option);
            return;
        }

        throw new UnsupportedOperationException("Unsupported k8s option");
    }

    private void init(KubeConfigOption option) throws IOException {
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(option.getKubeConfig())).build();
        api = new CoreV1Api(client);
    }

    @Override
    public ContainerManager getContainerManager() {
        return cm;
    }

    @Override
    public ImageManager getImageManager() {
        return null;
    }

    @Override
    public void close() {

    }

    private class ContainerManagerImpl implements ContainerManager {

        @Override
        public List<Unit> list(String statusFilter, String nameFilter) throws Exception {
            V1PodList podList = api.listNamespacedPod(option.getNamespace(), null, null, null, null, null, null, null, null, null);
            return null;
        }

        @Override
        public InspectContainerResponse inspect(String containerId) throws Exception {
            return null;
        }

        @Override
        public String start(DockerStartOption option) throws Exception {
            return null;
        }

        @Override
        public void wait(String containerId, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception {

        }

        @Override
        public void stop(String containerId) throws Exception {

        }

        @Override
        public void resume(String containerId) throws Exception {

        }

        @Override
        public void delete(String containerId) throws Exception {

        }
    }
}
