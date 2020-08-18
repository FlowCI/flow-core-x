package com.flowci.docker;

import com.flowci.docker.domain.*;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Frame;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class K8sManager implements DockerManager {

    private final static String LabelType = "flow-ci-type";
    private final static String LabelTypeValueAgent = "agent";

    private final static String LabelName = "flow-ci-name";

    public abstract static class Status {

        public static final String Running = "Running";

        public static final String Terminating = "Terminating";

        public static final String RunError = "RunContainerError";
    }

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
            List<Pod> pods = listPods(nameFilter, statusFilter);
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
        public String start(StartOption startOption) throws Exception {
            if (!(startOption instanceof PodStartOption)) {
                throw new IllegalArgumentException();
            }

            PodStartOption so = (PodStartOption) startOption;

            Container c = new ContainerBuilder()
                    .withImage(so.getImage())
                    .withImagePullPolicy("Always")
                    .withName(so.getName())
                    .withEnv(so.toK8sVarList())
                    .withCommand(so.getCommand())
                    .withArgs(so.getArgs())
                    .build();

            Pod pod = new PodBuilder()
                    .withNewSpec()
                    .withContainers(c)
                    .endSpec()
                    .withNewMetadata()
                    .withName(so.getName())
                    .addToLabels(LabelType, LabelTypeValueAgent)
                    .endMetadata()
                    .build();

            pod = client.pods().inNamespace(option.getNamespace()).create(pod);
            return new PodUnit(pod).getId();
        }

        @Override
        public void wait(String podName, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception {

        }

        @Override
        public void stop(String podName) throws Exception {
            // ignore: stop pod not supported
        }

        @Override
        public void resume(String podName) throws Exception {
            // ignore: resume pod not supported
        }

        @Override
        public void delete(String podName) throws Exception {
            List<Pod> pods = listPods(podName, null);
            if (pods.isEmpty()) {
                return;
            }

            Pod pod = pods.get(0);
            if (pod.getStatus().getPhase().equals(Status.Terminating)) {
                return;
            }

            Boolean delete = client.pods().inNamespace(option.getNamespace()).delete(pod);
            if (!delete) {
                throw new Exception(String.format("Pod %s not deleted", podName));
            }
        }
    }

    private List<Pod> listPods(String name, String status) {
        NonNamespaceOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> operation = client.pods()
                .inNamespace(option.getNamespace());

        if (StringHelper.hasValue(name)) {
            operation.withField("metadata.name", name);
        }

        if (StringHelper.hasValue(status)) {
            operation.withField("status.phase", status);
        }

        return operation.list().getItems();
    }
}
