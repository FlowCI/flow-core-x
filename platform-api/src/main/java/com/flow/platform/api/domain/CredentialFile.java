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
 * @author lhl
 */
public class CredentialFile {

    @Expose
    private  String path;

    @Expose
    private String p12Password;

    @Expose
    private String keyStorePassword;

    @Expose
    private String keyStoreAlias;

    @Expose
    private String keyStoreAliasPassword;

    @Expose
    private String type;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getP12Password() {
        return p12Password;
    }

    public void setP12Password(String p12Password) {
        this.p12Password = p12Password;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getKeyStoreAliasPassword() {
        return keyStoreAliasPassword;
    }

    public void setKeyStoreAliasPassword(String keyStoreAliasPassword) {
        this.keyStoreAliasPassword = keyStoreAliasPassword;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CredentialFile() {
    }
}
