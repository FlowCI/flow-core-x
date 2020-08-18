package com.flowci.docker.domain;

import io.fabric8.kubernetes.api.model.ContainerStateRunning;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.List;
import java.util.Optional;

/**
 * Single container pod unit
 */
public class PodUnit implements Unit {

    public abstract static class Phase {

        public static final String Pending = "Pending";

        public static final String Running = "Running";

        public static final String Terminating = "Terminating";

        public static final String Succeeded = "Succeeded";

        public static final String Failed = "Failed";

        public static final String RunError = "RunContainerError";
    }

    private final Pod pod;

    public PodUnit(Pod pod) {
        this.pod = pod;
    }

    @Override
    public String getId() {
        return pod.getMetadata().getName();
    }

    @Override
    public String getName() {
        return pod.getMetadata().getName();
    }

    @Override
    public String getStatus() {
        return pod.getStatus().getPhase();
    }

    @Override
    public Long getExitCode() {
        Optional<ContainerStateTerminated> optional = getTerminated();
        if (optional.isPresent()) {
            return optional.get().getExitCode().longValue();
        }
        return null;
    }

    @Override
    public Boolean isRunning() {
        return getRunning().isPresent();
    }

    private Optional<ContainerStateRunning> getRunning() {
        List<ContainerStatus> containers = pod.getStatus().getContainerStatuses();
        if (containers.isEmpty()) {
            return Optional.empty();
        }

        ContainerStatus container = containers.get(0);
        ContainerStateRunning running = container.getState().getRunning();
        return Optional.ofNullable(running);
    }


    private Optional<ContainerStateTerminated> getTerminated() {
        List<ContainerStatus> containers = pod.getStatus().getContainerStatuses();
        if (containers.isEmpty()) {
            return Optional.empty();
        }

        ContainerStatus container = containers.get(0);
        ContainerStateTerminated terminated = container.getState().getTerminated();
        return Optional.ofNullable(terminated);
    }
}
