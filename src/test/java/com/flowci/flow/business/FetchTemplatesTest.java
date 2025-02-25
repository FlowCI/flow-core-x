package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.common.config.AppProperties;
import com.flowci.common.exception.NotAvailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class FetchTemplatesTest extends SpringTest {

    @RegisterExtension
    private final static WireMockExtension WireMockServer =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private FetchTemplates fetchTemplates;

    @Autowired
    private CacheManager yamlTemplateCacheManager;

    @Test
    void whenFetchingSuccessfully_thenReturnListOfTemplates() {
        WireMockServer.stubFor(get("/git/templates.json")
                .willReturn(aResponse()
                        .withBody(getResourceAsString("flow_templates.json"))
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        appProperties.getTemplates()
                .setUrl(WireMockServer.url("/git/templates.json"));

        var templates = fetchTemplates.invoke();
        assertNotNull(templates);
        assertEquals(2, templates.size());
    }

    @Test
    void whenFetchingWith5xx_thenThrowNotAvailableException() {
        WireMockServer.stubFor(get("/git/templates.json")
                .willReturn(aResponse().withStatus(500)));

        appProperties.getTemplates()
                .setUrl(WireMockServer.url("/git/templates.json"));

        assertThrows(NotAvailableException.class, () -> fetchTemplates.invoke());
    }
}
