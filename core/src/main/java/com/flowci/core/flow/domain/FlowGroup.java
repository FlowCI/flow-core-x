package com.flowci.core.flow.domain;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "flow")
@EqualsAndHashCode(callSuper = true)
public class FlowGroup extends FlowItem {

    public FlowGroup() {
        super.type = Type.Group;
    }

}
