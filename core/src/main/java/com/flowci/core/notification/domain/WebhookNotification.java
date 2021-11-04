package com.flowci.core.notification.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Document(collection = "notification")
public class WebhookNotification extends Notification {

    private String url;

    private String httpMethod;

    private Map<String, String> headers;

    private String payload;

    public WebhookNotification() {
        setCategory(Category.WebHook);
    }
}
