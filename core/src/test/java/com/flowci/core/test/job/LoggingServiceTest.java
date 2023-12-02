package com.flowci.core.test.job;

import com.flowci.core.job.service.LoggingService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.job.domain.Step;
import com.flowci.store.FileManager;
import com.flowci.common.helper.StringHelper;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;

public class LoggingServiceTest extends SpringScenario {

    @Autowired
    private LoggingService loggingService;

    @MockBean
    private StepService stepService;

    @Autowired
    private FileManager fileManager;

    private String fileKey;

    @Test
    public void should_save_log_to_dir() throws IOException {
        // init:
        String cmdId = "dummy-cmd-id";

        // mock
        Step dummy = new Step();
        dummy.setFlowId("flowid");
        dummy.setBuildNumber(1L);
        Mockito.when(stepService.get(cmdId)).thenReturn(dummy);

        // when:
        String fileName = cmdId + ".log";
        fileKey = loggingService.save(fileName, load("flow.yml"));
        Assert.assertNotNull(fileKey);

        // then:
        Resource resource = loggingService.get(cmdId);
        Assert.assertNotNull(resource);
    }

    @After
    public void remove() throws IOException {
        if (StringHelper.hasValue(fileKey)) {
            fileManager.remove(fileKey);
        }
    }
}
