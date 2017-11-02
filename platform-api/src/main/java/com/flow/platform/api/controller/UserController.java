package com.flow.platform.api.controller;

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.request.ListParam;
import com.flow.platform.api.domain.request.LoginParam;
import com.flow.platform.api.domain.request.RegisterUserParam;
import com.flow.platform.api.domain.request.UpdateUserRoleParam;
import com.flow.platform.api.domain.response.LoginResponse;
import com.flow.platform.api.domain.response.UserListResponse;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.security.WebSecurity;
import com.flow.platform.api.service.user.UserService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Autowired
    private HttpServletRequest request;

    /**
     * @api {get} List
     * @apiName List users
     * @apiGroup User
     * @apiDescription Get user list with user joined flow and roles
     *
     * @apiSuccessExample {json} Success-Response
     *  [
     *      total: 8,
     *      adminCount: 2,
     *      {
     *          email: xxx,
     *          username: xxx,
     *          createdAt: xxx,
     *          updatedAt: xxx,
     *          flows: [
     *              flow-1,
     *              flow-2,
     *          ],
     *          roles: [
     *              {
     *                  id: xx,
     *                  name: xx,
     *                  description: xx,
     *                  createdBy: xxx,
     *                  createdAt: xxx,
     *                  updatedAt: xxx
     *              }
     *          ]
     *      }
     *  ]
     */
    @GetMapping
    @WebSecurity(action = Actions.ADMIN_SHOW)
    public UserListResponse list() {
        Long userCount = userService.usersCount();
        Long userAdminCount = userService.adminUserCount();
        List<User> users = userService.list(true, true);
        UserListResponse userListResponse = new UserListResponse(userCount, userAdminCount, users);
        return userListResponse;
    }

    /**
     * @api {post} /login Login
     * @apiName User Login
     * @apiGroup User
     * @apiDescription Login by request information
     *
     * @apiParamExample {json} Request-Body:
     *     {
     *         "emailOrUsername" : "admin",
     *         "password" : "admin"
     *     }
     *
     * @apiSuccessExample {String} Success-Response:
     *     HTTP/1.1 200 OK
     *
     *     xxx.xxx.xxx
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400 Bad Request
     *     {
     *         "message": "Illegal login request parameter: username format false"
     *     }
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginParam loginForm) {
        return userService.login(loginForm);
    }

    /**
     * @api {post} /register
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"email" : "test1@fir.im",
     *         	"username" : "test1",
     *         	"password" : "test1",
     *         	"isSendEmail" : "false",
     *         	"flows": {
     *         	    "arrays": ["xxxx", "xxx"]
     *         	},
     *         	"roles": {
     *         	    "arrays": ["xxx", "xxxx"]
     *         	}
     *     }
     * @apiName User Register
     * @apiGroup User
     * @apiDescription Register by request information
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400 Bad Request
     *     {
     *         "message": "Illegal register request parameter: email already exist"
     *     }
     */
    @PostMapping("/register")
    @WebSecurity(action = Actions.ADMIN_CREATE)
    public void register(@RequestBody RegisterUserParam registerUserParam) {
        User user = new User(registerUserParam.getEmail(), registerUserParam.getUsername(),
            registerUserParam.getPassword());
        userService.register(user, registerUserParam.getRoles().getArrays(), registerUserParam.isSendEmail(),
            registerUserParam.getFlows().getArrays());
    }

    /**
     * @api {delete} /delete Delete
     * @apiName Delete User
     * @apiGroup User
     * @apiDescription Delete user by email
     *
     * @apiParamExample {json} Request-Example:
     *     {
     *         "arrays": ["test1@fir.im","test2@fir.im"]
     *     }
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping(path = "/delete")
    @WebSecurity(action = Actions.ADMIN_DELETE)
    public void delete(@RequestBody ListParam<String> listParam) {
        userService.delete(listParam.getArrays());
    }

    /**
     * @api {post} /updateUserRole
     * @apiParamExample {json} Request-Body:
     *     {
     *         	"emailList" : {
     *         	    "arrays": ["test1@fir.im", "xxx@fir.im"]
     *         	}
     *         	"roles": {
     *         	    "arrays": ["xxx", "xxxx"]
     *         	}
     *     }
     * @apiName User Update role
     * @apiGroup User
     * @apiDescription update user role by request information
     *
     * @apiSuccessExample {json} Success-Response:
     *     HTTP/1.1 200 OK
     *
     * @apiErrorExample {json} Error-Response:
     *     HTTP/1.1 400
     *
     *     {
     *         "message": xxx
     *     }
     */
    @PostMapping("/role/update")
    @WebSecurity(action = Actions.ADMIN_UPDATE)
    public List<User> updateRole(@RequestBody UpdateUserRoleParam updateUserRoleParam) {
        return userService
            .updateUserRole(updateUserRoleParam.getEmailList().getArrays(), updateUserRoleParam.getRoles().getArrays());
    }

}
