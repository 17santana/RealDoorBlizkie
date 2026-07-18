package blizkie.realdoor.profile;

import java.util.List;

/** One uploaded document, its detected type, and the fields extracted from it awaiting review. */
public class DocumentProfile {

    private final String documentId;
    private final String documentType;
    private final String fileName;
    private final List<ProfileField> fields;

    public DocumentProfile(String documentId, String documentType, String fileName, List<ProfileField> fields) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.fields = fields;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public List<ProfileField> getFields() {
        return fields;
    }
}
