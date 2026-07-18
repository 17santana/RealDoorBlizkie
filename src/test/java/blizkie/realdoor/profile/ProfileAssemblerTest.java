package blizkie.realdoor.profile;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileAssemblerTest {

    private final ProfileService profileService =
            new ProfileService(new DocumentProfileExtractor(new FieldExtractionService()));
    private final ProfileAssembler assembler = new ProfileAssembler();

    @Test
    void assemblesConfirmedIdentityAndIncomeEvidence() throws IOException {
        ProfileSession session = profileService.createSession();
        String sessionId = session.getSessionId();

        addAndConfirmAll(sessionId, "application_summary", "hh-001_d01_application_summary.pdf");
        addAndConfirmAll(sessionId, "pay_stub", "hh-001_d03_pay_stub.pdf");

        HouseholdProfile profile = assembler.assemble(session);

        assertThat(profile.householdSize()).isEqualTo(1);
        assertThat(profile.personName()).isEqualTo("Mara North");
        assertThat(profile.presentDocumentTypes()).containsExactlyInAnyOrder("application_summary", "pay_stub");
        assertThat(profile.incomeEvidence()).singleElement().satisfies(e -> {
            assertThat(e.documentType()).isEqualTo("pay_stub");
            assertThat(e.amount()).isEqualTo(2166.0);
            assertThat(e.frequency()).isEqualTo("biweekly");
            assertThat(e.sourceDocumentId()).isEqualTo("doc-2");
        });
    }

    @Test
    void unconfirmedFieldsDoNotFlowIntoTheProfile() throws IOException {
        ProfileSession session = profileService.createSession();
        String sessionId = session.getSessionId();

        // Upload but confirm nothing.
        profileService.addDocument(sessionId, "application_summary", "a.pdf",
                readFixture("hh-001_d01_application_summary.pdf"));
        profileService.addDocument(sessionId, "pay_stub", "b.pdf",
                readFixture("hh-001_d03_pay_stub.pdf"));

        HouseholdProfile profile = assembler.assemble(session);

        assertThat(profile.householdSize()).isNull();
        assertThat(profile.personName()).isNull();
        assertThat(profile.incomeEvidence()).isEmpty();
        // Document presence is a fact independent of field confirmation.
        assertThat(profile.presentDocumentTypes()).containsExactlyInAnyOrder("application_summary", "pay_stub");
    }

    @Test
    void twoCorroboratingPayStubsRemainTwoSeparateEvidenceItems() throws IOException {
        ProfileSession session = profileService.createSession();
        String sessionId = session.getSessionId();

        addAndConfirmAll(sessionId, "pay_stub", "hh-001_d03_pay_stub.pdf");
        addAndConfirmAll(sessionId, "pay_stub", "hh-001_d03_pay_stub.pdf");

        HouseholdProfile profile = assembler.assemble(session);

        // The assembler does not de-duplicate — reconciling corroboration is the Understand stage's job.
        assertThat(profile.incomeEvidence()).hasSize(2);
    }

    @Test
    void correctedValueIsUsedNotTheOriginalExtraction() throws IOException {
        ProfileSession session = profileService.createSession();
        String sessionId = session.getSessionId();

        DocumentProfile doc = profileService.addDocument(sessionId, "pay_stub", "b.pdf",
                readFixture("hh-001_d03_pay_stub.pdf"));
        profileService.correctField(sessionId, doc.getDocumentId(), "gross_pay", 2000.0);
        profileService.confirmField(sessionId, doc.getDocumentId(), "pay_frequency");

        HouseholdProfile profile = assembler.assemble(session);

        assertThat(profile.incomeEvidence()).singleElement()
                .satisfies(e -> assertThat(e.amount()).isEqualTo(2000.0));
    }

    private void addAndConfirmAll(String sessionId, String documentType, String fileName) throws IOException {
        DocumentProfile doc = profileService.addDocument(sessionId, documentType, fileName, readFixture(fileName));
        for (ProfileField field : doc.getFields()) {
            profileService.confirmField(sessionId, doc.getDocumentId(), field.getField());
        }
    }

    private byte[] readFixture(String fileName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/documents/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: " + fileName);
            }
            return in.readAllBytes();
        }
    }
}
