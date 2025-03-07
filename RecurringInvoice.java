
package com.example.financial;

public class RecurringInvoice {
    private final int id;
    private final String invoiceId;
    private final String frequencyType;
    private final int frequencyInterval;
    private final String nextDate;
    private final String endDate;
    private final String lastGenerated;

    public RecurringInvoice(int id, String invoiceId, String frequencyType, int frequencyInterval, String nextDate, String endDate, String lastGenerated) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.frequencyType = frequencyType;
        this.frequencyInterval = frequencyInterval;
        this.nextDate = nextDate;
        this.endDate = endDate;
        this.lastGenerated = lastGenerated;
    }

    public int getId() {
        return id;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getFrequencyType() {
        return frequencyType;
    }

    public int getFrequencyInterval() {
        return frequencyInterval;
    }

    public String getNextDate() {
        return nextDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getLastGenerated() {
        return lastGenerated;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Invoice ID: " + invoiceId + ", Frequency: " + frequencyType + " every " + frequencyInterval + ", Next: " + nextDate;
    }
}
