package com.flowci.docker;

import com.flowci.docker.domain.*;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Frame;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class K8sManager implements DockerManager {

    private final static String LabelType = "flow-ci-type";
    private final static String LabelTypeValueAgent = "agent";

    private final static String LabelName = "flow-ci-name";

    private KubernetesClient client;

    private final K8sOption option;

    private final ContainerManager cm = new ContainerManagerImpl();

    public K8sManager(K8sOption option) throws IOException {
        this.option = option;

        if (option instanceof KubeConfigOption) {
            String content = ((KubeConfigOption) option).getKubeConfig();
            Config config = Config.fromKubeconfig(content);
            client = new DefaultKubernetesClient(config);
            return;
        }

        throw new UnsupportedOperationException("Unsupported k8s option");
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
            List<Pod> pods = client.pods().inNamespace(option.getNamespace()).list().getItems();
            List<Unit> list = new ArrayList<>(pods.size());
            for (Pod pod : pods) {
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
