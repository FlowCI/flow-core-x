package com.flowci.core.notification.service;

import com.flowci.core.notification.domain.EmailNotificationOption;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.domain.WebhookNotificationOption;

import java.util.List;

public interface NotificationService {

    void add(EmailNotificationOption option);

    void add(WebhookNotificationOption option);

    void send(List<Notification> list);
}
