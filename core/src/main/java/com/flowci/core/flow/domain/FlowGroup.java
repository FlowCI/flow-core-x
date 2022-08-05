package com.flowci.core.flow.domain;


import com.flowci.core.common.domain.Mongoable;
import com.flowci.domain.TypedVars;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document(collection = "flow_group")
public class FlowGroup extends Mongoable {

    @Indexed(name = "index_group_name")
    protected String name;

    protected Vars<VarValue> vars = new TypedVars();
}
