package com.example.financial;

import javafx.scene.control.ListView;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Stream; // Import for Stream class


import java.util.*;

public class ContactsPane extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactsPane.class);
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private final ExchangeRateService exchangeRateService;
    private ComboBox<String> contactTypeCombo;
    private TextField searchField;
    private ListView<String> contactList;
    private TableView<Map<String, String>> activityTable;
    private Label balanceLabel;
    private Button addCustomerButton, addSupplierButton;

    public ContactsPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService, ExchangeRateService exchangeRateService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        this.exchangeRateService = exchangeRateService;
        initializeUI();
    }

    //======================================

@SuppressWarnings("unchecked")
private void initializeUI() {
    setSpacing(10);
    setPadding(new javafx.geometry.Insets(10));

    VBox content = new VBox(10);

    contactTypeCombo = new ComboBox<>();
    contactTypeCombo.getItems().addAll("Customers", "Suppliers");
    contactTypeCombo.setValue("Customers");
    contactTypeCombo.setOnAction(e -> searchField.clear());
    content.getChildren().add(new Label("Contact Type"));
    content.getChildren().add(contactTypeCombo);

    searchField = new TextField();
    searchField.setPromptText("Type to search (e.g., Cust)");
    searchField.textProperty().addListener((obs, oldVal, newVal) -> searchContacts(newVal));
    content.getChildren().add(new Label("Search Contact"));
    content.getChildren().add(searchField);

    contactList = new ListView<>();
    contactList.setPrefHeight(200);
    contactList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> loadActivity());
    content.getChildren().add(new Label("Contacts"));
    content.getChildren().add(contactList);

    activityTable = new TableView<>();
    TableColumn<Map<String, String>, String> typeCol = new TableColumn<>("Type");
    typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("type")));
    TableColumn<Map<String, String>, String> idCol = new TableColumn<>("ID");
    idCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("id")));
    TableColumn<Map<String, String>, String> dateCol = new TableColumn<>("Date");
    dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("date")));
    TableColumn<Map<String, String>, String> amountCol = new TableColumn<>("Amount");
    amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("amount")));
    TableColumn<Map<String, String>, String> currencyCol = new TableColumn<>("Currency");
    currencyCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("currency")));
    TableColumn<Map<String, String>, String> statusCol = new TableColumn<>("Status");
    statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("status")));
    activityTable.getColumns().addAll((TableColumn<Map<String, String>, ?>[]) new TableColumn[] {typeCol, idCol, dateCol, amountCol, currencyCol, statusCol});
    activityTable.setPrefHeight(200);
    activityTable.setOnMouseClicked(e -> {
        if (e.getClickCount() == 2) {
            Map<String, String> selected = activityTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showActivityDetails(selected);
            }
        }
    });

    content.getChildren().add(new Label("Activity (Invoices/Payments)"));
    content.getChildren().add(activityTable);

    balanceLabel = new Label("Balance: $0.00");
    content.getChildren().add(balanceLabel);

    HBox buttonBox = new HBox(10);
    addCustomerButton = new Button("Add Customer");
    addCustomerButton.setOnAction(e -> addNewCustomer());
    addSupplierButton = new Button("Add Supplier");
    addSupplierButton.setOnAction(e -> addNewSupplier());
    Button markPaymentButton = new Button("Mark Payment"); // Added here
    markPaymentButton.setOnAction(e -> markPaymentForContact());
    buttonBox.getChildren().addAll(addCustomerButton, addSupplierButton, markPaymentButton); // Updated
    content.getChildren().add(buttonBox);

    getChildren().add(content);
}



//=============================

    // Moved getContactList() to class level
    public ListView<String> getContactList() {
        return contactList;
    }

    private void addNewCustomer() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add New Customer");
        dialog.setHeaderText("Enter Customer Details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField contactField = new TextField();
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Contact Info:"), 0, 1);
        grid.add(contactField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Map<String, String> result = new HashMap<>();
                result.put("name", nameField.getText());
                result.put("contactInfo", contactField.getText());
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                dbService.addCustomer(data.get("name"), data.get("contactInfo"));
                auditService.logAction("user", "contacts", null, "Added customer: " + data.get("name"), null, null);
                searchContacts(searchField.getText());
                new Alert(Alert.AlertType.INFORMATION, "Customer added: " + data.get("name")).showAndWait();
            } catch (DatabaseException e) {
                LOGGER.error("Failed to add customer", e);
                new Alert(Alert.AlertType.ERROR, "Failed to add customer: " + e.getMessage()).showAndWait();
            }
        });
    }

    private void addNewSupplier() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add New Supplier");
        dialog.setHeaderText("Enter Supplier Details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField contactField = new TextField();
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Contact Info:"), 0, 1);
        grid.add(contactField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Map<String, String> result = new HashMap<>();
                result.put("name", nameField.getText());
                result.put("contactInfo", contactField.getText());
                return result;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                dbService.addSupplier(data.get("name"), data.get("contactInfo"));
                auditService.logAction("user", "contacts", null, "Added supplier: " + data.get("name"), null, null);
                searchContacts(searchField.getText());
                new Alert(Alert.AlertType.INFORMATION, "Supplier added: " + data.get("name")).showAndWait();
            } catch (DatabaseException e) {
                LOGGER.error("Failed to add supplier", e);
                new Alert(Alert.AlertType.ERROR, "Failed to add supplier: " + e.getMessage()).showAndWait();
            }
        });
    }

    // Fixed typo: "puplic" -> "public"
    public void searchContacts(String query) {
        contactList.getItems().clear();
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        try {
            List<String> customers = dbService.getCustomersWithNames();
            List<String> suppliers = dbService.getSuppliersWithNames();
            Stream.concat(customers.stream(), suppliers.stream())
                .filter(name -> name.toLowerCase().contains(query.toLowerCase()))
                .forEach(contactList.getItems()::add);
        } catch (DatabaseException e) {
            LOGGER.error("Failed to search contacts", e);
        }
    }

 // =========================

public void loadActivity() {
    String selectedContact = contactList.getSelectionModel().getSelectedItem();
    if (selectedContact == null) {
        LOGGER.info("No contact selected for activity load");
        return;
    }

    int contactId;
    try {
        contactId = Integer.parseInt(selectedContact.split(" - ")[0]);
    } catch (NumberFormatException e) {
        LOGGER.error("Invalid contact ID format: {}", selectedContact);
        new Alert(Alert.AlertType.ERROR, "Invalid contact ID").showAndWait();
        return;
    }

    activityTable.getItems().clear();
    try {
        List<Map<String, String>> activity;
        double balance;
        String contactType = contactTypeCombo.getValue();
        if (contactType.equals("Customers")) {
            activity = dbService.getCustomerActivity(contactId);
            balance = dbService.getCustomerBalance(contactId);
            LOGGER.info("Loaded {} activities for contact ID {} as {}", activity.size(), contactId, contactType);
            if (activity.isEmpty()) {
                activityTable.setPlaceholder(new Label("No activity found"));
            } else {
                activityTable.getItems().addAll(activity);
            }
            // Customer logic: Positive = they owe us, Negative = we owe them
            String balanceText = balance >= 0 ?
                String.format("Owes Us: $%.2f", balance) :
                String.format("We Owe: -$%.2f", -balance); // Added negative sign
            balanceLabel.setText(balanceText);
            // Set color: green for "Owes Us", red for "We Owe"
            balanceLabel.setStyle(balanceText.startsWith("Owes Us") ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            LOGGER.info("Contact ID {} balance as {}: {}", contactId, contactType, balanceText);
        } else { // "Suppliers"
            activity = dbService.getSupplierActivity(contactId);
            balance = dbService.getSupplierBalance(contactId);
            LOGGER.info("Loaded {} activities for contact ID {} as {}", activity.size(), contactId, contactType);
            if (activity.isEmpty()) {
                activityTable.setPlaceholder(new Label("No activity found"));
            } else {
                activityTable.getItems().addAll(activity);
            }
            // Supplier logic: Positive = we owe them, Negative = they owe us
            String balanceText = balance >= 0 ?
                String.format("We Owe: -$%.2f", balance) : // Added negative sign
                String.format("Owes Us: $%.2f", -balance);
            balanceLabel.setText(balanceText);
            // Set color: green for "Owes Us", red for "We Owe"
            balanceLabel.setStyle(balanceText.startsWith("Owes Us") ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            LOGGER.info("Contact ID {} balance as {}: {}", contactId, contactType, balanceText);
        }
    } catch (DatabaseException e) {
        LOGGER.error("Failed to load activity for contact ID: {} as {}", contactId, contactTypeCombo.getValue(), e);
        new Alert(Alert.AlertType.ERROR, "Failed to load activity: " + e.getMessage()).showAndWait();
    }
}
 
//==========================


    @SuppressWarnings("unchecked")
    private void showActivityDetails(Map<String, String> activity) {
        String type = activity.get("type");
        String id = activity.get("id").trim(); // Trim whitespace

        LOGGER.info("Showing details for type: '{}', id: '{}'", type, id); // Debug ID
        Stage detailsStage = new Stage();
        detailsStage.setTitle(type + " Details: " + id);
        VBox detailsContent = new VBox(10);
        detailsContent.setPadding(new javafx.geometry.Insets(10));

        try {
            if (type.equals("Invoice") || type.equals("Import Purchase") || type.equals("Sale")) { // Broaden type check
                List<InvoiceDetails> invoices = dbService.searchInvoices("invoiceId = ?", Collections.singletonList(id));
                if (!invoices.isEmpty()) {
                    InvoiceDetails invoice = invoices.get(0);
                    detailsContent.getChildren().addAll(
                        new Label("Invoice ID: " + invoice.getInvoiceId()),
                        new Label(invoice.getInvoiceType().equals("Import Purchase") ? 
                                  "Supplier: " + invoice.getSupplierName() : 
                                  "Customer: " + invoice.getCustomerName()),
                        new Label("Date: " + invoice.getDate()),
                        new Label("Type: " + invoice.getInvoiceType()),
                        new Label("Total Amount: " + invoice.getTotalAmount() + " " + invoice.getCurrency()),
                        new Label("Status: " + invoice.getStatus()),
                        new Label("Payment Instructions: " + (invoice.getPaymentInstructions() != null ? invoice.getPaymentInstructions() : "N/A")),
                        new Label("Notes: " + (invoice.getNotes() != null ? invoice.getNotes() : "N/A"))
                    );
                    if (invoice.getInvoiceType().equals("Import Purchase")) {
                        detailsContent.getChildren().addAll(
                            new Label("Shipping Fee: " + invoice.getShippingCharge()),
                            new Label("Transporting Fee: " + invoice.getTransportingFee()),
                            new Label("Uploading Fee: " + invoice.getUploadingFee()),
                            new Label("Tax Fee: " + invoice.getTaxFee())
                        );
                    }
                    TableView<InvoiceLineItem> itemsTable = new TableView<>();
                    TableColumn<InvoiceLineItem, Integer> productCol = new TableColumn<>("Product ID");
                    productCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
                    TableColumn<InvoiceLineItem, Integer> qtyCol = new TableColumn<>("Quantity");
                    qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
                    TableColumn<InvoiceLineItem, String> unitCol = new TableColumn<>("Unit");
                    unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
                    TableColumn<InvoiceLineItem, Double> origPriceCol = new TableColumn<>("Original Price");
                    origPriceCol.setCellValueFactory(new PropertyValueFactory<>("originalUnitPrice"));
                    TableColumn<InvoiceLineItem, Double> adjPriceCol = new TableColumn<>("Adjusted Price");
                    adjPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
                    TableColumn<InvoiceLineItem, Double> totalCol = new TableColumn<>("Total");
                    totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
                    itemsTable.getColumns().addAll((TableColumn<InvoiceLineItem, ?>[]) new TableColumn[] {productCol, qtyCol, unitCol, origPriceCol, adjPriceCol, totalCol});
                    itemsTable.getItems().addAll(invoice.getLineItems());
                    itemsTable.setPrefHeight(200);
                    detailsContent.getChildren().add(itemsTable);
                } else {
                    LOGGER.warn("No invoice found for ID: '{}'", id);
                    detailsContent.getChildren().add(new Label("No details found for invoice: " + id));
                }
            } else if (type.equals("Payment")) {
                detailsContent.getChildren().addAll(
                    new Label("Payment ID: " + id),
                    new Label("Date: " + activity.get("date")),
                    new Label("Amount: " + activity.get("amount")),
                    new Label("Currency: " + activity.get("currency")),
                    new Label("Status: " + activity.get("status"))
                );
            }
        } catch (DatabaseException e) {
            LOGGER.error("Failed to load details for {}: {}", id, e.getMessage());
            detailsContent.getChildren().add(new Label("Error loading details: " + e.getMessage()));
        }

        Scene scene = new Scene(detailsContent, 600, 400);
        detailsStage.setScene(scene);
        detailsStage.show();
    }
//==================================

	private void markPaymentForContact() {
    String selectedContact = contactList.getSelectionModel().getSelectedItem();
    if (selectedContact == null) {
        new Alert(Alert.AlertType.WARNING, "Please select a contact first").showAndWait();
        return;
    }

    int contactId;
    try {
        contactId = Integer.parseInt(selectedContact.split(" - ")[0]);
    } catch (NumberFormatException e) {
        LOGGER.error("Invalid contact ID format: {}", selectedContact);
        new Alert(Alert.AlertType.ERROR, "Invalid contact ID").showAndWait();
        return;
    }

    // Access FinancialManagementApp to call triggerMarkPayment
    Stage stage = (Stage) getScene().getWindow();
    FinancialManagementApp app = (FinancialManagementApp) stage.getUserData();
    app.triggerMarkPayment(); // Updated to public method
}


//===================




} // Closing the class