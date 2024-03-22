package com.flowci.core.trigger.domain;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.Mongoable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
