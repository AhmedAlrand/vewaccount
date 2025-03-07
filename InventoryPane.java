package com.example.financial;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files; // Added import for Files.readAllBytes
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.financial.DatabaseService.Product;

public class InventoryPane extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryPane.class);
    private final ResourceBundle messages;
    private final DatabaseService dbService;
    private final AuditService auditService;
    private TableView<InventoryItem> inventoryTable;
    private TextField productField; // Smart search field
    private TextField warehouseIdField, quantityField, customerIdField, customerNameField;
    private ComboBox<String> unitCombo;
    private static InventoryPane activeInstance;
    private boolean isRefreshing = false;

    public InventoryPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
        this.messages = messages;
        this.dbService = dbService;
        this.auditService = auditService;
        activeInstance = this;
        initializeUI();
        loadInventory();
    }
    @SuppressWarnings("unchecked")
    private void initializeUI() {
        setSpacing(10);
        setPadding(new javafx.geometry.Insets(10));

        inventoryTable = new TableView<>();
        TableColumn<InventoryItem, Integer> productIdCol = new TableColumn<>("Product ID");
        productIdCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
        TableColumn<InventoryItem, Integer> warehouseIdCol = new TableColumn<>("Warehouse ID");
        warehouseIdCol.setCellValueFactory(new PropertyValueFactory<>("warehouseId"));
        TableColumn<InventoryItem, Integer> quantityCol = new TableColumn<>("Quantity (Pieces)");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        inventoryTable.getColumns().addAll((TableColumn<InventoryItem, ?>[]) new TableColumn[] {productIdCol, warehouseIdCol, quantityCol});
        inventoryTable.setPrefHeight(300);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);

        inputGrid.add(new Label(messages.getString("productId")), 0, 0);
        productField = new TextField();
        productField.setPromptText("Type to search (e.g., Prod)");
        productField.textProperty().addListener((obs, oldVal, newVal) -> suggestProducts(newVal));
        inputGrid.add(productField, 1, 0);

        inputGrid.add(new Label(messages.getString("warehouseId")), 0, 1);
        warehouseIdField = new TextField();
        inputGrid.add(warehouseIdField, 1, 1);

        inputGrid.add(new Label("Quantity Change (+/-)"), 0, 2);
        quantityField = new TextField();
        quantityField.setPromptText("e.g., +25 or -10");
        inputGrid.add(quantityField, 1, 2);

        inputGrid.add(new Label("Unit"), 0, 3);
        unitCombo = new ComboBox<>();
        unitCombo.setPromptText("Select Product first");
        inputGrid.add(unitCombo, 1, 3);

        inputGrid.add(new Label("Customer ID"), 0, 4);
        customerIdField = new TextField();
        customerIdField.setPromptText("e.g., 1");
        inputGrid.add(customerIdField, 1, 4);

        inputGrid.add(new Label("Customer Name"), 0, 5);
        customerNameField = new TextField();
        customerNameField.setPromptText("Type to search (e.g., Cust)");
        customerNameField.textProperty().addListener((obs, oldVal, newVal) -> suggestCustomers(customerNameField, newVal));
        inputGrid.add(customerNameField, 1, 5);

        HBox buttons = new HBox(10);
        Button addButton = new Button(messages.getString("addInventory"));
        addButton.setOnAction(e -> addInventoryItem());
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> loadInventory());
        Button addAttachmentButton = new Button("Add Attachment");
        addAttachmentButton.setOnAction(e -> addAttachment());
        Button viewAttachmentsButton = new Button("View Attachments");
        viewAttachmentsButton.setOnAction(e -> viewAttachments());
        buttons.getChildren().addAll(addButton, refreshButton, addAttachmentButton, viewAttachmentsButton);

        getChildren().addAll(inventoryTable, inputGrid, buttons);
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
            for (Product p : products) {
                String item = p.getId() + " - " + p.getName();
                if (item.toLowerCase().contains(lowerInput)) {
                    MenuItem menuItem = new MenuItem(item);
                    menuItem.setOnAction(e -> {
                        productField.setText(item);
                        updateUnitCombo(item);
                        suggestions.hide();
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

    private void suggestCustomers(TextField inputField, String input) {
        ContextMenu suggestions = new ContextMenu();
        if (input == null || input.trim().isEmpty()) {
            suggestions.hide();
            return;
        }
        try {
            String lowerInput = input.toLowerCase();
            List<String> customers = dbService.getCustomersWithNames();
            for (String customer : customers) {
                if (customer.toLowerCase().contains(lowerInput)) {
                    MenuItem item = new MenuItem(customer);
                    item.setOnAction(e -> {
                        inputField.setText(customer);
                        suggestions.hide();
                    });
                    suggestions.getItems().add(item);
                }
            }
            if (!suggestions.getItems().isEmpty()) {
                suggestions.show(inputField, javafx.geometry.Side.BOTTOM, 0, 0);
            } else {
                suggestions.hide();
            }
        } catch (DatabaseException e) {
            LOGGER.error("Failed to fetch customer names for suggestion", e);
            new Alert(Alert.AlertType.ERROR, "Error loading customers: " + e.getMessage()).showAndWait();
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
        } catch (NumberFormatException | DatabaseException e) {
            unitCombo.getItems().clear();
            unitCombo.setPromptText("Invalid Product");
        }
    }

    private void loadInventory() {
        if (isRefreshing) return;
        isRefreshing = true;
        CompletableFuture.runAsync(() -> {
            try {
                List<InventoryItem> items = dbService.getInventoryItems(1, 100);
                javafx.application.Platform.runLater(() -> {
                    inventoryTable.getItems().clear();
                    inventoryTable.getItems().addAll(items);
                    isRefreshing = false;
                });
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to load inventory", null);
                javafx.application.Platform.runLater(() -> isRefreshing = false);
            }
        }, FinancialManagementApp.executor);
    }

    private void addInventoryItem() {
        CompletableFuture.runAsync(() -> {
            try {
                String productText = productField.getText();
                if (productText == null || productText.trim().isEmpty()) {
                    javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "Please select a product").showAndWait());
                    return;
                }
                int productId = Integer.parseInt(productText.split(" - ")[0]);
                int warehouseId = Integer.parseInt(warehouseIdField.getText());
                int quantityChange = Integer.parseInt(quantityField.getText());
                String unit = unitCombo.getValue();
                int piecesChange = unit.equals("carton") ? quantityChange * 25 : quantityChange;
                dbService.addInventoryItem(productId, warehouseId, piecesChange);
                auditService.logAction("user", "inventory", null, "Adjusted inventory item: Product ID " + productId + ", Change: " + quantityChange + " " + unit, null, null);
                javafx.application.Platform.runLater(() -> {
                    loadInventory();
                    productField.clear();
                    warehouseIdField.clear();
                    quantityField.clear();
                    unitCombo.getItems().clear();
                    unitCombo.setPromptText("Select Product first");
                });
            } catch (NumberFormatException e) {
                ErrorHandler.handleException(e, "Invalid input - use numbers (e.g., +25 or -10)", null);
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to adjust inventory item", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void addAttachment() {
        CompletableFuture.runAsync(() -> {
            try {
                String productText = productField.getText();
                String warehouseIdText = warehouseIdField.getText();
                if (productText.isEmpty() || warehouseIdText.isEmpty()) {
                    javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "Please enter Product and Warehouse ID").showAndWait());
                    return;
                }
                String transactionId = productText.split(" - ")[0] + "-" + warehouseIdText;
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Add Attachment");
                File file = fileChooser.showOpenDialog(getScene().getWindow());
                if (file != null) {
                    byte[] fileData = Files.readAllBytes(file.toPath()); // Convert File to byte[]
                    dbService.addAttachment("Inventory", transactionId, fileData); // Updated to match new signature
                    auditService.logAction("user", "attachments", null, "Uploaded attachment for Inventory " + transactionId + ": " + file.getName(), null, null);
                    javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "Attachment added: " + file.getName()).showAndWait());
                }
            } catch (IOException e) {
                ErrorHandler.handleException(e, "Failed to read file for attachment", null);
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to add attachment", null);
            }
        }, FinancialManagementApp.executor);
    }
     
        @SuppressWarnings("unchecked")
    private void viewAttachments() {
        CompletableFuture.runAsync(() -> {
            try {
                String productText = productField.getText();
                String warehouseIdText = warehouseIdField.getText();
                if (productText.isEmpty() || warehouseIdText.isEmpty()) {
                    javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "Please enter Product and Warehouse ID").showAndWait());
                    return;
                }
                String transactionId = productText.split(" - ")[0] + "-" + warehouseIdText;
                List<Attachment> attachments = dbService.getAttachments("Inventory", transactionId);
                if (attachments.isEmpty()) {
                    javafx.application.Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, "No attachments found for this inventory item").showAndWait());
                    return;
                }
                    
                javafx.application.Platform.runLater(() -> {
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("View Attachments");
                    dialog.setHeaderText("Attachments for Inventory " + transactionId);

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
                });
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to load attachments", null);
            }
        }, FinancialManagementApp.executor);
    }

    private void downloadAttachment(int attachmentId, String fileName) {
        try {
            byte[] content = dbService.getAttachmentContent(attachmentId);
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Attachment");
            fileChooser.setInitialFileName(fileName);
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content);
                }
                auditService.logAction("user", "attachments", String.valueOf(attachmentId), "Downloaded attachment: " + fileName, null, null);
                new Alert(Alert.AlertType.INFORMATION, "Attachment saved to " + file.getAbsolutePath()).showAndWait();
            }
        } catch (DatabaseException | IOException e) {
            ErrorHandler.handleException(e, "Failed to download attachment", null);
        }
    }

    private void editAttachment(int attachmentId) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Replace Attachment");
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                dbService.updateAttachment(attachmentId, file);
                auditService.logAction("user", "attachments", String.valueOf(attachmentId), "Updated attachment: " + file.getName(), null, null);
                new Alert(Alert.AlertType.INFORMATION, "Attachment updated: " + file.getName()).showAndWait();
                viewAttachments();
            } catch (DatabaseException e) {
                ErrorHandler.handleException(e, "Failed to update attachment", null);
            }
        }
    }

    public static void refreshIfActive() {
        if (activeInstance != null) {
            activeInstance.loadInventory();
        }
    }
}