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
package com.flow.platform.api.domain.credential;

import com.flow.platform.api.domain.file.FileResource;
import com.flow.platform.api.domain.file.PasswordFileResource;
import com.google.gson.annotations.Expose;
import java.util.LinkedList;
import java.util.List;

/**
 * @author lhl
 */
public class IosCredentialDetail extends CredentialDetail {

    /**
     * Provisioning Profile
     */
    @Expose
    private List<FileResource> provisionProfiles = new LinkedList<>();

    @Expose
    private List<PasswordFileResource> p12s = new LinkedList<>();

    public IosCredentialDetail() {
        this.type = CredentialType.IOS;
    }

    public List<FileResource> getProvisionProfiles() {
        return provisionProfiles;
    }

    public void setProvisionProfiles(List<FileResource> provisionProfiles) {
        this.provisionProfiles = provisionProfiles;
    }

    public List<PasswordFileResource> getP12s() {
        return p12s;
    }

    public void setP12s(List<PasswordFileResource> p12s) {
        this.p12s = p12s;
    }
}
