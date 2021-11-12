package com.flowci.core.trigger.domain;

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
@Document(collection = "trigger")
public class Trigger extends Mongoable {

    public enum Category {

        Email,

        WebHook // send http request to target url
    }

    public enum Action {

//        OnUserCreated,
//
//        OnUserAddedToFlow,

        OnJobFinished,

        OnAgentStatusChange,
    }

    @Indexed(name = "index_trigger_name", unique = true)
    private String name;

    private Category category;

    @Indexed(name = "index_trigger_action")
    private Action action;

    private String condition;

    private String error;

    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }
}
