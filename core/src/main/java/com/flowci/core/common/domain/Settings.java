package com.flowci.core.common.domain;

import com.flowci.util.StringHelper;
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

    private String uniqueId = DefaultId;

    private String serverUrl;

    private String webUrl;

    /**
     * Indicate load resource(cn) from where
     */
    private String source;

    public String getServerUrl() {
        return trimEndSlash(serverUrl);
    }

    public String getWebUrl() {
        return trimEndSlash(webUrl);
    }

    private static String trimEndSlash(String url) {
        if (StringHelper.hasValue(url) && url.endsWith("/")) {
            url = url.substring(0, url.length() - 2);
        }
        return url;
    }
}
