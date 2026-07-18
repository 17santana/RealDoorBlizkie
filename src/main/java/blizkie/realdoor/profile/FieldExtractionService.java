package blizkie.realdoor.profile;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * These forms lay labels out in a grid: a row of labels, then a row of values directly
 * beneath, column-aligned by x-position (not simple reading-order adjacency, since a row
 * commonly holds several labels side by side). A field is omitted, never guessed, when its
 * label is missing or the aligned value fails to parse as the declared type.
 */
@Service
public class FieldExtractionService {

    private static final double COLUMN_TOLERANCE = 3.0;

    public List<ExtractedField> extract(String documentType, List<TextLine> lines) {
        List<FieldTemplate> templates = DocumentTemplates.BY_DOCUMENT_TYPE.get(documentType);
        if (templates == null) {
            throw new IllegalArgumentException("Unsupported document type: " + documentType);
        }

        List<ExtractedField> fields = new ArrayList<>();
        for (FieldTemplate template : templates) {
            findLabelLine(lines, template)
                    .flatMap(label -> findValueBelow(lines, label))
                    .flatMap(value -> parseValue(value, template.type()))
                    .ifPresent(field -> fields.add(new ExtractedField(
                            template.field(), field.value(), field.line().page(),
                            field.bbox(), 0.95)));
        }
        return fields;
    }

    private Optional<TextLine> findLabelLine(List<TextLine> lines, FieldTemplate template) {
        String label = template.labelText();
        return lines.stream().filter(line -> switch (template.match()) {
            case EXACT -> line.text().equalsIgnoreCase(label);
            case PREFIX -> line.text().regionMatches(true, 0, label, 0, label.length());
        }).findFirst();
    }

    private Optional<TextLine> findValueBelow(List<TextLine> lines, TextLine label) {
        return lines.stream()
                .filter(line -> line.page() == label.page())
                .filter(line -> line.y1() < label.y1())
                .filter(line -> Math.abs(line.x1() - label.x1()) <= COLUMN_TOLERANCE)
                .max(java.util.Comparator.comparingDouble(TextLine::y1));
    }

    private Optional<ParsedField> parseValue(TextLine valueLine, FieldType type) {
        try {
            Object value = switch (type) {
                case STRING -> valueLine.text();
                case INTEGER -> Integer.parseInt(valueLine.text().trim());
                case CURRENCY -> Double.parseDouble(valueLine.text().replace("$", "").replace(",", "").trim());
            };
            double[] bbox = {valueLine.x1(), valueLine.y1(), valueLine.x2(), valueLine.y2()};
            return Optional.of(new ParsedField(value, bbox, valueLine));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private record ParsedField(Object value, double[] bbox, TextLine line) {
    }
}
