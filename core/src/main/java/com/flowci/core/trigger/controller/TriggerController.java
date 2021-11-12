package com.flowci.core.trigger.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.trigger.domain.EmailTrigger;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.domain.TriggerOperations;
import com.flowci.core.trigger.domain.WebhookTrigger;
import com.flowci.core.trigger.service.TriggerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/triggers")
public class TriggerController {

    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @GetMapping
    @Action(TriggerOperations.LIST)
    public List<Trigger> list() {
        return triggerService.list();
    }

    @GetMapping("/{name}")
    @Action(TriggerOperations.GET)
    public Trigger get(@PathVariable String name) {
        return triggerService.getByName(name);
    }

    @PostMapping("/email")
    @Action(TriggerOperations.SAVE)
    public void save(@Validated @RequestBody EmailTrigger n) {
        triggerService.save(n);
    }

    @PostMapping("/webhook")
    @Action(TriggerOperations.SAVE)
    public void save(@Validated @RequestBody WebhookTrigger n) {
        triggerService.save(n);
    }

    @DeleteMapping("/{name}")
    @Action(TriggerOperations.DELETE)
    public Trigger delete(@PathVariable String name) {
        return triggerService.delete(name);
    }
}
