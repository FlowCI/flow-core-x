package com.flowci.core.flow.domain;

import com.flowci.domain.StringVars;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public final class SaveNotify {

    @NotEmpty
    private String plugin;

    private Map<String, String> inputs = new HashMap<>();

    public Notification toObj() {
        return new Notification()
                .setPlugin(plugin)
                .setInputs(new StringVars(inputs));
    }
}
