package com.flow.platform.api.domain.request;

/**
 * @author liangpengyv
 */
public class LoginParam {

    private String emailOrUsername;

    private String password;

    public String getEmailOrUsername() {
        return emailOrUsername;
    }

    public void setEmailOrUsername(String emailOrUsername) {
        this.emailOrUsername = emailOrUsername;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
