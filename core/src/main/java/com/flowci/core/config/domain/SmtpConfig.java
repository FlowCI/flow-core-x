package com.flowci.core.config.domain;

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

    private String server; // smtp server

    private Integer port; // smtp server port, 465 (SSL) or 587 (TLS)

    private SimpleAuthPair auth; // smtp server username, password

    private String secret; // could specify secret name instead auth pair

    public SmtpConfig() {
        setCategory(Category.SMTP);
    }

    public boolean hasSecret() {
        return StringHelper.hasValue(secret);
    }
}
