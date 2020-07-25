package com.flowci.docker;

import java.util.function.Consumer;

public interface ImageManager {

    /**
     * Sync call
     */
    void pull(String image, int timeoutInSeconds, Consumer<String> progress) throws Exception;
}
