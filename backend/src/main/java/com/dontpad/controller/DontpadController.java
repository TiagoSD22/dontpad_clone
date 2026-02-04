package com.dontpad.controller;

import com.dontpad.model.Dontpad;
import com.dontpad.store.DontpadStore;
import com.dontpad.websocket.DontpadWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for dontpad HTTP endpoints.
 */
@RestController
@CrossOrigin(origins = "*")
public class DontpadController {

    private final DontpadStore store;
    private final DontpadWebSocketHandler webSocketHandler;

    @Autowired
    public DontpadController(DontpadStore store, DontpadWebSocketHandler webSocketHandler) {
        this.store = store;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Get or create a dontpad by name.
     */
    @GetMapping("/dontpad/{name}")
    public DontpadResponse getDontpad(@PathVariable String name) {
        Dontpad dontpad = store.getOrCreate(name);
        return new DontpadResponse(
            dontpad.getName(),
            dontpad.getContent(),
            dontpad.getLastModified()
        );
    }

    /**
     * Get server statistics.
     */
    @GetMapping("/api/stats")
    public StatsResponse getStats() {
        return new StatsResponse(
            store.size(),
            webSocketHandler.getActiveSessionCount()
        );
    }

    public record DontpadResponse(
        String name,
        String content,
        Instant lastModified
    ) {}

    public record StatsResponse(
        int totalDontpads,
        int activeSessions
    ) {}
}
