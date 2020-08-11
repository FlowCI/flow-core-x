package com.flowci.core.auth.dao;

public interface CustomUserAuthDao {

    void update(String id, String token);

    void update(String id, String token, String refreshToken);
}
