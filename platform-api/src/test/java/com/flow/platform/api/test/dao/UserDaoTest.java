package com.flow.platform.api.test.dao;

import com.flow.platform.api.dao.UserDao;
import com.flow.platform.api.domain.User;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.StringEncodeUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
        user.setUserName("liangpengyv");
        user.setPassword(StringEncodeUtil.encodeByMD5("liangpengyv", "UTF-8"));
        userDao.save(user);
    }

    @Test
    public void should_save_and_get_success() {
        User user1 = userDao.get(user.getEmail());
        Assert.assertNotNull(user1);
        Assert.assertEquals(user.getEmail(), user1.getEmail());
    }

    @Test
    public void should_email_is_exist_success() {
        Assert.assertEquals(true, userDao.emailIsExist(user.getEmail()));
    }

    @Test
    public void should_user_name_is_exist_success() {
        Assert.assertEquals(true, userDao.userNameIsExist(user.getUserName()));
    }

    @Test
    public void should_password_of_email_is_true_success() {
        Assert.assertEquals(true, userDao.passwordOfEmailIsTrue(user.getEmail(), user.getPassword()));
    }

    @Test
    public void should_password_of_user_name_is_true_success() {
        Assert.assertEquals(true, userDao.passwordOfUserNameIsTrue(user.getUserName(), user.getPassword()));
    }
}
