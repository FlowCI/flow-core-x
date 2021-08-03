package com.flowci.core.flow.domain;

import com.flowci.core.common.domain.SourceWithDomain;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Template extends SourceWithDomain {

    private String desc;

    private boolean isDefault;
}
