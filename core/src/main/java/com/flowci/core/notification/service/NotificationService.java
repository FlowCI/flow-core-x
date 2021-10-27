package com.flowci.core.notification.service;

import com.flowci.core.notification.domain.EmailNotification;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.domain.WebhookNotification;
import com.flowci.domain.Vars;

import java.util.List;

public interface NotificationService {

    List<Notification> list();

    Notification get(String id);

    void save(EmailNotification e);

    void save(WebhookNotification w);

    void delete(String id);

    void send(Notification n, Vars<String> context);
}
