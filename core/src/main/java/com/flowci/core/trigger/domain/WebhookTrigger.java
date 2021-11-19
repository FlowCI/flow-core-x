package com.flowci.core.trigger.domain;

import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
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

    private Map<String, String> params;

    private Map<String, String> headers;

    private String body;

    public WebhookTrigger() {
        setCategory(Category.WebHook);
    }

    public boolean hasHeaders() {
        return ObjectsHelper.hasCollection(headers);
    }

    public boolean hasParams() {
        return ObjectsHelper.hasCollection(params);
    }

    public boolean hasBody() {
        return StringHelper.hasValue(body);
    }
}
