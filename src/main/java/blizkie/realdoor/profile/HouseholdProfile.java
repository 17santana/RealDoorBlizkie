package blizkie.realdoor.profile;

import java.util.List;
import java.util.Set;

/**
 * The confirmed, human-reviewed profile a session hands to the Understand stage — the Profile→Understand
 * contract, modelled on the starter pack's {@code example_profile.json}. It carries only values the
 * renter has confirmed or corrected; unconfirmed extractions never appear here. {@code householdSize}
 * and {@code personName} are nullable because the renter may not have confirmed them yet.
 *
 * <p>The Understand stage consumes this and produces the {@code submission.schema.json} output
 * (annualized income, threshold comparison, readiness status, citations). It must not need the source
 * PDFs or field-level extraction detail — everything it needs to reason about income is here.</p>
 */
public record HouseholdProfile(
        String householdId,
        Integer householdSize,
        String personName,
        Set<String> presentDocumentTypes,
        List<IncomeEvidence> incomeEvidence) {
}
