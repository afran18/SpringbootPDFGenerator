package com.ararts.quotation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps the "quotation:" block of application.yml to a typed object.
 */
@ConfigurationProperties(prefix = "quotation")
public record QuotationProperties(
        BigDecimal defaultGstPercent,
        int defaultValidityDays,
        List<String> corsAllowedOrigins,
        Company company,
        List<String> terms) {

    public record Company(
            String name,
            String services,
            String address,
            String phone,
            String email,
            String gstin) {
    }
}
