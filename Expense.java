
package com.example.financial;

public class Expense {
    private final String description;
    private final double amount;
    private final String currency;
    private final String category;
    private final String date;

    public Expense(String description, double amount, String currency, String category, String date) {
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    @Override
    public String toString() {
        return description + " - " + amount + " " + currency + " (" + category + ") on " + date;
    }
}
