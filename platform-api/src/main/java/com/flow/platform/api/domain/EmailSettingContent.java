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
public class EmailSettingContent extends SettingContent {

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

    public EmailSettingContent(String smtpUrl, Integer smtpPort, String sender) {
        super.setType(MessageType.EMAIl);
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

        EmailSettingContent that = (EmailSettingContent) o;

        return smtpUrl != null ? smtpUrl.equals(that.smtpUrl) : that.smtpUrl == null;
    }

    @Override
    public int hashCode() {
        return smtpUrl != null ? smtpUrl.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "EmailSettingContent{" +
            "smtpUrl='" + smtpUrl + '\'' +
            ", smtpPort=" + smtpPort +
            ", sender='" + sender + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", isAuthenticated=" + isAuthenticated +
            '}';
    }
}
