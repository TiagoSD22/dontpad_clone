package com.dontpad.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a dontpad document with its content, metadata, and snapshot history.
 */
public class Dontpad {
    private final String name;
    private String content;
    private Instant lastModified;
    private final List<Snapshot> snapshotHistory;

    public Dontpad(String name) {
        this.name = name;
        this.content = "";
        this.lastModified = Instant.now();
        this.snapshotHistory = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public synchronized void setContent(String content) {
        this.content = content;
        this.lastModified = Instant.now();
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public List<Snapshot> getSnapshotHistory() {
        return snapshotHistory;
    }

    public synchronized void addSnapshot() {
        snapshotHistory.add(new Snapshot(content, Instant.now()));
    }

    public static record Snapshot(String content, Instant timestamp) {}
}
