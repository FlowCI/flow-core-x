package com.flowci.core.job.controller;

import com.flowci.core.job.service.TtyService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Log4j2
@Controller
public class TtyController {

    @Autowired
    private TtyService ttyService;

    @MessageMapping("/tty/{jobId}/open")
    public void open(@DestinationVariable String jobId) {
        ttyService.open(jobId);
    }

    @MessageMapping("/tty/{jobId}/shell")
    public void shell(@DestinationVariable String jobId, @Payload String script) {
        ttyService.shell(jobId, script);
    }

    @MessageMapping("/tty/{jobId}/close")
    public void close(@DestinationVariable String jobId) {
        ttyService.close(jobId);
    }
}
