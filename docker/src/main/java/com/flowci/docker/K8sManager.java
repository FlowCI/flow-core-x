package com.flowci.docker;

import com.flowci.docker.domain.*;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class K8sManager implements DockerManager {

    private final static String LabelType = "flow-ci-type";
    private final static String LabelTypeValueAgent = "agent";

    private final static String LabelName = "flow-ci-name";

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
            List<Unit> list = new ArrayList<>(podList.getItems().size());
            for (V1Pod pod : podList.getItems()) {
                list.add(new PodUnit(pod));
            }
            return list;
        }

        @Override
        public InspectContainerResponse inspect(String podName) throws Exception {
            return null;
        }

        @Override
        public String start(DockerStartOption option) throws Exception {
            return null;
        }

        @Override
        public void wait(String podName, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception {

        }

        @Override
        public void stop(String podName) throws Exception {

        }

        @Override
        public void resume(String podName) throws Exception {

        }

        @Override
        public void delete(String podName) throws Exception {

        }
    }
}
