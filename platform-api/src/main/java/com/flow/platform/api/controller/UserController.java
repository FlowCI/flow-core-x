package com.flow.platform.api.controller;

import com.flow.platform.api.domain.request.LoginForm;
import com.flow.platform.api.domain.request.SwitchRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.service.user.UserService;
import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
     * @apiName User Login
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
     * @apiParam {String} roles Param Example: ?roles=admin,user
     * @apiName User Register
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
    public void register(@RequestBody User user, @RequestParam(required = false) String roles) {
        Set<String> roleNameSet = new HashSet<>(2);

        if (!Strings.isNullOrEmpty(roles)) {
            roles = roles.trim();

            for (String role : roles.trim().split(",")) {
                role = role.trim();
                if (!Strings.isNullOrEmpty(role)) {
                    roleNameSet.add(role);
                }
            }
        }

        userService.register(user, roleNameSet);
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
}
