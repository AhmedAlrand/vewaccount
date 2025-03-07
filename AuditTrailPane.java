package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class AuditTrailPane extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditTrailPane.class);
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private TableView<AuditEntry> auditTable;

    public AuditTrailPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        initializeUI();
        // Don’t load here—call loadAuditTrail() later
    }
      @SuppressWarnings("unchecked")
    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        auditTable = new TableView<>();
        TableColumn<AuditEntry, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        TableColumn<AuditEntry, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("user"));
        TableColumn<AuditEntry, String> tableNameCol = new TableColumn<>("Table Name");
        tableNameCol.setCellValueFactory(new PropertyValueFactory<>("tableName"));
        TableColumn<AuditEntry, String> recordIdCol = new TableColumn<>("Record ID");
        recordIdCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        TableColumn<AuditEntry, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        TableColumn<AuditEntry, String> oldValueCol = new TableColumn<>("Old Value");
        oldValueCol.setCellValueFactory(new PropertyValueFactory<>("oldValue"));
        TableColumn<AuditEntry, String> newValueCol = new TableColumn<>("New Value");
        newValueCol.setCellValueFactory(new PropertyValueFactory<>("newValue"));
        auditTable.getColumns().addAll((TableColumn<AuditEntry, ?>[]) new TableColumn[] {timestampCol, userCol, tableNameCol, recordIdCol, actionCol, oldValueCol, newValueCol});
        auditTable.setPrefHeight(400);
        auditTable.setPlaceholder(new Label("No audit logs available"));

        getChildren().add(auditTable);
    }

    public void loadAuditTrail() { // Made public to call externally
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Loading audit logs...");
                List<AuditEntry> logs = dbService.getAuditLogs(1, 100);
                LOGGER.info("Loaded {} audit logs", logs.size());
                javafx.application.Platform.runLater(() -> {
                    auditTable.getItems().clear();
                    if (logs.isEmpty()) {
                        LOGGER.info("No audit logs found");
                    } else {
                        auditTable.getItems().addAll(logs);
                    }
                });
            } catch (DatabaseException e) {
                LOGGER.error("Failed to load audit trail", e);
                javafx.application.Platform.runLater(() -> 
                    new Alert(Alert.AlertType.ERROR, "Failed to load audit trail: " + e.getMessage()).showAndWait());
            } catch (Exception e) {
                LOGGER.error("Unexpected error loading audit trail", e);
                javafx.application.Platform.runLater(() -> 
                    new Alert(Alert.AlertType.ERROR, "Unexpected error: " + e.getMessage()).showAndWait());
            }
        }, FinancialManagementApp.executor);
    }
}