package com.flow.platform.api.test.dao;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
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
        user.setPassword(StringEncodeUtil.encodeByMD5("liangpengyv", AppConfig.DEFAULT_CHARSET.name()));
        userDao.save(user);
    }

    @Test
    public void should_save_success() {
        // check whether can find user by email
        Assert.assertNotNull(userDao.get("liangpengyv@fir.im"));
        Assert.assertEquals("liangpengyv@fir.im", userDao.get("liangpengyv@fir.im").getEmail());
    }

    @Test
    public void should_return_false_if_email_not_exist() {
        Assert.assertTrue(userDao.emailIsExist("liangpengyv@fir.im"));
        Assert.assertFalse(userDao.emailIsExist("xxx@xxx.com"));
    }

    @Test
    public void should_return_false_if_username_not_exist() {
        Assert.assertTrue((userDao.usernameIsExist("liangpengyv")));
        Assert.assertFalse(userDao.usernameIsExist("xxxxxx"));
    }

    @Test
    public void should_return_false_if_password_of_email_is_not_true() {
        Assert.assertTrue(userDao.passwordOfEmailIsTrue("liangpengyv@fir.im", StringEncodeUtil.encodeByMD5("liangpengyv", AppConfig.DEFAULT_CHARSET.name())));
        Assert.assertFalse(userDao.passwordOfEmailIsTrue("liangpengyv@fir.im", StringEncodeUtil.encodeByMD5("xxxxx", AppConfig.DEFAULT_CHARSET.name())));
    }

    @Test
    public void should_return_false_if_password_of_username_is_not_true() {
        Assert.assertTrue(userDao.passwordOfUsernameIsTrue("liangpengyv", StringEncodeUtil.encodeByMD5("liangpengyv", AppConfig.DEFAULT_CHARSET.name())));
        Assert.assertFalse(userDao.passwordOfUsernameIsTrue("liangpengyv", StringEncodeUtil.encodeByMD5("xxxxx", AppConfig.DEFAULT_CHARSET.name())));
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
}
