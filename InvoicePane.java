package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javafx.application.Platform;
import java.time.LocalDate;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.financial.DatabaseService.Product;

public class InvoicePane extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoicePane.class);
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private final ExchangeRateService exchangeRateService;
    private Label invoiceIdLabel;
    private TextField customerField, supplierField, productField;
    private TextField totalAmountField, invoiceExchangeRateField, itemExchangeRateField;
    private ComboBox<String> invoiceTypeCombo, currencyCombo, paymentTermCombo;
    private Label invoiceRateLabel, itemRateLabel;
    private TextField paymentInstructionsField, notesField;
    private DatePicker datePicker;
    private TableView<InvoiceLineItem> lineItemsTable;
    private ComboBox<Integer> warehouseIdCombo;
    private TextField quantityField, unitPriceField, discountField;
    private ComboBox<String> itemCurrencyCombo, unitCombo;
    private Button addItemButton, deleteItemButton, editItemButton, saveButton, editInvoiceButton, deleteInvoiceButton, printButton, exportButton, attachmentButton, viewAttachmentsButton;
    private TextField shippingFeeField, transportingFeeField, uploadingFeeField, taxFeeField;
    private ComboBox<String> shippingFeeCurrencyCombo, transportingFeeCurrencyCombo, uploadingFeeCurrencyCombo, taxFeeCurrencyCombo;
    private TextField shippingFeeExchangeRateField, transportingFeeExchangeRateField, uploadingFeeExchangeRateField, taxFeeExchangeRateField;
    private String currentInvoiceId;

    public InvoicePane(ResourceBundle messages, DatabaseService dbService, AuditService auditService, ExchangeRateService exchangeRateService, boolean isNewWindow) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        this.exchangeRateService = exchangeRateService;
        this.currentInvoiceId = null;
        initializeUI();
        resetInvoice(); // Start with a clean slate
    }
             @SuppressWarnings("unchecked")
    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(10);
        scrollPane.setContent(content);
        scrollPane.setFitToWidth(true);

        invoiceIdLabel = new Label("Invoice ID: Not yet saved");
        content.getChildren().add(invoiceIdLabel);

        content.getChildren().add(new Label("Invoice Type"));
        invoiceTypeCombo = new ComboBox<>();
        invoiceTypeCombo.getItems().addAll("Sale", "Purchase", "Import Purchase", "Credit Note");
        invoiceTypeCombo.setValue("Sale");
        invoiceTypeCombo.setOnAction(e -> toggleFields());
        content.getChildren().add(invoiceTypeCombo);

        content.getChildren().add(new Label("Customer"));
        customerField = new TextField();
        customerField.setPromptText("Type to search (e.g., Cust)");
        customerField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (customerField.getScene() != null) {
                suggestCustomers(newVal);
            }
        });
        try {
            List<String> customers = dbService.getCustomersWithNames();
            if (!customers.isEmpty()) customerField.setText(customers.get(0));
        } catch (DatabaseException e) {
            LOGGER.error("Failed to load initial customer", e);
        }
        content.getChildren().add(customerField);

        content.getChildren().add(new Label("Supplier"));
        supplierField = new TextField();
        supplierField.setPromptText("Type to search (e.g., Supp)");
        supplierField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (supplierField.getScene() != null) {
                suggestSuppliers(newVal);
            }
        });
        try {
            List<String> suppliers = dbService.getSuppliersWithNames();
            if (!suppliers.isEmpty()) supplierField.setText(suppliers.get(0));
        } catch (DatabaseException e) {
            LOGGER.error("Failed to load initial supplier", e);
        }
        supplierField.setVisible(false);
        content.getChildren().add(supplierField);

        content.getChildren().add(new Label("Date"));
        datePicker = new DatePicker(LocalDate.now());
        content.getChildren().add(datePicker);

        content.getChildren().add(new Label("Total Amount"));
        totalAmountField = new TextField();
        totalAmountField.setEditable(false);
        content.getChildren().add(totalAmountField);

        HBox invoiceCurrencyBox = new HBox(10);
        invoiceCurrencyBox.getChildren().add(new Label("Invoice Currency"));
        currencyCombo = new ComboBox<>();
        currencyCombo.getItems().addAll("USD", "IQD", "RMB");
        currencyCombo.setValue("USD");
        currencyCombo.setOnAction(e -> updateTotalAmount());
        invoiceCurrencyBox.getChildren().add(currencyCombo);

        invoiceRateLabel = new Label("Exchange Rate (to USD)");
        invoiceRateLabel.visibleProperty().bind(currencyCombo.valueProperty().isNotEqualTo("USD"));
        invoiceExchangeRateField = new TextField("1.0");
        invoiceExchangeRateField.visibleProperty().bind(currencyCombo.valueProperty().isNotEqualTo("USD"));
        invoiceExchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        invoiceCurrencyBox.getChildren().addAll(invoiceRateLabel, invoiceExchangeRateField);
        content.getChildren().add(invoiceCurrencyBox);

        content.getChildren().add(new Label("Payment Instructions"));
        paymentInstructionsField = new TextField("Pay by check");
        content.getChildren().add(paymentInstructionsField);

        content.getChildren().add(new Label("Payment Term"));
        paymentTermCombo = new ComboBox<>();
        paymentTermCombo.getItems().addAll("Net 15", "Net 30", "Net 60");
        paymentTermCombo.setValue("Net 30");
        content.getChildren().add(paymentTermCombo);

        content.getChildren().add(new Label("Notes"));
        notesField = new TextField();
        content.getChildren().add(notesField);

        GridPane feeGrid = new GridPane();
        feeGrid.setHgap(10);
        feeGrid.setVgap(5);
        feeGrid.setVisible(false);

        feeGrid.add(new Label("Shipping Fee"), 0, 0);
        shippingFeeField = new TextField("0.0");
        shippingFeeField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(shippingFeeField, 1, 0);
        shippingFeeCurrencyCombo = new ComboBox<>();
        shippingFeeCurrencyCombo.getItems().addAll("USD", "IQD", "RMB");
        shippingFeeCurrencyCombo.setValue("USD");
        shippingFeeCurrencyCombo.setOnAction(e -> updateTotalAmount());
        feeGrid.add(shippingFeeCurrencyCombo, 2, 0);
        shippingFeeExchangeRateField = new TextField("1.0");
        shippingFeeExchangeRateField.visibleProperty().bind(shippingFeeCurrencyCombo.valueProperty().isNotEqualTo("USD"));
        shippingFeeExchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(shippingFeeExchangeRateField, 3, 0);

        feeGrid.add(new Label("Transporting Fee"), 0, 1);
        transportingFeeField = new TextField("0.0");
        transportingFeeField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(transportingFeeField, 1, 1);
        transportingFeeCurrencyCombo = new ComboBox<>();
        transportingFeeCurrencyCombo.getItems().addAll("USD", "IQD", "RMB");
        transportingFeeCurrencyCombo.setValue("USD");
        transportingFeeCurrencyCombo.setOnAction(e -> updateTotalAmount());
        feeGrid.add(transportingFeeCurrencyCombo, 2, 1);
        transportingFeeExchangeRateField = new TextField("1.0");
        transportingFeeExchangeRateField.visibleProperty().bind(transportingFeeCurrencyCombo.valueProperty().isNotEqualTo("USD"));
        transportingFeeExchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(transportingFeeExchangeRateField, 3, 1);

        feeGrid.add(new Label("Uploading Fee"), 0, 2);
        uploadingFeeField = new TextField("0.0");
        uploadingFeeField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(uploadingFeeField, 1, 2);
        uploadingFeeCurrencyCombo = new ComboBox<>();
        uploadingFeeCurrencyCombo.getItems().addAll("USD", "IQD", "RMB");
        uploadingFeeCurrencyCombo.setValue("USD");
        uploadingFeeCurrencyCombo.setOnAction(e -> updateTotalAmount());
        feeGrid.add(uploadingFeeCurrencyCombo, 2, 2);
        uploadingFeeExchangeRateField = new TextField("1.0");
        uploadingFeeExchangeRateField.visibleProperty().bind(uploadingFeeCurrencyCombo.valueProperty().isNotEqualTo("USD"));
        uploadingFeeExchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(uploadingFeeExchangeRateField, 3, 2);

        feeGrid.add(new Label("Tax Fee"), 0, 3);
        taxFeeField = new TextField("0.0");
        taxFeeField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(taxFeeField, 1, 3);
        taxFeeCurrencyCombo = new ComboBox<>();
        taxFeeCurrencyCombo.getItems().addAll("USD", "IQD", "RMB");
        taxFeeCurrencyCombo.setValue("USD");
        taxFeeCurrencyCombo.setOnAction(e -> updateTotalAmount());
        feeGrid.add(taxFeeCurrencyCombo, 2, 3);
        taxFeeExchangeRateField = new TextField("1.0");
        taxFeeExchangeRateField.visibleProperty().bind(taxFeeCurrencyCombo.valueProperty().isNotEqualTo("USD"));
        taxFeeExchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        feeGrid.add(taxFeeExchangeRateField, 3, 3);

        content.getChildren().add(feeGrid);

        lineItemsTable = new TableView<>();
        TableColumn<InvoiceLineItem, Integer> productIdCol = new TableColumn<>("Product ID");
        productIdCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
        TableColumn<InvoiceLineItem, Integer> warehouseIdCol = new TableColumn<>("Warehouse ID");
        warehouseIdCol.setCellValueFactory(new PropertyValueFactory<>("warehouseId"));
        TableColumn<InvoiceLineItem, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<InvoiceLineItem, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<InvoiceLineItem, Double> originalPriceCol = new TableColumn<>("Original Unit Price");
        originalPriceCol.setCellValueFactory(new PropertyValueFactory<>("originalUnitPrice"));
        TableColumn<InvoiceLineItem, Double> unitPriceCol = new TableColumn<>("Adjusted Unit Price");
        unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        TableColumn<InvoiceLineItem, Double> discountCol = new TableColumn<>("Discount (%)");
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discount"));
        TableColumn<InvoiceLineItem, Double> totalPriceCol = new TableColumn<>("Total Price");
        totalPriceCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        TableColumn<InvoiceLineItem, String> currencyCol = new TableColumn<>("Currency");
        currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        lineItemsTable.getColumns().addAll((TableColumn<InvoiceLineItem, ?>[]) new TableColumn[] {productIdCol, warehouseIdCol, quantityCol, unitCol, originalPriceCol, unitPriceCol, discountCol, totalPriceCol, currencyCol});
        content.getChildren().add(lineItemsTable);

        GridPane itemInput = new GridPane();
        itemInput.setHgap(10);
        itemInput.setVgap(5);

        itemInput.add(new Label("Product"), 0, 0);
        productField = new TextField();
        productField.setPromptText("Type to search (e.g., Prod)");
        productField.textProperty().addListener((obs, oldVal, newVal) -> suggestProducts(newVal));
        itemInput.add(productField, 1, 0);

        itemInput.add(new Label("Warehouse ID"), 0, 1);
        warehouseIdCombo = new ComboBox<>();
        warehouseIdCombo.getItems().add(1);
        warehouseIdCombo.setValue(1);
        itemInput.add(warehouseIdCombo, 1, 1);

        itemInput.add(new Label("Quantity"), 0, 2);
        quantityField = new TextField("1");
        itemInput.add(quantityField, 1, 2);

        itemInput.add(new Label("Unit"), 0, 3);
        unitCombo = new ComboBox<>();
        unitCombo.setPromptText("Select Product first");
        itemInput.add(unitCombo, 1, 3);

        itemInput.add(new Label("Unit Price"), 0, 4);
        unitPriceField = new TextField("100.0");
        itemInput.add(unitPriceField, 1, 4);

        itemInput.add(new Label("Discount (%)"), 0, 5);
        discountField = new TextField("0.0");
        itemInput.add(discountField, 1, 5);

        HBox itemCurrencyBox = new HBox(10);
        itemCurrencyBox.getChildren().add(new Label("Currency"));
        itemCurrencyCombo = new ComboBox<>();
        itemCurrencyCombo.getItems().addAll("USD", "IQD", "RMB");
        itemCurrencyCombo.setValue("USD");
        itemCurrencyBox.getChildren().add(itemCurrencyCombo);

        itemRateLabel = new Label("Exchange Rate (to USD)");
        itemRateLabel.visibleProperty().bind(itemCurrencyCombo.valueProperty().isNotEqualTo("USD"));
        itemExchangeRateField = new TextField("1.0");
        itemExchangeRateField.visibleProperty().bind(itemCurrencyCombo.valueProperty().isNotEqualTo("USD"));
        itemExchangeRateField.textProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        itemCurrencyBox.getChildren().addAll(itemRateLabel, itemExchangeRateField);
        itemInput.add(itemCurrencyBox, 1, 6);

        HBox itemButtons = new HBox(10);
        addItemButton = new Button("Add Item");
        addItemButton.setOnAction(e -> addLineItem());
        deleteItemButton = new Button("Delete Item");
        deleteItemButton.setOnAction(e -> deleteLineItem());
        editItemButton = new Button("Edit Item");
        editItemButton.setOnAction(e -> editLineItem());
        itemButtons.getChildren().addAll(addItemButton, deleteItemButton, editItemButton);
        itemInput.add(itemButtons, 1, 7);

        content.getChildren().add(itemInput);

        HBox invoiceButtons = new HBox(10);
        Button newInvoiceButton = new Button("New Invoice"); // Added New Invoice button
        newInvoiceButton.setOnAction(e -> resetInvoice());
        saveButton = new Button("Save");
        saveButton.setOnAction(e -> saveInvoice());
        editInvoiceButton = new Button("Browse Invoices");
        editInvoiceButton.setOnAction(e -> browseInvoices());
        deleteInvoiceButton = new Button("Delete Invoice");
        deleteInvoiceButton.setOnAction(e -> deleteInvoice());
        printButton = new Button("Print");
        printButton.setOnAction(e -> printInvoice());
        exportButton = new Button("Export");
        exportButton.setOnAction(e -> exportInvoice());
        attachmentButton = new Button("Add Attachment (0)");
        attachmentButton.setOnAction(e -> addAttachment());
        viewAttachmentsButton = new Button("View Attachments");
        viewAttachmentsButton.setOnAction(e -> viewAttachments());
        invoiceButtons.getChildren().addAll(newInvoiceButton, saveButton, editInvoiceButton, deleteInvoiceButton, printButton, exportButton, attachmentButton, viewAttachmentsButton);
        content.getChildren().add(invoiceButtons);

        getChildren().add(scrollPane);
        setPrefHeight(700);
        setPrefWidth(900);
        toggleFields();
    }

    private void resetInvoice() {
        currentInvoiceId = null;
        invoiceIdLabel.setText("Invoice ID: Not yet saved");
        invoiceTypeCombo.setValue("Sale");
        customerField.clear();
        supplierField.clear();
        datePicker.setValue(LocalDate.now());
        totalAmountField.clear();
        currencyCombo.setValue("USD");
        invoiceExchangeRateField.setText("1.0");
        paymentInstructionsField.setText("Pay by check");
        paymentTermCombo.setValue("Net 30");
        notesField.clear();
        shippingFeeField.setText("0.0");
        transportingFeeField.setText("0.0");
        uploadingFeeField.setText("0.0");
        taxFeeField.setText("0.0");
        shippingFeeCurrencyCombo.setValue("USD");
        transportingFeeCurrencyCombo.setValue("USD");
        uploadingFeeCurrencyCombo.setValue("USD");
        taxFeeCurrencyCombo.setValue("USD");
        shippingFeeExchangeRateField.setText("1.0");
        transportingFeeExchangeRateField.setText("1.0");
        uploadingFeeExchangeRateField.setText("1.0");
        taxFeeExchangeRateField.setText("1.0");
        productField.clear();
        warehouseIdCombo.setValue(1);
        quantityField.setText("1");
        unitCombo.getItems().clear();
        unitCombo.setPromptText("Select Product first");
        unitPriceField.setText("100.0");
        discountField.setText("0.0");
        itemCurrencyCombo.setValue("USD");
        itemExchangeRateField.setText("1.0");
        lineItemsTable.getItems().clear();
        updateAttachmentButton(); // Reset attachment count
        toggleFields();
    }

    //=========================
	private void saveInvoice() {
    Integer customerId = customerField.getText().isEmpty() ? null : Integer.parseInt(customerField.getText().split(" - ")[0]);
    String invoiceType = invoiceTypeCombo.getValue();
    String date = datePicker.getValue().toString();
    double totalAmount;
    try {
        totalAmount = Double.parseDouble(totalAmountField.getText());
    } catch (NumberFormatException e) {
        new Alert(Alert.AlertType.ERROR, "Invalid total amount").showAndWait();
        return;
    }
    double taxAmount = 0.0;
    String currency = currencyCombo.getValue();
    List<InvoiceLineItem> lineItems = new ArrayList<>(lineItemsTable.getItems());
    Integer supplierId = supplierField.getText().isEmpty() ? null : Integer.parseInt(supplierField.getText().split(" - ")[0]);
    String paymentInstructions = paymentInstructionsField.getText();
    String paymentTerm = paymentTermCombo.getValue();
    String status = "OPEN";
    String notes = notesField.getText();
    double exchangeRate = Double.parseDouble(invoiceExchangeRateField.getText());
    double shippingFee = Double.parseDouble(shippingFeeField.getText());
    double transportingFee = Double.parseDouble(transportingFeeField.getText());
    double uploadingFee = Double.parseDouble(uploadingFeeField.getText());
    double taxFee = Double.parseDouble(taxFeeField.getText());

    try {
        if (currentInvoiceId != null) {
            dbService.updateInvoice(currentInvoiceId, customerId, supplierId, invoiceType, date, totalAmount, taxAmount, currency,
                lineItems, status, paymentInstructions, paymentTerm, notes, exchangeRate, shippingFee, transportingFee, uploadingFee, taxFee);
            LOGGER.info("Invoice updated with ID: {}", currentInvoiceId);
            new Alert(Alert.AlertType.INFORMATION, "Invoice updated successfully").showAndWait();
        } else {
            currentInvoiceId = dbService.saveInvoice(customerId, supplierId, invoiceType, date, totalAmount, taxAmount, currency,
                lineItems, status, paymentInstructions, paymentTerm, notes, exchangeRate, shippingFee, transportingFee, uploadingFee, taxFee);
            LOGGER.info("Invoice saved with new ID: {}", currentInvoiceId);
            new Alert(Alert.AlertType.INFORMATION, "Invoice saved successfully").showAndWait();
        }

        // Distribute fees for Import Purchase after saving
        if (invoiceType.equals("Import Purchase")) {
            double totalFeesInUSD = getTotalFees();
            double totalItemsAmount = lineItems.stream()
                .mapToDouble(item -> item.getOriginalUnitPrice() * item.getQuantity())
                .sum();
            double factor = totalItemsAmount > 0 ? totalFeesInUSD / totalItemsAmount : 0;
            LOGGER.info("Total Fees: {}, Total Items Amount: {}, Factor: {}", totalFeesInUSD, totalItemsAmount, factor);
            List<InvoiceLineItem> updatedItems = new ArrayList<>();
            for (InvoiceLineItem item : lineItems) {
                double originalPrice = item.getOriginalUnitPrice();
                double newUnitPrice = originalPrice + (factor * originalPrice);
                item.setUnitPrice(newUnitPrice);
                item.setTotalPrice(newUnitPrice * item.getQuantity());
                updatedItems.add(item);
                LOGGER.info("Item {}: Original Price: {}, New Unit Price: {}, New Total: {}", 
                    item.getProductId(), originalPrice, newUnitPrice, item.getTotalPrice());
            }
            dbService.updateInvoiceLineItems(currentInvoiceId, updatedItems);
            lineItemsTable.getItems().clear();
            lineItemsTable.getItems().addAll(updatedItems);
            lineItemsTable.refresh();
            updateTotalAmount();
        }
        invoiceIdLabel.setText("Invoice ID: " + currentInvoiceId);

        // NEW: Refresh ContactsPane
        String contactName = customerId != null ? customerField.getText() : supplierField.getText();
        if (contactName != null && !contactName.isEmpty()) {
            FinancialManagementApp app = (FinancialManagementApp) saveButton.getScene().getWindow().getUserData();
            if (app != null) {
                app.refreshContactsPane(contactName);
            }
        }

        updateAttachmentButton();
    } catch (DatabaseException e) {
        LOGGER.error("Failed to save/update invoice", e);
        new Alert(Alert.AlertType.ERROR, "Failed to save invoice: " + e.getMessage()).showAndWait();
    }
}


//==================
      @SuppressWarnings("unchecked")  
    private void browseInvoices() {
        try {
            List<InvoiceDetails> invoices = dbService.getAllInvoices();
            TableView<InvoiceDetails> invoiceTable = new TableView<>();
            TableColumn<InvoiceDetails, String> idCol = new TableColumn<>("Invoice ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
            TableColumn<InvoiceDetails, String> typeCol = new TableColumn<>("Type");
            typeCol.setCellValueFactory(new PropertyValueFactory<>("invoiceType"));
            TableColumn<InvoiceDetails, String> customerCol = new TableColumn<>("Customer");
            customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
            TableColumn<InvoiceDetails, String> supplierCol = new TableColumn<>("Supplier");
            supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
            TableColumn<InvoiceDetails, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
            TableColumn<InvoiceDetails, Double> amountCol = new TableColumn<>("Total Amount");
            amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
            invoiceTable.getColumns().addAll((TableColumn<InvoiceDetails, ?>[]) new TableColumn[] {idCol, typeCol, customerCol, supplierCol, dateCol, amountCol});
            invoiceTable.getItems().addAll(invoices);

            invoiceTable.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    InvoiceDetails selected = invoiceTable.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        loadInvoiceForEdit(selected);
                    }
                }
            });

            Stage browseStage = new Stage();
            browseStage.setTitle("Browse Invoices");
            browseStage.setScene(new Scene(new VBox(invoiceTable), 800, 400));
            browseStage.show();
        } catch (DatabaseException e) {
            LOGGER.error("Failed to browse invoices", e);
            new Alert(Alert.AlertType.ERROR, "Failed to load invoices: " + e.getMessage()).showAndWait();
        }
    }

    private void toggleFields() {
        boolean isImportPurchase = invoiceTypeCombo.getValue().equals("Import Purchase");
        customerField.setVisible(!isImportPurchase);
        supplierField.setVisible(isImportPurchase);
        shippingFeeField.getParent().setVisible(isImportPurchase);
    }

    private void toggleFeeExchangeRate(ComboBox<String> feeCurrencyCombo, TextField feeExchangeRateField) {
        updateTotalAmount();
    }

    private void toggleInvoiceExchangeRate() {
        updateTotalAmount();
    }

    private void suggestCustomers(String input) {
        ContextMenu suggestions = new ContextMenu();
        if (input == null || input.trim().isEmpty()) {
            suggestions.hide();
            return;
        }
        try {
            String lowerInput = input.toLowerCase();
            List<String> customers = dbService.getCustomersWithNames();
            suggestions.getItems().clear();
            for (String customer : customers) {
                if (customer.toLowerCase().contains(lowerInput)) {
                    MenuItem menuItem = new MenuItem(customer);
                    menuItem.setOnAction(e -> {
                        customerField.setText(customer);
                        suggestions.hide();
                        customerField.requestFocus();
                    });
                    suggestions.getItems().add(menuItem);
                }
            }
            if (!suggestions.getItems().isEmpty()) {
                suggestions.show(customerField, javafx.geometry.Side.BOTTOM, 0, 0);
            } else {
                suggestions.hide();
            }
        } catch (DatabaseException e) {
            LOGGER.error("Failed to fetch customers for suggestion", e);
            new Alert(Alert.AlertType.ERROR, "Error loading customers: " + e.getMessage()).showAndWait();
        }
    }

    private void suggestSuppliers(String input) {
        ContextMenu suggestions = new ContextMenu();
        if (input == null || input.trim().isEmpty()) {
            suggestions.hide();
            return;
        }
        try {
            String lowerInput = input.toLowerCase();
            List<String> suppliers = dbService.getSuppliersWithNames();
            suggestions.getItems().clear();
            for (String supplier : suppliers) {
                if (supplier.toLowerCase().contains(lowerInput)) {
                    MenuItem menuItem = new MenuItem(supplier);
                    menuItem.setOnAction(e -> {
                        supplierField.setText(supplier);
                        suggestions.hide();
                        supplierField.requestFocus();
                    });
                    suggestions.getItems().add(menuItem);
                }
            }
            if (!suggestions.getItems().isEmpty()) {
                suggestions.show(supplierField, javafx.geometry.Side.BOTTOM, 0, 0);
            } else {
                suggestions.hide();
            }
        } catch (DatabaseException e) {
            LOGGER.error("Failed to fetch suppliers for suggestion", e);
            new Alert(Alert.AlertType.ERROR, "Error loading suppliers: " + e.getMessage()).showAndWait();
        }
    }

    private void suggestProducts(String input) {
        ContextMenu suggestions = new ContextMenu();
        if (input == null || input.trim().isEmpty()) {
            suggestions.hide();
            return;
        }
        try {
            String lowerInput = input.toLowerCase();
            List<Product> products = dbService.getProducts();
            suggestions.getItems().clear();
            for (Product p : products) {
                String item = p.getId() + " - " + p.getName();
                if (item.toLowerCase().contains(lowerInput)) {
                    MenuItem menuItem = new MenuItem(item);
                    menuItem.setOnAction(e -> {
                        productField.setText(item);
                        updateUnitCombo(item);
                        suggestions.hide();
                        productField.requestFocus();
                    });
                    suggestions.getItems().add(menuItem);
                }
            }
            if (!suggestions.getItems().isEmpty()) {
                suggestions.show(productField, javafx.geometry.Side.BOTTOM, 0, 0);
            } else {
                suggestions.hide();
            }
        } catch (DatabaseException e) {
            LOGGER.error("Failed to fetch products for suggestion", e);
            new Alert(Alert.AlertType.ERROR, "Error loading products: " + e.getMessage()).showAndWait();
        }
    }

    private void addLineItem() {
        try {
            String productText = productField.getText();
            if (productText == null || productText.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please select a product").showAndWait();
                return;
            }
            int productId = Integer.parseInt(productText.split(" - ")[0]);
            int warehouseId = warehouseIdCombo.getValue() != null ? warehouseIdCombo.getValue() : 1;
            int quantity = Integer.parseInt(quantityField.getText().isEmpty() ? "1" : quantityField.getText());
            String unit = unitCombo.getValue() != null ? unitCombo.getValue() : "piece";
            double unitPrice = Double.parseDouble(unitPriceField.getText().isEmpty() ? "0.0" : unitPriceField.getText());
            double discount = Double.parseDouble(discountField.getText().isEmpty() ? "0.0" : discountField.getText());
            String currency = itemCurrencyCombo.getValue() != null ? itemCurrencyCombo.getValue() : "USD";
            double itemExchangeRate = currency.equals("USD") ? 1.0 : Double.parseDouble(itemExchangeRateField.getText());

            double originalUnitPrice = unitPrice / itemExchangeRate;
            double adjustedUnitPrice = originalUnitPrice * (1 - discount / 100.0);
            double totalPrice = quantity * adjustedUnitPrice;

            InvoiceLineItem item = new InvoiceLineItem(
                0, productId, warehouseId, quantity, unit, originalUnitPrice, totalPrice, 0.0,
                0.0, discount, 0.0, 0.0, 0.0, "USD"
            );
            lineItemsTable.getItems().add(item);
            updateTotalAmount();

            productField.clear();
            warehouseIdCombo.setValue(1);
            quantityField.setText("1");
            unitCombo.getSelectionModel().clearSelection();
            unitCombo.setPromptText("Select Product first");
            unitPriceField.setText("100.0");
            discountField.setText("0.0");
            itemCurrencyCombo.setValue("USD");
        } catch (NumberFormatException ex) {
            LOGGER.error("Invalid numeric input in addLineItem: ", ex);
            new Alert(Alert.AlertType.ERROR, "Invalid numeric input: " + ex.getMessage()).showAndWait();
        }
    }

    private double getTotalFees() {
        double shipping = shippingFeeField.getText().isEmpty() ? 0 : Double.parseDouble(shippingFeeField.getText());
        double shippingRate = shippingFeeCurrencyCombo.getValue().equals("USD") ? 1.0 : Double.parseDouble(shippingFeeExchangeRateField.getText());
        double transporting = transportingFeeField.getText().isEmpty() ? 0 : Double.parseDouble(transportingFeeField.getText());
        double transportingRate = transportingFeeCurrencyCombo.getValue().equals("USD") ? 1.0 : Double.parseDouble(transportingFeeExchangeRateField.getText());
        double uploading = uploadingFeeField.getText().isEmpty() ? 0 : Double.parseDouble(uploadingFeeField.getText());
        double uploadingRate = uploadingFeeCurrencyCombo.getValue().equals("USD") ? 1.0 : Double.parseDouble(uploadingFeeExchangeRateField.getText());
        double tax = taxFeeField.getText().isEmpty() ? 0 : Double.parseDouble(taxFeeField.getText());
        double taxRate = taxFeeCurrencyCombo.getValue().equals("USD") ? 1.0 : Double.parseDouble(taxFeeExchangeRateField.getText());
        return (shipping / shippingRate) + (transporting / transportingRate) + (uploading / uploadingRate) + (tax / taxRate);
    }

    private void deleteLineItem() {
        InvoiceLineItem selected = lineItemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            lineItemsTable.getItems().remove(selected);
            updateTotalAmount();
        } else {
            new Alert(Alert.AlertType.WARNING, "Select an item to delete").showAndWait();
        }
    }

    private void editLineItem() {
        InvoiceLineItem selected = lineItemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            productField.setText(selected.getProductId() + " - " + getProductName(selected.getProductId()));
            warehouseIdCombo.setValue(selected.getWarehouseId());
            quantityField.setText(String.valueOf(selected.getQuantity()));
            unitCombo.setValue(selected.getUnit());
            unitPriceField.setText(String.valueOf(selected.getOriginalUnitPrice()));
            discountField.setText(String.valueOf(selected.getDiscount()));
            itemCurrencyCombo.setValue(selected.getCurrency());
            lineItemsTable.getItems().remove(selected);
            updateTotalAmount();
        } else {
            new Alert(Alert.AlertType.WARNING, "Select an item to edit").showAndWait();
        }
    }

    private String getProductName(int productId) {
        try {
            List<Product> products = dbService.getProducts();
            return products.stream()
                .filter(p -> p.getId() == productId)
                .findFirst()
                .map(Product::getName)
                .orElse("Unknown");
        } catch (DatabaseException e) {
            LOGGER.error("Failed to get product name for ID: " + productId, e);
            return "Unknown";
        }
    }

    private void updateTotalAmount() {
        try {
            String invoiceCurrency = currencyCombo.getValue();
            double invoiceRate = invoiceCurrency.equals("USD") ? 1.0 : Double.parseDouble(invoiceExchangeRateField.getText());
            double total = lineItemsTable.getItems().stream()
                .mapToDouble(item -> {
                    double itemTotal = item.getTotalPrice();
                    String itemCurrency = item.getCurrency();
                    double itemRate = itemCurrency.equals("USD") ? 1.0 : Double.parseDouble(itemExchangeRateField.getText());
                    double totalInUSD = itemCurrency.equals("USD") ? itemTotal : itemTotal / itemRate;
                    return invoiceCurrency.equals("USD") ? totalInUSD : totalInUSD * invoiceRate;
                })
                .sum();
            if (invoiceTypeCombo.getValue().equals("Import Purchase")) {
                total += getTotalFees();
            }
            totalAmountField.setText(String.format("%.2f", total));
        } catch (NumberFormatException ex) {
            totalAmountField.setText("Invalid Exchange Rate");
        }
    }

    private void loadInvoiceForEdit(InvoiceDetails invoice) {
        if (invoice != null) {
            currentInvoiceId = invoice.getInvoiceId();
            invoiceIdLabel.setText("Invoice ID: " + currentInvoiceId);
            invoiceTypeCombo.setValue(invoice.getInvoiceType());
            if (invoice.getInvoiceType().equals("Import Purchase")) {
                supplierField.setText(invoice.getSupplierName());
            } else {
                customerField.setText(invoice.getCustomerName());
            }
            datePicker.setValue(LocalDate.parse(invoice.getDate()));
            totalAmountField.setText(String.format("%.2f", invoice.getTotalAmount()));
            currencyCombo.setValue(invoice.getCurrency());
            paymentInstructionsField.setText(invoice.getPaymentInstructions());
            paymentTermCombo.setValue(invoice.getPaymentTerm());
            notesField.setText(invoice.getNotes());
            invoiceExchangeRateField.setText(String.valueOf(invoice.getExchangeRate()));
            shippingFeeField.setText(String.valueOf(invoice.getShippingCharge()));
            transportingFeeField.setText(String.valueOf(invoice.getTransportingFee()));
            uploadingFeeField.setText(String.valueOf(invoice.getUploadingFee()));
            taxFeeField.setText(String.valueOf(invoice.getTaxFee()));
            shippingFeeCurrencyCombo.setValue("USD");
            transportingFeeCurrencyCombo.setValue("USD");
            uploadingFeeCurrencyCombo.setValue("USD");
            taxFeeCurrencyCombo.setValue("USD");
            toggleFields();
            lineItemsTable.getItems().clear();
            lineItemsTable.getItems().addAll(invoice.getLineItems());
            updateTotalAmount();
            updateAttachmentButton();
        }
    }

    private void deleteInvoice() {
        if (currentInvoiceId != null) {
            try {
                dbService.deleteInvoice(currentInvoiceId);
                new Alert(Alert.AlertType.INFORMATION, "Invoice deleted successfully!").showAndWait();
                resetInvoice(); // Reset after deletion
            } catch (DatabaseException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to delete invoice").showAndWait();
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "No invoice loaded to delete").showAndWait();
        }
    }

    private void printInvoice() {
        if (currentInvoiceId == null) {
            new Alert(Alert.AlertType.WARNING, "No invoice to print").showAndWait();
            return;
        }
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Invoice PDF");
            fileChooser.setInitialFileName(currentInvoiceId + ".pdf");
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                PdfWriter writer = new PdfWriter(file);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                document.add(new Paragraph("Invoice ID: " + currentInvoiceId));
                document.add(new Paragraph("Contact: " + (invoiceTypeCombo.getValue().equals("Import Purchase") ? supplierField.getText() : customerField.getText())));
                document.add(new Paragraph("Date: " + datePicker.getValue()));
                document.add(new Paragraph("Total Amount: " + totalAmountField.getText() + " " + currencyCombo.getValue()));
                if (invoiceTypeCombo.getValue().equals("Import Purchase")) {
                    document.add(new Paragraph("Shipping Fee: " + shippingFeeField.getText() + " " + shippingFeeCurrencyCombo.getValue()));
                    document.add(new Paragraph("Transporting Fee: " + transportingFeeField.getText() + " " + transportingFeeCurrencyCombo.getValue()));
                    document.add(new Paragraph("Uploading Fee: " + uploadingFeeField.getText() + " " + uploadingFeeCurrencyCombo.getValue()));
                    document.add(new Paragraph("Tax Fee: " + taxFeeField.getText() + " " + taxFeeCurrencyCombo.getValue()));
                }
                document.add(new Paragraph("\nLine Items:"));
                for (InvoiceLineItem item : lineItemsTable.getItems()) {
                    document.add(new Paragraph(String.format("Product %d, %d %s, Original $%.2f, Adjusted $%.2f, Subtotal: $%.2f",
                        item.getProductId(), item.getQuantity(), item.getUnit(), item.getOriginalUnitPrice(), item.getUnitPrice(), item.getTotalPrice())));
                }
                document.close();
                new Alert(Alert.AlertType.INFORMATION, "Invoice printed to " + file.getAbsolutePath()).showAndWait();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to print invoice: " + e.getMessage()).showAndWait();
        }
    }

    private void exportInvoice() {
        if (currentInvoiceId == null) {
            new Alert(Alert.AlertType.WARNING, "No invoice to export").showAndWait();
            return;
        }
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Invoice CSV");
            fileChooser.setInitialFileName(currentInvoiceId + ".csv");
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                StringBuilder csv = new StringBuilder();
                csv.append("Invoice ID,Contact,Date,Total Amount,Currency\n");
                csv.append(String.format("%s,%s,%s,%s,%s\n", currentInvoiceId,
                    invoiceTypeCombo.getValue().equals("Import Purchase") ? supplierField.getText() : customerField.getText(),
                    datePicker.getValue(), totalAmountField.getText(), currencyCombo.getValue()));
                if (invoiceTypeCombo.getValue().equals("Import Purchase")) {
                    csv.append("Shipping Fee,Transporting Fee,Uploading Fee,Tax Fee\n");
                    csv.append(String.format("%s %s,%s %s,%s %s,%s %s\n",
                        shippingFeeField.getText(), shippingFeeCurrencyCombo.getValue(),
                        transportingFeeField.getText(), transportingFeeCurrencyCombo.getValue(),
                        uploadingFeeField.getText(), uploadingFeeCurrencyCombo.getValue(),
                        taxFeeField.getText(), taxFeeCurrencyCombo.getValue()));
                }
                csv.append("\nProduct ID,Quantity,Unit,Original Unit Price,Adjusted Unit Price,Discount,Total Price\n");
                for (InvoiceLineItem item : lineItemsTable.getItems()) {
                    csv.append(String.format("%d,%d,%s,%.2f,%.2f,%.2f,%.2f\n",
                        item.getProductId(), item.getQuantity(), item.getUnit(), item.getOriginalUnitPrice(),
                        item.getUnitPrice(), item.getDiscount(), item.getTotalPrice()));
                }
                Files.writeString(file.toPath(), csv.toString());
                new Alert(Alert.AlertType.INFORMATION, "Invoice exported to " + file.getAbsolutePath()).showAndWait();
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to export invoice: " + e.getMessage()).showAndWait();
        }
    }

    private void addAttachment() {
        if (currentInvoiceId == null) {
            new Alert(Alert.AlertType.WARNING, "Save the invoice first to add attachments").showAndWait();
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Attachment");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                dbService.addAttachment(currentInvoiceId, file.getName(), fileData);
                updateAttachmentButton();
                new Alert(Alert.AlertType.INFORMATION, "Attachment added: " + file.getName()).showAndWait();
            } catch (IOException | DatabaseException e) {
                LOGGER.error("Failed to add attachment for invoice {}: {}", currentInvoiceId, e.getMessage());
                new Alert(Alert.AlertType.ERROR, "Failed to add attachment: " + e.getMessage()).showAndWait();
            }
        }
    }
        @SuppressWarnings("unchecked") 
    private void viewAttachments() {
        if (currentInvoiceId == null) {
            new Alert(Alert.AlertType.WARNING, "No invoice loaded to view attachments").showAndWait();
            return;
        }
        try {
            List<Attachment> attachments = dbService.getAttachmentsForInvoice(currentInvoiceId);
            if (attachments.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No attachments found for this invoice").showAndWait();
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("View Attachments");
            dialog.setHeaderText("Attachments for Invoice " + currentInvoiceId);

            TableView<Attachment> attachmentTable = new TableView<>();
            TableColumn<Attachment, Integer> idCol = new TableColumn<>("ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            TableColumn<Attachment, String> nameCol = new TableColumn<>("File Name");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
            TableColumn<Attachment, Long> sizeCol = new TableColumn<>("Size (bytes)");
            sizeCol.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
            TableColumn<Attachment, String> dateCol = new TableColumn<>("Upload Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
            attachmentTable.getColumns().addAll((TableColumn<Attachment, ?>[]) new TableColumn[] {idCol, nameCol, sizeCol, dateCol});
            attachmentTable.getItems().addAll(attachments);
            attachmentTable.setPrefHeight(200);

            Button downloadButton = new Button("Download");
            downloadButton.setOnAction(e -> {
                Attachment selected = attachmentTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    downloadAttachment(selected.getId(), selected.getFileName());
                } else {
                    new Alert(Alert.AlertType.WARNING, "Select an attachment to download").showAndWait();
                }
            });

            Button editButton = new Button("Edit");
            editButton.setOnAction(e -> {
                Attachment selected = attachmentTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editAttachment(selected.getId());
                } else {
                    new Alert(Alert.AlertType.WARNING, "Select an attachment to edit").showAndWait();
                }
            });

            HBox buttonBox = new HBox(10, downloadButton, editButton);
            VBox dialogContent = new VBox(10, attachmentTable, buttonBox);
            dialog.getDialogPane().setContent(dialogContent);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        } catch (DatabaseException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load attachments: " + e.getMessage()).showAndWait();
        }
    }

    private void downloadAttachment(int attachmentId, String fileName) {
        try {
            byte[] content = dbService.getAttachmentContent(attachmentId);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Attachment");
            fileChooser.setInitialFileName(fileName);
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content);
                }
                new Alert(Alert.AlertType.INFORMATION, "Attachment saved to " + file.getAbsolutePath()).showAndWait();
            }
        } catch (DatabaseException | IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to download attachment: " + e.getMessage()).showAndWait();
        }
    }

    private void editAttachment(int attachmentId) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Replace Attachment");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                dbService.updateAttachment(attachmentId, file);
                new Alert(Alert.AlertType.INFORMATION, "Attachment updated: " + file.getName()).showAndWait();
                viewAttachments();
            } catch (DatabaseException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to update attachment: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void updateUnitCombo(String productText) {
        try {
            if (productText != null && !productText.trim().isEmpty()) {
                int productId = Integer.parseInt(productText.split(" - ")[0]);
                List<String> units = dbService.getProductUnits(productId);
                unitCombo.getItems().clear();
                unitCombo.getItems().addAll(units);
                unitCombo.setValue(units.get(0));
            } else {
                unitCombo.getItems().clear();
                unitCombo.setPromptText("Select Product first");
            }
        } catch (DatabaseException e) {
            unitCombo.getItems().clear();
            unitCombo.setPromptText("Error loading units");
        }
    }

    private void updateAttachmentButton() {
        if (currentInvoiceId != null) {
            try {
                int count = dbService.getAttachmentCount(currentInvoiceId, "invoices");
                attachmentButton.setText("Add Attachment (" + count + ")");
            } catch (DatabaseException e) {
                LOGGER.error("Failed to update attachment count for invoice {}: {}", currentInvoiceId, e.getMessage());
                attachmentButton.setText("Add Attachment (Error)");
            }
        } else {
            attachmentButton.setText("Add Attachment (0)");
        }
    }
}