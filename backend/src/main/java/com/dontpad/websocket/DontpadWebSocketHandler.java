package com.dontpad.websocket;

import com.dontpad.model.Dontpad;
import com.dontpad.model.WebSocketMessage;
import com.dontpad.store.DontpadStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring WebSocket handler for dontpad connections.
 * Manages sessions per dontpad and handles message routing.
 */
@Component
public class DontpadWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(DontpadWebSocketHandler.class);

    private final DontpadStore store;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, DontpadSession> sessions;

    public DontpadWebSocketHandler(DontpadStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String name = extractDontpadName(session);
        logger.info("WebSocket connect: {}", name);

        // Get or create session for this dontpad
        DontpadSession dontpadSession = sessions.computeIfAbsent(
            name,
            key -> new DontpadSession(key, store::snapshot)
        );
        dontpadSession.addConnection(session);

        // Send initial content
        Dontpad dontpad = store.getOrCreate(name);
        WebSocketMessage initMessage = WebSocketMessage.init(dontpad.getContent());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initMessage)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String name = extractDontpadName(session);
        String payload = message.getPayload();

        // Process message in virtual thread
        Thread.startVirtualThread(() -> {
            try {
                WebSocketMessage msg = objectMapper.readValue(payload, WebSocketMessage.class);

                if (msg.type() == WebSocketMessage.MessageType.CONTENT_UPDATE) {
                    // Update store
                    store.updateContent(name, msg.content());

                    // Broadcast to other clients
                    DontpadSession dontpadSession = sessions.get(name);
                    if (dontpadSession != null) {
                        dontpadSession.broadcast(payload, session);
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing message for dontpad '{}'", name, e);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String name = extractDontpadName(session);
        logger.info("WebSocket close: {}", name);

        DontpadSession dontpadSession = sessions.get(name);
        if (dontpadSession != null) {
            dontpadSession.removeConnection(session);

            // Remove session if no connections remain
            if (dontpadSession.getConnectionCount() == 0) {
                sessions.remove(name);
                logger.info("Removed session for dontpad '{}'", name);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String name = extractDontpadName(session);
        logger.error("WebSocket error for dontpad '{}'", name, exception);
    }

    private String extractDontpadName(WebSocketSession session) {
        String path = session.getUri().getPath();
        // Extract name from /ws/{name}
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
