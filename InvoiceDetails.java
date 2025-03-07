package com.example.financial;

import java.util.List;
import java.util.Map;

public class InvoiceDetails {
    private String invoiceId;
    private String invoiceType;
    private String customerName;
    private String supplierName;
    private String date;
    private double amountDue;
    private double amountPaid;
    private double amountReceived;
    private String currency;
    private String paymentTerm;
    private String status;
    private String paymentInstructions;
    private String notes;
    private List<InvoiceLineItem> lineItems;
    private double totalPaymentDue;
    private double totalAmount;
    private double totalTax;
    private double invoiceDiscount;
    private double invoiceFixedDiscount;
    private double invoiceTaxRate;
    private double shippingCharge;
    private double transportingFee; // New field
    private double uploadingFee;    // New field
    private double taxFee;          // New field
    private String logoPath;
    private double exchangeRate;
    private Map<String, String> customFields;
    private String paymentCurrency;

    public InvoiceDetails(String invoiceId, String invoiceType, String customerName, String supplierName, String date,
                          double amountDue, double amountPaid, double amountReceived, String currency,
                          String paymentTerm, String status, String paymentInstructions, String notes,
                          List<InvoiceLineItem> lineItems, double totalPaymentDue, double totalAmount, double totalTax,
                          double invoiceDiscount, double invoiceFixedDiscount, double invoiceTaxRate,
                          double shippingCharge, String logoPath, double exchangeRate, Map<String, String> customFields,
                          String paymentCurrency) {
        this.invoiceId = invoiceId;
        this.invoiceType = invoiceType;
        this.customerName = customerName;
        this.supplierName = supplierName;
        this.date = date;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.amountReceived = amountReceived;
        this.currency = currency;
        this.paymentTerm = paymentTerm;
        this.status = status;
        this.paymentInstructions = paymentInstructions;
        this.notes = notes;
        this.lineItems = lineItems;
        this.totalPaymentDue = totalPaymentDue;
        this.totalAmount = totalAmount;
        this.totalTax = totalTax;
        this.invoiceDiscount = invoiceDiscount;
        this.invoiceFixedDiscount = invoiceFixedDiscount;
        this.invoiceTaxRate = invoiceTaxRate;
        this.shippingCharge = shippingCharge;
        this.transportingFee = 0.0; // Default until set
        this.uploadingFee = 0.0;
        this.taxFee = 0.0;
        this.logoPath = logoPath;
        this.exchangeRate = exchangeRate;
        this.customFields = customFields;
        this.paymentCurrency = paymentCurrency;
    }

    // Getters
    public String getInvoiceId() { return invoiceId; }
    public String getInvoiceType() { return invoiceType; }
    public String getCustomerName() { return customerName; }
    public String getSupplierName() { return supplierName; }
    public String getDate() { return date; }
    public double getAmountDue() { return amountDue; }
    public double getAmountPaid() { return amountPaid; }
    public double getAmountReceived() { return amountReceived; }
    public String getCurrency() { return currency; }
    public String getPaymentTerm() { return paymentTerm; }
    public String getStatus() { return status; }
    public String getPaymentInstructions() { return paymentInstructions; }
    public String getNotes() { return notes; }
    public List<InvoiceLineItem> getLineItems() { return lineItems; }
    public double getTotalPaymentDue() { return totalPaymentDue; }
    public double getTotalAmount() { return totalAmount; }
    public double getTotalTax() { return totalTax; }
    public double getInvoiceDiscount() { return invoiceDiscount; }
    public double getInvoiceFixedDiscount() { return invoiceFixedDiscount; }
    public double getInvoiceTaxRate() { return invoiceTaxRate; }
    public double getShippingCharge() { return shippingCharge; }
    public double getTransportingFee() { return transportingFee; }
    public double getUploadingFee() { return uploadingFee; }
    public double getTaxFee() { return taxFee; }
    public String getLogoPath() { return logoPath; }
    public double getExchangeRate() { return exchangeRate; }
    public Map<String, String> getCustomFields() { return customFields; }
    public String getPaymentCurrency() { return paymentCurrency; }

    // Setters for new fields (needed for loading from DB)
    public void setTransportingFee(double transportingFee) { this.transportingFee = transportingFee; }
    public void setUploadingFee(double uploadingFee) { this.uploadingFee = uploadingFee; }
    public void setTaxFee(double taxFee) { this.taxFee = taxFee; }
}