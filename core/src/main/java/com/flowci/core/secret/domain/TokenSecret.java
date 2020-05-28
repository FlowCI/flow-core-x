package com.flowci.core.secret.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.SimpleSecret;
import com.flowci.domain.SimpleToken;
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

    private SimpleToken token;

    public TokenSecret() {
        this.token = new SimpleToken();
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
