package com.flowci.core.job.service;

import com.flowci.core.job.domain.SessionCmd;
import org.springframework.web.socket.WebSocketHandler;

public interface SessionCmdService extends WebSocketHandler {

    void sendToAgent(SessionCmd cmd);
}
