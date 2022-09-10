package com.flowci.core.flow.domain;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.domain.TypedVars;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import com.flowci.util.StringHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Document("flow")
public class FlowItem extends Mongoable {

    public final static String ROOT_ID = "-1";

    public final static String ROOT_NAME = "flows";

    public enum Type {
        Flow,

        Group
    }

    @Indexed(name = "index_flow_name")
    protected String name;

    protected Type type;

    protected Vars<VarValue> vars = new TypedVars();

    /**
     * Parent flow item id
     */
    protected String parentId = ROOT_ID;

    public boolean hasParentId() {
        return StringHelper.hasValue(parentId) && !parentId.equals(ROOT_ID);
    }

    public boolean hasRootParent() {
        return hasParentId() && parentId.equals(ROOT_ID);
    }
}
