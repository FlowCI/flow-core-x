package com.flowci.core.config.domain;

import com.flowci.common.helper.YamlHelper;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.common.exception.YmlException;
import lombok.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Objects;

import static com.flowci.core.config.domain.SmtpConfig.*;

public final class ConfigParser {

    public static Config parse(InputStream is) {
        Yaml yaml = YamlHelper.create(ConfigWrapper.class);
        ConfigWrapper load = yaml.load(is);
        return load.toConfig();
    }

    private static class ConfigWrapper {

        @NonNull
        public String kind;

        @NonNull
        public String name;

        public SmtpWrapper smtp;

        public Config toConfig() {
            if (Objects.equals(kind, "smtp")) {
                Objects.requireNonNull(smtp, "smtp option must be defined in yml");

                SmtpConfig config = new SmtpConfig();
                config.setName(name);
                config.setServer(smtp.server);
                config.setPort(smtp.port);
                config.setSecure(SecureType.valueOf(smtp.secure));
                config.setAuth(SimpleAuthPair.of(smtp.username, smtp.password));

                return config;
            }

            throw new YmlException("Unsupported kind {0} for config", kind);
        }
    }

    private static class SmtpWrapper {

        @NonNull
        public String server;

        @NonNull
        public Integer port;

        public String secure;

        public String username;

        public String password;

    }
}
