package com.flow.platform.api.controller;

import com.flow.platform.api.domain.request.LoginForm;
import com.flow.platform.api.domain.request.SwitchRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.UserService;
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

    /**
     * @api {Post} /login Login
     * @apiName UserLogin
     * @apiGroup User
     * @apiDescription Login by request information
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         "emailOrUsername" : "admin",
     *         "password" : "admin"
     *     }
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *     eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBmaXIuaW0iLCJleHAiOjE1MDM3MTk0NjF9.Lv3vSvQTv_qgpuFD8e59t60YbAWafZO6W5cjYMx5lcw
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400 Bad Request
     *     {
     *         "message": "Illegal login request parameter: username format false"
     *     }
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *         "message": "JSON parse error: java.io.EOFException: End of input at line 4 column 1 path $.password; nested exception is com.google.gson.JsonSyntaxException: java.io.EOFException: End of input at line 4 column 1 path $.password"
     *     }
     */
    @PostMapping("/login")
    public String login(@RequestBody LoginForm loginForm) {
        return userService.login(loginForm);
    }

    /**
     * @api {Post} /register Register
     * @apiName UserRegister
     * @apiGroup User
     * @apiDescription Register by request information
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         	"email" : "test1@fir.im",
     *         	"username" : "test1",
     *         	"password" : "test1",
     *         	"roleId" : "developer"
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400 Bad Request
     *     {
     *         "message": "Illegal register request parameter: email already exist"
     *     }
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *         "message": "JSON parse error: java.io.EOFException: End of input at line 6 column 1 path $.roleId; nested exception is com.google.gson.JsonSyntaxException: java.io.EOFException: End of input at line 6 column 1 path $.roleId"
     *     }
     */
    @PostMapping("/register")
    public void register(@RequestBody User user) {
        userService.register(user);
    }

    /**
     * @api {Post} /delete Delete
     * @apiName UserDelete
     * @apiGroup User
     * @apiDescription Delete by request information
     *
     * @apiParamExample {json} Request-Example:
     *     [
     *         "test1@fir.im",
     *         "test2@fir.im"
     *     ]
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *         "message": "JSON parse error: java.io.EOFException: End of input at line 4 column 1 path $[2]; nested exception is com.google.gson.JsonSyntaxException: java.io.EOFException: End of input at line 4 column 1 path $[2]"
     *     }
     */
    @PostMapping("/delete")
    public void delete(@RequestBody List<String> emailList) {
        userService.delete(emailList);
    }

    /**
     * @api {Post} /role/switch Switch role
     * @apiName UserSwitchRole
     * @apiGroup User
     * @apiDescription Switch role by request information
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         "emailList" : [
     *             "test1@fir.im",
     *             "test2@fir.im"
     *             ],
     *         "switchTo" : "developer"
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 500 Internal Server Error
     *     {
     *         "message": "JSON parse error: java.io.EOFException: End of input at line 7 column 1 path $.switchTo; nested exception is com.google.gson.JsonSyntaxException: java.io.EOFException: End of input at line 7 column 1 path $.switchTo"
     *     }
     */
    @PostMapping("/role/switch")
    public void switchRole(@RequestBody SwitchRole switchRole) {
        List<String> emailList = switchRole.getUsers();
        String roleId = switchRole.getSwitchTo();
        userService.switchRole(emailList, roleId);
    }
}
