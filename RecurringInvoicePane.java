package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class RecurringInvoicePane extends VBox {
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private TableView<RecurringInvoice> recurringTable;
    private TextField invoiceIdField, frequencyIntervalField;
    private ComboBox<String> frequencyTypeCombo;
    private Spinner<LocalDate> nextDateSpinner, endDateSpinner;

    public RecurringInvoicePane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        initializeUI();
        loadRecurringInvoices();
    }

    @SuppressWarnings("unchecked")
    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        recurringTable = new TableView<>();
        TableColumn<RecurringInvoice, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<RecurringInvoice, String> invoiceIdCol = new TableColumn<>("Invoice ID");
        invoiceIdCol.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        TableColumn<RecurringInvoice, String> frequencyCol = new TableColumn<>("Frequency");
        frequencyCol.setCellValueFactory(new PropertyValueFactory<>("frequencyType"));
        TableColumn<RecurringInvoice, Integer> intervalCol = new TableColumn<>("Interval");
        intervalCol.setCellValueFactory(new PropertyValueFactory<>("frequencyInterval"));
        TableColumn<RecurringInvoice, String> nextDateCol = new TableColumn<>("Next Date");
        nextDateCol.setCellValueFactory(new PropertyValueFactory<>("nextDate"));
        TableColumn<RecurringInvoice, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        TableColumn<RecurringInvoice, String> lastGeneratedCol = new TableColumn<>("Last Generated");
        lastGeneratedCol.setCellValueFactory(new PropertyValueFactory<>("lastGenerated"));
        recurringTable.getColumns().addAll((TableColumn<RecurringInvoice, ?>[]) new TableColumn[] {idCol, invoiceIdCol, frequencyCol, intervalCol, nextDateCol, endDateCol, lastGeneratedCol});
        recurringTable.setPrefHeight(300);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        inputGrid.add(new Label(messages.getString("invoiceId")), 0, 0);
        invoiceIdField = new TextField();
        inputGrid.add(invoiceIdField, 1, 0);

        inputGrid.add(new Label(messages.getString("frequencyType")), 0, 1);
        frequencyTypeCombo = new ComboBox<>();
        frequencyTypeCombo.getItems().addAll("DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "YEARLY");
        frequencyTypeCombo.setValue("MONTHLY");
        inputGrid.add(frequencyTypeCombo, 1, 1);

        inputGrid.add(new Label(messages.getString("frequencyInterval")), 0, 2);
        frequencyIntervalField = new TextField("1");
        inputGrid.add(frequencyIntervalField, 1, 2);

        inputGrid.add(new Label(messages.getString("nextDate")), 0, 3);
        nextDateSpinner = new Spinner<>(new LocalDateSpinnerValueFactory());
        nextDateSpinner.getValueFactory().setValue(LocalDate.now());
        inputGrid.add(nextDateSpinner, 1, 3);

        inputGrid.add(new Label(messages.getString("endDate")), 0, 4);
        endDateSpinner = new Spinner<>(new LocalDateSpinnerValueFactory());
        endDateSpinner.getValueFactory().setValue(null);
        inputGrid.add(endDateSpinner, 1, 4);

        Button addButton = new Button(messages.getString("addRecurring"));
        addButton.setOnAction(e -> addRecurringInvoice());

        getChildren().addAll(recurringTable, inputGrid, addButton);
    }

    private void loadRecurringInvoices() {
        CompletableFuture.runAsync(() -> {
            try {
                List<RecurringInvoice> invoices = dbService.getRecurringInvoices(1, 100);
                javafx.application.Platform.runLater(() -> {
                    recurringTable.getItems().clear();
                    recurringTable.getItems().addAll(invoices);
                });
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to load recurring invoices", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void addRecurringInvoice() {
        CompletableFuture.runAsync(() -> {
            try {
                String invoiceId = invoiceIdField.getText();
                String frequencyType = frequencyTypeCombo.getValue();
                int frequencyInterval = Integer.parseInt(frequencyIntervalField.getText());
                String nextDate = nextDateSpinner.getValue().toString();
                String endDate = endDateSpinner.getValue() != null ? endDateSpinner.getValue().toString() : null;

                dbService.saveRecurringInvoice(invoiceId, frequencyType, frequencyInterval, nextDate, endDate, false);
                auditService.logAction("user", "recurring_invoices", invoiceId, "Added recurring invoice", null, null);
                javafx.application.Platform.runLater(() -> {
                    loadRecurringInvoices();
                    invoiceIdField.setText("");
                    frequencyIntervalField.setText("1");
                    nextDateSpinner.getValueFactory().setValue(LocalDate.now());
                    endDateSpinner.getValueFactory().setValue(null);
                });
            } catch (Exception e) {
                ErrorHandler.handleException(e, "Failed to add recurring invoice", null);
            }
        }, FinancialManagementApp.executor);
    }

    // Custom SpinnerValueFactory for LocalDate
    private static class LocalDateSpinnerValueFactory extends SpinnerValueFactory<LocalDate> {
        public LocalDateSpinnerValueFactory() {
            setValue(LocalDate.now());
        }

        @Override
        public void decrement(int steps) {
            setValue(getValue().minusDays(steps));
        }

        @Override
        public void increment(int steps) {
            setValue(getValue().plusDays(steps));
        }
    }
}