package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.flow.platform.api.domain.Storage;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.CommandUtil.Unix;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @author yh@firim
 */
public class StorageControllerTest extends TestBase {

    private static final String CONTENT = "content";

    @Test
    public void should_create_success() throws Exception {
        String result = performRequestWith200Status(fileUpload(getUrlForStorage())
            .file(createFilePart("file", "file.zip"))
        );
        Storage storage = Jsonable.parse(result, Storage.class);

        Assert.assertNotNull(storage);
    }

    @Test
    public void should_get_success() throws Exception {
        String result = performRequestWith200Status(fileUpload(getUrlForStorage())
            .file(createFilePart("file", "file.zip"))
        );
        Storage storage = Jsonable.parse(result, Storage.class);

        Assert.assertNotNull(storage);

        String response = performRequestWith200Status(get(getUrlForStorage() + Unix.PATH_SEPARATOR + storage.getId()));
        Assert.assertNotNull(response);
        Assert.assertEquals(CONTENT, response);
    }

    private MockMultipartFile createFilePart(String paramName, String name) {
        return new MockMultipartFile(paramName, name, "zip", CONTENT.getBytes());
    }


    private String getUrlForStorage() {
        return "/storages";
    }
}