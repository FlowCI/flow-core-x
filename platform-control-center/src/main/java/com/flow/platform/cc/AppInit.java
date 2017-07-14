package com.flow.platform.cc;

import com.flow.platform.cc.config.WebConfig;
import com.flow.platform.cc.resource.AppResourceLoader;
import com.flow.platform.cc.resource.CloudJarResourceLoader;
import com.flow.platform.cc.resource.PropertyResourceLoader;
import com.flow.platform.util.Logger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
public class AppInit implements WebApplicationInitializer {

    private final static Logger LOGGER = new Logger(AppInit.class);

    public void onStartup(ServletContext servletContext) throws ServletException {
        LOGGER.trace("Initializing Application for %s", servletContext.getServerInfo());

        // get class loader
        ClassLoader appClassLoader = appClassLoader();

        // Create ApplicationContext and set class loader
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setClassLoader(appClassLoader);

        applicationContext.register(WebConfig.class);
        Set<Class<?>> cloudClasses = CloudJarResourceLoader.configClasses(appClassLoader);
        if (!cloudClasses.isEmpty()) {
            applicationContext.register((cloudClasses.toArray(new Class<?>[cloudClasses.size()])));
        }

        applicationContext.setServletContext(servletContext);

        // set app property resource
        AppResourceLoader propertyLoader = new PropertyResourceLoader();
        propertyLoader.register(applicationContext);

        // Add the servlet mapping manually and make it initialize automatically
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        ServletRegistration.Dynamic servlet = servletContext.addServlet("mvc-dispatcher", dispatcherServlet);

        servlet.addMapping("/");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(1);
    }

    private ClassLoader appClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            URL[] urls = CloudJarResourceLoader.findAllJar();
            if (urls.length > 0) {
                return new URLClassLoader(urls, loader);
            }
            return loader;
        } catch (Throwable ignore) {
            return loader;
        }
    }
}
