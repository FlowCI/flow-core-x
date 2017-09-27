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
package com.flow.platform.api.domain.response;

import com.flow.platform.api.domain.user.User;
import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * @author lhl
 */
public class UserListResponse extends Jsonable{

    @Expose
    private Long total;

    @Expose
    private Long adminCount;

    @Expose
    private List<User> users;

    public UserListResponse(Long total, Long adminCount, List<User> users) {
        this.total = total;
        this.adminCount = adminCount;
        this.users = users;
    }

    public void setAdminCount(Long adminCount) {
        this.adminCount = adminCount;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}

