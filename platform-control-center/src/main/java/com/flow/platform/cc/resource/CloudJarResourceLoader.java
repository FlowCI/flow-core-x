package com.flow.platform.cc.resource;

import com.flow.platform.util.ClassUtil;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Created by gy@fir.im on 14/07/2017.
 * Copyright fir.im
 */
public class CloudJarResourceLoader extends AppResourceLoader {

    private final static String ENV_FLOW_CLOUD_PATH = "FLOW_CLOUD_PATH";

    private final static String PROP_FLOW_CLOUD_PATH = "cloud.path";

    private final static String DEFAULT_CONFIG_PATH = "/etc/flow.ci/cloud";

    private final static String CLOUD_PKG_NAME = "com.flow.platform.cloud";

    @Override
    public String getEnvName() {
        return ENV_FLOW_CLOUD_PATH;
    }

    @Override
    public String getPropertyName() {
        return PROP_FLOW_CLOUD_PATH;
    }

    @Override
    public String getDefaultDir() {
        return DEFAULT_CONFIG_PATH;
    }

    @Override
    public String getClassPath() {
        return null;
    }

    @Override
    public String getDefault() {
        return null;
    }

    @Override
    public void register(ConfigurableApplicationContext context) {
        // ignore
    }

    public static URL[] findAllJar() {
        Resource resource = new CloudJarResourceLoader().find();
        if (resource == null) {
            return new URL[0];
        }

        try {
            File resourceDir = resource.getFile();
            File[] files = resourceDir.listFiles();
            if (!resourceDir.isDirectory() || files == null) {
                return new URL[0];
            }

            List<URL> urls = new ArrayList<>(files.length);
            for (File file : files) {
                try {
                    new JarFile(file); // check is jar file
                    urls.add(file.toURI().toURL());
                } catch (IOException ignore) {
                }
            }

            return urls.toArray(new URL[urls.size()]);
        } catch (IOException ignore) {
            return new URL[0];
        }
    }

    public static Set<Class<?>> configClasses(ClassLoader loader) {
        return ClassUtil.load(loader, CLOUD_PKG_NAME, null, Sets.newHashSet(Configuration.class));
    }
}
