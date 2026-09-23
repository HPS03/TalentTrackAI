package com.talenttrack.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileRequest(
            @Size(max = 200) String headline,
            @Size(max = 4000) String bio,
            @Size(max = 30) String phone,
            @Size(max = 120) String location,
            @Min(0) @Max(50) int experienceYears,
            @Size(max = 500) String education,
            @Size(max = 60) List<@NotBlank @Size(max = 50) String> skills,
            @Size(max = 255) String linkedinUrl,
            @Size(max = 255) String githubUrl,
            @Size(max = 255) String portfolioUrl) {
    }

    public record ProfileResponse(
            Long userId,
            String fullName,
            String email,
            String headline,
            String bio,
            String phone,
            String location,
            int experienceYears,
            String education,
            List<String> skills,
            String linkedinUrl,
            String githubUrl,
            String portfolioUrl,
            String resumeFileName,
            Instant resumeUploadedAt,
            int profileCompleteness) {
    }

    public record ResumeUploadResponse(ProfileResponse profile, List<String> extractedSkills,
                                       Integer detectedExperienceYears, ResumeQuality quality) {
    }

    public record ResumeQuality(int score, List<String> sectionsFound, List<String> tips) {
    }
}
