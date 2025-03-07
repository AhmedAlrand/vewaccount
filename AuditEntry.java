package com.example.financial;

public class AuditEntry {
    private final String timestamp;
    private final String user;
    private final String tableName;
    private final String recordId;
    private final String action;
    private final String oldValue;
    private final String newValue;

    public AuditEntry(String timestamp, String user, String tableName, String recordId, String action, String oldValue, String newValue) {
        this.timestamp = timestamp;
        this.user = user;
        this.tableName = tableName;
        this.recordId = recordId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getTableName() {
        return tableName;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return timestamp + " - " + user + ": " + action + " on " + tableName + " (ID: " + recordId + ")";
    }
}