package com.example.financial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import com.example.financial.Attachment;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);
    public static DataSource dataSource; // Set by DatabaseConfigDialog in FinancialManagementApp

    public DatabaseService() {
        // No initialization here; dataSource is set externally by DatabaseConfigDialog
    }

    public List<String> getCustomersWithNames() throws DatabaseException {
        List<String> customers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM Customers");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                customers.add(rs.getInt("id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch customers", e);
        }
        return customers;
    }

    public List<String> getSuppliersWithNames() throws DatabaseException {
        List<String> suppliers = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM Suppliers");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                suppliers.add(rs.getInt("id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch suppliers", e);
        }
        return suppliers;
    }

    public List<Product> getProducts() throws DatabaseException {
        List<Product> products = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM Products");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                products.add(new Product(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch products", e);
        }
        return products;
    }

    public List<String> getProductUnits(int productId) throws DatabaseException {
        List<String> units = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT unit FROM ProductUnits WHERE productId = ?")) {
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                units.add(rs.getString("unit"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch product units", e);
        }
        return units;
    }

    ///===========================

	public String saveInvoice(Integer customerId, Integer supplierId, String invoiceType, String date, double totalAmount,
                          double taxAmount, String currency, List<InvoiceLineItem> lineItems, String status,
                          String paymentInstructions, String paymentTerm, String notes, double exchangeRate,
                          double shippingFee, double transportingFee, double uploadingFee, double taxFee) 
                          throws DatabaseException {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        try {
            // Generate custom invoice ID
            String prefix;
            switch (invoiceType) {
                case "Sale": prefix = "Sell"; break;
                case "Purchase": prefix = "Purch"; break;
                case "Import Purchase": prefix = "Imp"; break;
                case "Credit Note": prefix = "Cred"; break;
                default: prefix = "Inv";
            }
            int nextNum = getNextInvoiceNumber(conn, prefix);
            String invoiceId = String.format("%s %05d", prefix, nextNum);

            try (PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Invoices (invoiceId, customerId, supplierId, invoiceType, date, totalAmount, taxAmount, " +
                 "currency, status, paymentInstructions, paymentTerm, notes, exchangeRate, shippingFee, transportingFee, " +
                 "uploadingFee, taxFee) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, invoiceId);
                stmt.setObject(2, customerId, java.sql.Types.INTEGER);
                stmt.setObject(3, supplierId, java.sql.Types.INTEGER);
                stmt.setString(4, invoiceType);
                stmt.setString(5, date);
                stmt.setDouble(6, totalAmount);
                stmt.setDouble(7, taxAmount);
                stmt.setString(8, currency);
                stmt.setString(9, status);
                stmt.setString(10, paymentInstructions);
                stmt.setString(11, paymentTerm);
                stmt.setString(12, notes);
                stmt.setDouble(13, exchangeRate);
                stmt.setDouble(14, shippingFee);
                stmt.setDouble(15, transportingFee);
                stmt.setDouble(16, uploadingFee);
                stmt.setDouble(17, taxFee);
                stmt.executeUpdate();
            }

            for (InvoiceLineItem item : lineItems) {
                try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO InvoiceLineItems (invoiceId, productId, warehouseId, quantity, unit, unitPrice, totalPrice, " +
                     "paidAmount, receivedAmount, discount, fixedDiscount, taxRate, totalTax, currency) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, invoiceId);
                    stmt.setInt(2, item.getProductId());
                    stmt.setInt(3, item.getWarehouseId());
                    stmt.setInt(4, item.getQuantity());
                    stmt.setString(5, item.getUnit());
                    stmt.setDouble(6, item.getUnitPrice());
                    stmt.setDouble(7, item.getTotalPrice());
                    stmt.setDouble(8, item.getPaidAmount());
                    stmt.setDouble(9, item.getReceivedAmount());
                    stmt.setDouble(10, item.getDiscount());
                    stmt.setDouble(11, item.getFixedDiscount());
                    stmt.setDouble(12, item.getTaxRate());
                    stmt.setDouble(13, item.getTotalTax());
                    stmt.setString(14, item.getCurrency());
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            LOGGER.info("Invoice saved with ID: {}", invoiceId);
            return invoiceId;
        } catch (SQLException e) {
            conn.rollback();
            throw new DatabaseException("Failed to save invoice", e);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to save invoice", e);
    }
}

private int getNextInvoiceNumber(Connection conn, String prefix) throws SQLException {
    String sql = "SELECT MAX(CAST(SUBSTRING(invoiceId, 6) AS INTEGER)) AS maxNum FROM Invoices WHERE invoiceId LIKE ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, prefix + " %");
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getObject("maxNum") != null) {
                return rs.getInt("maxNum") + 1;
            }
            return 1; // Start at 00001 if no previous invoices
        }
    }
}



  ///====================

public void updateInvoice(String invoiceId, Integer customerId, Integer supplierId, String invoiceType, String date, double totalAmount,
                          double taxAmount, String currency, List<InvoiceLineItem> lineItems, String status, String paymentInstructions,
                          String paymentTerm, String notes, double exchangeRate, double shippingFee, double transportingFee,
                          double uploadingFee, double taxFee) throws DatabaseException {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE Invoices SET customerId=?, supplierId=?, invoiceType=?, date=?, totalAmount=?, taxAmount=?, currency=?, status=?, paymentInstructions=?, paymentTerm=?, notes=?, exchangeRate=?, shippingFee=?, transportingFee=?, uploadingFee=?, taxFee=? " +
                 "WHERE invoiceId=?")) {
                stmt.setObject(1, customerId);
                stmt.setObject(2, supplierId);
                stmt.setString(3, invoiceType);
                stmt.setString(4, date);
                stmt.setDouble(5, totalAmount);
                stmt.setDouble(6, taxAmount);
                stmt.setString(7, currency);
                stmt.setString(8, status);
                stmt.setString(9, paymentInstructions);
                stmt.setString(10, paymentTerm);
                stmt.setString(11, notes);
                stmt.setDouble(12, exchangeRate);
                stmt.setDouble(13, shippingFee);
                stmt.setDouble(14, transportingFee);
                stmt.setDouble(15, uploadingFee);
                stmt.setDouble(16, taxFee);
                stmt.setString(17, invoiceId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM InvoiceLineItems WHERE invoiceId=?")) {
                stmt.setString(1, invoiceId);
                stmt.executeUpdate();
            }
            for (InvoiceLineItem item : lineItems) {
                try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO InvoiceLineItems (invoiceId, productId, warehouseId, quantity, unit, unitPrice, totalPrice, paidAmount, receivedAmount, discount, fixedDiscount, taxRate, totalTax, currency) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, invoiceId);
                    stmt.setInt(2, item.getProductId());
                    stmt.setInt(3, item.getWarehouseId());
                    stmt.setInt(4, item.getQuantity());
                    stmt.setString(5, item.getUnit());
                    stmt.setDouble(6, item.getOriginalUnitPrice());
                    stmt.setDouble(7, item.getTotalPrice());
                    stmt.setDouble(8, item.getPaidAmount());
                    stmt.setDouble(9, item.getReceivedAmount());
                    stmt.setDouble(10, item.getDiscount());
                    stmt.setDouble(11, item.getFixedDiscount());
                    stmt.setDouble(12, item.getTaxRate());
                    stmt.setDouble(13, item.getTotalTax());
                    stmt.setString(14, item.getCurrency());
                    stmt.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new DatabaseException("Failed to update invoice", e);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to update invoice", e);
    }
}

public void updateInvoiceLineItems(String invoiceId, List<InvoiceLineItem> lineItems) throws DatabaseException {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM InvoiceLineItems WHERE invoiceId=?")) {
                stmt.setString(1, invoiceId);
                stmt.executeUpdate();
            }
            for (InvoiceLineItem item : lineItems) {
                try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO InvoiceLineItems (invoiceId, productId, warehouseId, quantity, unit, unitPrice, totalPrice, paidAmount, receivedAmount, discount, fixedDiscount, taxRate, totalTax, currency) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    stmt.setString(1, invoiceId);
                    stmt.setInt(2, item.getProductId());
                    stmt.setInt(3, item.getWarehouseId());
                    stmt.setInt(4, item.getQuantity());
                    stmt.setString(5, item.getUnit());
                    stmt.setDouble(6, item.getUnitPrice()); // Use adjusted unitPrice
                    stmt.setDouble(7, item.getTotalPrice());
                    stmt.setDouble(8, item.getPaidAmount());
                    stmt.setDouble(9, item.getReceivedAmount());
                    stmt.setDouble(10, item.getDiscount());
                    stmt.setDouble(11, item.getFixedDiscount());
                    stmt.setDouble(12, item.getTaxRate());
                    stmt.setDouble(13, item.getTotalTax());
                    stmt.setString(14, item.getCurrency());
                    stmt.executeUpdate();
                }
            }
            conn.commit();
            LOGGER.info("Updated {} line items for invoice ID: {}", lineItems.size(), invoiceId);
        } catch (SQLException e) {
            conn.rollback();
            throw new DatabaseException("Failed to update invoice line items", e);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to update invoice line items", e);
    }
}

////==================

    public void updateInvoice(String invoiceId, Integer customerId, String invoiceType, String date, double totalAmount, double taxAmount,
                              String currency, List<InvoiceLineItem> lineItems, String supplierId, String paymentCurrency,
                              String paymentInstructions, String paymentTerm, String status, double exchangeRate,
                              double discount, double shippingCharge, double transportCharge, double shippingFee,
                              double transportingFee, double uploadingFee, double taxFee, String recurringId,
                              Map<String, String> customFields) throws DatabaseException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Invoices SET customerId = ?, supplierId = ?, invoiceType = ?, date = ?, totalAmount = ?, taxAmount = ?, " +
                     "currency = ?, status = ?, paymentInstructions = ?, paymentTerm = ?, exchangeRate = ?, " +
                     "shippingFee = ?, transportingFee = ?, uploadingFee = ?, taxFee = ? WHERE invoiceId = ?")) {
                stmt.setObject(1, customerId);
                stmt.setObject(2, supplierId != null ? Integer.parseInt(supplierId) : null);
                stmt.setString(3, invoiceType);
                stmt.setString(4, date);
                stmt.setDouble(5, totalAmount);
                stmt.setDouble(6, taxAmount);
                stmt.setString(7, currency);
                stmt.setString(8, status);
                stmt.setString(9, paymentInstructions);
                stmt.setString(10, paymentTerm);
                stmt.setDouble(11, exchangeRate);
                stmt.setDouble(12, shippingFee);
                stmt.setDouble(13, transportingFee);
                stmt.setDouble(14, uploadingFee);
                stmt.setDouble(15, taxFee);
                stmt.setString(16, invoiceId);
                stmt.executeUpdate();

                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM InvoiceLineItems WHERE invoiceId = ?")) {
                    deleteStmt.setString(1, invoiceId);
                    deleteStmt.executeUpdate();
                }

                for (InvoiceLineItem item : lineItems) {
                    try (PreparedStatement itemStmt = conn.prepareStatement(
                             "INSERT INTO InvoiceLineItems (invoiceId, productId, warehouseId, quantity, unit, unitPrice, totalPrice, " +
                             "paidAmount, receivedAmount, discount, fixedDiscount, taxRate, totalTax, currency) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        itemStmt.setString(1, invoiceId);
                        itemStmt.setInt(2, item.getProductId());
                        itemStmt.setInt(3, item.getWarehouseId());
                        itemStmt.setInt(4, item.getQuantity());
                        itemStmt.setString(5, item.getUnit());
                        itemStmt.setDouble(6, item.getUnitPrice());
                        itemStmt.setDouble(7, item.getTotalPrice());
                        itemStmt.setDouble(8, item.getPaidAmount());
                        itemStmt.setDouble(9, item.getReceivedAmount());
                        itemStmt.setDouble(10, item.getDiscount());
                        itemStmt.setDouble(11, item.getFixedDiscount());
                        itemStmt.setDouble(12, item.getTaxRate());
                        itemStmt.setDouble(13, item.getTotalTax());
                        itemStmt.setString(14, item.getCurrency());
                        itemStmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update invoice", e);
        }
    }

    public List<InvoiceDetails> getAllInvoices() throws DatabaseException {
        List<InvoiceDetails> invoices = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT i.invoiceId, i.customerId, c.name AS customerName, i.supplierId, s.name AS supplierName, i.date, " +
                 "i.totalAmount, i.taxAmount AS totalTax, i.currency, i.status, i.paymentInstructions, i.paymentTerm, i.notes, " +
                 "i.exchangeRate, i.shippingFee AS shippingCharge, i.transportingFee, i.uploadingFee, i.taxFee, i.invoiceType " +
                 "FROM Invoices i " +
                 "LEFT JOIN Customers c ON i.customerId = c.id " +
                 "LEFT JOIN Suppliers s ON i.supplierId = s.id")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                List<InvoiceLineItem> lineItems = getInvoiceLineItems(rs.getString("invoiceId"));
                InvoiceDetails invoice = new InvoiceDetails(
                    rs.getString("invoiceId"), rs.getString("invoiceType"), rs.getString("customerName"),
                    rs.getString("supplierName"), rs.getString("date"), rs.getDouble("totalAmount"), 0.0, 0.0,
                    rs.getString("currency"), rs.getString("paymentTerm"), rs.getString("status"),
                    rs.getString("paymentInstructions"), rs.getString("notes"), lineItems, rs.getDouble("totalAmount"),
                    rs.getDouble("totalAmount"), rs.getDouble("totalTax"), 0.0, 0.0, 0.0,
                    rs.getDouble("shippingCharge"), null, rs.getDouble("exchangeRate"), new HashMap<>(),
                    rs.getString("currency")
                );
                invoice.setTransportingFee(rs.getDouble("transportingFee"));
                invoice.setUploadingFee(rs.getDouble("uploadingFee"));
                invoice.setTaxFee(rs.getDouble("taxFee"));
                invoices.add(invoice);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all invoices", e);
        }
        return invoices;
    }

    //====================

	private List<InvoiceLineItem> getInvoiceLineItems(String invoiceId) throws DatabaseException {
    List<InvoiceLineItem> items = new ArrayList<>();
    String sql = "SELECT * FROM InvoiceLineItems WHERE invoiceId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, invoiceId);
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                items.add(new InvoiceLineItem(
                    rs.getInt("lineItemId"), rs.getInt("productId"), rs.getInt("warehouseId"),
                    rs.getInt("quantity"), rs.getString("unit"), rs.getDouble("unitPrice"),
                    rs.getDouble("totalPrice"), rs.getDouble("paidAmount"), rs.getDouble("receivedAmount"),
                    rs.getDouble("discount"), rs.getDouble("fixedDiscount"), rs.getDouble("taxRate"),
                    rs.getDouble("totalTax"), rs.getString("currency")
                ));
            }
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to get invoice line items", e);
    }
    return items;
}

	//=======

    public void deleteInvoice(String invoiceId) throws DatabaseException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt1 = conn.prepareStatement("DELETE FROM InvoiceLineItems WHERE invoiceId = ?");
                 PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM Invoices WHERE invoiceId = ?")) {
                stmt1.setString(1, invoiceId);
                stmt1.executeUpdate();
                stmt2.setString(1, invoiceId);
                stmt2.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete invoice: " + invoiceId, e);
        }
    }

    //=========================

	public List<InvoiceDetails> searchInvoices(String whereClause, List<String> params) throws DatabaseException {
    List<InvoiceDetails> invoices = new ArrayList<>();
    String sql = "SELECT * FROM Invoices WHERE " + whereClause;
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        for (int i = 0; i < params.size(); i++) {
            stmt.setString(i + 1, params.get(i));
        }
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String invoiceType = rs.getString("invoiceType");
                String customerName = invoiceType.equals("Import Purchase") ? "" : 
                                     (rs.getInt("customerId") != 0 ? getCustomerName(rs.getInt("customerId")) : "");
                String supplierName = invoiceType.equals("Import Purchase") ? 
                                     (rs.getInt("supplierId") != 0 ? getSupplierName(rs.getInt("supplierId")) : "") : "";
                List<InvoiceLineItem> lineItems = getInvoiceLineItems(rs.getString("invoiceId"));
                InvoiceDetails invoice = new InvoiceDetails(
                    rs.getString("invoiceId"), invoiceType,
                    customerName, supplierName,
                    rs.getString("date"), rs.getDouble("totalAmount") - rs.getDouble("taxAmount"),
                    0.0, 0.0, rs.getString("currency"), rs.getString("paymentTerm"), rs.getString("status"),
                    rs.getString("paymentInstructions"), rs.getString("notes"), lineItems,
                    rs.getDouble("totalAmount"), rs.getDouble("totalAmount"), rs.getDouble("taxAmount"),
                    0.0, 0.0, 0.0, rs.getDouble("shippingFee"), null, rs.getDouble("exchangeRate"), null, null
                );
                invoice.setTransportingFee(rs.getDouble("transportingFee"));
                invoice.setUploadingFee(rs.getDouble("uploadingFee"));
                invoice.setTaxFee(rs.getDouble("taxFee"));
                invoices.add(invoice);
            }
            LOGGER.info("Retrieved {} invoices for query: {} with params: {}", invoices.size(), whereClause, params);
            if (invoices.isEmpty()) {
                LOGGER.warn("No invoices found for query: {} with params: {}", whereClause, params);
            }
        }
    } catch (SQLException e) {
        LOGGER.error("Failed to search invoices with query: {} and params: {}", whereClause, params, e);
        throw new DatabaseException("Failed to search invoices: " + e.getMessage(), e);
    }
    return invoices;
}

//===========================

private String getCustomerName(int customerId) throws DatabaseException {
    String sql = "SELECT name FROM Customers WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, customerId);
        try (ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getString("name") : "Unknown Customer";
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to get customer name for ID: " + customerId, e);
    }
}

private String getSupplierName(int supplierId) throws DatabaseException {
    String sql = "SELECT name FROM Suppliers WHERE id = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, supplierId);
        try (ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getString("name") : "Unknown Supplier";
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to get supplier name for ID: " + supplierId, e);
    }
}


//==========================

    public double getCustomerBalance(int customerId) throws DatabaseException {
    double balance = 0.0;
    String invoiceSql = "SELECT SUM(totalAmount) FROM Invoices WHERE customerId = ? AND invoiceType = 'Sale' AND status != 'PAID'";
    String paymentSql = "SELECT SUM(amount) FROM Payments WHERE customerId = ? AND supplierId IS NULL";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement invoiceStmt = conn.prepareStatement(invoiceSql);
         PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {
        invoiceStmt.setInt(1, customerId);
        try (ResultSet rs = invoiceStmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                balance += rs.getDouble(1);
                LOGGER.info("Customer ID {} invoice total: {}", customerId, balance);
            } else {
                LOGGER.info("No unpaid invoices for customer ID {}", customerId);
            }
        }
        paymentStmt.setInt(1, customerId);
        try (ResultSet rs = paymentStmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                balance -= rs.getDouble(1);
                LOGGER.info("Customer ID {} payment total subtracted, new balance: {}", customerId, balance);
            } else {
                LOGGER.info("No payments for customer ID {}", customerId);
            }
        }
    } catch (SQLException e) {
        LOGGER.error("Failed to get customer balance for ID {}", customerId, e);
        throw new DatabaseException("Failed to get customer balance: " + e.getMessage(), e);
    }
    return balance;
}
    //==========
	public double getSupplierBalance(int supplierId) throws DatabaseException {
    double balance = 0.0;
    String invoiceSql = "SELECT SUM(totalAmount) FROM Invoices WHERE supplierId = ? AND invoiceType = 'Import Purchase' AND status != 'PAID'";
    String paymentSql = "SELECT SUM(amount) FROM Payments WHERE supplierId = ? AND customerId IS NULL";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement invoiceStmt = conn.prepareStatement(invoiceSql);
         PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {
        invoiceStmt.setInt(1, supplierId);
        try (ResultSet rs = invoiceStmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                balance += rs.getDouble(1);
                LOGGER.info("Supplier ID {} invoice total: {}", supplierId, balance);
            } else {
                LOGGER.info("No unpaid invoices for supplier ID {}", supplierId);
            }
        }
        paymentStmt.setInt(1, supplierId);
        try (ResultSet rs = paymentStmt.executeQuery()) {
            if (rs.next() && rs.getObject(1) != null) {
                balance -= rs.getDouble(1);
                LOGGER.info("Supplier ID {} payment total subtracted, new balance: {}", supplierId, balance);
            } else {
                LOGGER.info("No payments for supplier ID {}", supplierId);
            }
        }
    } catch (SQLException e) {
        LOGGER.error("Failed to get supplier balance for ID {}", supplierId, e);
        throw new DatabaseException("Failed to get supplier balance: " + e.getMessage(), e);
    }
    return balance;
}

///=============================
	public void markPaymentReceived(String customerName, double amount, String currency, double exchangeRate, String supplierName, String invoiceId) 
    throws DatabaseException {
    Integer customerId = null;
    Integer supplierId = null;
    try {
        if (customerName != null) {
            customerId = Integer.parseInt(customerName.split(" - ")[0]);
        } else if (supplierName != null) {
            supplierId = Integer.parseInt(supplierName.split(" - ")[0]);
        }
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
        LOGGER.error("Failed to parse ID from name: customer={}, supplier={}", customerName, supplierName, e);
        throw new DatabaseException("Invalid contact name format", e);
    }
    String sql = "INSERT INTO Payments (customerId, supplierId, amount, currency, exchangeRate, date, status) " +
                 "VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
        stmt.setObject(1, customerId, java.sql.Types.INTEGER);
        stmt.setObject(2, supplierId, java.sql.Types.INTEGER);
        stmt.setDouble(3, amount);
        stmt.setString(4, currency);
        stmt.setDouble(5, exchangeRate);
        stmt.setString(6, customerId != null ? "RECEIVED" : "PAID"); // Set status dynamically
        int rows = stmt.executeUpdate();
        LOGGER.info("Inserted payment: {} rows affected for {}", rows, customerName != null ? customerName : supplierName);
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
                LOGGER.info("Payment ID generated: {}", rs.getInt(1));
            }
        }
        // Update invoice status if linked
        if (invoiceId != null) {
            updateInvoiceStatus(invoiceId, amount / exchangeRate, customerId != null);
        }
        conn.commit();
        LOGGER.info("Payment committed: {} {} (converted to {} USD) for {}", 
            amount, currency, amount / exchangeRate, customerName != null ? customerName : supplierName);
    } catch (SQLException e) {
        LOGGER.error("Failed to mark payment for customer={}, supplier={}", customerName, supplierName, e);
        throw new DatabaseException("Failed to mark payment received: " + e.getMessage(), e);
    }
}

private void updateInvoiceStatus(String invoiceId, double paymentAmount, boolean isCustomer) throws DatabaseException {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        try {
            // Get invoice details
            String sql = "SELECT totalAmount, status FROM Invoices WHERE invoiceId = ?";
            double totalAmount = 0.0;
            String currentStatus = "";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, invoiceId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalAmount = rs.getDouble("totalAmount");
                        currentStatus = rs.getString("status");
                    }
                }
            }
            if ("OPEN".equals(currentStatus)) {
                double remaining = totalAmount - paymentAmount;
                String newStatus = remaining <= 0 ? "PAID" : "PARTIALLY_PAID";
                try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE Invoices SET status = ? WHERE invoiceId = ?")) {
                    stmt.setString(1, newStatus);
                    stmt.setString(2, invoiceId);
                    stmt.executeUpdate();
                    LOGGER.info("Updated invoice {} status to {}", invoiceId, newStatus);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new DatabaseException("Failed to update invoice status", e);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to update invoice status", e);
    }
}

///============================

    public void addCustomer(String name, String contactInfo) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Customers (name, contactInfo) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, contactInfo);
            stmt.executeUpdate();
            LOGGER.debug("Added new customer: {}", name);
        } catch (SQLException e) {
            LOGGER.error("Failed to add customer: {}", name, e);
            throw new DatabaseException("Failed to add customer", e);
        }
    }

    public void addSupplier(String name, String contactInfo) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Suppliers (name, contactInfo) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setString(2, contactInfo);
            stmt.executeUpdate();
            LOGGER.debug("Added new supplier: {}", name);
        } catch (SQLException e) {
            LOGGER.error("Failed to add supplier: {}", name, e);
            throw new DatabaseException("Failed to add supplier", e);
        }
    }

    //======

public List<Map<String, String>> getCustomerActivity(int customerId) throws DatabaseException {
    List<Map<String, String>> activities = new ArrayList<>();
    String invoiceSql = "SELECT invoiceId AS id, invoiceType AS type, date, totalAmount AS amount, currency, status " +
                       "FROM Invoices WHERE customerId = ? AND invoiceType = 'Sale'";
    String paymentSql = "SELECT paymentId AS id, 'Payment' AS type, date, amount, currency, status " +
                       "FROM Payments WHERE customerId = ? AND supplierId IS NULL";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement invoiceStmt = conn.prepareStatement(invoiceSql);
         PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {
        // Invoices
        invoiceStmt.setInt(1, customerId);
        try (ResultSet rs = invoiceStmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> activity = new HashMap<>();
                String invoiceId = rs.getString("id");
                activity.put("id", invoiceId);
                activity.put("type", rs.getString("type"));
                activity.put("date", rs.getString("date"));
                activity.put("amount", String.valueOf(rs.getDouble("amount")));
                activity.put("currency", rs.getString("currency"));
                activity.put("status", rs.getString("status"));
                activities.add(activity);
                LOGGER.info("Customer activity added: ID='{}', Type='{}'", invoiceId, rs.getString("type"));
            }
        }
        // Payments
        paymentStmt.setInt(1, customerId);
        try (ResultSet rs = paymentStmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> activity = new HashMap<>();
                String paymentId = String.valueOf(rs.getInt("id"));
                activity.put("id", paymentId);
                activity.put("type", rs.getString("type"));
                activity.put("date", rs.getString("date"));
                activity.put("amount", String.valueOf(rs.getDouble("amount")));
                activity.put("currency", rs.getString("currency"));
                activity.put("status", rs.getString("status"));
                activities.add(activity);
                LOGGER.info("Customer payment added: ID='{}'", paymentId);
            }
        }
        LOGGER.info("Retrieved {} activities for customer ID {}", activities.size(), customerId);
    } catch (SQLException e) {
        LOGGER.error("Failed to get customer activity for ID {}", customerId, e);
        throw new DatabaseException("Failed to get customer activity: " + e.getMessage(), e);
    }
    return activities;
}



//=========

public List<Map<String, String>> getSupplierActivity(int supplierId) throws DatabaseException {
    List<Map<String, String>> activities = new ArrayList<>();
    String invoiceSql = "SELECT invoiceId AS id, invoiceType AS type, date, totalAmount AS amount, currency, status " +
                       "FROM Invoices WHERE supplierId = ? AND invoiceType = 'Import Purchase'";
    String paymentSql = "SELECT paymentId AS id, 'Payment' AS type, date, amount, currency, status " +
                       "FROM Payments WHERE supplierId = ? AND customerId IS NULL";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement invoiceStmt = conn.prepareStatement(invoiceSql);
         PreparedStatement paymentStmt = conn.prepareStatement(paymentSql)) {
        // Invoices
        invoiceStmt.setInt(1, supplierId);
        try (ResultSet rs = invoiceStmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> activity = new HashMap<>();
                String invoiceId = rs.getString("id");
                activity.put("id", invoiceId);
                activity.put("type", rs.getString("type"));
                activity.put("date", rs.getString("date"));
                activity.put("amount", String.valueOf(rs.getDouble("amount")));
                activity.put("currency", rs.getString("currency"));
                activity.put("status", rs.getString("status"));
                activities.add(activity);
                LOGGER.info("Supplier activity added: ID='{}', Type='{}'", invoiceId, rs.getString("type"));
            }
        }
        // Payments
        paymentStmt.setInt(1, supplierId);
        try (ResultSet rs = paymentStmt.executeQuery()) {
            while (rs.next()) {
                Map<String, String> activity = new HashMap<>();
                String paymentId = String.valueOf(rs.getInt("id"));
                activity.put("id", paymentId);
                activity.put("type", rs.getString("type"));
                activity.put("date", rs.getString("date"));
                activity.put("amount", String.valueOf(rs.getDouble("amount")));
                activity.put("currency", rs.getString("currency"));
                activity.put("status", rs.getString("status"));
                activities.add(activity);
                LOGGER.info("Supplier payment added: ID='{}'", paymentId);
            }
        }
        LOGGER.info("Retrieved {} activities for supplier ID {}", activities.size(), supplierId);
    } catch (SQLException e) {
        LOGGER.error("Failed to get supplier activity for ID {}", supplierId, e);
        throw new DatabaseException("Failed to get supplier activity: " + e.getMessage(), e);
    }
    return activities;
}

//================

    public List<InventoryItem> getInventoryItems(int page, int pageSize) throws DatabaseException {
        List<InventoryItem> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT productId, warehouseId, quantity FROM Inventory LIMIT ? OFFSET ?")) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(new InventoryItem(rs.getInt("productId"), rs.getInt("warehouseId"), rs.getInt("quantity")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get inventory items", e);
        }
        return items;
    }

    public void addInventoryItem(int productId, int warehouseId, int quantityChange) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Inventory (productId, warehouseId, quantity) VALUES (?, ?, ?) " +
                 "ON DUPLICATE KEY UPDATE quantity = quantity + ?")) {
            stmt.setInt(1, productId);
            stmt.setInt(2, warehouseId);
            stmt.setInt(3, quantityChange);
            stmt.setInt(4, quantityChange);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add inventory item", e);
        }
    }
//================
    public List<Attachment> getAttachments(String entityType, String entityId) throws DatabaseException {
    List<Attachment> attachments = new ArrayList<>();
    String sql = "SELECT a.id, a.entityType, a.entityId, a.contactId, " +
                "CASE WHEN i.invoiceType = 'Import Purchase' THEN COALESCE(s.name, c.name, a.contactName, '') " +
                "     WHEN i.invoiceType = 'Sale' THEN COALESCE(c.name, s.name, a.contactName, '') " +
                "     ELSE COALESCE(c.name, s.name, a.contactName, '') END AS contactName, " +
                "a.fileName, a.fileSize, a.uploadDate, a.content, i.invoiceType " +
                "FROM Attachments a " +
                "LEFT JOIN Invoices i ON a.entityId = i.invoiceId " +
                "LEFT JOIN Customers c ON i.customerId = c.id " +
                "LEFT JOIN Suppliers s ON i.supplierId = s.id " +
                "WHERE a.entityType = ? AND a.entityId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, entityType);
        stmt.setString(2, entityId);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            attachments.add(new Attachment(
                rs.getInt("id"),
                rs.getString("entityType"),
                rs.getString("entityId"),
                rs.getInt("contactId"),
                rs.getString("contactName"),
                rs.getString("fileName"),
                rs.getLong("fileSize"),
                rs.getString("uploadDate"),
                rs.getBytes("content"),
                rs.getString("invoiceType")
            ));
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to get attachments", e);
    }
    return attachments;
}
    //=======================

     public void addAttachment(String entityId, String fileName, byte[] fileData) throws DatabaseException {
        String sql = "INSERT INTO Attachments (entityType, entityId, fileName, fileSize, uploadDate, content) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "invoices"); // Default to "invoices"; adjust based on context if needed
            stmt.setString(2, entityId);
            stmt.setString(3, fileName);
            stmt.setLong(4, fileData.length);
            stmt.setString(5, LocalDate.now().toString());
            stmt.setBytes(6, fileData);
            int rows = stmt.executeUpdate();
            LOGGER.info("Added attachment for entity {}: {} ({} rows affected)", entityId, fileName, rows);
        } catch (SQLException e) {
            LOGGER.error("Failed to add attachment for entity {}: {}", entityId, e.getMessage());
            throw new DatabaseException("Failed to add attachment: " + e.getMessage(), e);
        }
    }
//==============================

public int getAttachmentCount(String entityId, String entityType) throws DatabaseException {
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "SELECT COUNT(*) FROM Attachments WHERE entityId = ? AND entityType = ?")) {
        stmt.setString(1, entityId);
        stmt.setString(2, entityType);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    } catch (SQLException e) {
        LOGGER.error("Failed to get attachment count for entity {}: {}", entityId, e.getMessage());
        throw new DatabaseException("Failed to get attachment count: " + e.getMessage(), e);
    }
}

//==========================


//public List<Map<String, Object>> searchAttachments(String query) throws DatabaseException {
    //List<Map<String, Object>> results = new ArrayList<>();
    //String sql = "SELECT a.attachmentId, a.invoiceId, a.fileName, a.uploadDate, " +
               //  "i.customerId, i.supplierId, " +
                // "COALESCE(c.name, '') AS customerName, COALESCE(s.name, '') AS supplierName " +
              //   "FROM Attachments a " +
            //     "JOIN Invoices i ON a.invoiceId = i.invoiceId " +
          //       "LEFT JOIN Customers c ON i.customerId = c.customerId " +
        //         "LEFT JOIN Suppliers s ON i.supplierId = s.supplierId " +
      //           "WHERE c.name LIKE ? OR s.name LIKE ? OR a.invoiceId LIKE ?";
    //try (Connection conn = dataSource.getConnection();
         //PreparedStatement stmt = conn.prepareStatement(sql)) {
        //String likeQuery = "%" + query + "%";
        //stmt.setString(1, likeQuery);
        //stmt.setString(2, likeQuery);
        //stmt.setString(3, likeQuery);
        //try (ResultSet rs = stmt.executeQuery()) {
            //while (rs.next()) {
                //Map<String, Object> attachment = new HashMap<>();
                //attachment.put("attachmentId", rs.getLong("attachmentId"));
                //attachment.put("invoiceId", rs.getString("invoiceId"));
                //attachment.put("fileName", rs.getString("fileName"));
                //attachment.put("uploadDate", rs.getTimestamp("uploadDate"));
                //attachment.put("customerName", rs.getString("customerName"));
              //  attachment.put("supplierName", rs.getString("supplierName"));
            //    results.add(attachment);
          //  }
        //    LOGGER.info("Found {} attachments for query: {}", results.size(), query);
      //  }
    //} catch (SQLException e) {
        //LOGGER.error("Failed to search attachments for query {}: {}", query, e.getMessage());
      //  throw new DatabaseException("Failed to search attachments: " + e.getMessage(), e);
    //}
  //  return results;
//}

public byte[] getAttachmentData(long attachmentId) throws DatabaseException {
    String sql = "SELECT fileData FROM Attachments WHERE attachmentId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setLong(1, attachmentId);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                byte[] data = rs.getBytes("fileData");
                LOGGER.info("Retrieved attachment data for ID {}", attachmentId);
                return data;
            }
            LOGGER.warn("No attachment found for ID {}", attachmentId);
            return null;
        }
    } catch (SQLException e) {
        LOGGER.error("Failed to get attachment data for ID {}: {}", attachmentId, e.getMessage());
        throw new DatabaseException("Failed to get attachment data: " + e.getMessage(), e);
    }
}


  //====================  

    public byte[] getAttachmentContent(int attachmentId) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT content FROM Attachments WHERE id = ?")) {
            stmt.setInt(1, attachmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("content");
            }
            return new byte[0];
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get attachment content", e);
        }
    }

//==========================
    public void updateAttachment(int attachmentId, File file) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE Attachments SET fileName = ?, fileSize = ?, uploadDate = ? WHERE id = ?")) {
            stmt.setString(1, file.getName());
            stmt.setLong(2, file.length());
            stmt.setString(3, LocalDate.now().toString());
            stmt.setInt(4, attachmentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update attachment", e);
        }
    }

    

    public List<Expense> getExpenses(int page, int pageSize) throws DatabaseException {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT description, amount, currency, category, date FROM Expenses LIMIT ? OFFSET ?")) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenses.add(new Expense(
                    rs.getString("description"), rs.getDouble("amount"), rs.getString("currency"),
                    rs.getString("category"), rs.getString("date")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get expenses", e);
        }
        return expenses;
    }

    public void addExpense(String category, double amount, String date, String description, String currency) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Expenses (category, amount, date, description, currency) VALUES (?, ?, ?, ?, ?)")) {
            stmt.setString(1, category);
            stmt.setDouble(2, amount);
            stmt.setString(3, date);
            stmt.setString(4, description);
            stmt.setString(5, currency);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add expense", e);
        }
    }

    public void setBudget(String category, double amount, String startDate, String endDate) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Budgets (category, amount, startDate, endDate) VALUES (?, ?, ?, ?) " +
                 "ON DUPLICATE KEY UPDATE amount = ?, startDate = ?, endDate = ?")) {
            stmt.setString(1, category);
            stmt.setDouble(2, amount);
            stmt.setString(3, startDate);
            stmt.setString(4, endDate);
            stmt.setDouble(5, amount);
            stmt.setString(6, startDate);
            stmt.setString(7, endDate);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to set budget", e);
        }
    }

    public List<RecurringInvoice> getRecurringInvoices(int page, int pageSize) throws DatabaseException {
        List<RecurringInvoice> invoices = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, invoiceId, frequencyType, frequencyInterval, nextDate, endDate, lastGenerated " +
                 "FROM RecurringInvoices LIMIT ? OFFSET ?")) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                invoices.add(new RecurringInvoice(
                    rs.getInt("id"), rs.getString("invoiceId"), rs.getString("frequencyType"),
                    rs.getInt("frequencyInterval"), rs.getString("nextDate"), rs.getString("endDate"),
                    rs.getString("lastGenerated")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get recurring invoices", e);
        }
        return invoices;
    }

    public void saveRecurringInvoice(String customerId, String frequency, int interval, String startDate, String endDate, boolean active) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO RecurringInvoices (invoiceId, frequencyType, frequencyInterval, nextDate, endDate, lastGenerated) " +
                 "VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, customerId); // Assuming customerId is used as invoiceId here
            stmt.setString(2, frequency);
            stmt.setInt(3, interval);
            stmt.setString(4, startDate); // Next date starts as startDate
            stmt.setString(5, endDate);
            stmt.setString(6, null); // Last generated starts null
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save recurring invoice", e);
        }
    }

    public double getTotalSales() throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT SUM(totalAmount) AS total FROM Invoices WHERE invoiceType = 'Sale' AND status != 'CANCELLED'")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get total sales", e);
        }
    }

    public double getOverdueAmount() throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT SUM(totalAmount) AS overdue FROM Invoices WHERE status = 'OPEN' AND date < ?")) {
            stmt.setString(1, LocalDate.now().toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("overdue");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get overdue amount", e);
        }
    }

    //========

public double getCashBalance() throws DatabaseException {
    double balance = 0.0;
    String sql = "SELECT (SELECT COALESCE(SUM(amount), 0) FROM Payments WHERE customerId IS NOT NULL AND supplierId IS NULL) - " +
                 "(SELECT COALESCE(SUM(amount), 0) FROM Payments WHERE supplierId IS NOT NULL AND customerId IS NULL) AS balance";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                balance = rs.getDouble("balance");
                LOGGER.info("Cash balance calculated: {}", balance);
            }
        }
    } catch (SQLException e) {
        LOGGER.error("Failed to get cash balance", e);
        throw new DatabaseException("Failed to get cash balance: " + e.getMessage(), e);
    }
    return balance;
}

//========

    public Map<String, Double> getSalesByCustomer(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> sales = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT c.name, SUM(i.totalAmount) AS total " +
                 "FROM Invoices i JOIN Customers c ON i.customerId = c.id " +
                 "WHERE i.invoiceType = 'Sale' AND i.date BETWEEN ? AND ? " +
                 "GROUP BY c.name")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sales.put(rs.getString("name"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get sales by customer", e);
        }
        return sales;
    }

    public Map<String, Double> getTaxReport(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> taxes = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT currency, SUM(taxAmount) AS totalTax FROM Invoices " +
                 "WHERE date BETWEEN ? AND ? GROUP BY currency")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                taxes.put(rs.getString("currency"), rs.getDouble("totalTax"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get tax report", e);
        }
        return taxes;
    }

    public List<Adjustment> getAdjustments(int page, int pageSize) throws DatabaseException {
        List<Adjustment> adjustments = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, type, description, amount, accountFrom, accountTo, date, currency, exchangeRate " +
                 "FROM Adjustments LIMIT ? OFFSET ?")) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                adjustments.add(new Adjustment(
                    rs.getInt("id"), rs.getString("type"), rs.getString("description"), rs.getDouble("amount"),
                    rs.getInt("accountFrom") == 0 ? null : rs.getInt("accountFrom"),
                    rs.getInt("accountTo") == 0 ? null : rs.getInt("accountTo"),
                    rs.getString("date"), rs.getString("currency"), rs.getDouble("exchangeRate")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get adjustments", e);
        }
        return adjustments;
    }

    public void addAdjustment(String type, String entityId, double amount, Integer productId, Integer warehouseId, String date, String reason, double exchangeRate) throws DatabaseException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO Adjustments (type, description, amount, accountFrom, accountTo, date, currency, exchangeRate) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, type);
            stmt.setString(2, reason); // Using reason as description
            stmt.setDouble(3, amount);
            stmt.setObject(4, productId); // Assuming accountFrom maps to productId
            stmt.setObject(5, warehouseId); // Assuming accountTo maps to warehouseId
            stmt.setString(6, date);
            stmt.setString(7, "USD"); // Default currency, adjust as needed
            stmt.setDouble(8, exchangeRate);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to add adjustment", e);
        }
    }

    public Map<String, Double> generateCompanyFinancialSummary(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> summary = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT SUM(totalAmount) AS sales, SUM(taxAmount) AS taxes FROM Invoices " +
                 "WHERE date BETWEEN ? AND ? AND invoiceType = 'Sale'")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                summary.put("Sales", rs.getDouble("sales"));
                summary.put("Taxes", rs.getDouble("taxes"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate financial summary", e);
        }
        return summary;
    }

    public Map<String, Double> generateCashFlowStatement(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> cashFlow = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT " +
                 "(SELECT COALESCE(SUM(amount), 0) FROM Payments WHERE contactType = 'Customer' AND date BETWEEN ? AND ?) AS inflow, " +
                 "(SELECT COALESCE(SUM(amount), 0) FROM Payments WHERE contactType = 'Supplier' AND date BETWEEN ? AND ?) AS outflow")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setString(3, startDate);
            stmt.setString(4, endDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                cashFlow.put("Inflow", rs.getDouble("inflow"));
                cashFlow.put("Outflow", rs.getDouble("outflow"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate cash flow statement", e);
        }
        return cashFlow;
    }

    public Map<String, Double> generateBalanceSheet(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> balanceSheet = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT SUM(totalAmount) AS receivables FROM Invoices WHERE status = 'OPEN' AND date <= ? AND customerId IS NOT NULL")) {
            stmt.setString(1, endDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                balanceSheet.put("Receivables", rs.getDouble("receivables"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate balance sheet", e);
        }
        return balanceSheet;
    }

    public Map<String, Double> generateProfitLossStatement(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> profitLoss = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT SUM(totalAmount) AS revenue FROM Invoices WHERE invoiceType = 'Sale' AND date BETWEEN ? AND ?")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                profitLoss.put("Revenue", rs.getDouble("revenue"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate profit/loss statement", e);
        }
        return profitLoss;
    }

    public List<Object[]> getAgingAnalysis() throws DatabaseException {
        List<Object[]> aging = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT invoiceId, totalAmount, DATEDIFF(CURRENT_DATE, date) AS daysOverdue " +
                 "FROM Invoices WHERE status = 'OPEN' AND date < ?")) {
            stmt.setString(1, LocalDate.now().toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                aging.add(new Object[]{rs.getString("invoiceId"), rs.getDouble("totalAmount"), rs.getInt("daysOverdue")});
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get aging analysis", e);
        }
        return aging;
    }

    public Map<String, Double> getSalesByProduct(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> sales = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT p.name, SUM(li.totalPrice) AS total " +
                 "FROM InvoiceLineItems li JOIN Products p ON li.productId = p.id " +
                 "JOIN Invoices i ON li.invoiceId = i.invoiceId " +
                 "WHERE i.date BETWEEN ? AND ? AND i.invoiceType = 'Sale' " +
                 "GROUP BY p.name")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sales.put(rs.getString("name"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get sales by product", e);
        }
        return sales;
    }

    // Adjusted to make 'months' optional by providing a default value
    public Map<String, Double> getCashFlowProjection(String startDate, String endDate, int months) throws DatabaseException {
        return new HashMap<>(); // Stub implementation
    }

    public Map<String, Double> getCashFlowProjection(String startDate, String endDate) throws DatabaseException {
        return getCashFlowProjection(startDate, endDate, 1); // Default to 1 month
    }

    public Map<String, Double> getBudgetVsActual(String startDate, String endDate) throws DatabaseException {
        Map<String, Double> budgetVsActual = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT b.category, b.amount AS budget, COALESCE(SUM(e.amount), 0) AS actual " +
                 "FROM Budgets b LEFT JOIN Expenses e ON b.category = e.category " +
                 "AND e.date BETWEEN ? AND ? " +
                 "WHERE b.startDate <= ? AND b.endDate >= ? " +
                 "GROUP BY b.category, b.amount")) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setString(3, startDate);
            stmt.setString(4, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                budgetVsActual.put(rs.getString("category") + "_Budget", rs.getDouble("budget"));
                budgetVsActual.put(rs.getString("category") + "_Actual", rs.getDouble("actual"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get budget vs actual", e);
        }
        return budgetVsActual;
    }

    public Map<String, Double> getCashFlowForecast(String startDate, String endDate, int months) throws DatabaseException {
        return new HashMap<>(); // Stub
    }

    public List<AuditEntry> getAuditLogs(int page, int pageSize) throws DatabaseException {
        List<AuditEntry> logs = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT timestamp, userId AS \"user\", entityType AS tableName, entityId AS recordId, action, " +
                 "description AS oldValue, description AS newValue FROM AuditLogs LIMIT ? OFFSET ?")) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(new AuditEntry(
                    rs.getString("timestamp"), rs.getString("user"), rs.getString("tableName"),
                    rs.getString("recordId"), rs.getString("action"), rs.getString("oldValue"),
                    rs.getString("newValue")
                ));
            }
            LOGGER.info("Retrieved {} audit logs", logs.size());
        } catch (SQLException e) {
            LOGGER.error("Failed to fetch audit logs", e);
            throw new DatabaseException("Failed to get audit logs", e);
        }
        return logs;
    }

    public List<String> getOverduePayments(int page, int pageSize) throws DatabaseException {
        List<String> overdue = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT invoiceId, totalAmount FROM Invoices WHERE status = 'OPEN' AND date < ? LIMIT ? OFFSET ?")) {
            stmt.setString(1, LocalDate.now().toString());
            stmt.setInt(2, pageSize);
            stmt.setInt(3, (page - 1) * pageSize);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                overdue.add("Invoice " + rs.getString("invoiceId") + ": " + rs.getDouble("totalAmount"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get overdue payments", e);
        }
        return overdue;
    }

    public void processRecurringInvoices() throws DatabaseException {
        // Stub
    }

    public List<String> getPendingReminders() throws DatabaseException {
        return new ArrayList<>(); // Stub
    }

    public void markReminderSent(String invoiceId) throws DatabaseException {
        // Stub
    }

    public void createNewDatabase(String dbName) throws DatabaseException {
        // Stub
    }

    public void close() {
        // Stub for closing dataSource if needed
    }

    public static class Product {
        private final int id;
        private final String name;

        public Product(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name;}

}
//==================================== 	

public int markPaymentReceived(String customerName, double amount, String currency, double exchangeRate, 
                               Integer customerId, Integer supplierId, String date, String status) throws DatabaseException {
    String sql = "INSERT INTO Payments (customerId, supplierId, amount, currency, exchangeRate, date, status) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?)"; // Adjusted to 7 placeholders
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        pstmt.setObject(1, customerId, Types.INTEGER);
        pstmt.setObject(2, supplierId, Types.INTEGER);
        pstmt.setDouble(3, amount);
        pstmt.setString(4, currency);
        pstmt.setDouble(5, exchangeRate);
        pstmt.setString(6, date);
        pstmt.setString(7, status);
        int rows = pstmt.executeUpdate();
        if (rows > 0) {
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int paymentId = rs.getInt(1);
                LOGGER.info("Inserted payment: {} rows affected, ID: {} for {}", rows, paymentId, customerName != null ? customerName : supplierId);
                return paymentId;
            }
        }
        throw new DatabaseException("Failed to retrieve generated payment ID", null);
    } catch (SQLException e) {
        throw new DatabaseException("Failed to mark payment", e);
    }
}


//-------------


public Map<String, String> getPaymentDetails(int paymentId) throws DatabaseException {
    String sql = "SELECT paymentId, amount, currency, date, status FROM Payments WHERE paymentId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, paymentId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            Map<String, String> payment = new HashMap<>();
            payment.put("paymentId", String.valueOf(rs.getInt("paymentId")));
            payment.put("amount", String.valueOf(rs.getDouble("amount")));
            payment.put("currency", rs.getString("currency"));
            payment.put("date", rs.getString("date"));
            payment.put("status", rs.getString("status"));
            return payment;
        }
        return null;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to retrieve payment details", e);
    }
}

//----------

public void updatePayment(int paymentId, double amount, String date, String currency, String status) throws DatabaseException {
    String sql = "UPDATE Payments SET amount = ?, date = ?, currency = ?, status = ? WHERE paymentId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setDouble(1, amount);
        pstmt.setString(2, date);
        pstmt.setString(3, currency);
        pstmt.setString(4, status);
        pstmt.setInt(5, paymentId);
        int rows = pstmt.executeUpdate();
        if (rows > 0) {
            LOGGER.info("Updated payment #{}", paymentId);
        } else {
            throw new DatabaseException("Payment #" + paymentId + " not found", null);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to update payment", e);
    }
}

//------------

public boolean paymentExists(int paymentId) throws DatabaseException {
    String sql = "SELECT COUNT(*) FROM Payments WHERE paymentId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, paymentId);
        ResultSet rs = pstmt.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to check payment existence", e);
    }
}

//---------------

public void deletePayment(int paymentId) throws DatabaseException {
    String sql = "DELETE FROM Payments WHERE paymentId = ?";
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, paymentId);
        int rows = pstmt.executeUpdate();
        if (rows > 0) {
            LOGGER.info("Deleted payment #{}", paymentId);
        } else {
            throw new DatabaseException("Payment #" + paymentId + " not found", null);
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to delete payment", e);
    }
}


//-------------

public List<Map<String, String>> getCustomerPayments(int customerId) throws DatabaseException {
    String sql = "SELECT paymentId, amount, currency, date, status FROM Payments WHERE customerId = ?";
    return getPayments(sql, customerId);
}

public List<Map<String, String>> getSupplierPayments(int supplierId) throws DatabaseException {
    String sql = "SELECT paymentId, amount, currency, date, status FROM Payments WHERE supplierId = ?";
    return getPayments(sql, supplierId);
}

private List<Map<String, String>> getPayments(String sql, int contactId) throws DatabaseException {
    List<Map<String, String>> payments = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, contactId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            Map<String, String> payment = new HashMap<>();
            payment.put("paymentId", String.valueOf(rs.getInt("paymentId")));
            payment.put("amount", String.valueOf(rs.getDouble("amount")));
            payment.put("currency", rs.getString("currency"));
            payment.put("date", rs.getString("date"));
            payment.put("status", rs.getString("status"));
            payments.add(payment);
        }
        return payments;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to retrieve payments", e);
    }
}


//================

public List<Attachment> getAttachmentsForInvoice(String invoiceId) throws DatabaseException {
    String sql = "SELECT id, entityType, entityId, contactId, contactName, fileName, fileSize, uploadDate, content " +
                 "FROM Attachments WHERE entityType = 'INVOICE' AND entityId = ?";
    List<Attachment> attachments = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, invoiceId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            Attachment attachment = new Attachment(
                rs.getInt("id"),
                rs.getString("entityType"),
                rs.getString("entityId"),
                rs.getInt("contactId"),
                rs.getString("contactName"),
                rs.getString("fileName"),
                rs.getLong("fileSize"),
                rs.getString("uploadDate"),
                rs.getBytes("content")
            );
            attachments.add(attachment);
        }
        LOGGER.info("Retrieved {} attachments for invoice {}", attachments.size(), invoiceId);
        return attachments;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to retrieve attachments for invoice " + invoiceId, e);
    }
}

public List<Attachment> searchAttachments(String query) throws DatabaseException {
    String sql = "SELECT id, entityType, entityId, contactId, contactName, fileName, fileSize, uploadDate, content " +
                 "FROM Attachments WHERE fileName LIKE ? OR entityId LIKE ?";
    List<Attachment> attachments = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        String likeQuery = "%" + query + "%";
        pstmt.setString(1, likeQuery);
        pstmt.setString(2, likeQuery);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            Attachment attachment = new Attachment(
                rs.getInt("id"),
                rs.getString("entityType"),
                rs.getString("entityId"),
                rs.getInt("contactId"),
                rs.getString("contactName"),
                rs.getString("fileName"),
                rs.getLong("fileSize"),
                rs.getString("uploadDate"),
                rs.getBytes("content")
            );
            attachments.add(attachment);
        }
        LOGGER.info("Found {} attachments matching query '{}'", attachments.size(), query);
        return attachments;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to search attachments with query " + query, e);
    }
}


//===============================
// Ensure the class closes here
} 





