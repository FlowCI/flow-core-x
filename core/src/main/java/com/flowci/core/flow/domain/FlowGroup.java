package com.flowci.core.flow.domain;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedList;
import java.util.List;

@Setter
@Getter
@Document(collection = "flow")
public class FlowGroup extends FlowItem {

    private List<String> flows = new LinkedList<>();
}
