package com.flowci.core.notification.dao;

import com.flowci.core.notification.domain.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDao extends MongoRepository<Notification, String> {
}
