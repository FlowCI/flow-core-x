package com.flow.platform.api.test.service;

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
        user.setUserName("liangpengyv");
        user.setPassword("liangpengyv");
        user.setRoleId("developer");
    }

    @Test
    public void should_login_by_email_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        String msg = userService.loginByEmail("liangpengyv@fir.im", "liangpengyv");
        Assert.assertTrue(msg.length() > 20);
    }

    @Test
    public void should_login_by_user_name_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        String msg = userService.loginByUserName("liangpengyv", "liangpengyv");
        Assert.assertTrue(msg.length() > 20);
    }

    @Test
    public void should_register_success() {
        userService.register(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_delete_user_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        List<String> emailList = new LinkedList<>();
        emailList.add("liangpengyv@fir.im");
        userService.delete(emailList);
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_switch_role_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
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
    public void should_check_user_name_format_success() {
        String userName1 = "test1";  //the correct user_name format
        Assert.assertTrue(userService.checkUserNameFormatIsPass(userName1));
        String userName2 = "test";  //the wrong user_name format
        Assert.assertFalse(userService.checkUserNameFormatIsPass(userName2));
        String userName3 = "testtesttesttesttest1";  //the wrong user_name format
        Assert.assertFalse(userService.checkUserNameFormatIsPass(userName3));
        String userName4 = "#test";  //the wrong user_name format
        Assert.assertFalse(userService.checkUserNameFormatIsPass(userName4));
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
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertTrue(userService.emailIsExist("liangpengyv@fir.im"));
    }

    @Test
    public void should_verify_user_name_is_exist_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertTrue(userService.userNameIsExist("liangpengyv"));
    }

    @Test
    public void should_verify_password_of_email_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertTrue(userService.passwordOfEmailIsTrue("liangpengyv@fir.im", StringEncodeUtil.encodeByMD5("liangpengyv", "UTF-8")));
    }

    @Test
    public void should_verify_password_of_user_name_success() {
        user.setPassword(StringEncodeUtil.encodeByMD5(user.getPassword(), "UTF-8"));
        userDao.save(user);
        Assert.assertTrue(userService.passwordOfUserNameIsTrue("liangpengyv", StringEncodeUtil.encodeByMD5("liangpengyv", "UTF-8")));
    }
}
