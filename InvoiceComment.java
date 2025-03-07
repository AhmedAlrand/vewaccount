
package com.example.financial;

import java.time.LocalDateTime;

public class InvoiceComment {
    private final String user;
    private final String commentText;
    private final LocalDateTime timestamp;

    public InvoiceComment(String user, String commentText) {
        this.user = user;
        this.commentText = commentText;
        this.timestamp = LocalDateTime.now();
    }

    public InvoiceComment(String user, String commentText, LocalDateTime timestamp) {
        this.user = user;
        this.commentText = commentText;
        this.timestamp = timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getCommentText() {
        return commentText;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timestamp + " - " + user + ": " + commentText;
    }
}
