package com.dontpad.store;

import com.dontpad.model.Dontpad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for dontpad documents using ConcurrentHashMap.
 * Thread-safe for concurrent access from multiple virtual threads.
 */
public class DontpadStore {
    private static final Logger logger = LoggerFactory.getLogger(DontpadStore.class);
    private final ConcurrentHashMap<String, Dontpad> store;

    public DontpadStore() {
        this.store = new ConcurrentHashMap<>();
    }

    /**
     * Get or create a dontpad by name.
     */
    public Dontpad getOrCreate(String name) {
        return store.computeIfAbsent(name, key -> {
            logger.info("Creating new dontpad: {}", key);
            return new Dontpad(key);
        });
    }

    /**
     * Get a dontpad by name, return null if not found.
     */
    public Dontpad get(String name) {
        return store.get(name);
    }

    /**
     * Update the content of a dontpad.
     */
    public void updateContent(String name, String content) {
        Dontpad dontpad = getOrCreate(name);
        dontpad.setContent(content);
        logger.debug("Updated dontpad: {} (length: {})", name, content.length());
    }

    /**
     * Create a snapshot for a dontpad.
     */
    public void snapshot(String name) {
        Dontpad dontpad = get(name);
        if (dontpad != null) {
            dontpad.addSnapshot();
            logger.info("Created snapshot for: {} (total snapshots: {})", 
                name, dontpad.getSnapshotHistory().size());
        }
    }

    /**
     * Get the number of active dontpads.
     */
    public int size() {
        return store.size();
    }
}
