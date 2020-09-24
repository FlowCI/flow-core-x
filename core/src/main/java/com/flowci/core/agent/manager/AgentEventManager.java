package com.flowci.core.agent.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.event.OnConnectedEvent;
import com.flowci.core.agent.event.OnDisconnectedEvent;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.common.manager.SpringEventManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle event from agent via websocket
 */

@Log4j2
@Component
public class AgentEventManager extends BinaryWebSocketHandler {

    private final static int EventLength = 10;

    private final static String EventConnect = "connect___";

    private final static String EventShellLog = "slog______";

    private final static String EventTTYLog = "tlog______";

    private final static String HeaderToken = "Token";

    private final Map<String, WebSocketSession> agentSessionStore = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringEventManager eventManager;

    public <T> void writeMessage(String token, ResponseMessage<T> msg) {
        WebSocketSession session = agentSessionStore.get(token);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(msg);
            session.sendMessage(new BinaryMessage(bytes));
        } catch (IOException e) {
            log.warn("Unable to write response message for agent {}: {}", token, e.getMessage());
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // ignore, handle connect event on connect event
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] bytes = message.getPayload().array();

        if (bytes.length < (EventLength + 2)) {
            log.warn("invalid binary message");
            return;
        }

        String token = getToken(session);
        String event = getEvent(bytes);
        String body = getBody(bytes);

        if (EventConnect.equals(event)) {
            onConnected(session, token, body);
            return;
        }

        if (EventShellLog.equals(event)) {
            return;
        }

        if (EventTTYLog.equals(event)) {
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // ignore, handled in afterConnectionClosed
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String token = getToken(session);
        eventManager.publish(new OnDisconnectedEvent(this, token, session));
        agentSessionStore.remove(token, session);
    }

    private void onConnected(WebSocketSession session, String token, String body) {
        try {
            AgentInit init = objectMapper.readValue(body, AgentInit.class);
            Objects.requireNonNull(init.getStatus(), "Agent status is missing");

            init.setToken(token);
            init.setIp(session.getRemoteAddress() == null ? null : session.getRemoteAddress().toString());

            eventManager.publish(new OnConnectedEvent(this, token, session, init));
            agentSessionStore.put(token, session);
            writeMessage(token, new ResponseMessage<Void>(StatusCode.OK, null));
            log.debug("Agent {} is connected with status {}", token, init.getStatus());
        } catch (Exception e) {
            log.warn(e);
            writeMessage(token, new ResponseMessage<Void>(StatusCode.FATAL, e.getMessage(), null));
        }
    }

    private static String getToken(WebSocketSession session) {
        return session.getHandshakeHeaders().get(HeaderToken).get(0);
    }

    private static String getEvent(byte[] bytes) {
        return new String(Arrays.copyOf(bytes, EventLength)).trim();
    }

    private static String getBody(byte[] bytes) {
        return new String(Arrays.copyOfRange(bytes, EventLength + 1, bytes.length)).trim();
    }
}
