package com.flowci.core.trigger.domain;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Document(collection = "trigger")
public class Trigger extends Mongoable {

    public enum Category {

        Email,

        WebHook // send http request to target url
    }

    public enum Event {

//        OnUserCreated,
//
//        OnUserAddedToFlow,

        OnJobFinished,

        OnAgentStatusChange,
    }

    @NotEmpty
    @Indexed(name = "index_trigger_name", unique = true)
    private String name;

    @NotNull
    private Category category;

    @NotNull
    @Indexed(name = "index_trigger_event")
    private Event event;

    private String condition;

    public boolean hasCondition() {
        return StringHelper.hasValue(condition);
    }
}
