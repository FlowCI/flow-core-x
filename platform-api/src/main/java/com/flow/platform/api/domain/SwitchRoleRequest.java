package com.flow.platform.api.domain;

import java.util.List;

/**
 * @author liangpengyv
 */
public class SwitchRoleRequest {

    private List<String> emailList;

    private String switchTo;

    public List<String> getUsers() {
        return emailList;
    }

    public void setUsers(List<String> users) {
        this.emailList = users;
    }

    public String getSwitchTo() {
        return switchTo;
    }

    public void setSwitchTo(String switchTo) {
        this.switchTo = switchTo;
    }
}
