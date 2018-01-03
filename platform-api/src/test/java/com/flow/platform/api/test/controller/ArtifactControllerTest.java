package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.flow.platform.api.domain.Artifact;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import java.io.IOException;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class ArtifactControllerTest extends TestBase {

    @Before
    public void before_action() throws IOException {
        stubDemo();
        createRootFlow("flow", "yml/flow.yaml");
    }

    @Test
    public void should_create_artifact_success() throws Exception {
        Artifact artifact = new Artifact("flow", "hello.apk", "http://fir.im/aabcc");
        String result = performRequestWith200Status(post("/artifacts").contentType(
            String.valueOf(ContentType.APPLICATION_JSON)).content(Jsonable.GSON_CONFIG.toJson(artifact)));
        artifact = Jsonable.parse(result, Artifact.class);

        Assert.assertNotNull(artifact);
    }

    @Test
    public void should_list_artifacts_success() throws Exception {
        Artifact artifact = new Artifact("flow", "hello.apk", "http://fir.im/aabcc");
        String result = performRequestWith200Status(post("/artifacts").contentType(
            String.valueOf(ContentType.APPLICATION_JSON)).content(Jsonable.GSON_CONFIG.toJson(artifact)));
        artifact = Jsonable.parse(result, Artifact.class);

        Assert.assertNotNull(artifact);

        result = performRequestWith200Status(get("/artifacts?flow=flow"));
        Artifact[] artifacts = Jsonable.GSON_CONFIG.fromJson(result, Artifact[].class);
        Assert.assertEquals(1, artifacts.length);
    }

}