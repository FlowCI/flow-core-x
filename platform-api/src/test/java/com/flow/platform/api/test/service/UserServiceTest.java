package com.flow.platform.api.test.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.UserDao;
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

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword("liangpengyv");
        user.setRoleId("developer");
    }

    @Test
    public void should_login_by_email_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        String msg = userService.loginByEmail("liangpengyv@fir.im", "liangpengyv");
        Assert.assertTrue(msg.length() > 20);
    }

    @Test
    public void should_login_by_username_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        String msg = userService.loginByUsername("liangpengyv", "liangpengyv");
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

    @Test
    public void should_check_email_format_success() {
        String email1 = "test1@fir.im";  //the correct email format
        Assert.assertTrue(userService.checkEmailFormatIsPass(email1));
        String email2 = "test2fir.im";  //the wrong email format
        Assert.assertFalse(userService.checkEmailFormatIsPass(email2));
        String email3 = "test3@firim";  //the wrong email format
        Assert.assertFalse(userService.checkEmailFormatIsPass(email3));
        String email4 = "test4@.im";  //the wrong email format
        Assert.assertFalse(userService.checkEmailFormatIsPass(email4));
    }

    @Test
    public void should_check_username_format_success() {
        String username1 = "test1";  //the correct username format
        Assert.assertTrue(userService.checkUsernameFormatIsPass(username1));
        String username2 = "test";  //the wrong username format
        Assert.assertFalse(userService.checkUsernameFormatIsPass(username2));
        String username3 = "testtesttesttesttest1";  //the wrong username format
        Assert.assertFalse(userService.checkUsernameFormatIsPass(username3));
        String username4 = "#test";  //the wrong username format
        Assert.assertFalse(userService.checkUsernameFormatIsPass(username4));
    }

    @Test
    public void should_check_password_format_success() {
        String password1 = "test1";  //the correct password format
        Assert.assertTrue(userService.checkPasswordFormatIsPass(password1));
        String password2 = "test";  //the wrong password format
        Assert.assertFalse(userService.checkPasswordFormatIsPass(password2));
        String password3 = "testtesttesttesttest1";  //the wrong password format
        Assert.assertFalse(userService.checkPasswordFormatIsPass(password3));
        String password4 = "#test";  //the wrong password format
        Assert.assertFalse(userService.checkPasswordFormatIsPass(password4));
    }

    @Test
    public void should_verify_email_is_exist_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertTrue(userService.emailIsExist("liangpengyv@fir.im"));
    }

    @Test
    public void should_verify_username_is_exist_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertTrue(userService.usernameIsExist("liangpengyv"));
    }

    @Test
    public void should_verify_password_of_email_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertTrue(userService.passwordOfEmailIsTrue("liangpengyv@fir.im", StringEncodeUtil.encodeByMD5("liangpengyv", AppConfig.DEFAULT_CHARSET.name())));
    }

    @Test
    public void should_verify_password_of_username_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
        Assert.assertTrue(userService.passwordOfUsernameIsTrue("liangpengyv", StringEncodeUtil.encodeByMD5("liangpengyv", AppConfig.DEFAULT_CHARSET.name())));
    }
}
