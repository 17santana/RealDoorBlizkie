package blizkie.realdoor.profile;

public record ExtractedField(String field, Object value, int page, double[] bbox, double confidence) {
}
