package com.flowci.core.test.job;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.service.LoggingService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.test.SpringScenario;
import com.flowci.store.FileManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LoggingServiceTest extends SpringScenario {

    @Autowired
    private LoggingService loggingService;

    @MockBean
    private StepService stepService;

    @Autowired
    private FileManager fileManager;

    private String fileKey;

    @Test
    void should_save_log_to_dir() throws IOException {
        // init:
        String cmdId = "dummy-cmd-id";

        // mock
        Step dummy = new Step();
        dummy.setFlowId("flowid");
        dummy.setBuildNumber(1L);
        Mockito.when(stepService.get(cmdId)).thenReturn(dummy);

        // when:
        String fileName = cmdId + ".log";
        var file = new MockMultipartFile(fileName, fileName, null, load("flow.yml"));

        fileKey = loggingService.save(file);
        assertNotNull(fileKey);

        // then:
        Resource resource = loggingService.get(cmdId);
        assertNotNull(resource);
    }

    @AfterEach
    void remove() throws IOException {
        if (StringHelper.hasValue(fileKey)) {
            fileManager.remove(fileKey);
        }
    }
}
