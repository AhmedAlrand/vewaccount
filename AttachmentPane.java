
package com.example.financial;

import javafx.stage.FileChooser;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javafx.beans.property.SimpleStringProperty;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttachmentPane extends VBox {
  private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentPane.class);
  private final ResourceBundle messages;
  private final DatabaseService dbService;
  private final AuditService auditService;
  private TableView<Map<String, Object>> attachmentsTable;
  private TextField searchField;
  private Label attachmentCountLabel;
  private String currentTransactionType;
  private String currentTransactionId;

  private final SimpleStringProperty invoiceId = new SimpleStringProperty("");
  private final SimpleStringProperty fileName = new SimpleStringProperty("");
  private final SimpleStringProperty customerName = new SimpleStringProperty("");
  private final SimpleStringProperty supplierName = new SimpleStringProperty("");
  private final SimpleStringProperty uploadDate = new SimpleStringProperty("");

  public AttachmentPane(ResourceBundle messages, DatabaseService dbService, AuditService auditService) {
      this.messages = messages;
      this.dbService = dbService;
      this.auditService = auditService;
      initializeUI();
  }
  @SuppressWarnings("unchecked")
  private void initializeUI() {
      setSpacing(10);
      setPadding(new javafx.geometry.Insets(10));

      attachmentsTable = new TableView<>();
      TableColumn<Map<String, Object>, String> invoiceIdCol = new TableColumn<>("Invoice ID");
      invoiceIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(
          cellData.getValue().get("invoiceId") != null ? cellData.getValue().get("invoiceId").toString() : ""));
      TableColumn<Map<String, Object>, String> fileNameCol = new TableColumn<>("File Name");
      fileNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(
          cellData.getValue().get("fileName") != null ? cellData.getValue().get("fileName").toString() : ""));
      TableColumn<Map<String, Object>, String> customerNameCol = new TableColumn<>("Customer Name");
      customerNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(
          cellData.getValue().get("customerName") != null ? cellData.getValue().get("customerName").toString() : ""));
      TableColumn<Map<String, Object>, String> supplierNameCol = new TableColumn<>("Supplier Name");
      supplierNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(
          cellData.getValue().get("supplierName") != null ? cellData.getValue().get("supplierName").toString() : ""));
      TableColumn<Map<String, Object>, String> uploadDateCol = new TableColumn<>("Upload Date");
      uploadDateCol.setCellValueFactory(cellData -> new SimpleStringProperty(
          cellData.getValue().get("uploadDate") != null ? cellData.getValue().get("uploadDate").toString() : ""));
      attachmentsTable.getColumns().addAll((TableColumn<Map<String, Object>, ?>[]) new TableColumn[] { invoiceIdCol, fileNameCol, customerNameCol, supplierNameCol, uploadDateCol});
      attachmentsTable.setPrefHeight(300);

      searchField = new TextField();
      searchField.setPromptText(messages.getString("searchAttachments"));
      searchField.textProperty().addListener((obs, oldVal, newVal) -> searchAttachments(newVal));

      attachmentCountLabel = new Label("Attachments: 0");

      HBox controls = new HBox(10);
      Button addButton = new Button(messages.getString("addAttachment"));
      addButton.setOnAction(e -> addAttachment());
      Button viewButton = new Button(messages.getString("viewAttachment"));
      viewButton.setOnAction(e -> viewSelectedAttachment());
      controls.getChildren().addAll(searchField, addButton, viewButton);

      getChildren().addAll(attachmentsTable, controls, attachmentCountLabel);
  }

  private void addAttachment() {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(messages.getString("addAttachment"));
      File file = fileChooser.showOpenDialog(getScene().getWindow());
      if (file != null) {
          try {
              byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
              dbService.addAttachment(currentTransactionId, file.getName(), fileData);
              auditService.logAction("user", "attachments", currentTransactionId, "Added attachment: " + file.getName(), null, null);
              loadAttachments(currentTransactionType, currentTransactionId);
              new Alert(Alert.AlertType.INFORMATION, "Attachment added: " + file.getName()).showAndWait();
          } catch (Exception e) {
              ErrorHandler.handleException(e, "Failed to add attachment", null);
          }
      }
  }

  private void viewSelectedAttachment() {
      Map<String, Object> selected = attachmentsTable.getSelectionModel().getSelectedItem();
      if (selected == null) {
          new Alert(Alert.AlertType.WARNING, "No attachment selected").showAndWait();
          return;
      }
      int attachmentId = (int) selected.get("attachmentId");
      downloadAttachment(attachmentId, (String) selected.get("fileName"));
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
      } catch (Exception e) {
          ErrorHandler.handleException(e, "Failed to download attachment", null);
      }
  }

  public void loadAttachments(String transactionType, String transactionId) {
      currentTransactionType = transactionType;
      currentTransactionId = transactionId;
      CompletableFuture.runAsync(() -> {
          try {
              List<Attachment> attachments = transactionType.equalsIgnoreCase("invoices") ?
                  dbService.getAttachmentsForInvoice(transactionId) : 
                  dbService.getAttachments(transactionType, transactionId);
              javafx.application.Platform.runLater(() -> {
                  attachmentsTable.getItems().clear();
                  for (Attachment att : attachments) {
                      Map<String, Object> map = new HashMap<>();
                      map.put("invoiceId", att.getEntityId());
                      map.put("fileName", att.getFileName());
                      String invoiceType = att.getInvoiceType();
                      if ("Sale".equalsIgnoreCase(invoiceType)) {
                          map.put("customerName", att.getContactName() != null ? att.getContactName() : "");
                          map.put("supplierName", "");
                      } else if ("Import Purchase".equalsIgnoreCase(invoiceType)) {
                          map.put("customerName", "");
                          map.put("supplierName", att.getContactName() != null ? att.getContactName() : "");
                      } else {
                          map.put("customerName", "");
                          map.put("supplierName", "");
                      }
                      map.put("uploadDate", att.getUploadDate());
                      map.put("attachmentId", att.getId());
                      attachmentsTable.getItems().add(map);
                  }
                  attachmentCountLabel.setText("Attachments: " + attachmentsTable.getItems().size());
                  attachmentsTable.refresh();
              });
          } catch (DatabaseException e) {
              ErrorHandler.handleException(e, "Failed to load attachments", null);
          }
      }, FinancialManagementApp.executor);
  }

  private void searchAttachments(String query) {
      CompletableFuture.runAsync(() -> {
          try {
              List<Attachment> attachments = dbService.searchAttachments(query);
              LOGGER.info("Fetched {} attachments for query '{}'", attachments.size(), query);
              for (Attachment att : attachments) {
                  LOGGER.info("Attachment: entityId={}, fileName={}, contactName={}, invoiceType={}", 
                      att.getEntityId(), att.getFileName(), att.getContactName(), att.getInvoiceType());
              }
              javafx.application.Platform.runLater(() -> {
                  attachmentsTable.getItems().clear();
                  for (Attachment att : attachments) {
                      Map<String, Object> map = new HashMap<>();
                      map.put("invoiceId", att.getEntityId());
                      map.put("fileName", att.getFileName());
                      String invoiceType = att.getInvoiceType();
                      if ("Sale".equalsIgnoreCase(invoiceType)) {
                          map.put("customerName", att.getContactName() != null ? att.getContactName() : "");
                          map.put("supplierName", "");
                      } else if ("Import Purchase".equalsIgnoreCase(invoiceType)) {
                          map.put("customerName", "");
                          map.put("supplierName", att.getContactName() != null ? att.getContactName() : "");
                      } else {
                          map.put("customerName", "");
                          map.put("supplierName", "");
                      }
                      map.put("uploadDate", att.getUploadDate());
                      map.put("attachmentId", att.getId());
                      attachmentsTable.getItems().add(map);
                  }
                  attachmentCountLabel.setText("Attachments: " + attachmentsTable.getItems().size());
                  attachmentsTable.refresh();
              });
          } catch (DatabaseException e) {
              ErrorHandler.handleException(e, "Failed to search attachments", null);
          }
      }, FinancialManagementApp.executor);
  }
} // Add this closing brace to close the class