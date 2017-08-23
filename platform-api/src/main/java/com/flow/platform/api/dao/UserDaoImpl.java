package com.flow.platform.api.dao;

import com.flow.platform.api.domain.User;
import com.flow.platform.core.dao.AbstractBaseDao;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    @Override
    public void deleteList(List<String> emailList) {
        String result = "";
        for (String str : emailList) {
            result += "'" + str + "',";
        }
        result = result.substring(0, result.length() - 1);
        String emailListString = result;

        execute((Session session) -> {
            String delete = String.format("delete from User where email in (%s)", emailListString);
            int affectedRows = session.createQuery(delete).executeUpdate();
            if (affectedRows == emailList.size()) {
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public void switchUserRoleIdTo(List<String> emailList, String roleId) {
        String result = "";
        for (String str : emailList) {
            result += "'" + str + "',";
        }
        result = result.substring(0, result.length() - 1);
        String emailListString = result;

        execute((Session session) -> {
            String update = String.format("update User set role_id='%s' where email in (%s)", roleId, emailListString);
            int affectedRows = session.createQuery(update).executeUpdate();
            if (affectedRows == 0) {
                return false;
            } else {
                return true;
            }
        });
    }
}
