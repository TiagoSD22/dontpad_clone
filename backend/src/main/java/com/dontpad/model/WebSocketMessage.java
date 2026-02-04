package com.dontpad.model;

/**
 * Represents a WebSocket message for content updates.
 */
public record WebSocketMessage(
    MessageType type,
    String content,
    long timestamp
) {
    public enum MessageType {
        CONTENT_UPDATE,
        INIT,
        HEARTBEAT
    }

    public static WebSocketMessage contentUpdate(String content) {
        return new WebSocketMessage(MessageType.CONTENT_UPDATE, content, System.currentTimeMillis());
    }

    public static WebSocketMessage init(String content) {
        return new WebSocketMessage(MessageType.INIT, content, System.currentTimeMillis());
    }

    public static WebSocketMessage heartbeat() {
        return new WebSocketMessage(MessageType.HEARTBEAT, null, System.currentTimeMillis());
    }
}
