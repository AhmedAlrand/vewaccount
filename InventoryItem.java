
package com.example.financial;

public class InventoryItem {
    private final int productId;
    private final int warehouseId;
    private final int quantity;

    public InventoryItem(int productId, int warehouseId, int quantity) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public int getProductId() {
        return productId;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "Product ID: " + productId + ", Warehouse ID: " + warehouseId + ", Quantity: " + quantity;
    }
}

