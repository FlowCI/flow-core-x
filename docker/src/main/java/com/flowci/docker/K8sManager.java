package com.flowci.docker;

import com.flowci.docker.domain.*;
import com.flowci.util.StringHelper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.flowci.docker.domain.PodUnit.Phase.Failed;
import static com.flowci.docker.domain.PodUnit.Phase.Succeeded;

public class K8sManager implements DockerManager {

    public static Config parse(String content) throws IOException {
        return Config.fromKubeconfig(content);
    }

    private final static String LabelApp = "flow-ci-app";

    private final KubernetesClient client;

    private final K8sOption option;

    private final ContainerManager cm = new ContainerManagerImpl();

    private final ImageManager im = new ImageManagerImpl();

    public K8sManager(K8sOption option) throws IOException {
        this.option = option;

        if (option instanceof KubeConfigOption) {
            String content = ((KubeConfigOption) option).getKubeConfig();
            Config config = parse(content);
            client = new DefaultKubernetesClient(config);
            return;
        }

        throw new UnsupportedOperationException("Unsupported k8s option");
    }

    public void createNamespace() {
        Resource<Namespace, DoneableNamespace> resource = client.namespaces().withName(option.getNamespace());
        Namespace namespace = resource.get();
        if (Objects.isNull(namespace)) {
            Namespace built = new NamespaceBuilder()
                    .withMetadata(new ObjectMetaBuilder().withName(option.getNamespace()).build())
                    .build();
            resource.create(built);
        }
    }

    public void createEndpoint(K8sCreateEndpointOption createOption) throws Exception {
        EndpointPort epPort = new EndpointPortBuilder()
                .withProtocol(createOption.getProtocol())
                .withPort(createOption.getPort())
                .build();

        ServicePort svcPort = new ServicePortBuilder()
                .withProtocol(createOption.getProtocol())
                .withPort(createOption.getPort())
                .build();

        ObjectMeta metadata = new ObjectMetaBuilder().withName(createOption.getName()).build();
        EndpointAddress address = new EndpointAddressBuilder().withIp(createOption.getIp()).build();
        ServiceSpec spec = new ServiceSpecBuilder().withPorts(svcPort).build();

        Endpoints ep = new EndpointsBuilder()
                .withMetadata(metadata)
                .withSubsets(new EndpointSubsetBuilder()
                        .withAddresses(address)
                        .withPorts(epPort)
                        .build())
                .build();

        Service svc = new ServiceBuilder()
                .withMetadata(metadata)
                .withSpec(spec)
                .build();

        try {
            client.endpoints().inNamespace(option.getNamespace()).createOrReplace(ep);
            client.services().inNamespace(option.getNamespace()).createOrReplace(svc);
        } catch (KubernetesClientException e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public ContainerManager getContainerManager() {
        return cm;
    }

    @Override
    public ImageManager getImageManager() {
        return im;
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
                    .addToLabels(LabelApp, so.getLabel())
                    .endMetadata()
                    .build();

            pod = client.pods().inNamespace(option.getNamespace()).create(pod);
            return new PodUnit(pod).getId();
        }

        @Override
        public void wait(String podName, int timeoutInSeconds, Consumer<Output> onLog) throws Exception {
            podResource(podName).waitUntilReady(
                    timeoutInSeconds,
                    TimeUnit.SECONDS
            );

            LogWatch watch = podResource(podName).watchLog();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(watch.getOutput()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (onLog != null) {
                        onLog.accept(new Output(line));
                    }
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

    private class ImageManagerImpl implements ImageManager {

        @Override
        public void pull(String image, int timeoutInSeconds, Consumer<String> progress) throws Exception {
            // ignore
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

        return operation.withLabel(LabelApp).list().getItems();
    }

    private PodResource<Pod, DoneablePod> podResource(String name) {
        return client.pods()
                .inNamespace(option.getNamespace())
                .withName(name);
    }
}
