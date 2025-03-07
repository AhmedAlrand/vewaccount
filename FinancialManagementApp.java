package com.example.financial;

import javafx.application.Application;
import javafx.application.Platform; // NEW IMPORT for Platform.runLater
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import javafx.scene.control.ScrollPane;
import javafx.print.PrinterJob;

public class FinancialManagementApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialManagementApp.class);
    private static HikariDataSource dataSource;
    private static final int SCHEDULER_POOL_SIZE = 1;
    private static final long SCHEDULER_INTERVAL_MINUTES = 1440; // 1 day
    public static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private ResourceBundle messages;
    private ScheduledExecutorService scheduler;
    private DatabaseService dbService;
    private AuditService auditService;
    private ExchangeRateService exchangeRateService;
    private String currentUser;
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private Label statusLabel;
    private ProgressBar progressBar;
    private ContactsPane contactsPane; // NEW: Field to hold ContactsPane instance

    @Override
    public void init() throws Exception {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'");
            config.setDriverClassName("org.h2.Driver");
            config.setUsername("sa");
            config.setPassword("");
            dataSource = new HikariDataSource(config);
            LOGGER.info("Database configured with URL: {}", config.getJdbcUrl());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        dbService = new DatabaseService();
        auditService = new AuditService(dbService);
        exchangeRateService = new ExchangeRateService();
        currentUser = "user";

        setLanguage("en");

        BorderPane root = new BorderPane();
        HBox topBar = createTopBar();
        HBox statusBar = createStatusBar();

        root.setTop(topBar);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 900);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle(messages.getString("title"));
        primaryStage.setScene(scene);

        if (!configureDatabase(primaryStage)) {
            LOGGER.info("Database configuration cancelled, exiting...");
            primaryStage.close();
            return;
        }

        TabPane tabPane = createTabPane();
        root.setCenter(tabPane);

        Tab auditTab = tabPane.getTabs().stream()
            .filter(tab -> tab.getText().equals(messages.getString("auditTrail")))
            .findFirst()
            .orElse(null);
        if (auditTab != null) {
            AuditTrailPane auditPane = (AuditTrailPane) auditTab.getContent();
            auditPane.loadAuditTrail();
        }

        primaryStage.setUserData(this); // NEW: Set app instance as UserData for access from panes
        primaryStage.show();
        startScheduledTasks();
    }

    private void setLanguage(String language) {
        try {
            messages = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));
        } catch (Exception e) {
            LOGGER.error("Error loading language properties for {}", language, e);
            messages = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
        updateUIWithMessages();
    }

    private void updateUIWithMessages() {
        if (statusLabel != null) {
            statusLabel.setText(messages.getString("statusReady"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            if (stage != null) {
                stage.setTitle(messages.getString("title"));
            }
            TabPane tabPane = (TabPane) ((BorderPane) statusLabel.getScene().getRoot()).getCenter();
            if (tabPane != null) {
                tabPane.getTabs().get(0).setText(messages.getString("dashboard"));
                tabPane.getTabs().get(1).setText(messages.getString("importPurchase"));
                tabPane.getTabs().get(2).setText(messages.getString("recurringInvoices"));
                tabPane.getTabs().get(3).setText(messages.getString("inventory"));
                tabPane.getTabs().get(4).setText(messages.getString("attachments"));
                tabPane.getTabs().get(5).setText(messages.getString("customersSuppliers"));
                tabPane.getTabs().get(6).setText(messages.getString("expenses"));
                tabPane.getTabs().get(7).setText(messages.getString("reports"));
                tabPane.getTabs().get(8).setText(messages.getString("adjustments"));
                tabPane.getTabs().get(9).setText(messages.getString("auditTrail"));
            }
        }
    }

    private boolean configureDatabase(Stage stage) {
        DatabaseConfigDialog configDialog = new DatabaseConfigDialog(stage, messages, dbService, this);
        configDialog.showAndWait();
        return configDialog.isConfirmed();
    }

    //==========================

private HBox createTopBar() {
    HBox topBar = new HBox(10);
    topBar.setPadding(new javafx.geometry.Insets(5));

    ComboBox<String> themeCombo = new ComboBox<>();
    themeCombo.getItems().addAll("Light", "Dark");
    themeCombo.setValue("Light");
    themeCombo.setOnAction(e -> changeTheme(themeCombo.getValue()));
    topBar.getChildren().add(themeCombo);

    ComboBox<String> languageCombo = new ComboBox<>();
    languageCombo.getItems().addAll("English", "Arabic");
    languageCombo.setValue("English");
    languageCombo.setOnAction(e -> setLanguage(languageCombo.getValue().equals("English") ? "en" : "ar"));
    topBar.getChildren().add(languageCombo);

    Button newInvoiceButton = new Button(messages.getString("newInvoice"));
    newInvoiceButton.setOnAction(e -> openNewInvoiceWindow());
    topBar.getChildren().add(newInvoiceButton);

    Button markPaymentButton = new Button("Mark Payment"); // Renamed statically for now
    markPaymentButton.setOnAction(e -> triggerMarkPayment()); // Updated to public method
    topBar.getChildren().add(markPaymentButton);

    Button addCustomerButton = new Button("Add Customer");
    addCustomerButton.setOnAction(e -> openAddCustomerWindow());
    topBar.getChildren().add(addCustomerButton);

    Button addSupplierButton = new Button("Add Supplier");
    addSupplierButton.setOnAction(e -> openAddSupplierWindow());
    topBar.getChildren().add(addSupplierButton);

    MenuButton databaseMenuButton = new MenuButton(messages.getString("database"));
    MenuItem changeDbItem = new MenuItem(messages.getString("changeDatabase"));
    changeDbItem.setOnAction(e -> changeDatabase());
    MenuItem newDbItem = new MenuItem(messages.getString("newDatabase"));
    newDbItem.setOnAction(e -> createNewDatabase());
    databaseMenuButton.getItems().addAll(changeDbItem, newDbItem);
    topBar.getChildren().add(databaseMenuButton);

    return topBar;
}


//=======================

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        contactsPane = new ContactsPane(messages, dbService, auditService, exchangeRateService); // NEW: Initialize contactsPane field
        tabPane.getTabs().addAll(
            new Tab(messages.getString("dashboard"), new CustomDashboardPane(messages, dbService, auditService, exchangeRateService)),
            new Tab(messages.getString("importPurchase"), new InvoicePane(messages, dbService, auditService, exchangeRateService, false)),
            new Tab(messages.getString("recurringInvoices"), new RecurringInvoicePane(messages, dbService, auditService)),
            new Tab(messages.getString("inventory"), new InventoryPane(messages, dbService, auditService)),
            new Tab(messages.getString("attachments"), new AttachmentPane(messages, dbService, auditService)),
            new Tab(messages.getString("customersSuppliers"), contactsPane), // Updated to use field
            new Tab(messages.getString("expenses"), new ExpensesPane(messages, dbService, auditService)),
            new Tab(messages.getString("reports"), new ReportPane(messages, dbService, auditService)),
            new Tab(messages.getString("adjustments"), new AdjustmentsPane(messages, dbService, auditService)),
            new Tab(messages.getString("auditTrail"), new AuditTrailPane(messages, dbService, auditService))
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        restrictAccessBasedOnRole(tabPane, currentUser);
        return tabPane;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new javafx.geometry.Insets(5));
        statusLabel = new Label(messages.getString("statusReady"));
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        statusBar.getChildren().addAll(statusLabel, progressBar);
        return statusBar;
    }

    private void changeTheme(String theme) {
        Scene scene = statusLabel.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        if ("Dark".equals(theme)) {
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        }
    }

    private void openNewInvoiceWindow() {
        Stage newStage = new Stage();
        newStage.setTitle(messages.getString("newInvoice"));
        InvoicePane invoicePane = new InvoicePane(messages, dbService, auditService, exchangeRateService, true);
        Scene scene = new Scene(invoicePane, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        newStage.setScene(scene);
        newStage.show();
    }

    private void openAddCustomerWindow() {
        Stage newStage = new Stage();
        newStage.setTitle("Add New Customer");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Customer Name");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact Info (e.g., email)");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Contact Info:"), 0, 1);
        grid.add(contactField, 1, 1);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String name = nameField.getText();
            String contactInfo = contactField.getText();
            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name is required").showAndWait();
                return;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    dbService.addCustomer(name, contactInfo);
                    auditService.logAction(currentUser, "Customers", null, "Added customer: " + name, null, null);
                    javafx.application.Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION, "Customer added: " + name).showAndWait();
                        newStage.close();
                    });
                } catch (DatabaseException ex) {
                    ErrorHandler.handleException(ex, "Failed to add customer", null);
                }
            }, executor);
        });

        grid.add(saveButton, 1, 2);

        Scene scene = new Scene(grid, 400, 200);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        newStage.setScene(scene);
        newStage.show();
    }

    private void openAddSupplierWindow() {
        Stage newStage = new Stage();
        newStage.setTitle("Add New Supplier");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Supplier Name");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact Info (e.g., email)");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Contact Info:"), 0, 1);
        grid.add(contactField, 1, 1);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String name = nameField.getText();
            String contactInfo = contactField.getText();
            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Name is required").showAndWait();
                return;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    dbService.addSupplier(name, contactInfo);
                    auditService.logAction(currentUser, "Suppliers", null, "Added supplier: " + name, null, null);
                    javafx.application.Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION, "Supplier added: " + name).showAndWait();
                        newStage.close();
                    });
                } catch (DatabaseException ex) {
                    ErrorHandler.handleException(ex, "Failed to add supplier", null);
                }
            }, executor);
        });

        grid.add(saveButton, 1, 2);

        Scene scene = new Scene(grid, 400, 200);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        newStage.setScene(scene);
        newStage.show();
    }

    //===========================
	private void markPaymentReceived() {
    Dialog<String> customerDialog = new Dialog<>();
    customerDialog.setTitle("Mark Payment");
    customerDialog.setHeaderText(messages.getString("enterCustomerName"));

    VBox dialogContent = new VBox(10);
    TextField customerField = new TextField();
    customerField.setPromptText("Type to search (e.g., Cust or Supp)");
    customerField.textProperty().addListener((obs, oldVal, newVal) -> {
        try {
            suggestContacts(customerField, newVal);
        } catch (DatabaseException e) {
            LOGGER.error("Failed to suggest contacts", e);
            javafx.application.Platform.runLater(() -> 
                new Alert(Alert.AlertType.ERROR, "Error suggesting contacts: " + e.getMessage()).showAndWait());
        }
    });
    dialogContent.getChildren().add(customerField);

    customerDialog.getDialogPane().setContent(dialogContent);
    customerDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    customerDialog.setResultConverter(dialogButton -> dialogButton == ButtonType.OK ? customerField.getText() : null);

    Optional<String> customerResult = customerDialog.showAndWait();
    customerResult.ifPresent(contactName -> {
        if (contactName == null || contactName.trim().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please select a customer or supplier").showAndWait();
            return;
        }

        int contactId;
        try {
            contactId = Integer.parseInt(contactName.split(" - ")[0]);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid contact ID format: {}", contactName, e);
            new Alert(Alert.AlertType.ERROR, "Invalid contact ID format: " + contactName).showAndWait();
            return;
        }

        try {
            boolean isCustomer = dbService.getCustomersWithNames().contains(contactName);
            double initialBalance = isCustomer ? dbService.getCustomerBalance(contactId) : dbService.getSupplierBalance(contactId);

            Dialog<ButtonType> paymentDialog = new Dialog<>();
            paymentDialog.setTitle(isCustomer ? "Mark Payment Received" : "Mark Payment Sent");
            paymentDialog.setHeaderText((isCustomer ? "Payment from " : "Payment to ") + contactName);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            Label initialBalanceLabel = new Label("Current Balance: " + String.format("%.2f", initialBalance));
            Label paymentIdLabel = new Label("Payment ID: Auto-generated");
            DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
            TextField amountField = new TextField();
            amountField.setPromptText("Enter amount");
            ComboBox<String> currencyDropdown = new ComboBox<>();
            currencyDropdown.getItems().addAll("USD", "IQD", "RMB");
            currencyDropdown.setValue("USD");
            Label exchangeRateLabel = new Label("Exchange Rate (to USD):");
            TextField exchangeRateField = new TextField("1.0");
            exchangeRateLabel.visibleProperty().bind(currencyDropdown.valueProperty().isNotEqualTo("USD"));
            exchangeRateField.visibleProperty().bind(currencyDropdown.valueProperty().isNotEqualTo("USD"));
            Label newBalanceLabel = new Label("New Balance: N/A");

            amountField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    double amount = Double.parseDouble(newVal);
                    double exchangeRate = currencyDropdown.getValue().equals("USD") ? 1.0 : Double.parseDouble(exchangeRateField.getText());
                    double adjustedAmount = amount / exchangeRate;
                    double newBalance = initialBalance - adjustedAmount;
                    newBalanceLabel.setText("New Balance: " + String.format("%.2f", newBalance));
                } catch (NumberFormatException e) {
                    newBalanceLabel.setText("New Balance: Invalid input");
                }
            });
            exchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    double exchangeRate = currencyDropdown.getValue().equals("USD") ? 1.0 : Double.parseDouble(newVal);
                    double adjustedAmount = amount / exchangeRate;
                    double newBalance = initialBalance - adjustedAmount;
                    newBalanceLabel.setText("New Balance: " + String.format("%.2f", newBalance));
                } catch (NumberFormatException e) {
                    newBalanceLabel.setText("New Balance: Invalid input");
                }
            });

            grid.add(initialBalanceLabel, 0, 0, 2, 1);
            grid.add(paymentIdLabel, 0, 1, 2, 1);
            grid.add(new Label("Date:"), 0, 2);
            grid.add(datePicker, 1, 2);
            grid.add(new Label("Payment Amount:"), 0, 3);
            grid.add(amountField, 1, 3);
            grid.add(new Label("Currency:"), 0, 4);
            grid.add(currencyDropdown, 1, 4);
            grid.add(exchangeRateLabel, 0, 5);
            grid.add(exchangeRateField, 1, 5);
            grid.add(newBalanceLabel, 0, 6, 2, 1);

            Button printButton = new Button("Print Receipt");
            printButton.setOnAction(e -> printReceipt(contactName, initialBalanceLabel.getText(), amountField.getText(), 
                currencyDropdown.getValue(), exchangeRateField.getText(), newBalanceLabel.getText(), 
                paymentIdLabel.getText(), datePicker.getValue().toString()));
            Button editButton = new Button("Edit Payment");
            editButton.setOnAction(e -> editPayment(contactId, isCustomer, contactName));
            Button deleteButton = new Button("Delete Payment");
            deleteButton.setOnAction(e -> deletePayment(contactId, isCustomer, contactName));
            Button searchButton = new Button("Search Payments");
            searchButton.setOnAction(e -> searchPayments(contactId, isCustomer, contactName));

            HBox buttonBox = new HBox(10, printButton, editButton, deleteButton, searchButton);
            grid.add(buttonBox, 0, 7, 2, 1);

            paymentDialog.getDialogPane().setContent(grid);
            paymentDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> result = paymentDialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                CompletableFuture.runAsync(() -> {
                    try {
                        double amount = Double.parseDouble(amountField.getText());
                        String date = datePicker.getValue().toString();
                        String currency = currencyDropdown.getValue();
                        double exchangeRate = currency.equals("USD") ? 1.0 : Double.parseDouble(exchangeRateField.getText());
                        String status = "COMPLETED"; // Default status

                        showProgress(true);
                        int paymentId = dbService.markPaymentReceived(contactName, amount, currency, exchangeRate, 
                            isCustomer ? contactId : null, isCustomer ? null : contactId, date, status);
                        auditService.logAction(currentUser, "payments", String.valueOf(paymentId), 
                            isCustomer ? "Payment received from customer: " + amount + " " + currency + " from " + contactName + " (Payment #" + paymentId + ")" :
                                         "Payment sent to supplier: " + amount + " " + currency + " to " + contactName + " (Payment #" + paymentId + ")", null, null);
                        double adjustedAmount = amount / exchangeRate;
                        double newBalance = initialBalance - adjustedAmount;
                        javafx.application.Platform.runLater(() -> {
                            paymentIdLabel.setText("Payment ID: " + paymentId); // Update label after saving
                            new Alert(Alert.AlertType.INFORMATION, 
                                (isCustomer ? "Payment received: " : "Payment sent: ") + amount + " " + currency + 
                                "\nPayment ID: " + paymentId + "\nDate: " + date +
                                "\nNew Balance: " + String.format("%.2f", newBalance)).showAndWait();
                            refreshContactsPane(contactName);
                        });
                    } catch (DatabaseException e) {
                        ErrorHandler.handleException(e, "Failed to mark payment", null);
                    } catch (NumberFormatException e) {
                        ErrorHandler.handleException(e, "Invalid payment amount or exchange rate", null);
                    } finally {
                        showProgress(false);
                    }
                }, executor);
            }
        } catch (DatabaseException e) {
            ErrorHandler.handleException(e, "Failed to process payment details", null);
        }
    });
}
//============

public void triggerMarkPayment() {
    markPaymentReceived();
}

//=========================

private void printReceipt(String contactName, String currentBalance, String amount, String currency, 
                         String exchangeRate, String newBalance, String paymentIdText, String date) {
    PrinterJob job = PrinterJob.createPrinterJob();
    if (job != null && job.showPrintDialog(null)) {
        VBox receipt = new VBox(10);
        receipt.setPadding(new javafx.geometry.Insets(10));
        receipt.getChildren().addAll(
            new Label("Payment Receipt"),
            new Label("Contact: " + contactName),
            new Label(paymentIdText),
            new Label("Date: " + date),
            new Label(currentBalance),
            new Label("Payment Amount: " + amount + " " + currency),
            new Label("Exchange Rate (to USD): " + (currency.equals("USD") ? "1.0" : exchangeRate)),
            new Label(newBalance)
        );

        boolean success = job.printPage(receipt);
        if (success) {
            job.endJob();
            LOGGER.info("Receipt printed for {} (Payment #{})", contactName, paymentIdText.split(": ")[1]);
        } else {
            LOGGER.error("Failed to print receipt for {} (Payment #{})", contactName, paymentIdText.split(": ")[1]);
            new Alert(Alert.AlertType.ERROR, "Failed to print receipt").showAndWait();
        }
    } else {
        LOGGER.warn("Print job cancelled or no printer available");
    }
}

private void editPayment(int contactId, boolean isCustomer, String contactName) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Edit Payment");
    dialog.setHeaderText("Enter Payment ID to Edit");
    dialog.setContentText("Payment ID:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(paymentId -> {
        try {
            Map<String, String> payment = dbService.getPaymentDetails(Integer.parseInt(paymentId));
            if (payment == null) {
                new Alert(Alert.AlertType.ERROR, "Payment #" + paymentId + " not found").showAndWait();
                return;
            }

            Dialog<ButtonType> editDialog = new Dialog<>();
            editDialog.setTitle("Edit Payment for " + contactName);
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            TextField amountField = new TextField(payment.get("amount"));
            DatePicker datePicker = new DatePicker(java.time.LocalDate.parse(payment.get("date")));
            ComboBox<String> currencyDropdown = new ComboBox<>();
            currencyDropdown.getItems().addAll("USD", "IQD", "RMB");
            currencyDropdown.setValue(payment.get("currency"));
            ComboBox<String> statusDropdown = new ComboBox<>();
            statusDropdown.getItems().addAll("PENDING", "COMPLETED");
            statusDropdown.setValue(payment.get("status"));

            grid.add(new Label("Amount:"), 0, 0);
            grid.add(amountField, 1, 0);
            grid.add(new Label("Date:"), 0, 1);
            grid.add(datePicker, 1, 1);
            grid.add(new Label("Currency:"), 0, 2);
            grid.add(currencyDropdown, 1, 2);
            grid.add(new Label("Status:"), 0, 3);
            grid.add(statusDropdown, 1, 3);

            editDialog.getDialogPane().setContent(grid);
            editDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            Optional<ButtonType> editResult = editDialog.showAndWait();
            if (editResult.isPresent() && editResult.get() == ButtonType.OK) {
                double newAmount = Double.parseDouble(amountField.getText());
                String newDate = datePicker.getValue().toString();
                String newCurrency = currencyDropdown.getValue();
                String newStatus = statusDropdown.getValue();
                dbService.updatePayment(Integer.parseInt(paymentId), newAmount, newDate, newCurrency, newStatus);
                auditService.logAction(currentUser, "payments", paymentId, 
                    "Edited payment #" + paymentId + " for " + contactName, null, null);
                new Alert(Alert.AlertType.INFORMATION, "Payment #" + paymentId + " updated").showAndWait();
                refreshContactsPane(contactName);
            }
        } catch (DatabaseException e) {
            ErrorHandler.handleException(e, "Failed to edit payment #" + paymentId, null);
        }
    });
}

private void deletePayment(int contactId, boolean isCustomer, String contactName) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Delete Payment");
    dialog.setHeaderText("Enter Payment ID to Delete");
    dialog.setContentText("Payment ID:");

    Optional<String> result = dialog.showAndWait();
    result.ifPresent(paymentId -> {
        try {
            if (!dbService.paymentExists(Integer.parseInt(paymentId))) {
                new Alert(Alert.AlertType.ERROR, "Payment #" + paymentId + " not found").showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete payment #" + paymentId + "?", ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                dbService.deletePayment(Integer.parseInt(paymentId));
                auditService.logAction(currentUser, "payments", paymentId, 
                    "Deleted payment #" + paymentId + " for " + contactName, null, null);
                new Alert(Alert.AlertType.INFORMATION, "Payment #" + paymentId + " deleted").showAndWait();
                refreshContactsPane(contactName);
            }
        } catch (DatabaseException e) {
            ErrorHandler.handleException(e, "Failed to delete payment #" + paymentId, null);
        }
    });
}

private void searchPayments(int contactId, boolean isCustomer, String contactName) {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Search Payments");
    dialog.setHeaderText("Enter Payment ID to Search (or leave blank for all)");
    dialog.setContentText("Payment ID:");

    Optional<String> result = dialog.showAndWait();
    try {
        List<Map<String, String>> payments;
        if (result.isPresent() && !result.get().isEmpty()) {
            String paymentId = result.get();
            Map<String, String> payment = dbService.getPaymentDetails(Integer.parseInt(paymentId));
            payments = payment != null ? Collections.singletonList(payment) : Collections.emptyList();
        } else {
            payments = isCustomer ? dbService.getCustomerPayments(contactId) : dbService.getSupplierPayments(contactId);
        }

        if (payments.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No payments found").showAndWait();
            return;
        }

        VBox paymentList = new VBox(10);
        for (Map<String, String> payment : payments) {
            paymentList.getChildren().add(new Label("Payment #" + payment.get("paymentId") + ": " + 
                payment.get("amount") + " " + payment.get("currency") + ", Date: " + payment.get("date") + 
                ", Status: " + payment.get("status")));
        }
        ScrollPane scrollPane = new ScrollPane(paymentList);
        scrollPane.setFitToWidth(true);

        Alert paymentAlert = new Alert(Alert.AlertType.INFORMATION);
        paymentAlert.setTitle("Payments for " + contactName);
        paymentAlert.getDialogPane().setContent(scrollPane);
        paymentAlert.showAndWait();
    } catch (DatabaseException e) {
        ErrorHandler.handleException(e, "Failed to search payments", null);
    }
}


//========================

    private void suggestContacts(TextField field, String input) throws DatabaseException {
        ContextMenu suggestions = new ContextMenu();
        if (input == null || input.trim().isEmpty()) {
            suggestions.hide();
            return;
        }
        try {
            String lowerInput = input.toLowerCase();
            List<String> customers = dbService.getCustomersWithNames();
            List<String> suppliers = dbService.getSuppliersWithNames();
            suggestions.getItems().clear();
            for (String customer : customers) {
                if (customer.toLowerCase().contains(lowerInput)) {
                    MenuItem item = new MenuItem(customer);
                    item.setOnAction(e -> {
                        field.setText(customer);
                        suggestions.hide();
                        field.requestFocus();
                    });
                    suggestions.getItems().add(item);
                }
            }
            for (String supplier : suppliers) {
                if (supplier.toLowerCase().contains(lowerInput)) {
                    MenuItem item = new MenuItem(supplier);
                    item.setOnAction(e -> {
                        field.setText(supplier);
                        suggestions.hide();
                        field.requestFocus();
                    });
                    suggestions.getItems().add(item);
                }
            }
            if (!suggestions.getItems().isEmpty()) {
                suggestions.show(field, javafx.geometry.Side.BOTTOM, 0, 0);
            } else {
                suggestions.hide();
            }
        } catch (DatabaseException e) {
            throw e;
        }
    }

    private void changeDatabase() {
        DatabaseConfigDialog configDialog = new DatabaseConfigDialog((Stage) statusLabel.getScene().getWindow(), messages, dbService, this);
        configDialog.showAndWait();
        if (configDialog.isConfirmed()) {
            new Alert(Alert.AlertType.INFORMATION, messages.getString("dbConfigUpdated")).showAndWait();
        }
    }

    private void createNewDatabase() {
        TextInputDialog dbNameDialog = new TextInputDialog();
        dbNameDialog.setTitle("New Database");
        dbNameDialog.setHeaderText(messages.getString("enterNewDbName"));
        dbNameDialog.showAndWait().ifPresent(dbName -> {
            CompletableFuture.runAsync(() -> {
                try {
                    showProgress(true);
                    dbService.createNewDatabase(dbName);
                    auditService.logAction(currentUser, "databases", null, "Created new database: " + dbName, null, null);
                    javafx.application.Platform.runLater(() -> {
                        new Alert(Alert.AlertType.INFORMATION, messages.getString("dbCreated") + ": " + dbName).showAndWait();
                        changeDatabase();
                    });
                } catch (DatabaseException e) {
                    ErrorHandler.handleException(e, messages.getString("errorCreatingDb") + ": " + dbName, null);
                } finally {
                    showProgress(false);
                }
            }, executor);
        });
    }

    private void restrictAccessBasedOnRole(TabPane tabPane, String role) {
        if ("user".equals(role)) {
            tabPane.getTabs().get(8).setDisable(true); // Adjustments
            tabPane.getTabs().get(9).setDisable(true); // Audit Trail
        }
    }

    private void startScheduledTasks() {
        scheduler = Executors.newScheduledThreadPool(SCHEDULER_POOL_SIZE);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isDisposed.get()) return;
                showProgress(true);
                CompletableFuture.runAsync(() -> {
                    try {
                        List<String> overduePayments = dbService.getOverduePayments(1, 100);
                        if (!overduePayments.isEmpty()) {
                            StringBuilder message = new StringBuilder(messages.getString("overduePayments") + "\n");
                            overduePayments.forEach(payment -> message.append(payment).append("\n"));
                            javafx.application.Platform.runLater(() ->
                                new Alert(Alert.AlertType.WARNING, message.toString(), ButtonType.OK).showAndWait());
                        }

                        dbService.processRecurringInvoices();

                        List<String> reminders = dbService.getPendingReminders();
                        if (!reminders.isEmpty()) {
                            StringBuilder reminderMessage = new StringBuilder("Payment Reminders:\n");
                            reminders.forEach(reminder -> {
                                reminderMessage.append(reminder).append("\n");
                                String invoiceId = reminder.split(",")[0].split(":")[1].trim();
                                try {
                                    dbService.markReminderSent(invoiceId);
                                } catch (DatabaseException e) {
                                    LOGGER.error("Failed to mark reminder sent for invoice: " + invoiceId, e);
                                }
                            });
                            javafx.application.Platform.runLater(() ->
                                new Alert(Alert.AlertType.INFORMATION, reminderMessage.toString(), ButtonType.OK).showAndWait());
                        }

                        exchangeRateService.updateExchangeRates();
                    } catch (DatabaseException e) {
                        LOGGER.error("Error in scheduled task", e);
                    }
                }, executor).exceptionally(throwable -> {
                    LOGGER.error("Error in scheduled task", throwable);
                    return null;
                });
            } finally {
                showProgress(false);
            }
        }, 0, SCHEDULER_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    private void showProgress(boolean show) {
        javafx.application.Platform.runLater(() -> {
            progressBar.setVisible(show);
            statusLabel.setText(show ? messages.getString("statusProcessing") : messages.getString("statusReady"));
        });
    }

    // NEW: Step 1 - Refresh callback for ContactsPane
    public void refreshContactsPane(String contactName) {
        if (contactName != null && contactsPane != null) {
            Platform.runLater(() -> {
                contactsPane.searchContacts(contactName.split(" - ")[1]); // Search by name part
                contactsPane.getContactList().getSelectionModel().select(contactName);
                contactsPane.loadActivity();
            });
        }
    }

    // NEW: Getter for ContactsPane (optional, for clarity)
    public ContactsPane getContactsPane() {
        return contactsPane;
    }

    @Override
    public void stop() {
        if (isDisposed.compareAndSet(false, true)) {
            try {
                if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdownNow();
                if (executor != null && !executor.isShutdown()) executor.shutdownNow();
            } finally {
                dbService.close();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Updated DatabaseConfigDialog class
    static class DatabaseConfigDialog extends Dialog<Void> {
        private static final String DEFAULT_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema.sql'";
        private static final String DEFAULT_USERNAME = "sa";
        private TextField urlField, usernameField;
        private PasswordField passwordField;
        private boolean confirmed = false;
        private ResourceBundle messages;
        private DatabaseService dbService;
        private FinancialManagementApp app;

        public DatabaseConfigDialog(Stage owner, ResourceBundle messages, DatabaseService dbService, FinancialManagementApp app) {
            super();
            initOwner(owner);
            setTitle(messages.getString("dbConfigTitle"));
            this.messages = messages;
            this.dbService = dbService;
            this.app = app;
            initializeUI();
        }

        private void initializeUI() {
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(10));

            grid.add(new Label(messages.getString("dbUrl")), 0, 0);
            urlField = new TextField(DEFAULT_DB_URL);
            grid.add(urlField, 1, 0);

            grid.add(new Label(messages.getString("dbUsername")), 0, 1);
            usernameField = new TextField(DEFAULT_USERNAME);
            grid.add(usernameField, 1, 1);

            grid.add(new Label(messages.getString("dbPassword")), 0, 2);
            passwordField = new PasswordField();
            grid.add(passwordField, 1, 2);

            Button connectButton = new Button(messages.getString("connect"));
            connectButton.setOnAction(e -> connect());
            grid.add(connectButton, 0, 3, 2, 1);

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
        }

        private void connect() {
            String url = urlField.getText();
            String username = usernameField.getText();
            String password = passwordField.getText();
            try {
                if (url.isEmpty() || username.isEmpty()) {
                    throw new IllegalArgumentException("URL and username are required");
                }
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(url);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.h2.Driver");
                config.setMaximumPoolSize(10);
                DatabaseService.dataSource = new HikariDataSource(config);
                LOGGER.info("Database configured with URL: {}", url);
                confirmed = true;

                javafx.application.Platform.runLater(() -> {
                    BorderPane root = (BorderPane) app.statusLabel.getScene().getRoot();
                    TabPane newTabPane = app.createTabPane();
                    root.setCenter(newTabPane);
                });

                close();
            } catch (Exception e) {
                LOGGER.error("Failed to configure database", e);
                new Alert(Alert.AlertType.ERROR, "Invalid database configuration: " + e.getMessage()).showAndWait();
            }
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }
}