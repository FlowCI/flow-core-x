package com.flowci.core.job.controller;

import com.flowci.core.auth.WebAuth;
import com.flowci.core.job.service.TtyService;
import com.flowci.core.user.domain.User;
import com.flowci.exception.AuthenticationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Log4j2
@Controller
public class TtyController {

    @Autowired
    private WebAuth webAuth;

    @Autowired
    private TtyService ttyService;

    @MessageExceptionHandler(AuthenticationException.class)
    @SendToUser("/topic/tty/errors")
    public Exception handleExceptions(AuthenticationException e) {
        return e;
    }

    @MessageMapping("/tty/{jobId}/open")
    public void open(@DestinationVariable String jobId, MessageHeaders headers) {
        validate(headers);
        ttyService.open(jobId);
    }

    @MessageMapping("/tty/{jobId}/shell")
    public void shell(@DestinationVariable String jobId, @Payload String script, MessageHeaders headers) {
        validate(headers);
        ttyService.shell(jobId, script);
    }

    @MessageMapping("/tty/{jobId}/close")
    public void close(@DestinationVariable String jobId, MessageHeaders headers) {
        validate(headers);
        ttyService.close(jobId);
    }

    private void validate(MessageHeaders headers) {
        User user = webAuth.validate(headers);
        if (!user.isAdmin()) {
            throw new AuthenticationException("Admin permission is required");
        }
    }
}
