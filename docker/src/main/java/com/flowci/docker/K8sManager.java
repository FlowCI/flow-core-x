package com.flowci.docker;

import com.flowci.common.helper.StringHelper;
import com.flowci.docker.domain.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class K8sManager implements DockerManager {

    public static boolean validate(String content) {
        try {
            KubeConfigUtils.parseConfigFromString(content);
            return true;
        } catch (IOException e) {
            return false;
        }
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
            client = new DefaultKubernetesClient(io.fabric8.kubernetes.client.Config.fromKubeconfig(content));
            return;
        }

        throw new UnsupportedOperationException("Unsupported k8s option");
    }

    public boolean hasNamespace() {
        Namespace namespace = client.namespaces().withName(option.getNamespace()).get();
        return namespace != null;
    }

    public void createNamespace() {
        Resource<Namespace> resource = client.namespaces().withName(option.getNamespace());
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

    /**
     * Create ClusterRoleBinding to set service account user
     * "system:serviceaccount:(NAMESPACE):(SERVICEACCOUNT)" to role
     */
    public void createClusterRoleBindingToAdmin(RoleBindingOption option) throws Exception {
        String namespace = this.option.getNamespace();

        RoleBinding roleBinding = client.rbac()
                .roleBindings()
                .inNamespace(namespace)
                .withName(option.getName())
                .get();

        if (roleBinding != null) {
            return;
        }

        RoleBinding build = new RoleBindingBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(option.getName())
                                .build()
                )
                .withSubjects(
                        new SubjectBuilder()
                                .withKind("ServiceAccount")
                                .withName(option.getAccountName())
                                .withNamespace(namespace)
                                .build()
                )
                .withRoleRef(
                        new RoleRefBuilder()
                                .withKind("ClusterRole")
                                .withName("cluster-admin")
                                .withApiGroup("rbac.authorization.k8s.io")
                                .build()
                )
                .build();

        try {
            client.rbac().roleBindings().inNamespace(namespace).create(build);
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

        /**
         * List all pods
         *
         * @param statusFilter pod phase
         * @param nameFilter   pod name, or if end with wildcard, then find from label.
         *                     ex: ci-agent-* will fetch name from label
         */
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
            Pod pod = so.buildPod(LabelApp, so.getLabel()).build();

            pod = client.pods().inNamespace(option.getNamespace()).create(pod);
            return new PodUnit(pod).getId();
        }

        @Override
        public void wait(String podName, int timeoutInSeconds, Consumer<Output> onLog) throws Exception {
            podResource(podName).waitUntilCondition(
                    PodUnit.Phase::isPending,
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
                    PodUnit.Phase::isFinish,
                    timeoutInSeconds,
                    TimeUnit.SECONDS
            );
        }

        @Override
        public void stop(String podName) throws Exception {
            throw new UnsupportedOperationException("Un-support stop pod on k8s");
        }

        @Override
        public void resume(String podName) throws Exception {
            throw new UnsupportedOperationException("Un-support resume pod on k8s");
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
        NonNamespaceOperation<Pod, PodList, PodResource<Pod>> operation = client.pods().inNamespace(option.getNamespace());

        if (StringHelper.hasValue(name)) {
            if (name.endsWith("*")) {
                operation.withLabel(LabelApp, name.replace("*", ""));
            } else {
                operation.withField("metadata.name", name);
            }
        }

        if (StringHelper.hasValue(status)) {
            operation.withField("status.phase", status);
        }

        return operation.list().getItems();
    }

    private PodResource<Pod> podResource(String name) {
        return client.pods()
                .inNamespace(option.getNamespace())
                .withName(name);
    }
}
