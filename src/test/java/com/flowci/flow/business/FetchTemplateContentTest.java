package com.flowci.flow.business;

import com.flowci.SpringTest;
import com.flowci.common.exception.NotAvailableException;
import com.flowci.flow.model.YamlTemplate;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class FetchTemplateContentTest extends SpringTest {

    @RegisterExtension
    private final static WireMockExtension WireMockServer =
            WireMockExtension.newInstance()
                    .options(wireMockConfig().dynamicPort())
                    .build();

    @MockBean
    private FetchTemplates fetchTemplates;

    @Autowired
    private FetchTemplateContent fetchTemplateContent;

    @Autowired
    private CacheManager yamlTemplateCacheManager;

    @Test
    void whenFetchingContentSuccessfully_thenReturnYamlContent() {
        var mockUrl = WireMockServer.url("/git/templates/helloworld.yaml");
        WireMockServer.stubFor(
                get("/git/templates/helloworld.yaml")
                        .willReturn(ok(getResourceAsString("template_helloworld.yaml"))));

        when(fetchTemplates.invoke())
                .thenReturn(List.of(
                        new YamlTemplate(
                                "helloworld",
                                "",
                                mockUrl,
                                false
                        )
                ));

        var content = fetchTemplateContent.invoke("helloworld");
        assertEquals(getResourceAsString("template_helloworld.yaml"), content);

        var cache = yamlTemplateCacheManager.getCache("template.content");
        var cached = cache.get("helloworld");
        assertNotNull(cached);
    }

    @Test
    void whenFetchingContentNotFound_thenThrowException() {
        when(fetchTemplates.invoke()).thenReturn(Collections.emptyList());
        assertThrows(NotAvailableException.class, () -> fetchTemplateContent.invoke("helloworld"));
    }
}
