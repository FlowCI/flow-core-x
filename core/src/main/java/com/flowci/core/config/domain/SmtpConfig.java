package com.flowci.core.config.domain;

import com.flowci.domain.SimpleAuthPair;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@Setter
@Accessors(chain = true)
@Document(collection = "configuration")
public class SmtpConfig extends Config {

    public enum SecureType {
        NONE,

        SSL,

        TLS,
    }

    private String server;

    private Integer port;

    private SecureType secure = SecureType.NONE;

    private SimpleAuthPair auth;

    public SmtpConfig() {
        setCategory(Category.SMTP);
    }
}
