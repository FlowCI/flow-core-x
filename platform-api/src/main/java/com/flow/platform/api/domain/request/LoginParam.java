package com.flow.platform.api.domain.request;

import lombok.Data;

/**
 * @author liangpengyv
 */
@Data
public class LoginParam {

    private String emailOrUsername;

    private String password;
}
