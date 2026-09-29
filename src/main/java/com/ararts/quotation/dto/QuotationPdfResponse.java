package com.ararts.quotation.dto;

/**
 * What React receives. "base64" is the PDF file encoded as text.
 */
public record QuotationPdfResponse(
        String fileName,
        String contentType,
        long sizeInBytes,
        String base64) {
}
