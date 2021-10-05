package com.flowci.core.notification.service;

import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.notification.domain.EmailNotificationOption;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.domain.WebhookNotificationOption;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void add(EmailNotificationOption option) {

    }

    @Override
    public void add(WebhookNotificationOption option) {

    }

    @Override
    public void send(List<Notification> list) {

    }

    @EventListener
    public void onJobStatusChange(JobStatusChangeEvent event) {

    }

    @EventListener
    public void onAgentStatusChange(AgentStatusEvent event) {

    }
}
