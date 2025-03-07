
package com.example.financial;

public class Adjustment {
    private final int id;
    private final String type;
    private final String description;
    private final double amount;
    private final Integer accountFrom;
    private final Integer accountTo;
    private final String date;
    private final String currency;
    private final double exchangeRate;

    public Adjustment(int id, String type, String description, double amount, Integer accountFrom, Integer accountTo, String date, String currency, double exchangeRate) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.amount = amount;
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.date = date;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public Integer getAccountFrom() {
        return accountFrom;
    }

    public Integer getAccountTo() {
        return accountTo;
    }

    public String getDate() {
        return date;
    }

    public String getCurrency() {
        return currency;
    }

    public double getExchangeRate() {
        return exchangeRate;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Type: " + type + ", Description: " + description + ", Amount: " + amount + " " + currency + ", Date: " + date;
    }
}
