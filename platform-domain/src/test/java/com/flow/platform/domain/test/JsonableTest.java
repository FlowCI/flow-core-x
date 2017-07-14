package com.flow.platform.domain.test;

import com.flow.platform.domain.Agent;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gy@fir.im on 29/05/2017.
 * Copyright fir.im
 */
public class JsonableTest {

    @Test
    public void should_to_json_all_fields() {
        // given:
        Agent agent = new Agent("zone", "name");

        String jsonString = agent.toJson();
        Assert.assertNotNull(jsonString);

        Agent fromJson = Agent.parse(jsonString, Agent.class);
        Assert.assertNotNull(fromJson);
    }
}
