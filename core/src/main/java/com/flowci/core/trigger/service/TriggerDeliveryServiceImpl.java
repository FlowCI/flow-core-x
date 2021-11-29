package com.flowci.core.trigger.service;

import com.flowci.core.trigger.dao.TriggerDeliveryDao;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.domain.TriggerDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TriggerDeliveryServiceImpl implements TriggerDeliveryService {

    private final static int MaxRecentDelivery = 100;

    private final TriggerDeliveryDao triggerDeliveryDao;

    public TriggerDeliveryServiceImpl(TriggerDeliveryDao triggerDeliveryDao) {
        this.triggerDeliveryDao = triggerDeliveryDao;
    }

    @Override
    public void init(Trigger t) {
        var d = new TriggerDelivery();
        d.setTriggerId(t.getId());
        triggerDeliveryDao.save(d);
    }

    @Override
    public void add(Trigger t, TriggerDelivery.Item item) {
        triggerDeliveryDao.addDelivery(t.getId(), item, MaxRecentDelivery);
    }

    @Override
    public Page<TriggerDelivery.Item> list(Trigger t, int page, int size) {
        return triggerDeliveryDao.listDeliveries(t.getId(), PageRequest.of(page, size));
    }
}
