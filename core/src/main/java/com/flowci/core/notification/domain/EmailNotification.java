package com.flowci.core.notification.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@Document(collection = "notification")
public class EmailNotification extends Notification {

    @NotEmpty
    private String smtpConfig;

    @NotEmpty
    private String from;

    @NotEmpty
    private String to;

    @NotEmpty
    private String subject;

    @NotEmpty
    private String template = "default"; // template name

    public EmailNotification() {
        setType(Type.Email);
    }
}
