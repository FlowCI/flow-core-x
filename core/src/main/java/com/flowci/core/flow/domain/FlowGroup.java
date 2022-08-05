package com.flowci.core.flow.domain;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "flow_group")
public class FlowGroup extends FlowItem {

}
