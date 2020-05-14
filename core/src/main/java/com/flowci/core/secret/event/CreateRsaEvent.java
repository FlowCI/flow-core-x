package com.flowci.core.secret.event;

import com.flowci.core.secret.domain.RSASecret;
import com.flowci.domain.SimpleKeyPair;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class CreateRsaEvent extends ApplicationEvent {

    private final String name;

    private final SimpleKeyPair pair;

    private RSASecret secret;

    private RuntimeException err;

    public CreateRsaEvent(Object source, String name, SimpleKeyPair pair) {
        super(source);
        this.name = name;
        this.pair = pair;
    }
}
