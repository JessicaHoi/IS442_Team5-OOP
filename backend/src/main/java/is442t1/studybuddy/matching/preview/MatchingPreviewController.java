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

    public MatchingPreviewController(MatchingPreviewService previewService) {
        this.previewService = previewService;
    }

    /**
     * Lists preview samples: {@code GET /api/admin/matching-config/preview/students} ,
     *     administrator only.
     * Returns HTTP 200 with students who have at least one preference, sorted by name.
     *
     * @return response described above, with the documented HTTP status and payload
     */
    @GetMapping("/students")
    public List<PreviewStudent> students() {
        return previewService.sampleStudents();
    }

    /**
     * Compares saved and proposed rankings: {@code POST /api/admin/matching-config/preview} ,
     *     administrator only.
     * The body is {@link PreviewRequest}; blank course uses the sample student's first preference.
     * Returns HTTP 200 with both top-match lists and cross-ranking positions without saving
     *     settings.
     * Invalid proposed rules are 400 and a missing sample student is 404.
     *
     * @param request JSON body described above
     * @return response described above, with the documented HTTP status and payload
     */
    @PostMapping
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return previewService.preview(request);
    }
}
