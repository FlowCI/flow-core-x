package com.flow.platform.api.domain;

import java.time.ZonedDateTime;

/**
 * @author liangpengyv
 */
public class User {

    private String email;

    private String userName;

    private String password;

    private String flowAuth;

    private String roleId;

    private String token;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFlowAuth() {
        return flowAuth;
    }

    public void setFlowAuth(String flowAuth) {
        this.flowAuth = flowAuth;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
