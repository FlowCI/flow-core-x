package com.flowci.core.trigger.service;

import com.flowci.core.trigger.domain.EmailTrigger;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.domain.WebhookTrigger;
import com.flowci.common.domain.Vars;

import java.util.List;

public interface TriggerService {

    List<Trigger> list();

    Trigger get(String id);

    Trigger getByName(String name);

    Trigger save(EmailTrigger e);

    Trigger save(WebhookTrigger w);

    void send(Trigger n, Vars<String> context);

    Trigger delete(String name);
}
