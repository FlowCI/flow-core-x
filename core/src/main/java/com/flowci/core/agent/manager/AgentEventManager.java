package com.flowci.core.agent.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.domain.ShellLog;
import com.flowci.core.agent.domain.TtyCmd;
import com.flowci.core.agent.event.*;
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

    private final static String EventCmdOut = "cmd_out___";

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
            writeMessage(session, bytes);
        } catch (IOException e) {
            log.warn("Unable to write response message for agent {}: {}", token, e.getMessage());
        }
    }

    public void writeMessage(String token, byte[] bytes) {
        WebSocketSession session = agentSessionStore.get(token);
        if (session == null) {
            log.warn("Agent {} not connected", token);
            return;
        }
        writeMessage(session, bytes);
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
        byte[] body = getBody(bytes);

        if (EventConnect.equals(event)) {
            onConnected(session, token, body);
            return;
        }

        if (EventCmdOut.equals(event)) {
            onCmdOut(token, body);
            return;
        }

        if (EventShellLog.equals(event)) {
            onShellLog(body);
            return;
        }

        if (EventTTYLog.equals(event)) {
            onTtyLog(body);
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

    private <T> void writeMessage(WebSocketSession session, ResponseMessage<T> msg) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(msg);
            writeMessage(session, bytes);
        } catch (IOException e) {
            log.warn(e);
        }
    }

    private void writeMessage(WebSocketSession session, byte[] body) {
        synchronized (session) {
            try {
                session.sendMessage(new BinaryMessage(body));
            } catch (IOException e) {
                log.warn(e);
            }
        }
    }

    private void onConnected(WebSocketSession session, String token, byte[] body) {
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
            writeMessage(session, new ResponseMessage<Void>(StatusCode.FATAL, e.getMessage(), null));
        }
    }

    private void onCmdOut(String token, byte[] body) {
        try {
            eventManager.publish(new OnCmdOutEvent(this, body));
            log.debug("Agent {} got cmd back: {}", token, new String(body));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    private void onShellLog(byte[] body) {
        try {
            ShellLog item = objectMapper.readValue(body, ShellLog.class);
            eventManager.publish(new OnShellLogEvent(this, item.getJobId(), item.getStepId(), item.getLog()));
        } catch (IOException e) {
            log.warn(e);
        }
    }

    private void onTtyLog(byte[] body) {
        try {
            TtyCmd.Log item = objectMapper.readValue(body, TtyCmd.Log.class);
            eventManager.publish(new OnTTYLogEvent(this, item.getId(), item.getLog()));
        } catch (IOException e) {
            log.warn(e);
        }
    }

    private static String getToken(WebSocketSession session) {
        return session.getHandshakeHeaders().get(HeaderToken).get(0);
    }

    private static String getEvent(byte[] bytes) {
        return new String(Arrays.copyOf(bytes, EventLength)).trim();
    }

    private static byte[] getBody(byte[] bytes) {
        return Arrays.copyOfRange(bytes, EventLength + 1, bytes.length);
    }
}
