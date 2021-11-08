package com.flowci.core.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@Document(collection = "notification")
public class EmailNotification extends Notification {

    public static final String FLOW_USERS = "FLOW_USERS";

    @NotEmpty
    private String smtpConfig;

    private String from;

    @NotEmpty
    private String to;

    @NotEmpty
    private String subject;

    @NotEmpty
    private String template = "default"; // template name

    public EmailNotification() {
        setCategory(Category.Email);
    }

    public boolean isToFlowUsers() {
        return FLOW_USERS.equals(to);
    }

    @JsonIgnore
    public boolean hasFrom() {
        return StringHelper.hasValue(from);
    }
}
