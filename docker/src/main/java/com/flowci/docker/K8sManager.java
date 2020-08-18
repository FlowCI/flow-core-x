package com.flowci.docker;

import com.flowci.docker.domain.*;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.model.Frame;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.flowci.docker.domain.PodUnit.Phase.Failed;
import static com.flowci.docker.domain.PodUnit.Phase.Succeeded;

public class K8sManager implements DockerManager {

    private final static String LabelType = "flow-ci-type";
    private final static String LabelTypeValueAgent = "agent";

    private final static String LabelName = "flow-ci-name";

    private final KubernetesClient client;

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
        public Unit inspect(String podName) throws Exception {
            return new PodUnit(podResource(podName).get());
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
                    .withRestartPolicy("Never")
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
            podResource(podName).waitUntilReady(
                    timeoutInSeconds,
                    TimeUnit.SECONDS
            );

            LogWatch watch = podResource(podName).watchLog();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(watch.getOutput()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            podResource(podName).waitUntilCondition(
                    pod -> {
                        String phase = pod.getStatus().getPhase();
                        return phase.equals(Succeeded) || phase.equals(Failed);
                    },
                    timeoutInSeconds,
                    TimeUnit.SECONDS
            );
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
            Boolean delete = podResource(podName)
                    .withGracePeriod(0)
                    .delete();

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

    private PodResource<Pod, DoneablePod> podResource(String name) {
        return client.pods()
                .inNamespace(option.getNamespace())
                .withName(name);
    }
}
