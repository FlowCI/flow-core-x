package com.flowci.core.flow.domain;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.domain.TypedVars;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;

@Setter
@Getter
@ToString(of = {"name"})
public abstract class FlowItem extends Mongoable {

    @Indexed(name = "index_flow_name")
    protected String name;

    protected Vars<VarValue> vars = new TypedVars();

}
