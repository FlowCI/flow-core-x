package com.flowci.core.notification.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "notification")
public class EmailNotification extends Notification {

    private String smtpConfig;

    private String htmlTemplateInB64;

}
