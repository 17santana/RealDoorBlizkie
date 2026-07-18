package blizkie.realdoor.profile;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link HouseholdProfile} from a session's documents using only confirmed/corrected fields,
 * enforcing the challenge rule that data must be human-reviewed "before reuse". The mapping is purely
 * mechanical — it turns document fields into income evidence and identity facts and makes no judgement
 * about countability, corroboration, or conflicts (those belong to the Understand stage).
 */
@Service
public class ProfileAssembler {

    /** Which fields on each income-bearing document type supply the amount and its frequency. */
    private record IncomeMapping(String amountField, String frequencyField, String frequencyConstant) {
    }

    private static final Map<String, IncomeMapping> INCOME_BY_TYPE = Map.of(
            "pay_stub", new IncomeMapping("gross_pay", "pay_frequency", null),
            "benefit_letter", new IncomeMapping("monthly_benefit", "benefit_frequency", null),
            "gig_statement", new IncomeMapping("gross_receipts", null, "monthly")
    );

    public HouseholdProfile assemble(ProfileSession session) {
        Integer householdSize = null;
        String personName = null;
        Set<String> presentTypes = new LinkedHashSet<>();
        List<IncomeEvidence> evidence = new ArrayList<>();

        for (DocumentProfile document : session.getDocuments()) {
            presentTypes.add(document.getDocumentType());

            if (document.getDocumentType().equals("application_summary")) {
                Integer size = reusableInt(document, "household_size");
                if (size != null) {
                    householdSize = size;
                }
                String name = reusableString(document, "person_name");
                if (name != null) {
                    personName = name;
                }
            }

            IncomeMapping mapping = INCOME_BY_TYPE.get(document.getDocumentType());
            if (mapping != null) {
                incomeEvidence(document, mapping).ifPresent(evidence::add);
            }
        }

        return new HouseholdProfile(session.getSessionId(), householdSize, personName, presentTypes, evidence);
    }

    private Optional<IncomeEvidence> incomeEvidence(DocumentProfile document, IncomeMapping mapping) {
        Double amount = reusableDouble(document, mapping.amountField());
        String frequency = mapping.frequencyConstant() != null
                ? mapping.frequencyConstant()
                : reusableString(document, mapping.frequencyField());
        if (amount == null || frequency == null) {
            // Income fields not yet confirmed; do not let unreviewed data flow downstream.
            return Optional.empty();
        }
        return Optional.of(new IncomeEvidence(
                document.getDocumentType(), document.getDocumentId(), amount, frequency));
    }

    private Optional<ProfileField> reusableField(DocumentProfile document, String fieldName) {
        return document.getFields().stream()
                .filter(f -> f.getField().equals(fieldName))
                .filter(ProfileField::isReusable)
                .findFirst();
    }

    private String reusableString(DocumentProfile document, String fieldName) {
        return reusableField(document, fieldName).map(f -> String.valueOf(f.currentValue())).orElse(null);
    }

    private Integer reusableInt(DocumentProfile document, String fieldName) {
        return reusableField(document, fieldName).map(f -> toInt(f.currentValue())).orElse(null);
    }

    private Double reusableDouble(DocumentProfile document, String fieldName) {
        return reusableField(document, fieldName).map(f -> toDouble(f.currentValue())).orElse(null);
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value).replace("$", "").replace(",", "").trim());
    }
}
