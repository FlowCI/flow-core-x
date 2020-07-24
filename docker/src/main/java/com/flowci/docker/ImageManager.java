package com.flowci.docker;

import com.github.dockerjava.api.model.PullResponseItem;

import java.util.function.Consumer;

public interface ImageManager {

    /**
     * Sync call
     */
    void pull(String image, int timeoutInSeconds, Consumer<PullResponseItem> progress) throws Exception;
}
