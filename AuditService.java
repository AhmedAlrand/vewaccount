package com.example.financial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);
    private final DatabaseService dbService;

    public AuditService(DatabaseService dbService) {
        this.dbService = dbService;
    }

    public void logAction(String user, String tableName, String recordId, String action, String oldValue, String newValue) {
        try (Connection conn = DatabaseService.dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO AuditLog (`user`, tableName, recordId, action, oldValue, newValue, timestamp) " +
                 "VALUES (?, ?, ?, ?, ?, ?, NOW())")) {
            stmt.setString(1, user);
            stmt.setString(2, tableName);
            stmt.setString(3, recordId);
            stmt.setString(4, action);
            stmt.setString(5, oldValue);
            stmt.setString(6, newValue);
            stmt.executeUpdate();
            LOGGER.info("Audit log entry created: {} by {} on {} (ID: {})", action, user, tableName, recordId);
        } catch (SQLException e) {
            LOGGER.error("Failed to log audit action", e);
        }
    }
}