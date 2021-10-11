package com.flowci.core.notification.dao;

import com.flowci.core.notification.domain.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDao extends MongoRepository<Notification, String> {

    List<Notification> findAllByTrigger(Notification.TriggerAction trigger);
}
