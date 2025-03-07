package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class ExpensesPane extends VBox {
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private TableView<Expense> expensesTable;
    private TextField descriptionField, amountField, currencyField, categoryField, dateField,
                      budgetAmountField, budgetStartDateField, budgetEndDateField;

    public ExpensesPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        initializeUI();
        loadExpenses();
    }
       @SuppressWarnings("unchecked")
    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        expensesTable = new TableView<>();
        TableColumn<Expense, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Expense, String> currencyCol = new TableColumn<>("Currency");
        currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        expensesTable.getColumns().addAll((TableColumn<Expense, ?>[]) new TableColumn[] {descriptionCol, amountCol, currencyCol, categoryCol, dateCol});
        expensesTable.setPrefHeight(300);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        inputGrid.add(new Label(messages.getString("description")), 0, 0);
        descriptionField = new TextField();
        inputGrid.add(descriptionField, 1, 0);

        inputGrid.add(new Label(messages.getString("amount")), 0, 1);
        amountField = new TextField();
        inputGrid.add(amountField, 1, 1);

        inputGrid.add(new Label(messages.getString("currency")), 0, 2);
        currencyField = new TextField("USD");
        inputGrid.add(currencyField, 1, 2);

        inputGrid.add(new Label(messages.getString("category")), 0, 3);
        categoryField = new TextField();
        inputGrid.add(categoryField, 1, 3);

        inputGrid.add(new Label(messages.getString("date")), 0, 4);
        dateField = new TextField(new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        inputGrid.add(dateField, 1, 4);

        inputGrid.add(new Label(messages.getString("budgetAmount")), 0, 5);
        budgetAmountField = new TextField();
        inputGrid.add(budgetAmountField, 1, 5);

        inputGrid.add(new Label(messages.getString("budgetStartDate")), 0, 6);
        budgetStartDateField = new TextField(new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
        inputGrid.add(budgetStartDateField, 1, 6);

        inputGrid.add(new Label(messages.getString("budgetEndDate")), 0, 7);
        budgetEndDateField = new TextField("");
        inputGrid.add(budgetEndDateField, 1, 7);

        HBox buttonBox = new HBox(10);
        Button addButton = new Button(messages.getString("addExpense"));
        addButton.setOnAction(e -> addExpense());
        Button setBudgetButton = new Button(messages.getString("setBudget"));
        setBudgetButton.setOnAction(e -> setBudget());
        buttonBox.getChildren().addAll(addButton, setBudgetButton);

        getChildren().addAll(expensesTable, inputGrid, buttonBox);
    }

    private void loadExpenses() {
        CompletableFuture.runAsync(() -> {
            try {
                List<Expense> expenses = dbService.getExpenses(1, 100);
                javafx.application.Platform.runLater(() -> {
                    expensesTable.getItems().clear();
                    expensesTable.getItems().addAll(expenses);
                });
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to load expenses", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void addExpense() {
        CompletableFuture.runAsync(() -> {
            try {
                String description = descriptionField.getText();
                double amount = Double.parseDouble(amountField.getText());
                String currency = currencyField.getText();
                String category = categoryField.getText();
                String date = dateField.getText();
                dbService.addExpense(description, amount, currency, category, date);
                auditService.logAction("user", "expenses", null, "Added expense: " + description, null, null);
                javafx.application.Platform.runLater(() -> {
                    loadExpenses();
                    descriptionField.setText("");
                    amountField.setText("");
                    currencyField.setText("USD");
                    categoryField.setText("");
                    dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
                });
            } catch (Exception e) {
                ErrorHandler.handleException(e, "Failed to add expense", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void setBudget() {
        CompletableFuture.runAsync(() -> {
            try {
                String category = categoryField.getText();
                double amount = Double.parseDouble(budgetAmountField.getText());
                String startDate = budgetStartDateField.getText();
                String endDate = budgetEndDateField.getText().isEmpty() ? LocalDate.now().plusMonths(1).toString() : budgetEndDateField.getText();
                dbService.setBudget(category, amount, startDate, endDate);
                auditService.logAction("user", "budgets", null, "Set budget for " + category, null, null);
                javafx.application.Platform.runLater(() -> 
                    new Alert(Alert.AlertType.INFORMATION, "Budget set for " + category).showAndWait());
            } catch (Exception e) {
                ErrorHandler.handleException(e, "Failed to set budget", null);
            }
        }, FinancialManagementApp.executor);
    }
}