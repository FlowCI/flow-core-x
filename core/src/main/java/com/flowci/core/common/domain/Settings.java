package com.flowci.core.common.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * System settings
 */
@Document
@Getter
@Setter
public class Settings extends Mongoable {

    public final static String DefaultId = "system_settings";

    private String serverUrl;

    public Settings() {
        setId(DefaultId);
    }
}
