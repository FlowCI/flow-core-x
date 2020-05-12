package com.flowci.core.config.domain;

import com.flowci.domain.SimpleAuthPair;
import com.flowci.exception.YmlException;
import com.flowci.util.YamlHelper;
import lombok.NonNull;
import org.yaml.snakeyaml.Yaml;

import javax.validation.constraints.NotEmpty;
import java.io.InputStream;
import java.util.Objects;

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

                SmtpOption option = new SmtpOption();
                option.setServer(smtp.server);
                option.setPort(smtp.port);
                option.setSecure(SmtpOption.SecureType.valueOf(smtp.secure));
                option.setAuth(SimpleAuthPair.of(smtp.username, smtp.password));

                SmtpConfig config = new SmtpConfig();
                config.setName(name);
                config.setSmtp(option);
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
