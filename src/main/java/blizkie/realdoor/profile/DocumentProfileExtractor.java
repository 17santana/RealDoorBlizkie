package blizkie.realdoor.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentProfileExtractor {

    private final FieldExtractionService fieldExtractionService;
    private final OcrLineExtractor ocrLineExtractor;

    @Autowired
    public DocumentProfileExtractor(FieldExtractionService fieldExtractionService) {
        this(fieldExtractionService, new OcrLineExtractor());
    }

    DocumentProfileExtractor(FieldExtractionService fieldExtractionService, OcrLineExtractor ocrLineExtractor) {
        this.fieldExtractionService = fieldExtractionService;
        this.ocrLineExtractor = ocrLineExtractor;
    }

    public List<ExtractedField> extract(byte[] pdfBytes, String documentType) throws IOException {
        List<TextLine> lines = PdfLineExtractor.extractLines(pdfBytes);
        if (lines.isEmpty()) {
            // No text layer: this is a rasterized (scanned-style) document, so fall back to OCR.
            lines = ocrLineExtractor.extractLines(pdfBytes);
        }
        return fieldExtractionService.extract(documentType, lines);
    }
}
