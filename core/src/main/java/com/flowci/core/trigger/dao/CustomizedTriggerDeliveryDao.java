package com.flowci.core.trigger.dao;

import com.flowci.core.trigger.domain.TriggerDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface CustomizedTriggerDeliveryDao {

    void addDelivery(String triggerId, TriggerDelivery.Item item);

    Page<TriggerDelivery.Item> listDeliveries(String triggerId, PageRequest pageRequest);
}
