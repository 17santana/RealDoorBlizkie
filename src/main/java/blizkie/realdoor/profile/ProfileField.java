package blizkie.realdoor.profile;

/**
 * An extracted field plus its human-review state. The machine fills {@code extractedValue} once;
 * the renter then confirms it as-is or corrects it. {@link #currentValue()} is the value a
 * downstream stage is allowed to reuse — and only when {@link #isReusable()} is true.
 */
public class ProfileField {

    private final String field;
    private final Object extractedValue;
    private final int page;
    private final double[] bbox;
    private final double confidence;

    private FieldStatus status = FieldStatus.UNCONFIRMED;
    private Object correctedValue;

    public ProfileField(ExtractedField extracted) {
        this.field = extracted.field();
        this.extractedValue = extracted.value();
        this.page = extracted.page();
        this.bbox = extracted.bbox();
        this.confidence = extracted.confidence();
    }

    public void confirm() {
        this.status = FieldStatus.CONFIRMED;
        this.correctedValue = null;
    }

    public void correct(Object newValue) {
        this.correctedValue = newValue;
        this.status = FieldStatus.CORRECTED;
    }

    public Object currentValue() {
        return status == FieldStatus.CORRECTED ? correctedValue : extractedValue;
    }

    public boolean isReusable() {
        return status == FieldStatus.CONFIRMED || status == FieldStatus.CORRECTED;
    }

    public String getField() {
        return field;
    }

    public Object getExtractedValue() {
        return extractedValue;
    }

    public Object getCorrectedValue() {
        return correctedValue;
    }

    public int getPage() {
        return page;
    }

    public double[] getBbox() {
        return bbox;
    }

    public double getConfidence() {
        return confidence;
    }

    public FieldStatus getStatus() {
        return status;
    }
}
