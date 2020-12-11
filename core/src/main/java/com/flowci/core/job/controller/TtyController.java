package com.flowci.core.job.controller;

import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.auth.WebAuth;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.event.TtyStatusUpdateEvent;
import com.flowci.core.job.service.TtyService;
import com.flowci.core.user.domain.User;
import com.flowci.exception.AuthenticationException;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Log4j2
@Controller
public class TtyController {

    @Autowired
    private WebAuth webAuth;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private TtyService ttyService;

    @MessageExceptionHandler(AuthenticationException.class)
    public void onAuthException(AuthenticationException e) {
        TtyCmd.In in = (TtyCmd.In) e.getExtra();

        TtyCmd.Out out = new TtyCmd.Out()
                .setId(in.getId())
                .setAction(in.getAction())
                .setSuccess(false)
                .setError(e.getMessage());

        eventManager.publish(new TtyStatusUpdateEvent(this, out));
    }

    @MessageMapping("/tty/{jobId}/{b64Path}/open")
    public void open(@DestinationVariable String jobId, @DestinationVariable String b64Path, MessageHeaders headers) {
        TtyCmd.In in = new TtyCmd.In()
                .setId(jobId)
                .setNodePath(StringHelper.fromBase64(b64Path))
                .setAction(TtyCmd.Action.OPEN);

        validate(in, headers);
        ttyService.execute(in);
    }

    @MessageMapping("/tty/{jobId}/{b64Path}/shell")
    public void shell(@DestinationVariable String jobId,
                      @DestinationVariable String b64Path,
                      @Payload String script,
                      MessageHeaders headers) {
        TtyCmd.In in = new TtyCmd.In()
                .setId(jobId)
                .setNodePath(StringHelper.fromBase64(b64Path))
                .setAction(TtyCmd.Action.SHELL)
                .setInput(script);

        validate(in, headers);
        ttyService.execute(in);
    }

    @MessageMapping("/tty/{jobId}/{b64Path}close")
    public void close(@DestinationVariable String jobId, @DestinationVariable String b64Path, MessageHeaders headers) {
        TtyCmd.In in = new TtyCmd.In()
                .setId(jobId)
                .setNodePath(StringHelper.fromBase64(b64Path))
                .setAction(TtyCmd.Action.CLOSE);

        validate(in, headers);
        ttyService.execute(in);
    }

    private void validate(TtyCmd.In in, MessageHeaders headers) {
        try {
            User user = webAuth.validate(headers);
            if (!user.isAdmin()) {
                throw new AuthenticationException("Admin permission is required");
            }
        } catch (AuthenticationException e) {
            e.setExtra(in);
            throw e;
        }
    }
}
