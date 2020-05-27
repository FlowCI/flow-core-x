package com.flowci.core.flow.domain;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public abstract class Settings {

    @Data
    public static class RenameFlow {

        @NotEmpty
        private String name;
    }

    @Data
    public static class UpdateTimeout {

        @NotNull
        @Min(value = 10) // min is 10 seconds
        private Integer jobTimeout;

        @NotNull
        @Min(value = 10) // min is 10 seconds
        private Integer stepTimeout;
    }

    @Data
    public static class UpdateYAMLSource {

        @NotNull
        private Boolean isYamlFromRepo;

        @NotEmpty
        private String yamlRepoBranch;
    }
}
