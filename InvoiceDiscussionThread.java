package com.example.financial;

import java.util.ArrayList;
import java.util.List;

public class InvoiceDiscussionThread {
    private final String invoiceId;
    private final List<InvoiceComment> commentHistory;

    public InvoiceDiscussionThread(String invoiceId) {
        this.invoiceId = invoiceId;
        this.commentHistory = new ArrayList<>();
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public List<InvoiceComment> getCommentHistory() {
        return commentHistory;
    }

    public void addComment(String user, String commentText) {
        commentHistory.add(new InvoiceComment(user, commentText));
    }

    public void addComment(InvoiceComment comment) {
        commentHistory.add(comment);
    }

    @Override
    public String toString() {
        return "Discussion for Invoice #" + invoiceId + " (" + commentHistory.size() + " comments)";
    }
}
