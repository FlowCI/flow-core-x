package com.flow.platform.api.test.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.LoginForm;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.service.UserService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.List;

/**
 * @author liangpengyv
 */
public class UserServiceTest extends TestBase {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserService userService;

    private User user;

    private LoginForm loginForm;

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword("liangpengyv");
        user.setRoleId("developer");
    }

    @Test
    public void should_login_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        loginForm = new LoginForm();
        loginForm.setEmailOrUsername("liangpengyv@fir.im");
        loginForm.setPassword("liangpengyv");
        String msg = userService.login(loginForm);
        Assert.assertTrue(msg.length() > 20);

        loginForm.setEmailOrUsername("liangpengyv");
        Assert.assertTrue(msg.length() > 20);

    }

    @Test
    public void should_register_success() {
        userService.register(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_delete_user_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        List<String> emailList = new LinkedList<>();
        emailList.add("liangpengyv@fir.im");
        userService.delete(emailList);
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_switch_role_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));
        Assert.assertEquals("developer", userDao.get("liangpengyv@fir.im").getRoleId());

        List<String> emailList = new LinkedList<>();
        emailList.add("liangpengyv@fir.im");
        userService.switchRole(emailList, "admin");
        Assert.assertEquals("admin", userDao.get("liangpengyv@fir.im").getRoleId());
    }
}
