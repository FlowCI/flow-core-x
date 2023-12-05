package com.flowci.core.secret.event;

import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.common.domain.SimpleAuthPair;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class CreateAuthEvent extends ApplicationEvent {

    private final String name;

    private final SimpleAuthPair pair;

    private AuthSecret secret;

    private RuntimeException err;

    public CreateAuthEvent(Object source, String name, SimpleAuthPair pair) {
        super(source);
        this.name = name;
        this.pair = pair;
    }
}
