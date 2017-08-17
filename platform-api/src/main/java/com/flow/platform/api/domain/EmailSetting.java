/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.domain;

import com.google.gson.annotations.Expose;

/**
 * @author yh@firim
 */
public class EmailSetting extends MessageSetting {

    @Expose
    private String smtpUrl;

    @Expose
    private Integer smtpPort;

    @Expose
    private String sender;

    @Expose
    private String username;

    @Expose
    private String password;

    @Expose
    private boolean isAuthenticated;

    public EmailSetting(String smtpUrl, Integer smtpPort, String sender) {
        super.setType("EmailSetting");
        this.smtpUrl = smtpUrl;
        this.smtpPort = smtpPort;
        this.sender = sender;
    }

    public String getSmtpUrl() {
        return smtpUrl;
    }

    public void setSmtpUrl(String smtpUrl) {
        this.smtpUrl = smtpUrl;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
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

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EmailSetting that = (EmailSetting) o;

        if (isAuthenticated != that.isAuthenticated) {
            return false;
        }
        if (smtpUrl != null ? !smtpUrl.equals(that.smtpUrl) : that.smtpUrl != null) {
            return false;
        }
        if (smtpPort != null ? !smtpPort.equals(that.smtpPort) : that.smtpPort != null) {
            return false;
        }
        if (sender != null ? !sender.equals(that.sender) : that.sender != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        return password != null ? password.equals(that.password) : that.password == null;
    }

    @Override
    public int hashCode() {
        int result = smtpUrl != null ? smtpUrl.hashCode() : 0;
        result = 31 * result + (smtpPort != null ? smtpPort.hashCode() : 0);
        result = 31 * result + (sender != null ? sender.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (isAuthenticated ? 1 : 0);
        return result;
    }

    @Override
    public String
    toString() {
        return "EmailSetting{" +
            "smtpUrl='" + smtpUrl + '\'' +
            ", smtpPort=" + smtpPort +
            ", sender='" + sender + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", isAuthenticated=" + isAuthenticated +
            '}';
    }
}
