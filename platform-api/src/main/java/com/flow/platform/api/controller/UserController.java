package com.flow.platform.api.controller;

import com.flow.platform.api.domain.SwitchRoleRequest;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author liangpengyv
 */

@RestController
@RequestMapping(path = "/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody User user) {
        if (user.getEmail() == null) {
            // Use username to login
            return userService.loginByUsername(user.getUsername(), user.getPassword());
        } else {
            // Use email to login
            return userService.loginByEmail(user.getEmail(), user.getPassword());
        }
    }

    @PostMapping("/register")
    public void register(@RequestBody User user) {
        userService.register(user);
    }

    @PostMapping("/delete")
    public void delete(@RequestBody List<String> emailList) {
        userService.delete(emailList);
    }

    @PostMapping("/switch_role")
    public void switchRole(@RequestBody SwitchRoleRequest switchRoleRequest) {
        List<String> emailList = switchRoleRequest.getUsers();
        String roleId = switchRoleRequest.getSwitchTo();
        userService.switchRole(emailList, roleId);
    }
}
