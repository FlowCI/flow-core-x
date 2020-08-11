package com.flowci.core.auth.dao;

import com.flowci.core.auth.domain.UserAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class CustomUserAuthDaoImpl implements CustomUserAuthDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public void update(String id, String token) {
        operations.findAndModify(
                query(where("_id").is(id)),
                new Update().set("token", token),
                UserAuth.class);
    }

    @Override
    public void update(String id, String token, String refreshToken) {
        operations.findAndModify(
                query(where("_id").is(id)),
                new Update().set("token", token).set("refreshToken", refreshToken),
                UserAuth.class);
    }
}
