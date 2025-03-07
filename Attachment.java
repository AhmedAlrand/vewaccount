package com.example.financial;

public class Attachment {
    private final int id;
    private final String entityType;
    private final String entityId;
    private final int contactId;
    private final String contactName;
    private final String fileName;
    private final long fileSize;
    private final String uploadDate;
    private final byte[] content;
    private final String invoiceType; // New field

    public Attachment(int id, String entityType, String entityId, int contactId, String contactName, 
                      String fileName, long fileSize, String uploadDate, byte[] content) {
        this(id, entityType, entityId, contactId, contactName, fileName, fileSize, uploadDate, content, null);
    }

    public Attachment(int id, String entityType, String entityId, int contactId, String contactName, 
                      String fileName, long fileSize, String uploadDate, byte[] content, String invoiceType) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.contactId = contactId;
        this.contactName = contactName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
        this.content = content;
        this.invoiceType = invoiceType;
    }

    public int getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public int getContactId() { return contactId; }
    public String getContactName() { return contactName; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getUploadDate() { return uploadDate; }
    public byte[] getContent() { return content; }
    public String getInvoiceType() { return invoiceType; }
}