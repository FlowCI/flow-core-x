package com.flowci.core.test.trigger;

import com.flowci.core.test.SpringScenario;
import com.flowci.core.trigger.dao.TriggerDeliveryDao;
import com.flowci.core.trigger.domain.TriggerDelivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TriggerDeliveryDaoTest extends SpringScenario {

    @Autowired
    private TriggerDeliveryDao triggerDeliveryDao;

    private TriggerDelivery delivery;

    @BeforeEach
    void init() {
        delivery = new TriggerDelivery();
        delivery.setTriggerId("1123");
        triggerDeliveryDao.save(delivery);
    }

    @Test
    public void should_add_delivery_and_load_items() {
        createItems(10);

        var optional = triggerDeliveryDao.findById(delivery.getId());
        assertFalse(optional.isEmpty());

        var loaded = optional.get();
        assertEquals(10, loaded.getDeliveries().size());

        Page<TriggerDelivery.Item> items1 = triggerDeliveryDao.listDeliveries(delivery.getTriggerId(), PageRequest.of(0, 2));
        assertEquals("9", items1.getContent().get(0).getDesc());
        assertEquals("8", items1.getContent().get(1).getDesc());

        Page<TriggerDelivery.Item> items2 = triggerDeliveryDao.listDeliveries(delivery.getTriggerId(), PageRequest.of(4, 2));
        assertEquals("1", items2.getContent().get(0).getDesc());
        assertEquals("0", items2.getContent().get(1).getDesc());
    }

    private void createItems(int total) {
        for (int i = 0; i < total; i++) {
            var item = new TriggerDelivery.Item();
            item.setDesc(String.valueOf(i));
            item.setTimestamp(new Date());
            item.setStatus(TriggerDelivery.Item.Status.Success);
            triggerDeliveryDao.addDelivery(delivery.getTriggerId(), item, 10);
        }
    }
}
