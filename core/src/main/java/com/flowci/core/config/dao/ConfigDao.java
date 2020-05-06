package com.flowci.core.config.dao;

import com.flowci.core.config.domain.Config;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigDao extends MongoRepository<Config, String> {

    Optional<Config> findByName(String name);

    List<Config> findAllByCategoryOrderByCreatedAtAsc(Config.Category category);
}
