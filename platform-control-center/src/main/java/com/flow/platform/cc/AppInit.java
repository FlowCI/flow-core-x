package com.flow.platform.cc;

import com.flow.platform.cc.config.WebConfig;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

/**
 * Created by gy@fir.im on 17/05/2017.
 *
 * Copyright fir.im
 */
public class AppInit implements WebApplicationInitializer {

    public void onStartup(ServletContext servletContext) throws ServletException {
        System.out.println("Initializing Application for " + servletContext.getServerInfo());

        // Create ApplicationContext
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();

        applicationContext.register(WebConfig.class);
        applicationContext.setServletContext(servletContext);

        // Add the servlet mapping manually and make it initialize automatically
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        ServletRegistration.Dynamic servlet = servletContext.addServlet("mvc-dispatcher", dispatcherServlet);

        servlet.addMapping("/");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(1);
    }
}
