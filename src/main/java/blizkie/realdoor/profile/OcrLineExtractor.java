package blizkie.realdoor.profile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads rasterized (scanned-style) fixtures that carry no text layer by rendering each page and
 * running Tesseract OCR on the pixels.
 *
 * <p>Tesseract is invoked as an external process reading the image from stdin. This is deliberate:
 * the locally installed Leptonica corrupts file <em>paths</em> (it truncates to the basename), but
 * reading an image stream from stdin works correctly and avoids a fragile JNI/native binding.</p>
 *
 * <p>Word boxes come back in image pixels with a top-left origin; they are scaled to PDF points and
 * flipped to the bottom-left origin so downstream matching is identical to the text-layer path.
 * Individual OCR word tokens are then re-grouped into cells (e.g. the label "GROSS PAY") so the same
 * label/value column logic applies.</p>
 */
public class OcrLineExtractor {

    private static final float RENDER_DPI = 200f;
    private static final double OCR_ROW_TOLERANCE = 6.0;
    private static final double OCR_CELL_GAP = 18.0;
    private static final double MIN_WORD_CONFIDENCE = 40.0;

    private final String tesseractCommand;

    public OcrLineExtractor() {
        this("tesseract");
    }

    public OcrLineExtractor(String tesseractCommand) {
        this.tesseractCommand = tesseractCommand;
    }

    public List<TextLine> extractLines(byte[] pdfBytes) throws IOException {
        List<TextLine> words = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                double pageHeightPoints = page.getMediaBox().getHeight();
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI);
                double scale = pageHeightPoints / image.getHeight();
                words.addAll(ocrPage(image, pageIndex + 1, pageHeightPoints, scale));
            }
        }
        return TextLines.mergeSameRow(words, OCR_ROW_TOLERANCE, OCR_CELL_GAP, true);
    }

    private List<TextLine> ocrPage(BufferedImage image, int pageNumber, double pageHeightPoints, double scale)
            throws IOException {
        String tsv = runTesseract(image);
        List<TextLine> words = new ArrayList<>();
        for (String line : tsv.split("\n")) {
            String[] cols = line.split("\t");
            if (cols.length < 12 || cols[0].equals("level")) {
                continue;
            }
            String text = cols[11].trim();
            if (text.isEmpty() || parseDouble(cols[10]) < MIN_WORD_CONFIDENCE) {
                continue;
            }
            double left = Double.parseDouble(cols[6]);
            double top = Double.parseDouble(cols[7]);
            double width = Double.parseDouble(cols[8]);
            double height = Double.parseDouble(cols[9]);

            double x1 = left * scale;
            double x2 = (left + width) * scale;
            double y2 = pageHeightPoints - (top * scale);
            double y1 = pageHeightPoints - ((top + height) * scale);
            words.add(new TextLine(pageNumber, text, x1, y1, x2, y2));
        }
        return words;
    }

    private String runTesseract(BufferedImage image) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);

        ProcessBuilder builder = new ProcessBuilder(tesseractCommand, "-", "stdout", "tsv");
        builder.redirectErrorStream(false);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException("Could not launch '" + tesseractCommand
                    + "'. Install it (e.g. `brew install tesseract`) to read rasterized documents.", e);
        }

        try {
            process.getOutputStream().write(png.toByteArray());
            process.getOutputStream().close();
            byte[] out = process.getInputStream().readAllBytes();
            process.getErrorStream().readAllBytes();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IOException("Tesseract exited with code " + exit);
            }
            return new String(out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR interrupted", e);
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
