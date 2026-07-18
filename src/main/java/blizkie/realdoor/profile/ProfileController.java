package blizkie.realdoor.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Stage 1 (Profile) HTTP surface: upload a document, review the extracted fields, and confirm or
 * correct each one before it may be reused. Session deletion wipes the applicant's data.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileAssembler profileAssembler;

    public ProfileController(ProfileService profileService, ProfileAssembler profileAssembler) {
        this.profileService = profileService;
        this.profileAssembler = profileAssembler;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createSession() {
        return Map.of("sessionId", profileService.createSession().getSessionId());
    }

    @GetMapping("/sessions/{sessionId}")
    public ProfileSession getSession(@PathVariable String sessionId) {
        return profileService.get(sessionId);
    }

    /**
     * The confirmed household profile handed to the Understand stage. Contains only fields the renter
     * has confirmed or corrected — unconfirmed extractions are intentionally absent.
     */
    @GetMapping("/sessions/{sessionId}/household")
    public HouseholdProfile getHouseholdProfile(@PathVariable String sessionId) {
        return profileAssembler.assemble(profileService.get(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        boolean removed = profileService.delete(sessionId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/sessions/{sessionId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentProfile uploadDocument(@PathVariable String sessionId,
                                          @RequestParam("documentType") String documentType,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        return profileService.addDocument(sessionId, documentType, file.getOriginalFilename(), file.getBytes());
    }

    @PostMapping("/sessions/{sessionId}/documents/{documentId}/fields/{field}/confirm")
    public ProfileField confirmField(@PathVariable String sessionId,
                                     @PathVariable String documentId,
                                     @PathVariable String field) {
        return profileService.confirmField(sessionId, documentId, field);
    }

    @PutMapping("/sessions/{sessionId}/documents/{documentId}/fields/{field}")
    public ProfileField correctField(@PathVariable String sessionId,
                                     @PathVariable String documentId,
                                     @PathVariable String field,
                                     @RequestBody CorrectionRequest body) {
        return profileService.correctField(sessionId, documentId, field, body.value());
    }

    public record CorrectionRequest(Object value) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
