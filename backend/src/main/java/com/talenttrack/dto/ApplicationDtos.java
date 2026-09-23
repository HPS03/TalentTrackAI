package com.talenttrack.dto;

import com.talenttrack.entity.ActivityType;
import com.talenttrack.entity.ApplicationStage;
import com.talenttrack.entity.InterviewMode;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record ApplyRequest(@Size(max = 5000) String coverLetter) {
    }

    public record StageChangeRequest(@NotNull ApplicationStage stage, @Min(0) Integer position,
                                     @Size(max = 1000) String comment) {
    }

    public record NoteRequest(@NotBlank @Size(max = 2000) String message) {
    }

    public record RatingRequest(@NotNull @Min(1) @Max(5) Integer rating) {
    }

    public record InterviewRequest(@NotNull @Future Instant scheduledAt,
                                   @Min(15) @Max(480) int durationMin,
                                   @NotNull InterviewMode mode,
                                   @Size(max = 500) String meetingLink,
                                   @Size(max = 2000) String notes) {
    }

    public record AtsBreakdown(int overall, int skillScore, int semanticScore, int experienceScore,
                               List<String> matchedSkills, List<String> missingSkills, String summary) {
    }

    /** Candidate-side view of an application. */
    public record MyApplication(Long id, JobDtos.JobResponse job, ApplicationStage stage, AtsBreakdown ats,
                                Instant appliedAt, Instant updatedAt, List<InterviewResponse> interviews) {
    }

    /** A card on the Kanban board. */
    public record BoardCard(Long id, Long candidateId, String candidateName, String candidateEmail,
                            String headline, int atsScore, List<String> matchedSkills,
                            Integer rating, int position, Instant appliedAt,
                            Set<ApplicationStage> allowedTransitions) {
    }

    public record BoardColumn(ApplicationStage stage, List<BoardCard> cards) {
    }

    public record BoardResponse(JobDtos.JobResponse job, List<BoardColumn> columns, Map<ApplicationStage, Long> counts) {
    }

    public record ActivityResponse(Long id, ActivityType type, ApplicationStage fromStage, ApplicationStage toStage,
                                   String message, String actorName, Instant createdAt) {
    }

    public record InterviewResponse(Long id, Long applicationId, String jobTitle, String companyName,
                                    String candidateName, Instant scheduledAt, int durationMin,
                                    InterviewMode mode, String meetingLink, String notes) {
    }

    /** Full detail shown in the recruiter's candidate drawer. */
    public record ApplicationDetail(Long id, ApplicationStage stage, Set<ApplicationStage> allowedTransitions,
                                    String coverLetter, AtsBreakdown ats, Integer rating,
                                    Instant appliedAt, ProfileDtos.ProfileResponse candidate,
                                    JobDtos.JobResponse job, List<ActivityResponse> activities,
                                    List<InterviewResponse> interviews) {
    }

    public record AnalyzeRequest(@NotBlank @Size(min = 30, max = 10000) String jobDescription,
                                 @Size(max = 30) List<String> requiredSkills) {
    }
}
