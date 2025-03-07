package com.example.financial;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.FileOutputStream;

public class AsyncInvoicePdfService {
    private final InvoiceDetails invoice;

    public AsyncInvoicePdfService(InvoiceDetails invoice) {
        this.invoice = invoice;
    }

    public void exportToPdf() throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream("Invoice_" + invoice.getInvoiceId() + ".pdf"));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.add(new Paragraph("Invoice #" + invoice.getInvoiceId()));
        document.add(new Paragraph("Customer: " + invoice.getCustomerName()));
        document.add(new Paragraph("Date: " + invoice.getDate()));
        document.add(new Paragraph("Total: " + invoice.getTotalAmount() + " " + invoice.getCurrency()));
        document.close();
    }

    public void printInvoice() throws Exception {
        exportToPdf();
        System.out.println("Printing invoice #" + invoice.getInvoiceId());
    }
}