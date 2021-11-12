package com.flowci.core.trigger.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Document(collection = "trigger")
public class WebhookTrigger extends Trigger {

    private String url;

    private String httpMethod;

    private Map<String, String> headers;

    private String payload;

    public WebhookTrigger() {
        setCategory(Category.WebHook);
    }
}
