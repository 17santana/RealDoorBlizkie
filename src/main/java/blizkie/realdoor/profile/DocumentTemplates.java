package blizkie.realdoor.profile;

import java.util.List;
import java.util.Map;

import static blizkie.realdoor.profile.FieldTemplate.LabelMatch.PREFIX;

/** Field allowlists per document type, derived from the starter-pack gold field schema. */
public final class DocumentTemplates {

    /**
     * Marker banner the fixtures print above embedded prompt-injection text. Capturing the line
     * below it as a quarantined {@code untrusted_instruction_text} field surfaces the attack to the
     * human reviewer; it is data to be flagged, never an instruction to act on.
     */
    private static final FieldTemplate UNTRUSTED_INSTRUCTION = new FieldTemplate(
            "untrusted_instruction_text", "UNTRUSTED DOCUMENT TEXT", FieldType.STRING, PREFIX);

    public static final Map<String, List<FieldTemplate>> BY_DOCUMENT_TYPE = Map.of(
            "application_summary", List.of(
                    new FieldTemplate("person_name", "APPLICANT", FieldType.STRING),
                    new FieldTemplate("household_size", "HOUSEHOLD SIZE", FieldType.INTEGER),
                    new FieldTemplate("address", "MAILING ADDRESS", FieldType.STRING),
                    new FieldTemplate("application_date", "APPLICATION DATE", FieldType.STRING)
            ),
            "pay_stub", List.of(
                    new FieldTemplate("person_name", "EMPLOYEE", FieldType.STRING),
                    new FieldTemplate("pay_date", "PAY DATE", FieldType.STRING),
                    new FieldTemplate("pay_period_start", "PAY PERIOD", FieldType.STRING),
                    new FieldTemplate("pay_period_end", "THROUGH", FieldType.STRING),
                    new FieldTemplate("pay_frequency", "PAY FREQUENCY", FieldType.STRING),
                    new FieldTemplate("regular_hours", "REGULAR HOURS", FieldType.INTEGER),
                    new FieldTemplate("hourly_rate", "HOURLY RATE", FieldType.CURRENCY),
                    new FieldTemplate("gross_pay", "GROSS PAY", FieldType.CURRENCY),
                    new FieldTemplate("net_pay", "NET PAY", FieldType.CURRENCY),
                    UNTRUSTED_INSTRUCTION
            ),
            "employment_letter", List.of(
                    new FieldTemplate("person_name", "EMPLOYEE", FieldType.STRING),
                    new FieldTemplate("document_date", "LETTER DATE", FieldType.STRING),
                    new FieldTemplate("weekly_hours", "HOURS PER WEEK", FieldType.INTEGER),
                    new FieldTemplate("hourly_rate", "HOURLY RATE", FieldType.CURRENCY)
            ),
            "benefit_letter", List.of(
                    new FieldTemplate("person_name", "RECIPIENT", FieldType.STRING),
                    new FieldTemplate("document_date", "LETTER DATE", FieldType.STRING),
                    new FieldTemplate("monthly_benefit", "MONTHLY AMOUNT", FieldType.CURRENCY),
                    new FieldTemplate("benefit_frequency", "FREQUENCY", FieldType.STRING)
            ),
            "gig_statement", List.of(
                    new FieldTemplate("person_name", "WORKER", FieldType.STRING),
                    new FieldTemplate("statement_month", "STATEMENT MONTH", FieldType.STRING),
                    new FieldTemplate("gross_receipts", "GROSS RECEIPTS", FieldType.CURRENCY),
                    new FieldTemplate("platform_fees", "PLATFORM FEES", FieldType.CURRENCY),
                    UNTRUSTED_INSTRUCTION
            )
    );

    private DocumentTemplates() {
    }
}
