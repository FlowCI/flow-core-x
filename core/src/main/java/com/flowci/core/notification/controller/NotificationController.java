package com.flowci.core.notification.controller;

import com.flowci.core.notification.domain.EmailNotification;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.domain.WebhookNotification;
import com.flowci.core.notification.service.NotificationService;
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
    public List<Notification> list() {
        return notificationService.list();
    }

    @GetMapping("/{name}")
    public Notification get(@PathVariable String name) {
        return notificationService.getByName(name);
    }

    @PostMapping("/email")
    public void save(EmailNotification n) {
        notificationService.save(n);
    }

    @PostMapping("/webhook")
    public void save(WebhookNotification n) {
        notificationService.save(n);
    }
}
