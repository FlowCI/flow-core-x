package com.flowci.core.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.SessionCmd;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Optional;

/**
 * @author yang
 */
@Log4j2
@Service
public class SessionCmdServiceImpl extends BinaryWebSocketHandler implements SessionCmdService {

    @Autowired
    private JobDao jobDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String s = new String(message.getPayload().array());
        log.debug("----- ws message recevied: {} {}", s, Thread.currentThread().getName());

        try {
            SessionCmd cmd = objectMapper.readValue(message.getPayload().array(), SessionCmd.class);
            sendToAgent(cmd);
        } catch (Exception e) {
            log.warn("Invalid session cmd");
        }
    }

    @Override
    public void sendToAgent(SessionCmd cmd) {
        Optional<Job> optional = jobDao.findById(cmd.getJobId());

        if (!optional.isPresent()) {
            log.warn("Invalid job id in session cmd");
            return;
        }

        log.debug("--- cmd = {}", cmd.getScript());
    }
}
