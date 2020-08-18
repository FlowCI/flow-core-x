package com.flowci.docker;

import com.flowci.docker.domain.Inspected;
import com.flowci.docker.domain.StartOption;
import com.flowci.docker.domain.Unit;
import com.github.dockerjava.api.model.Frame;

import java.util.List;
import java.util.function.Consumer;

public interface ContainerManager {

    List<Unit> list(String statusFilter, String nameFilter) throws Exception;

    Inspected inspect(String id) throws Exception;

    String start(StartOption option) throws Exception;

    void wait(String id, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception;

    void stop(String id) throws Exception;

    void resume(String id) throws Exception;

    void delete(String id) throws Exception;
}
