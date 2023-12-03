package com.flowci.core.trigger.domain;

import com.flowci.common.helper.ObjectsHelper;
import com.flowci.common.helper.StringHelper;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@Document(collection = "trigger")
public class WebhookTrigger extends Trigger {

    @NotEmpty(message = "webhook trigger url is required")
    private String url;

    @NotEmpty(message = "webhook trigger http method is required")
    @Pattern(regexp = "GET|POST|PUT|DELETE", message = "Only GET, POST, PUT or DELETE accepted")
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
