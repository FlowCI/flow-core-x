package com.flowci.core.flow.domain;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class UpdateYAMLSource {

    @NotNull
    private Boolean isYamlFromRepo;

    @NotEmpty
    private String yamlRepoBranch;

}
