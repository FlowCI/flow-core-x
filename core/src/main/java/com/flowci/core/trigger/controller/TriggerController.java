package com.flowci.core.trigger.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.trigger.domain.*;
import com.flowci.core.trigger.service.TriggerDeliveryService;
import com.flowci.core.trigger.service.TriggerService;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/triggers")
public class TriggerController {

    private static final String DefaultPage = "0";

    private static final String DefaultSize = "20";

    private final TriggerService triggerService;

    private final TriggerDeliveryService triggerDeliveryService;

    public TriggerController(TriggerService triggerService,
                             TriggerDeliveryService triggerDeliveryService) {
        this.triggerService = triggerService;
        this.triggerDeliveryService = triggerDeliveryService;
    }

    @GetMapping
    @Action(TriggerActions.LIST)
    public List<Trigger> list() {
        return triggerService.list();
    }

    @GetMapping("/{name}")
    @Action(TriggerActions.GET)
    public Trigger get(@PathVariable String name) {
        return triggerService.getByName(name);
    }

    @PostMapping("/email")
    @Action(TriggerActions.SAVE)
    public Trigger save(@Validated @RequestBody EmailTrigger n) {
        return triggerService.save(n);
    }

    @PostMapping("/webhook")
    @Action(TriggerActions.SAVE)
    public Trigger save(@Validated @RequestBody WebhookTrigger n) {
        return triggerService.save(n);
    }

    @DeleteMapping("/{name}")
    @Action(TriggerActions.DELETE)
    public Trigger delete(@PathVariable String name) {
        return triggerService.delete(name);
    }

    @GetMapping("/{name}/deliveries")
    @Action(TriggerActions.LIST)
    public Page<TriggerDelivery.Item> deliveries(@PathVariable String name,
                                                 @RequestParam(required = false, defaultValue = DefaultPage) int page,
                                                 @RequestParam(required = false, defaultValue = DefaultSize) int size) {
        Trigger t = triggerService.getByName(name);
        return triggerDeliveryService.list(t, page, size);
    }
}
