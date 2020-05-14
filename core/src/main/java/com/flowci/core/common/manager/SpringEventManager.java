package com.flowci.core.common.manager;

import org.springframework.context.ApplicationEvent;

public interface SpringEventManager {

    <T extends ApplicationEvent > T publish(T event);
}
