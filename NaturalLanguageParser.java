package com.example.financial;

import java.util.HashMap;
import java.util.Map;

public class NaturalLanguageParser {
    public Map<String, String> parseInvoiceQuery(String query) {
        Map<String, String> conditions = new HashMap<>();
        String[] tokens = query.toLowerCase().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "unpaid": conditions.put("unpaid", "true"); break;
                case "customer": if (i + 1 < tokens.length) conditions.put("customer", tokens[++i]); break;
                case "year": if (i + 1 < tokens.length) conditions.put("year", tokens[++i]); break;
                case "month": if (i + 1 < tokens.length) conditions.put("month", tokens[++i]); break;
                case "status": if (i + 1 < tokens.length) conditions.put("status", tokens[++i].toUpperCase()); break;
                case "start": if (i + 1 < tokens.length) conditions.put("startDate", tokens[++i]); break;
                case "end": if (i + 1 < tokens.length) conditions.put("endDate", tokens[++i]); break;
                case "sales": conditions.put("totalSales", "true"); break;
            }
        }
        return conditions;
    }

    public Map<String, String> parseExpenseQuery(String query) {
        Map<String, String> conditions = new HashMap<>();
        String[] tokens = query.toLowerCase().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "category": if (i + 1 < tokens.length) conditions.put("category", tokens[++i]); break;
                case "year": if (i + 1 < tokens.length) conditions.put("year", tokens[++i]); break;
                case "month": if (i + 1 < tokens.length) conditions.put("month", tokens[++i]); break;
            }
        }
        return conditions;
    }

    public Map<String, String> parseAdjustmentQuery(String query) {
        Map<String, String> conditions = new HashMap<>();
        String[] tokens = query.toLowerCase().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "type": if (i + 1 < tokens.length) conditions.put("type", tokens[++i].toUpperCase()); break;
                case "year": if (i + 1 < tokens.length) conditions.put("year", tokens[++i]); break;
                case "month": if (i + 1 < tokens.length) conditions.put("month", tokens[++i]); break;
            }
        }
        return conditions;
    }
}