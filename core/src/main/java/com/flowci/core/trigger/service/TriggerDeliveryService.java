package com.flowci.core.trigger.service;

import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.domain.TriggerDelivery;
import org.springframework.data.domain.Page;

public interface TriggerDeliveryService {

    void init(Trigger t);

    void add(Trigger t, TriggerDelivery.Item item);

    Page<TriggerDelivery.Item> list(Trigger t, int page, int size);
}
