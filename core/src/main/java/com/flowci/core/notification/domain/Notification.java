package com.flowci.core.notification.domain;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification")
public class Notification extends Mongoable {

    public enum Type {

        Email,

        Http // send http request to target url
    }

    public enum TriggerAction {

        OnJobStatusChange,

        OnAgentStatusChange
    }

    @Indexed(name = "index_notification_name", unique = true)
    private String name;

    private Type type;

    @Indexed(name = "index_trigger_name")
    private TriggerAction trigger;

    private String condition;

    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }
}
