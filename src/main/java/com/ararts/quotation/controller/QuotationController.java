package com.ararts.quotation.controller;

import com.ararts.quotation.dto.QuotationPdfResponse;
import com.ararts.quotation.dto.QuotationRequest;
import com.ararts.quotation.service.GeneratedPdf;
import com.ararts.quotation.service.QuotationService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    /**
     * MAIN ENDPOINT (used by React): returns the PDF as a base64 string inside JSON.
     */
    @PostMapping("/pdf")
    public QuotationPdfResponse generateBase64(@Valid @RequestBody QuotationRequest request) {
        GeneratedPdf pdf = quotationService.generatePdf(request);
        String base64 = Base64.getEncoder().encodeToString(pdf.content());

        return new QuotationPdfResponse(pdf.fileName(), MediaType.APPLICATION_PDF_VALUE,
                pdf.content().length, base64);
    }

    /**
     * DEV HELPER: returns the raw PDF so you can open it directly in Postman / browser
     * without decoding base64.
     */
    @PostMapping(value = "/pdf/raw", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateRaw(@Valid @RequestBody QuotationRequest request) {
        GeneratedPdf pdf = quotationService.generatePdf(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.fileName()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }

    /**
     * DEV HELPER: returns the rendered HTML (before PDF conversion). Great for learning
     * Handlebars and debugging layout problems.
     */
    @PostMapping(value = "/html", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@Valid @RequestBody QuotationRequest request) {
        return quotationService.renderHtml(request);
    }
}
