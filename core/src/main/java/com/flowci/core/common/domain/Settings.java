package com.flowci.core.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * System settings
 */
@Document
@Getter
@Setter
public class Settings extends Mongoable {

    public static class Action {

        public static final String GET = "get_sys_settings";

        public static final String UPDATE = "update_sys_settings";

        public static final List<String> ALL = ImmutableList.of(
                GET,
                UPDATE
        );
    }

    public final static String DefaultId = "system_settings";

    private String serverUrl;

    /**
     * Indicate load resource(cn) from where
     */
    private String source;

    public Settings() {
        setId(DefaultId);
    }

    @JsonIgnore
    public String getId() {
        return this.id;
    }
}
