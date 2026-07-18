package blizkie.realdoor.profile;

/**
 * One piece of income evidence taken verbatim from a single confirmed document. This is a fact, not
 * a reconciled income stream: two corroborating pay stubs yield two evidence items, and the Understand
 * stage is responsible for de-duplicating corroboration, detecting conflicts, judging whether gig
 * income counts, summing distinct streams, and annualizing. Shape mirrors the {@code income_sources}
 * entries in the starter pack's {@code example_profile.json}.
 */
public record IncomeEvidence(String documentType, String sourceDocumentId, double amount, String frequency) {
}
