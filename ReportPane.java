package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class ReportPane extends VBox {
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private ComboBox<String> reportTypeCombo;
    private DatePicker startDatePicker, endDatePicker;
    private TextArea reportArea;

    public ReportPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        initializeUI();
    }

    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        HBox controls = new HBox(10);
        controls.setPadding(new javafx.geometry.Insets(10));

        reportTypeCombo = new ComboBox<>();
        reportTypeCombo.getItems().addAll("Financial Summary", "Cash Flow", "Balance Sheet", "Profit & Loss", "Aging Report",
            "Sales by Customer", "Sales by Product", "Tax Report", "Cash Flow Projection", "Budget vs Actual", "Cash Flow Forecast");
        reportTypeCombo.setValue("Financial Summary");
        controls.getChildren().add(new Label(messages.getString("reportType")));
        controls.getChildren().add(reportTypeCombo);

        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("YYYY-MM-DD");
        controls.getChildren().add(new Label(messages.getString("startDate")));
        controls.getChildren().add(startDatePicker);

        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("YYYY-MM-DD");
        controls.getChildren().add(new Label(messages.getString("endDate")));
        controls.getChildren().add(endDatePicker);

        Button generateButton = new Button(messages.getString("generate"));
        generateButton.setOnAction(e -> generateReport());
        controls.getChildren().add(generateButton);

        reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefHeight(400);
        reportArea.setWrapText(true);

        getChildren().addAll(controls, reportArea);
    }

    private void generateReport() {
        CompletableFuture.runAsync(() -> {
            try {
                String reportType = reportTypeCombo.getValue();
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    javafx.application.Platform.runLater(() -> 
                        new Alert(Alert.AlertType.ERROR, "Please select start and end dates").showAndWait());
                    return;
                }
                String startDate = startDatePicker.getValue().toString();
                String endDate = endDatePicker.getValue().toString();
                StringBuilder report = new StringBuilder(reportType + " Report (" + startDate + " to " + endDate + ")\n");
                report.append("------------------------------------------------\n");

                switch (reportType) {
                    case "Financial Summary":
                        Map<String, Double> financialSummary = dbService.generateCompanyFinancialSummary(startDate, endDate);
                        financialSummary.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Cash Flow":
                        Map<String, Double> cashFlow = dbService.generateCashFlowStatement(startDate, endDate);
                        cashFlow.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Balance Sheet":
                        Map<String, Double> balanceSheet = dbService.generateBalanceSheet(startDate, endDate);
                        balanceSheet.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Profit & Loss":
                        Map<String, Double> profitLoss = dbService.generateProfitLossStatement(startDate, endDate);
                        profitLoss.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Aging Report":
                        List<Object[]> aging = dbService.getAgingAnalysis();
                        report.append("Invoice ID | Customer | Amount | Days Overdue\n");
                        for (Object[] row : aging) {
                            report.append(row[0]).append(" | ").append(row[1]).append(" | ")
                                  .append(String.format("%.2f", (Double) row[2])).append(" | ").append(row[3]).append("\n");
                        }
                        break;
                    case "Sales by Customer":
                        Map<String, Double> salesByCustomer = dbService.getSalesByCustomer(startDate, endDate);
                        salesByCustomer.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Sales by Product":
                        Map<String, Double> salesByProduct = dbService.getSalesByProduct(startDate, endDate);
                        salesByProduct.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Tax Report":
                        Map<String, Double> taxReport = dbService.getTaxReport(startDate, endDate);
                        taxReport.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Cash Flow Projection":
                        Map<String, Double> projection = dbService.getCashFlowProjection(startDate, endDate);
                        projection.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Budget vs Actual":
                        Map<String, Double> budgetVsActual = dbService.getBudgetVsActual(startDate, endDate);
                        budgetVsActual.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    case "Cash Flow Forecast":
                        Map<String, Double> forecast = dbService.getCashFlowForecast(startDate, endDate, 7);
                        forecast.forEach((k, v) -> report.append(k).append(": ").append(String.format("%.2f", v)).append(" USD\n"));
                        break;
                    default:
                        report.append("Unknown report type\n");
                }

                javafx.application.Platform.runLater(() -> reportArea.setText(report.toString()));
                auditService.logAction("user", "reports", null, "Generated " + reportType + " report", null, null);
            } catch (Exception e) {
                ErrorHandler.handleException(e, "Failed to generate report", null);
            }
        }, FinancialManagementApp.executor);
    }
}