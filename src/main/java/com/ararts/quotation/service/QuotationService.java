package com.ararts.quotation.service;

import com.ararts.quotation.config.QuotationProperties;
import com.ararts.quotation.dto.CustomerDto;
import com.ararts.quotation.dto.ItemDto;
import com.ararts.quotation.dto.QuotationRequest;
import com.ararts.quotation.util.AmountInWords;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Orchestrates everything:
 *   request JSON  ->  calculations + model Map  ->  HTML (Handlebars)  ->  PDF bytes
 */
@Service
public class QuotationService {

    private static final String TEMPLATE = "quotation";
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TemplateRenderer templateRenderer;
    private final PdfGenerator pdfGenerator;
    private final QuotationProperties properties;

    public QuotationService(TemplateRenderer templateRenderer,
                            PdfGenerator pdfGenerator,
                            QuotationProperties properties) {
        this.templateRenderer = templateRenderer;
        this.pdfGenerator = pdfGenerator;
        this.properties = properties;
    }

    /** Full flow: data -> HTML -> PDF. */
    public GeneratedPdf generatePdf(QuotationRequest request) {
        Map<String, Object> model = buildModel(request);
        String html = templateRenderer.render(TEMPLATE, model);
        byte[] pdf = pdfGenerator.fromHtml(html);

        String fileName = "Quotation-" + sanitize((String) model.get("quotationNumber")) + ".pdf";
        return new GeneratedPdf(fileName, pdf);
    }

    /** Only the HTML - handy while designing the template (open it in a browser). */
    public String renderHtml(QuotationRequest request) {
        return templateRenderer.render(TEMPLATE, buildModel(request));
    }

    // ------------------------------------------------------------------
    // Model building: this Map is exactly what the .hbs file can access
    // ------------------------------------------------------------------

    Map<String, Object> buildModel(QuotationRequest request) {
        LocalDate date = request.quotationDate() != null ? request.quotationDate() : LocalDate.now();
        int validDays = request.validForDays() != null
                ? request.validForDays() : properties.defaultValidityDays();
        BigDecimal gstPercent = request.gstPercent() != null
                ? request.gstPercent() : properties.defaultGstPercent();
        BigDecimal discount = money(request.discount() != null ? request.discount() : BigDecimal.ZERO);

        // ---- items + subtotal
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemDto item : request.items()) {
            BigDecimal amount = money(item.quantity().multiply(item.rate()));
            subtotal = subtotal.add(amount);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", item.name().trim());
            row.put("description", blankToEmpty(item.description()));
            row.put("size", blankToEmpty(item.size()));
            row.put("unit", blankToEmpty(item.unit()));
            row.put("quantity", item.quantity().stripTrailingZeros().toPlainString());
            row.put("rate", money(item.rate()));
            row.put("amount", amount);
            rows.add(row);
        }

        // ---- totals
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount cannot be greater than the subtotal");
        }
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal gstAmount = money(taxable.multiply(gstPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal total = taxable.add(gstAmount);

        // ---- model
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("company", companyModel());
        model.put("customer", customerModel(request.customer()));
        model.put("items", rows);

        model.put("quotationNumber", StringUtils.hasText(request.quotationNumber())
                ? request.quotationNumber().trim() : generateNumber(date));
        model.put("quotationDate", date.format(DISPLAY_DATE));
        model.put("validUntil", date.plusDays(validDays).format(DISPLAY_DATE));
        model.put("validForDays", validDays);

        model.put("subtotal", subtotal);
        model.put("discount", discount);
        model.put("hasDiscount", discount.signum() > 0);
        model.put("taxable", taxable);
        model.put("gstPercent", gstPercent.stripTrailingZeros().toPlainString());
        model.put("gstAmount", gstAmount);
        model.put("total", total);
        model.put("amountInWords", AmountInWords.rupees(total));

        model.put("notes", blankToEmpty(request.notes()));
        model.put("terms", properties.terms() != null ? properties.terms() : List.of());
        return model;
    }

    private Map<String, Object> companyModel() {
        QuotationProperties.Company c = properties.company();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.name());
        m.put("initials", initials(c.name()));
        m.put("services", c.services());
        m.put("address", blankToEmpty(c.address()));
        m.put("phone", blankToEmpty(c.phone()));
        m.put("email", blankToEmpty(c.email()));
        m.put("gstin", blankToEmpty(c.gstin()));
        return m;
    }

    private Map<String, Object> customerModel(CustomerDto c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.name().trim());
        m.put("phone", blankToEmpty(c.phone()));
        m.put("email", blankToEmpty(c.email()));
        m.put("address", blankToEmpty(c.address()));
        m.put("gstin", blankToEmpty(c.gstin()));
        return m;
    }

    // ------------------------------------------------------------------
    // small helpers
    // ------------------------------------------------------------------

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** "AR Arts" -> "AR" ;  "Sri Lakshmi Flex" -> "SL" */
    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (!word.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.toString();
    }

    /** Demo numbering. In a real app use a DB sequence: AR/2026-27/0001. */
    private static String generateNumber(LocalDate date) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "AR-" + date.format(NUMBER_DATE) + "-" + random;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
