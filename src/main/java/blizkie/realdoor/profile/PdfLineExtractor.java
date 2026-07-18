package blizkie.realdoor.profile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts one {@link TextLine} per drawn text run, with bbox converted to PDF-points,
 * bottom-left origin (matching the gold field convention) from PDFBox's top-down glyph coordinates.
 *
 * <p>These fixtures stamp a large rotated "SYNTHETIC — NOT A REAL DOCUMENT" watermark that
 * spatially overlaps real form values; PDFBox's default word grouping sometimes fuses a
 * watermark glyph run onto an adjacent value (e.g. "...OCUMENT6-07-06"). The watermark is set
 * in a much larger font (28pt) than any real form field (10-12pt), so glyphs are filtered
 * per-glyph by font size rather than by their (rotation-distorted) bounding box. That can still
 * leave a genuine value split across two runs (a kerning-adjusted TJ array), so adjacent
 * same-row runs with a small x-gap are re-merged afterward.</p>
 */
public class PdfLineExtractor extends PDFTextStripper {

    private static final double WATERMARK_FONT_SIZE = 15.0;
    private static final double SAME_ROW_TOLERANCE = 1.5;
    private static final double ADJACENT_RUN_GAP = 6.0;

    private final List<TextLine> lines = new ArrayList<>();
    private double pageHeight;

    private PdfLineExtractor() throws IOException {
        super();
        setSortByPosition(true);
    }

    public static List<TextLine> extractLines(byte[] pdfBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PdfLineExtractor stripper = new PdfLineExtractor();
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            stripper.getText(document);
            return TextLines.mergeSameRow(stripper.lines, SAME_ROW_TOLERANCE, ADJACENT_RUN_GAP, false);
        }
    }

    @Override
    protected void startPage(PDPage page) throws IOException {
        pageHeight = page.getMediaBox().getHeight();
        super.startPage(page);
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) {
        StringBuilder kept = new StringBuilder();
        double x1 = Double.MAX_VALUE;
        double x2 = -Double.MAX_VALUE;
        double topDownTop = Double.MAX_VALUE;
        double topDownBottom = -Double.MAX_VALUE;
        for (TextPosition position : textPositions) {
            if (position.getFontSizeInPt() > WATERMARK_FONT_SIZE) {
                continue;
            }
            double left = position.getXDirAdj();
            double right = left + position.getWidthDirAdj();
            double bottom = position.getYDirAdj();
            double top = bottom - position.getHeightDir();
            kept.append(position.getUnicode());
            x1 = Math.min(x1, left);
            x2 = Math.max(x2, right);
            topDownTop = Math.min(topDownTop, top);
            topDownBottom = Math.max(topDownBottom, bottom);
        }
        if (kept.isEmpty() || kept.toString().isBlank()) {
            return;
        }
        double y1 = pageHeight - topDownBottom;
        double y2 = pageHeight - topDownTop;
        lines.add(new TextLine(getCurrentPageNo(), kept.toString().trim(), x1, y1, x2, y2));
    }
}
