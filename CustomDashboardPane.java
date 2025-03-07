package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class CustomDashboardPane extends VBox {
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private final ExchangeRateService exchangeRateService;
    private Label totalSalesLabel, overdueAmountLabel, cashBalanceLabel, topCustomerLabel, totalTaxLabel;
    private ComboBox<String> timePeriodCombo;

    public CustomDashboardPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService, ExchangeRateService exchangeRateService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        this.exchangeRateService = exchangeRateService;
        initializeUI();
        loadDashboardData("This Month");
    }

    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        HBox controlBox = new HBox(10);
        // Line 32: Safely handle missing "timePeriod" key
        String timePeriodLabelText;
        try {
            timePeriodLabelText = messages.getString("timePeriod");
        } catch (java.util.MissingResourceException e) {
            timePeriodLabelText = "Time Period"; // Fallback value
        }
        controlBox.getChildren().add(new Label(timePeriodLabelText));
        
        timePeriodCombo = new ComboBox<>();
        timePeriodCombo.getItems().addAll("This Week", "This Month", "This Quarter", "This Year");
        timePeriodCombo.setValue("This Month");
        timePeriodCombo.setOnAction(e -> loadDashboardData(timePeriodCombo.getValue()));
        controlBox.getChildren().add(timePeriodCombo);

        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(10);
        metricsGrid.setVgap(10);

        totalSalesLabel = new Label(messages.getString("totalSales") + "0.00 USD");
        metricsGrid.add(totalSalesLabel, 0, 0);
        overdueAmountLabel = new Label(messages.getString("overdueAmount") + "0.00 USD");
        metricsGrid.add(overdueAmountLabel, 1, 0);
        cashBalanceLabel = new Label(messages.getString("cashBalance") + "0.00 USD");
        metricsGrid.add(cashBalanceLabel, 0, 1);
        topCustomerLabel = new Label(messages.getString("topCustomer") + "N/A");
        metricsGrid.add(topCustomerLabel, 1, 1);
        totalTaxLabel = new Label(messages.getString("totalTax") + "0.00 USD");
        metricsGrid.add(totalTaxLabel, 0, 2);

        getChildren().addAll(controlBox, metricsGrid);
    }

    private void loadDashboardData(String period) {
        CompletableFuture.runAsync(() -> {
            try {
                String startDate, endDate;
                switch (period) {
                    case "This Week":
                        startDate = LocalDate.now().minusDays(7).toString();
                        endDate = LocalDate.now().toString();
                        break;
                    case "This Month":
                        startDate = LocalDate.now().withDayOfMonth(1).toString();
                        endDate = LocalDate.now().toString();
                        break;
                    case "This Quarter":
                        startDate = LocalDate.now().minusMonths(3).withDayOfMonth(1).toString();
                        endDate = LocalDate.now().toString();
                        break;
                    case "This Year":
                        startDate = LocalDate.now().withDayOfYear(1).toString();
                        endDate = LocalDate.now().toString();
                        break;
                    default:
                        startDate = endDate = LocalDate.now().toString();
                }

                double totalSales = dbService.getTotalSales();
                double overdueAmount = dbService.getOverdueAmount();
                double cashBalance = dbService.getCashBalance();
                Map<String, Double> salesByCustomer = dbService.getSalesByCustomer(startDate, endDate);
                Map<String, Double> taxReport = dbService.getTaxReport(startDate, endDate);

                String topCustomer = salesByCustomer.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("N/A");
                double topCustomerSales = salesByCustomer.getOrDefault(topCustomer, 0.0);
                double totalTax = taxReport.getOrDefault("Total Taxes", 0.0);

                javafx.application.Platform.runLater(() -> {
                    totalSalesLabel.setText(messages.getString("totalSales") + String.format("%.2f", totalSales) + " USD");
                    overdueAmountLabel.setText(messages.getString("overdueAmount") + String.format("%.2f", overdueAmount) + " USD");
                    cashBalanceLabel.setText(messages.getString("cashBalance") + String.format("%.2f", cashBalance) + " USD");
                    topCustomerLabel.setText(messages.getString("topCustomer") + topCustomer + " (" + String.format("%.2f", topCustomerSales) + " USD)");
                    totalTaxLabel.setText(messages.getString("totalTax") + String.format("%.2f", totalTax) + " USD");
                });
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to load dashboard data", null);
            }
        }, FinancialManagementApp.executor);
    }
}