package com.flowci.core.flow.domain;

import com.flowci.common.helper.StringHelper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public final class Settings {

    @NotNull
    private Boolean isYamlFromRepo;

    @NotEmpty
    private String yamlRepoBranch;

    @NotNull
    @Min(value = 10) // min is 10 seconds
    private Integer jobTimeout;

    @NotNull
    @Min(value = 10) // min is 10 seconds
    private Integer stepTimeout;

    private String cron;

    public boolean hasCron() {
        return StringHelper.hasValue(cron);
    }
}
