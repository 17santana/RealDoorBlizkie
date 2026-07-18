package blizkie.realdoor.profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shared helpers for reassembling positioned text fragments into logical cells. */
public final class TextLines {

    /**
     * Merges fragments that sit on the same visual row and are separated by no more than {@code maxGap}
     * points into a single cell (e.g. the two words of a "GROSS PAY" label), while keeping
     * column-separated cells apart. Fragments are joined left-to-right; a space is inserted between
     * them only when {@code spaceBetween} is true (OCR yields word tokens that need spacing; the PDF
     * text path yields character runs that must be concatenated verbatim).
     */
    public static List<TextLine> mergeSameRow(List<TextLine> fragments, double rowTolerance,
                                              double maxGap, boolean spaceBetween) {
        // Group into rows first, then order strictly left-to-right within each row. Sorting the whole
        // list by an exact y would let a fragment that is a fraction of a point higher jump ahead of
        // one to its left in the same row, which fuses adjacent columns in the wrong order.
        List<TextLine> byRow = new ArrayList<>(fragments);
        byRow.sort(Comparator.comparingInt(TextLine::page)
                .thenComparing(Comparator.comparingDouble(TextLine::y1).reversed()));

        List<TextLine> merged = new ArrayList<>();
        int i = 0;
        while (i < byRow.size()) {
            TextLine anchor = byRow.get(i);
            List<TextLine> row = new ArrayList<>();
            int j = i;
            while (j < byRow.size()
                    && byRow.get(j).page() == anchor.page()
                    && Math.abs(byRow.get(j).y1() - anchor.y1()) <= rowTolerance) {
                row.add(byRow.get(j));
                j++;
            }
            row.sort(Comparator.comparingDouble(TextLine::x1));
            merged.addAll(mergeRow(row, maxGap, spaceBetween));
            i = j;
        }
        return merged;
    }

    private static List<TextLine> mergeRow(List<TextLine> row, double maxGap, boolean spaceBetween) {
        List<TextLine> cells = new ArrayList<>();
        TextLine current = null;
        for (TextLine fragment : row) {
            if (current != null && (fragment.x1() - current.x2()) <= maxGap) {
                String joined = spaceBetween
                        ? current.text() + " " + fragment.text()
                        : current.text() + fragment.text();
                current = new TextLine(current.page(), joined,
                        current.x1(), Math.min(current.y1(), fragment.y1()),
                        Math.max(current.x2(), fragment.x2()), Math.max(current.y2(), fragment.y2()));
            } else {
                if (current != null) {
                    cells.add(current);
                }
                current = fragment;
            }
        }
        if (current != null) {
            cells.add(current);
        }
        return cells;
    }

    private TextLines() {
    }
}
