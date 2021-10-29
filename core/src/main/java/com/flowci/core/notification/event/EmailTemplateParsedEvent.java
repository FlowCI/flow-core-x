package com.flowci.core.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class EmailTemplateParsedEvent extends ApplicationEvent {

    @Getter
    private final String template;

    public EmailTemplateParsedEvent(Object source, String template) {
        super(source);
        this.template = template;
    }
}
