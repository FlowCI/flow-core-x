package com.flow.platform.api.dao.user;

import com.flow.platform.api.domain.user.User;
import com.flow.platform.core.dao.AbstractBaseDao;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author liangpengyv
 */
@Repository
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
    public Long count() {
        return execute(session -> session.createQuery("select count(email) from User", Long.class).uniqueResult());
    }

    @Override
    public User getByUsername(String username) {
        return execute(session -> session.createQuery("from User where username = :username", User.class)
            .setParameter("username", username)
            .uniqueResult());
    }

    @Override
    public void delete(List<String> emailList) {
        execute((Session session) -> {
            Query query = session.createQuery("delete from User where email in :list");
            query.setParameterList("list", emailList);

            int affectedRows = query.executeUpdate();
            return affectedRows == emailList.size();
        });
    }
}
