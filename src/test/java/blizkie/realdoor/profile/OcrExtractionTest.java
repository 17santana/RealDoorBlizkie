package blizkie.realdoor.profile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Verifies the OCR fallback path against rasterized fixtures. These tests need the {@code tesseract}
 * binary on the PATH; if it is absent they are skipped rather than failed, so the build still passes
 * on machines without OCR installed.
 */
class OcrExtractionTest {

    private final DocumentProfileExtractor extractor = new DocumentProfileExtractor(new FieldExtractionService());

    @BeforeAll
    static void requireTesseract() {
        assumeThat(tesseractAvailable())
                .as("tesseract binary available on PATH")
                .isTrue();
    }

    @Test
    void ocrExtractsRasterizedPayStubFields() throws IOException {
        List<ExtractedField> fields = extractor.extract(
                readFixture("hh-001_d02_pay_stub.pdf"), "pay_stub");

        assertThat(fieldValues(fields)).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("person_name", "Mara North"),
                Map.entry("pay_date", "2026-06-27"),
                Map.entry("pay_period_start", "2026-06-10"),
                Map.entry("pay_period_end", "2026-06-23"),
                Map.entry("pay_frequency", "biweekly"),
                Map.entry("regular_hours", 76),
                Map.entry("hourly_rate", 28.5),
                Map.entry("gross_pay", 2166.0),
                Map.entry("net_pay", 1689.48)
        ));
    }

    @Test
    void ocrExtractsRasterizedEmploymentLetterFields() throws IOException {
        List<ExtractedField> fields = extractor.extract(
                readFixture("hh-002_d04_employment_letter.pdf"), "employment_letter");

        assertThat(fieldValues(fields)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "person_name", "Jonas Vale",
                "document_date", "2026-07-06",
                "weekly_hours", 40,
                "hourly_rate", 24.0
        ));
    }

    @Test
    void ocrFieldsCarryPdfPointBoxesCloseToGold() throws IOException {
        List<ExtractedField> fields = extractor.extract(
                readFixture("hh-001_d02_pay_stub.pdf"), "pay_stub");

        ExtractedField grossPay = fields.stream()
                .filter(f -> f.field().equals("gross_pay")).findFirst().orElseThrow();

        // Gold box for gross_pay is [340, 528, 397.38, 544] in PDF points; OCR boxes are tight to the
        // glyphs, so allow a few points of slack rather than demanding an exact match.
        assertThat(grossPay.bbox()[0]).isBetween(330.0, 350.0);
        assertThat(grossPay.bbox()[1]).isBetween(520.0, 535.0);
        assertThat(grossPay.page()).isEqualTo(1);
    }

    private Map<String, Object> fieldValues(List<ExtractedField> fields) {
        return fields.stream().collect(java.util.stream.Collectors.toMap(
                ExtractedField::field, ExtractedField::value));
    }

    private byte[] readFixture(String fileName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/documents/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: " + fileName);
            }
            return in.readAllBytes();
        }
    }

    private static boolean tesseractAvailable() {
        try {
            Process p = new ProcessBuilder("tesseract", "--version").start();
            p.getInputStream().readAllBytes();
            p.getErrorStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
