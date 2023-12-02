package com.flowci.core.common.manager;

import com.flowci.common.exception.UnsupportedException;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ResourceManager {

    private final HttpClient httpClient;

    public ResourceManager(HttpClient httpClient) {
        this.httpClient = httpClient;

    }

    public InputStream getResource(Resource r) throws Exception {
        if (r instanceof FileUrlResource) {
            return r.getInputStream();
        }

        if (r instanceof UrlResource) {
            return getResource((UrlResource) r);
        }

        throw new UnsupportedException("Unsupported resource type");
    }

    private InputStream getResource(UrlResource r) throws Exception {
        var get = HttpRequest.newBuilder(r.getURL().toURI()).GET().build();
        var response = httpClient.send(get, HttpResponse.BodyHandlers.ofInputStream());
        return response.body();
    }
}
