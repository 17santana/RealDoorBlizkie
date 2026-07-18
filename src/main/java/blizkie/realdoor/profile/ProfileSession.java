package blizkie.realdoor.profile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** All documents a renter has uploaded in one ephemeral, in-memory session. */
public class ProfileSession {

    private final String sessionId;
    private final Instant createdAt = Instant.now();
    private final List<DocumentProfile> documents = new ArrayList<>();
    private final AtomicInteger documentSequence = new AtomicInteger();

    public ProfileSession(String sessionId) {
        this.sessionId = sessionId;
    }

    /** Session-scoped id so the first document in any session is always {@code doc-1}. */
    public String nextDocumentId() {
        return "doc-" + documentSequence.incrementAndGet();
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<DocumentProfile> getDocuments() {
        return documents;
    }

    public DocumentProfile document(String documentId) {
        return documents.stream()
                .filter(d -> d.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document: " + documentId));
    }
}
