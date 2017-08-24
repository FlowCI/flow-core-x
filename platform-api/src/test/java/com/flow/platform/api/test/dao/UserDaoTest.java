package com.flow.platform.api.test.dao;

import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liangpengyv
 */
public class UserDaoTest extends TestBase {

    @Autowired
    private UserDao userDao;

    private User user;

    @Before
    public void beforeTest() {
        user = new User();
        user.setEmail("liangpengyv@fir.im");
        user.setUsername("liangpengyv");
        user.setPassword(StringEncodeUtil.encodeByMD5("liangpengyv", "UTF-8"));
        user.setRoleId("developer");
        userDao.save(user);
    }

    @Test
    public void should_save_and_get_by_email() {
        // check whether can find user by email
        Assert.assertNotNull(userDao.get(user.getEmail()));
        Assert.assertEquals(user.getEmail(), userDao.get(user.getEmail()).getEmail());
    }

    @Test
    public void should_return_null_if_email_not_exist() {
        Assert.assertNull(userDao.get("xxx.com"));
    }

    @Test
    public void should_email_is_exist_success() {
        Assert.assertEquals(true, userDao.emailIsExist(user.getEmail()));
    }

    @Test
    public void should_username_is_exist_success() {
        Assert.assertEquals(true, userDao.usernameIsExist(user.getUsername()));
    }

    @Test
    public void should_password_of_email_is_true_success() {
        Assert.assertEquals(true, userDao.passwordOfEmailIsTrue(user.getEmail(), user.getPassword()));
    }

    @Test
    public void should_password_of_username_is_true_success() {
        Assert.assertEquals(true, userDao.passwordOfUsernameIsTrue(user.getUsername(), user.getPassword()));
    }

    @Test
    public void should_get_email_success() {
        Assert.assertEquals("liangpengyv@fir.im", userDao.getEmailBy("username", "liangpengyv"));
    }

    @Test
    public void should_delete_list_success() {
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));

        List<String> emailList = new ArrayList<>();
        emailList.add("liangpengyv@fir.im");
        userDao.deleteList(emailList);
        Assert.assertNull(userDao.get("liangpengyv@fir.im"));
    }

    @Test
    public void should_switch_role_success() {
        Assert.assertEquals("developer", userDao.get("liangpengyv@fir.im").getRoleId());

        List<String> emailList = new ArrayList<>();
        emailList.add("liangpengyv@fir.im");
        userDao.switchUserRoleIdTo(emailList, "admin");
        Assert.assertEquals("admin", userDao.get("liangpengyv@fir.im").getRoleId());
    }
}
