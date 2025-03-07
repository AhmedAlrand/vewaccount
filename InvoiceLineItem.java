package com.example.financial;

public class InvoiceLineItem {
    private int id;
    private int productId;
    private int warehouseId;
    private int quantity;
    private String unit;
    private double originalUnitPrice; // New field for original price
    private double unitPrice;         // Adjusted price after fees
    private double totalPrice;
    private double paidAmount;
    private double receivedAmount;
    private double discount;
    private double fixedDiscount;
    private double taxRate;
    private double totalTax;
    private String currency;

    public InvoiceLineItem(int id, int productId, int warehouseId, int quantity, String unit, double unitPrice, 
                           double totalPrice, double paidAmount, double receivedAmount, double discount, 
                           double fixedDiscount, double taxRate, double totalTax, String currency) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.unit = unit;
        this.originalUnitPrice = unitPrice; // Store original price
        this.unitPrice = unitPrice;         // Initialize adjusted price as original
        this.totalPrice = totalPrice;
        this.paidAmount = paidAmount;
        this.receivedAmount = receivedAmount;
        this.discount = discount;
        this.fixedDiscount = fixedDiscount;
        this.taxRate = taxRate;
        this.totalTax = totalTax;
        this.currency = currency;
    }

    // Getters
    public int getId() { return id; }
    public int getProductId() { return productId; }
    public int getWarehouseId() { return warehouseId; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getOriginalUnitPrice() { return originalUnitPrice; } // New getter
    public double getUnitPrice() { return unitPrice; }
    public double getTotalPrice() { return totalPrice; }
    public double getPaidAmount() { return paidAmount; }
    public double getReceivedAmount() { return receivedAmount; }
    public double getDiscount() { return discount; }
    public double getFixedDiscount() { return fixedDiscount; }
    public double getTaxRate() { return taxRate; }
    public double getTotalTax() { return totalTax; }
    public String getCurrency() { return currency; }

    // Setters
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}