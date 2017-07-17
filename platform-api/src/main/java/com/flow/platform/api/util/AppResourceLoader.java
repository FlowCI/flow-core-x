package com.flow.platform.api.util;

import com.google.common.base.Strings;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * The context resource cloud load from
 * - System env
 * - System property
 * - Default file location
 * - Classpath
 * - Default from classpath
 *
 * Created by gy@fir.im on 14/07/2017.
 * Copyright fir.im
 */
public abstract class AppResourceLoader {

    public abstract String getEnvName();

    public abstract String getPropertyName();

    public abstract String getDefaultDir();

    public abstract String getClassPath();

    public abstract String getDefault();

    public abstract void register(ConfigurableApplicationContext context);

    public Resource find() {
        // find property file path from system env
        String envName = getEnvName();
        if (envName != null) {
            String path = System.getenv(envName);
            if (!Strings.isNullOrEmpty(path) && Files.exists(Paths.get(path))) {
                return new FileSystemResource(path);
            }
        }

        // find property file path from property
        String propertyName = getPropertyName();
        if (propertyName != null) {
            String path = System.getProperty(propertyName);
            if (!Strings.isNullOrEmpty(path) && Files.exists(Paths.get(path))) {
                return new FileSystemResource(path);
            }
        }

        // find property file path from default directory
        String dirName = getDefaultDir();
        if (dirName != null) {
            String path = getDefaultDir();
            if (Files.exists(Paths.get(path))) {
                return new FileSystemResource(path);
            }
        }

        // find default app-test.properties from class path
        String classPath = getClassPath();
        if (classPath != null) {
            ClassPathResource classPathResource = new ClassPathResource(classPath);
            if (classPathResource.isReadable()) {
                return classPathResource;
            }
        }

        // apply default config
        String aDefault = getDefault();
        if (aDefault != null) {
            ClassPathResource defaultResource = new ClassPathResource(aDefault);
            if (defaultResource.isReadable()) {
                return defaultResource;
            }
        }

        return null;
    }
}