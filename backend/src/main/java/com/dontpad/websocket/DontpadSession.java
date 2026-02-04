package com.dontpad.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manages WebSocket connections for a single dontpad.
 * Uses virtual threads for connection management and broadcasting.
 */
public class DontpadSession {
    private static final Logger logger = LoggerFactory.getLogger(DontpadSession.class);
    private static final long SNAPSHOT_INTERVAL_SECONDS = 10;

    private final String name;
    private final Set<WebSocketSession> connections;
    private final SnapshotCallback snapshotCallback;
    private final ScheduledExecutorService snapshotScheduler;

    public DontpadSession(String name, SnapshotCallback snapshotCallback) {
        this.name = name;
        this.connections = ConcurrentHashMap.newKeySet();
        this.snapshotCallback = snapshotCallback;
        
        // Create scheduler for periodic snapshots
        this.snapshotScheduler = new ScheduledThreadPoolExecutor(1, Thread.ofVirtual().factory());
        startSnapshotScheduler();
    }

    public void addConnection(WebSocketSession session) {
        connections.add(session);
        logger.info("Connection added to dontpad '{}'. Total: {}", name, connections.size());
    }

    public void removeConnection(WebSocketSession session) {
        connections.remove(session);
        logger.info("Connection removed from dontpad '{}'. Total: {}", name, connections.size());
        
        // If last connection, create final snapshot and shutdown scheduler
        if (connections.isEmpty()) {
            logger.info("Last connection closed for dontpad '{}'. Creating final snapshot.", name);
            snapshotCallback.createSnapshot(name);
            shutdown();
        }
    }

    /**
     * Broadcast a message to all connected clients.
     * Uses virtual thread for non-blocking broadcast.
     */
    public void broadcast(String message, WebSocketSession sender) {
        Thread.startVirtualThread(() -> {
            logger.debug("Broadcasting to {} connections for dontpad '{}'", connections.size(), name);
            for (WebSocketSession session : connections) {
                if (session != sender && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (Exception e) {
                        logger.error("Error broadcasting to client in dontpad '{}'", name, e);
                    }
                }
            }
        });
    }

    public int getConnectionCount() {
        return connections.size();
    }

    private void startSnapshotScheduler() {
        snapshotScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    logger.debug("Creating periodic snapshot for dontpad '{}'", name);
                    snapshotCallback.createSnapshot(name);
                } catch (Exception e) {
                    logger.error("Error creating snapshot for dontpad '{}'", name, e);
                }
            },
            SNAPSHOT_INTERVAL_SECONDS,
            SNAPSHOT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    private void shutdown() {
        snapshotScheduler.shutdown();
        try {
            if (!snapshotScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                snapshotScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            snapshotScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface SnapshotCallback {
        void createSnapshot(String name);
    }
}
