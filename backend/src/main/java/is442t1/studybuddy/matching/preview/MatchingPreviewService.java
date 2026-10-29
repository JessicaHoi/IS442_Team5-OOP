package is442t1.studybuddy.matching.preview;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import is442t1.studybuddy.common.exception.ResourceNotFoundException;
import is442t1.studybuddy.matching.MatchResult;
import is442t1.studybuddy.matching.MatchingConfigService;
import is442t1.studybuddy.matching.MatchingRules;
import is442t1.studybuddy.matching.MatchingService;
import is442t1.studybuddy.student.Student;
import is442t1.studybuddy.student.StudentRepository;

/**
 * Runs one sample student through the saved matching settings and through
 * unsaved ones, so the administrator can see the effect before saving.
 * Nothing is stored.
 */
@Service
public class MatchingPreviewService {

    private final MatchingService matchingService;
    private final MatchingConfigService configService;
    private final StudentRepository studentRepository;
    private final PreviewProperties properties;

    public MatchingPreviewService(MatchingService matchingService, MatchingConfigService configService,
            StudentRepository studentRepository, PreviewProperties properties) {
        this.matchingService = matchingService;
        this.configService = configService;
        this.studentRepository = studentRepository;
        this.properties = properties;
    }

    /** Students with at least one study preference, sorted by name. */
    @Transactional(readOnly = true)
    public List<PreviewStudent> sampleStudents() {
        return studentRepository.findAll().stream()
                .filter(student -> !student.getStudyPreferences().isEmpty())
                .map(student -> new PreviewStudent(student.getId(), student.getName(),
                        student.getStudyPreferences().stream()
                                .map(preference -> preference.getCourse().getCourseCode())
                                .toList()))
                .sorted(Comparator.comparing(PreviewStudent::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(PreviewRequest request) {
        MatchingRules proposedRules = configService.toRules(request.proposed());
        MatchingRules currentRules = configService.currentRules();
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));

        List<MatchResult> current = matchingService.rankWith(student.getId(), request.course(), currentRules);
        List<MatchResult> proposed = matchingService.rankWith(student.getId(), request.course(), proposedRules);
        Map<UUID, Integer> currentRanks = ranksOf(current);
        Map<UUID, Integer> proposedRanks = ranksOf(proposed);

        String course = request.course() == null || request.course().isBlank() ? null : request.course();
        return new PreviewResponse(student.getName(), course,
                new PreviewResponse.Ranking(currentRules.strategy(), top(current, proposedRanks)),
                new PreviewResponse.Ranking(proposedRules.strategy(), top(proposed, currentRanks)));
    }

    private List<PreviewResponse.Entry> top(List<MatchResult> results, Map<UUID, Integer> otherRanks) {
        return IntStream.range(0, Math.min(properties.size(), results.size()))
                .mapToObj(index -> {
                    MatchResult result = results.get(index);
                    UUID id = result.student().id();
                    return new PreviewResponse.Entry(index + 1, otherRanks.get(id), id, result.student().name(),
                            result.score(), result.overlapMinutes(), result.breakdown());
                })
                .toList();
    }

    private static Map<UUID, Integer> ranksOf(List<MatchResult> results) {
        Map<UUID, Integer> ranks = new HashMap<>();
        for (int index = 0; index < results.size(); index++) {
            ranks.put(results.get(index).student().id(), index + 1);
        }
        return ranks;
    }
}
