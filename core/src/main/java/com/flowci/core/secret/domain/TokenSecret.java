package com.flowci.core.secret.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.common.domain.SimpleSecret;
import com.flowci.common.domain.SecretField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "secret")
public class TokenSecret extends Secret {

    private SecretField token;

    public TokenSecret() {
        this.token = new SecretField();
        this.setCategory(Category.TOKEN);
    }

    @JsonIgnore
    public String getTokenData() {
        return token.getData();
    }

    public void setTokenData(String val) {
        token.setData(val);
    }

    @Override
    public SimpleSecret toSimpleSecret() {
        return token;
    }
}
