package com.ararts.quotation.service;

import com.ararts.quotation.exception.PdfGenerationException;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * STEP 1:  .hbs template + data (Map)  ->  HTML string
 */
@Service
public class TemplateRenderer {

    private final Handlebars handlebars;

    public TemplateRenderer(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    /**
     * @param templateName file name without folder/extension, e.g. "quotation"
     * @param model        the values the template can use, e.g. {{customer.name}}
     */
    public String render(String templateName, Map<String, Object> model) {
        try {
            Template template = handlebars.compile(templateName);
            return template.apply(model);
        } catch (IOException e) {
            throw new PdfGenerationException("Failed to render template '" + templateName + "'", e);
        }
    }
}
