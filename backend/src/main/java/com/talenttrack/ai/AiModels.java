package com.talenttrack.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/** Wire contracts shared with the Python ai-service (snake_case JSON). */
public final class AiModels {

    private AiModels() {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ParsedResume(String text, List<String> skills, Integer experienceYears, String email,
                               String phone, List<String> sectionsFound, int qualityScore, List<String> tips) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MatchRequest(String resumeText, List<String> candidateSkills, int candidateExperience,
                               String jobDescription, List<String> jobSkills, int minExperience) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record MatchResult(int overall, int skillScore, int semanticScore, int experienceScore,
                              List<String> matchedSkills, List<String> missingSkills, String summary) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record JobDocument(Long id, String text, List<String> skills, int minExperience) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendRequest(String candidateText, List<String> candidateSkills, int candidateExperience,
                                   List<JobDocument> jobs, int topK) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Recommendation(Long jobId, int score, List<String> matchedSkills, List<String> missingSkills) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RecommendResponse(List<Recommendation> results) {
    }
}
