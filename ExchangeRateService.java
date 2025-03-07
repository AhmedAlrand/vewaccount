package com.example.financial;

public class ExchangeRateService {
    public double getExchangeRate(String fromCurrency, String toCurrency) throws DatabaseException {
        // Simplified mock implementation; in reality, fetch from DB or API
        if (fromCurrency.equals(toCurrency)) return 1.0;
        if (fromCurrency.equals("USD") && toCurrency.equals("IQD")) return 1310.0;
        if (fromCurrency.equals("IQD") && toCurrency.equals("USD")) return 1.0 / 1310.0;
        if (fromCurrency.equals("USD") && toCurrency.equals("RMB")) return 7.0;
        if (fromCurrency.equals("RMB") && toCurrency.equals("USD")) return 1.0 / 7.0;
        return 1.0; // Default fallback
    }

    public void updateExchangeRates() {
        // Mock implementation; real version would fetch from an API or DB
        System.out.println("Exchange rates updated (mock)");
    }
}