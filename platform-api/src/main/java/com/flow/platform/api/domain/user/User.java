package com.flow.platform.api.domain.user;

import com.flow.platform.api.domain.CreateUpdateObject;

/**
 * @author liangpengyv
 */
public class User extends CreateUpdateObject {

    private String email;

    private String username;

    private String password;

    private String flowAuth;

    public User(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public User() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return email != null ? email.equals(user.email) : user.email == null;
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{" +
            "email='" + email + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            "} ";
    }
}
