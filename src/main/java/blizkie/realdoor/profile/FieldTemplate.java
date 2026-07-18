package blizkie.realdoor.profile;

/** Maps one allowlisted field to the label text that precedes its value line in the source PDF. */
public record FieldTemplate(String field, String labelText, FieldType type, LabelMatch match) {

    public FieldTemplate(String field, String labelText, FieldType type) {
        this(field, labelText, type, LabelMatch.EXACT);
    }

    public enum LabelMatch {
        /** The label line equals the template text (normal form labels). */
        EXACT,
        /** The label line starts with the template text (e.g. the adversarial-text marker banner). */
        PREFIX
    }
}
