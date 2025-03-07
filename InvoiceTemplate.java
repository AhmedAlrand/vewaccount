package com.example.financial;

import java.util.List;
import java.util.Map;

public class InvoiceTemplate {
    private String templateName;
    private String invoiceType;
    private Integer customerId;
    private String date;
    private String defaultTerms;
    private Map<String, Object> templateStyle;
    private Map<String, String> brandingColors;
    private String brandingFont;
    private List<InvoiceLineItem> defaultItems;

    public InvoiceTemplate() {}

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDefaultTerms() { return defaultTerms; }
    public void setDefaultTerms(String defaultTerms) { this.defaultTerms = defaultTerms; }
    public Map<String, Object> getTemplateStyle() { return templateStyle; }
    public void setTemplateStyle(Map<String, Object> templateStyle) { this.templateStyle = templateStyle; }
    public Map<String, String> getBrandingColors() { return brandingColors; }
    public void setBrandingColors(Map<String, String> brandingColors) { this.brandingColors = brandingColors; }
    public String getBrandingFont() { return brandingFont; }
    public void setBrandingFont(String brandingFont) { this.brandingFont = brandingFont; }
    public List<InvoiceLineItem> getDefaultItems() { return defaultItems; }
    public void setDefaultItems(List<InvoiceLineItem> defaultItems) { this.defaultItems = defaultItems; }

    @Override
    public String toString() {
        return "Template: " + templateName + " (" + invoiceType + ")";
    }
}