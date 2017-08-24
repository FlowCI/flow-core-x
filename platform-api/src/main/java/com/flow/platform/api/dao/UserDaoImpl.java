package com.flow.platform.api.dao;

import com.flow.platform.api.domain.User;
import com.flow.platform.core.dao.AbstractBaseDao;
import org.hibernate.Session;
import org.hibernate.query.Query;
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
            }
            return true;
        });
    }

    @Override
    public Boolean usernameIsExist(String username) {
        return execute((Session session) -> {
            String select = String.format("select count(username) from User where username='%s'", username);
            Long num = (Long) session.createQuery(select).uniqueResult();
            if (num == null || num == 0) {
                return false;
            }
            return true;
        });
    }

    @Override
    public Boolean passwordOfEmailIsTrue(String email, String password) {
        return execute((Session session) -> {
            String select = String.format("select password from User where email='%s'", email);
            String string = (String) session.createQuery(select).uniqueResult();
            if (password.equals(string)) {
                return true;
            }
            return false;
        });
    }

    @Override
    public Boolean passwordOfUsernameIsTrue(String username, String password) {
        return execute((Session session) -> {
            String select = String.format("select password from User where username='%s'", username);
            String string = (String) session.createQuery(select).uniqueResult();
            if (password.equals(string)) {
                return true;
            }
            return false;
        });
    }

    @Override
    public String getEmailBy(String whereWhatFieldName, String whereWhatFieldValue) {
        return execute((Session session) -> {
            String select = String.format("select email from User where %s='%s'", whereWhatFieldName, whereWhatFieldValue);
            String email = (String) session.createQuery(select).uniqueResult();
            return email;
        });
    }

    @Override
    public void deleteList(List<String> emailList) {
        execute((Session session) -> {
            String delete = String.format("delete from User where email in (:list)");
            Query query = session.createQuery(delete);
            query.setParameterList("list", emailList);
            int affectedRows = query.executeUpdate();
            if (affectedRows == emailList.size()) {
                return true;
            }
            return false;
        });
    }

    @Override
    public void switchUserRoleIdTo(List<String> emailList, String roleId) {
        execute((Session session) -> {
            String update = String.format("update User set role_id='%s' where email in (:list)", roleId);
            Query query = session.createQuery(update);
            query.setParameterList("list", emailList);
            int affectedRows = query.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }
            return true;
        });
    }
}
