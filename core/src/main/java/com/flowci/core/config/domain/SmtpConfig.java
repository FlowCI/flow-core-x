package com.flowci.core.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Accessors(chain = true)
@Document(collection = "configuration")
public class SmtpConfig extends Config {

    private SmtpOption smtp;

    public SmtpConfig() {
        setCategory(Category.SMTP);
    }

    public boolean hasSecret() {
        return StringHelper.hasValue(smtp.getSecret());
    }

    @JsonIgnore
    public String getSecret() {
        return smtp.getSecret();
    }

    @JsonIgnore
    public SimpleAuthPair getAuth() {
        return smtp.getAuth();
    }

    public void setAuth(SimpleAuthPair auth) {
        this.smtp.setAuth(auth);
    }
}
