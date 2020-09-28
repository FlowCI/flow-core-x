package com.flowci.core.common.dao;

import com.flowci.core.common.domain.Settings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsDao extends MongoRepository<Settings, String> {
}
