package com.flow.platform.api.dao;

import com.flow.platform.api.domain.User;
import com.flow.platform.core.dao.AbstractBaseDao;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

/**
 * @author liangpengyv
 */
@Repository(value = "userDao")
public class UserDaoImpl extends AbstractBaseDao<String, User> implements UserDao {

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    protected String getKeyName() {
        return "email";
    }

    @Override
    public Boolean emailIsExist(String email) {
        return execute((Session session) -> {
            String select = String.format("select count(email) from User where email='%s'", email);
            Long num = (Long) session.createQuery(select).uniqueResult();
            if (num == null || num == 0) {
                return false;
            } else {
                return true;
            }
        });
    }

    @Override
    public Boolean userNameIsExist(String userName) {
        return execute((Session session) -> {
            String select = String.format("select count(user_name) from User where user_name='%s'", userName);
            Long num = (Long) session.createQuery(select).uniqueResult();
            if (num == null || num == 0) {
                return false;
            } else {
                return true;
            }
        });
    }

    @Override
    public Boolean passwordOfEmailIsTrue(String email, String password) {
        return execute((Session session) -> {
            String select = String.format("select password from User where email='%s'", email);
            String string = (String) session.createQuery(select).uniqueResult();
            if (password.equals(string)) {
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public Boolean passwordOfUserNameIsTrue(String userName, String password) {
        return execute((Session session) -> {
            String select = String.format("select password from User where user_name='%s'", userName);
            String string = (String) session.createQuery(select).uniqueResult();
            if (password.equals(string)) {
                return true;
            } else {
                return false;
            }
        });
    }
}
