# AR Arts — Quotation PDF Generator (Spring Boot + Handlebars)

Learning project: how to keep a PDF's layout in a `.hbs` (Handlebars) template,
fill it with data from a request body, turn it into a PDF, and hand it to a
frontend as base64. No frontend included — this is backend only, as requested.

## How the pieces fit together

```
POST /api/quotations/pdf  (JSON: customer + items)
        │
        ▼
QuotationController                – receives & validates the request
        │
        ▼
QuotationService.buildModel()      – does the math (line totals, GST, discount,
        │                             amount-in-words) and builds a Map<String,Object>
        ▼
TemplateRenderer.render()          – Handlebars: quotation.hbs + the Map  →  HTML string
        │
        ▼
PdfGenerator.fromHtml()            – jsoup cleans the HTML → OpenHTMLToPDF → PDF bytes
        │
        ▼
QuotationController                – Base64.getEncoder()  →  JSON response
```

* **Template engine**: [jknack/handlebars.java](https://github.com/jknack/handlebars.java) —
  the real Handlebars syntax (`{{ }}`, `{{#each}}`, `{{#if}}`, partials, custom helpers),
  not Thymeleaf. Templates live in `src/main/resources/templates/*.hbs`.
* **HTML → PDF**: [OpenHTMLToPDF](https://github.com/danfickle/openhtmltopdf) — a pure-Java
  CSS2.1/print renderer. It understands `@page`, page-break rules, and page-number counters,
  which is why the template can look like a real printed quotation.
* **Fonts**: Noto Sans is embedded (`src/main/resources/fonts`) because the ₹ symbol isn't in
  OpenHTMLToPDF's built-in PDF fonts. If you use different text/currency you can drop this.

## Project layout

```
src/main/java/com/ararts/quotation/
├── ArArtsQuotationApplication.java
├── config/
│   ├── QuotationProperties.java   # typed view of the "quotation:" block in application.yml
│   ├── HandlebarsConfig.java      # Handlebars bean + custom helpers ({{money}}, {{inc}})
│   └── WebConfig.java             # CORS for the React dev server
├── controller/
│   └── QuotationController.java   # 3 endpoints, see below
├── dto/                           # request/response records + bean validation
├── service/
│   ├── TemplateRenderer.java      # .hbs + data  ->  HTML   (STEP 1)
│   ├── PdfGenerator.java          # HTML         ->  PDF    (STEP 2)
│   ├── QuotationService.java      # orchestration + all the money math
│   └── GeneratedPdf.java
├── exception/                     # -> clean 400/500 JSON errors instead of stack traces
└── util/AmountInWords.java        # "₹31,116.60" -> "Rupees Thirty One Thousand ... Only"

src/main/resources/
├── application.yml                # company details, GST %, T&Cs — all editable, no rebuild
├── templates/
│   ├── quotation.hbs              # the whole PDF layout + CSS (this is the "design")
│   └── partials/header.hbs        # reusable header, included with {{> partials/header}}
└── fonts/                         # Noto Sans (Regular + Bold), embedded for the ₹ symbol
```

## Running it

This machine's sandbox can't reach Maven Central, so the build hasn't been compiled here —
but the code is complete and was **validated by rendering the actual `.hbs` template through
a real Handlebars engine and converting it to PDF** (see `docs/sample-request.json` for the
input and the screenshot shown in chat for the output). On your own machine, with internet
access:

```bash
mvn spring-boot:run
```

Then either endpoint below. If you don't have `mvn` installed, run:
`mvn -N wrapper:wrapper` once (needs internet) to generate `mvnw`/`mvnw.cmd`, or install
Maven / open the folder in IntelliJ, which bundles its own.

### 1. The endpoint your React app will call

```
POST http://localhost:8080/api/quotations/pdf
Content-Type: application/json
```

Body: `docs/sample-request.json` (customer + items; everything else is optional and
falls back to `application.yml` defaults). Response:

```json
{
  "fileName": "Quotation-AR-20260928-4821.pdf",
  "contentType": "application/pdf",
  "sizeInBytes": 48213,
  "base64": "JVBERi0xLjcNJeLjz9MNCjEgMCBvYmoNPDwvVHlwZS9DYXRhbG9n..."
}
```

In React:
```js
const res = await fetch("http://localhost:8080/api/quotations/pdf", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(payload),
});
const { base64, fileName } = await res.json();

// open it in a new tab
const win = window.open();
win.document.write(
  `<iframe src="data:application/pdf;base64,${base64}" width="100%" height="100%"></iframe>`
);

// or download it
const link = document.createElement("a");
link.href = `data:application/pdf;base64,${base64}`;
link.download = fileName;
link.click();
```

### 2. Dev helpers (skip base64 while you're building)

* `POST /api/quotations/pdf/raw` → same body, returns the raw PDF file directly (open in
  Postman or a browser tab).
* `POST /api/quotations/html` → returns the rendered HTML *before* PDF conversion — the
  fastest way to tweak `quotation.hbs` and refresh a browser tab to see CSS changes.

## Editing the design

Everything visual is in `quotation.hbs` (plain HTML + CSS in a `<style>` block) and
`partials/header.hbs`. A few things worth knowing about the Handlebars/OpenHTMLToPDF combo:

* `{{customer.name}}` — dot access into nested Map keys.
* `{{#each items}} ... {{/each}}` — loop; `{{inc @index}}` turns the 0-based index into a
  1-based serial number (custom helper in `HandlebarsConfig`).
* `{{#if hasDiscount}} ... {{/if}}` — booleans/conditionals; note Handlebars has no built-in
  "greater than", so booleans like `hasDiscount` are computed in `QuotationService` rather
  than in the template.
* `{{money amount}}` — custom helper for `1,25,000.00`-style Indian number formatting.
* `@page { size: A4; margin: ...; @bottom-right { content: "Page " counter(page) ... } }` —
  OpenHTMLToPDF-specific CSS for print page size and running footers.
* `page-break-inside: avoid` on `.items tr` and `.summary` — stops a row or the totals box
  from being split across two pages.

## Editable business settings (no code change needed)

`src/main/resources/application.yml`:
* `quotation.company.*` — your real company name, address, phone, GSTIN
* `quotation.default-gst-percent`, `quotation.default-validity-days`
* `quotation.terms` — the numbered Terms & Conditions list
* `quotation.cors-allowed-origins` — add your real React URL here later

## What comes from the frontend vs. what the backend computes

Per your requirement, **customer details, items, and price come from the frontend**
(`QuotationRequest` in the POST body). The backend:
* validates everything (`@NotBlank`, `@DecimalMin`, email format, etc. — bad input gets a
  400 with a field → message map, see `GlobalExceptionHandler`)
* calculates line amounts (`qty × rate`), subtotal, discount, GST, grand total, and the
  amount-in-words line — so the frontend can never send a manipulated total
* fills in quotation number/date/validity and company info if the frontend omits them

## Next steps to explore

* Swap the in-memory quotation-number generator for a DB sequence.
* Add a `logo` field (base64 image) to `application.yml` and an `<img>` in the header.
* Try a second `.hbs` template (e.g. `invoice.hbs`) — the same `TemplateRenderer` and
  `PdfGenerator` work for any template, just point `QuotationService` (or a new service)
  at a different template name.
