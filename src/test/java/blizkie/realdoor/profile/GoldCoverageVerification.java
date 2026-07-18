package blizkie.realdoor.profile;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Whole-corpus regression guard: runs extraction over every document in the starter pack and asserts
 * we recover 100% of the gold fields. Reads the pack from its working-directory-relative path (which
 * is git-ignored), so it self-skips wherever the pack is absent.
 */
class GoldCoverageVerification {

    private static final Path PACK = Path.of("realdoor-hackathon-starter-pack/synthetic_documents");
    private final DocumentProfileExtractor extractor = new DocumentProfileExtractor(new FieldExtractionService());

    @Test
    void everyGoldFieldInEveryDocumentIsExtractedExactly() throws Exception {
        assumeThat(Files.exists(PACK))
                .as("starter pack present at " + PACK.toAbsolutePath())
                .isTrue();
        ObjectMapper mapper = new ObjectMapper();
        List<String> goldLines = Files.readAllLines(PACK.resolve("gold/document_gold.jsonl"));

        int docs = 0, fullyCovered = 0, totalGold = 0, totalMatched = 0;
        System.out.println("\n===== GOLD COVERAGE =====");
        for (String line : goldLines) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode row = mapper.readTree(line);
            String docId = row.get("document_id").asString();
            String docType = row.get("document_type").asString();
            String fileName = row.get("file_name").asString();
            boolean raster = row.path("rasterized").asBoolean(false);

            Map<String, String> gold = new LinkedHashMap<>();
            for (JsonNode f : row.get("fields")) {
                gold.put(f.get("field").asString(), normalize(f.get("value").asString()));
            }

            byte[] pdf = Files.readAllBytes(PACK.resolve("documents/" + fileName));
            Map<String, String> got = new LinkedHashMap<>();
            for (ExtractedField ef : extractor.extract(pdf, docType)) {
                got.put(ef.field(), normalize(String.valueOf(ef.value())));
            }

            int matched = 0;
            StringBuilder misses = new StringBuilder();
            for (var e : gold.entrySet()) {
                if (e.getValue().equals(got.get(e.getKey()))) {
                    matched++;
                } else {
                    misses.append(" ").append(e.getKey())
                            .append("(gold=").append(e.getValue())
                            .append(", got=").append(got.get(e.getKey())).append(")");
                }
            }
            docs++;
            totalGold += gold.size();
            totalMatched += matched;
            boolean full = matched == gold.size();
            if (full) {
                fullyCovered++;
            }
            System.out.printf("%-11s %-19s %-4s %d/%d%s%n",
                    docId, docType, raster ? "OCR" : "text", matched, gold.size(),
                    full ? "  OK" : "  MISS:" + misses);
        }
        System.out.printf("%n%d/%d documents fully covered; %d/%d gold fields matched exactly.%n",
                fullyCovered, docs, totalMatched, totalGold);

        assertThat(totalMatched)
                .as("every gold field extracted exactly across all documents")
                .isEqualTo(totalGold);
    }

    /** Compare on numeric value where possible so 850 and 850.0 match, else on trimmed text. */
    private static String normalize(String v) {
        try {
            return String.valueOf(Double.parseDouble(v));
        } catch (NumberFormatException e) {
            return v.trim();
        }
    }
}
