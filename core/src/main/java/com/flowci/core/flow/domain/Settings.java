package com.flowci.core.flow.domain;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
}
