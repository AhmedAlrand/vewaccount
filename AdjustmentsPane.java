package com.example.financial;


import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class AdjustmentsPane extends VBox {
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private TableView<Adjustment> adjustmentsTable;
    private TextField typeField, descriptionField, amountField, accountFromField, accountToField, dateField, currencyField, exchangeRateField;

    public AdjustmentsPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        initializeUI();
        loadAdjustments();
    }

	@SuppressWarnings("unchecked")
    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        adjustmentsTable = new TableView<>();
        TableColumn<Adjustment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Adjustment, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<Adjustment, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Adjustment, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Adjustment, Integer> accountFromCol = new TableColumn<>("From Account");
        accountFromCol.setCellValueFactory(new PropertyValueFactory<>("accountFrom"));
        TableColumn<Adjustment, Integer> accountToCol = new TableColumn<>("To Account");
        accountToCol.setCellValueFactory(new PropertyValueFactory<>("accountTo"));
        TableColumn<Adjustment, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Adjustment, String> currencyCol = new TableColumn<>("Currency");
        currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        TableColumn<Adjustment, Double> exchangeRateCol = new TableColumn<>("Exchange Rate");
        exchangeRateCol.setCellValueFactory(new PropertyValueFactory<>("exchangeRate"));
        adjustmentsTable.getColumns().addAll((TableColumn<Adjustment, ?>[]) new TableColumn[] {idCol, typeCol, descriptionCol, amountCol, accountFromCol, accountToCol, dateCol, currencyCol, exchangeRateCol});
        //adjustmentsTable.getColumns().addAll(idCol, typeCol, descriptionCol, amountCol, accountFromCol, accountToCol, dateCol, currencyCol, exchangeRateCol);
        adjustmentsTable.setPrefHeight(300);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        inputGrid.add(new Label(messages.getString("type")), 0, 0);
        typeField = new TextField();
        inputGrid.add(typeField, 1, 0);

        inputGrid.add(new Label(messages.getString("description")), 0, 1);
        descriptionField = new TextField();
        inputGrid.add(descriptionField, 1, 1);

        inputGrid.add(new Label(messages.getString("amount")), 0, 2);
        amountField = new TextField();
        inputGrid.add(amountField, 1, 2);

        inputGrid.add(new Label(messages.getString("accountFrom")), 0, 3);
        accountFromField = new TextField();
        inputGrid.add(accountFromField, 1, 3);

        inputGrid.add(new Label(messages.getString("accountTo")), 0, 4);
        accountToField = new TextField();
        inputGrid.add(accountToField, 1, 4);

        inputGrid.add(new Label(messages.getString("date")), 0, 5);
        dateField = new TextField();
        inputGrid.add(dateField, 1, 5);

        inputGrid.add(new Label(messages.getString("currency")), 0, 6);
        currencyField = new TextField("USD");
        inputGrid.add(currencyField, 1, 6);

        inputGrid.add(new Label(messages.getString("exchangeRate")), 0, 7);
        exchangeRateField = new TextField("1.0");
        inputGrid.add(exchangeRateField, 1, 7);

        Button addButton = new Button(messages.getString("addAdjustment"));
        addButton.setOnAction(e -> addAdjustment());

        getChildren().addAll(adjustmentsTable, inputGrid, addButton);
    }

    private void loadAdjustments() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Adjustment> adjustments = dbService.getAdjustments(1, 100);
                javafx.application.Platform.runLater(() -> {
                    adjustmentsTable.getItems().clear();
                    adjustmentsTable.getItems().addAll(adjustments);
                });
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to load adjustments", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void addAdjustment() {
        CompletableFuture.runAsync(() -> {
            try {
                String type = typeField.getText();
                String description = descriptionField.getText();
                double amount = Double.parseDouble(amountField.getText());
                Integer accountFrom = accountFromField.getText().isEmpty() ? null : Integer.parseInt(accountFromField.getText());
                Integer accountTo = accountToField.getText().isEmpty() ? null : Integer.parseInt(accountToField.getText());
                String date = dateField.getText();
                String currency = currencyField.getText();
                double exchangeRate = Double.parseDouble(exchangeRateField.getText());

                dbService.addAdjustment(type, description, amount, accountFrom, accountTo, date, currency, exchangeRate);
                auditService.logAction("user", "adjustments", null, "Added adjustment: " + description, null, null);
                javafx.application.Platform.runLater(() -> {
                    loadAdjustments();
                    clearFields();
                });
            } catch (Exception e) {
                ErrorHandler.handleException(e, "Failed to add adjustment", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void clearFields() {
        typeField.setText("");
        descriptionField.setText("");
        amountField.setText("");
        accountFromField.setText("");
        accountToField.setText("");
        dateField.setText("");
        currencyField.setText("USD");
        exchangeRateField.setText("1.0");
    }
}