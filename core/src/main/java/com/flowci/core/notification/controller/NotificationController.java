package com.flowci.core.notification.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.notification.domain.EmailNotification;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.domain.NotificationAction;
import com.flowci.core.notification.domain.WebhookNotification;
import com.flowci.core.notification.service.NotificationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Action(NotificationAction.LIST)
    public List<Notification> list() {
        return notificationService.list();
    }

    @GetMapping("/{name}")
    @Action(NotificationAction.GET)
    public Notification get(@PathVariable String name) {
        return notificationService.getByName(name);
    }

    @PostMapping("/email")
    @Action(NotificationAction.SAVE)
    public void save(@Validated @RequestBody EmailNotification n) {
        notificationService.save(n);
    }

    @PostMapping("/webhook")
    @Action(NotificationAction.SAVE)
    public void save(@Validated @RequestBody WebhookNotification n) {
        notificationService.save(n);
    }
}
