package com.ararts.quotation.config;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Sets up the Handlebars engine:
 *  - where the .hbs files live  (src/main/resources/templates/*.hbs)
 *  - custom helpers you can call from a template, e.g. {{money total}}
 */
@Configuration
public class HandlebarsConfig {

    private static final Locale INDIA = Locale.forLanguageTag("en-IN");

    @Bean
    public Handlebars handlebars() {
        // "quotation"  ->  classpath:/templates/quotation.hbs
        // "{{> partials/header}}" -> classpath:/templates/partials/header.hbs
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");

        Handlebars handlebars = new Handlebars(loader);
        handlebars.with(new ConcurrentMapTemplateCache()); // compile each template only once

        // {{money 125000}}  ->  1,25,000.00   (Indian digit grouping)
        handlebars.registerHelper("money", (Helper<Object>) (value, options) -> {
            if (value == null) {
                return "0.00";
            }
            NumberFormat format = NumberFormat.getNumberInstance(INDIA);
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return format.format(new BigDecimal(value.toString()));
        });

        // {{inc @index}}  ->  @index is 0-based, this gives 1-based serial numbers
        handlebars.registerHelper("inc", (Helper<Object>) (value, options) ->
                ((Number) value).intValue() + 1);

        return handlebars;
    }
}
