package com.ararts.quotation.service;

import com.ararts.quotation.exception.PdfGenerationException;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * STEP 2:  HTML string  ->  PDF bytes
 */
@Component
public class PdfGenerator {

    public byte[] fromHtml(String html) {
        String xhtml = toXhtml(html);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // Embed fonts. The built-in PDF fonts have no rupee (Rs) glyph, Noto Sans has.
            // The family names must match font-family in the CSS of the template.
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf"),
                    "Noto Sans", 400, FontStyle.NORMAL, true);
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/NotoSans-Bold.ttf"),
                    "Noto Sans", 700, FontStyle.NORMAL, true);

            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to convert HTML to PDF", e);
        }
    }

    /**
     * OpenHTMLToPDF parses the HTML as XML, so it must be well-formed
     * (every tag closed, no &nbsp; etc.). jsoup repairs and re-serialises it as XHTML.
     */
    private String toXhtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
        return doc.html();
    }
}
