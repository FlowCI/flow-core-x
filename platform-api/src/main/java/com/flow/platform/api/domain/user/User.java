package com.flow.platform.api.domain.user;

import com.flow.platform.api.domain.CreateUpdateObject;
import com.google.gson.annotations.Expose;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author liangpengyv
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"email"}, callSuper = false)
@ToString(of = {"email", "username", "password"})
public class User extends CreateUpdateObject {

    @Expose
    @Getter
    @Setter
    private String email;

    @Expose
    @Getter
    @Setter
    private String username;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    @Getter
    @Setter
    private String password;

    @Expose
    @Getter
    @Setter
    private List<String> flows;

    @Expose
    @Getter
    @Setter
    private List<Role> roles;

    public User(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }
}
