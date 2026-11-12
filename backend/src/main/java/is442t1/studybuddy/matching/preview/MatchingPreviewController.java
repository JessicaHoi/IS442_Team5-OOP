package is442t1.studybuddy.matching.preview;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import is442t1.studybuddy.auth.RequireRole;
import is442t1.studybuddy.model.enums.Role;

/** Lets the administrator compare saved and unsaved matching settings on a sample student. */
@RestController
@RequestMapping("/api/admin/matching-config/preview")
@RequireRole(Role.ADMIN)
public class MatchingPreviewController {

    private final MatchingPreviewService previewService;

    /**
     * Creates the matching preview controller.
     *
     * @param previewService the matching preview service
     */
    public MatchingPreviewController(MatchingPreviewService previewService) {
        this.previewService = previewService;
    }

    /**
     * Lists students available for preview.
     *
     * @return the sample students
     */
    @GetMapping("/students")
    public List<PreviewStudent> students() {
        return previewService.sampleStudents();
    }

    /**
     * Compares saved and proposed matching rules.
     *
     * @param request the sample student and proposed configuration
     * @return the comparison
     */
    @PostMapping
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return previewService.preview(request);
    }
}
