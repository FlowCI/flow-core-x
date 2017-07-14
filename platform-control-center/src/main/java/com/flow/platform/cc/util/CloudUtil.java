package com.flow.platform.cc.util;

import com.flow.platform.util.ClassUtil;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.springframework.context.annotation.Configuration;

/**
 * Load extra cloud services to handle cloud provider
 */
public class CloudUtil {

    private final static Logger LOGGER = new Logger(CloudUtil.class);

    private final static String ENV_FLOW_CLOUD_PATH = "FLOW_CLOUD_PATH";

    private final static String PROP_FLOW_CLOUD_PATH = "cloud.path";

    private final static String CLOUD_PKG_NAME = "com.flow.platform.cloud";

    public static Set<Class<?>> loadConfigClass(ClassLoader loader) {
        return ClassUtil.load(loader, CLOUD_PKG_NAME, null, Sets.newHashSet(Configuration.class));
    }

    public static Path findPath() {
        String path = System.getenv(ENV_FLOW_CLOUD_PATH);
        if (!Strings.isNullOrEmpty(path) && Files.exists(Paths.get(path))) {
            LOGGER.trace("Load cloud jar from system env: %s", path);
            return Paths.get(path);
        }

        path = System.getProperty(PROP_FLOW_CLOUD_PATH);
        if (!Strings.isNullOrEmpty(path) && Files.exists(Paths.get(path))) {
            LOGGER.trace("Load cloud jar from system property: %s", path);
            return Paths.get(path);
        }

        return null;
    }
}
