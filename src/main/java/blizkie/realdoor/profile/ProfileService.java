package blizkie.realdoor.profile;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral, in-memory store of renter profile sessions. Nothing is persisted to disk, so deleting
 * a session truly removes the applicant's data. Raw uploaded bytes are never retained — only the
 * allowlisted extracted fields.
 */
@Service
public class ProfileService {

    private final DocumentProfileExtractor extractor;
    private final ConcurrentHashMap<String, ProfileSession> sessions = new ConcurrentHashMap<>();

    public ProfileService(DocumentProfileExtractor extractor) {
        this.extractor = extractor;
    }

    public ProfileSession createSession() {
        String id = UUID.randomUUID().toString();
        ProfileSession session = new ProfileSession(id);
        sessions.put(id, session);
        return session;
    }

    public ProfileSession get(String sessionId) {
        ProfileSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown session: " + sessionId);
        }
        return session;
    }

    /** Removes the session and everything in it. Returns true if a session was actually deleted. */
    public boolean delete(String sessionId) {
        return sessions.remove(sessionId) != null;
    }

    public DocumentProfile addDocument(String sessionId, String documentType, String fileName, byte[] pdfBytes)
            throws IOException {
        ProfileSession session = get(sessionId);
        List<ExtractedField> extracted = extractor.extract(pdfBytes, documentType);
        List<ProfileField> fields = extracted.stream().map(ProfileField::new).toList();

        DocumentProfile document = new DocumentProfile(
                session.nextDocumentId(), documentType, fileName, fields);
        session.getDocuments().add(document);
        return document;
    }

    public ProfileField confirmField(String sessionId, String documentId, String fieldName) {
        ProfileField field = field(sessionId, documentId, fieldName);
        field.confirm();
        return field;
    }

    public ProfileField correctField(String sessionId, String documentId, String fieldName, Object newValue) {
        ProfileField field = field(sessionId, documentId, fieldName);
        field.correct(newValue);
        return field;
    }

    private ProfileField field(String sessionId, String documentId, String fieldName) {
        return get(sessionId).document(documentId).getFields().stream()
                .filter(f -> f.getField().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field: " + fieldName));
    }
}
