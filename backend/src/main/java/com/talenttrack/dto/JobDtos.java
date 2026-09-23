package com.talenttrack.dto;

import com.talenttrack.entity.JobStatus;
import com.talenttrack.entity.JobType;
import com.talenttrack.entity.WorkMode;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class JobDtos {

    private JobDtos() {
    }

    public record JobRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 150) String companyName,
            @NotBlank @Size(max = 120) String location,
            @NotNull JobType jobType,
            @NotNull WorkMode workMode,
            @Min(0) @Max(30) int minExperience,
            @PositiveOrZero Integer salaryMin,
            @PositiveOrZero Integer salaryMax,
            @NotBlank @Size(min = 30, max = 10000) String description,
            @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 50) String> skills,
            @Min(1) @Max(1000) int openings,
            @FutureOrPresent LocalDate deadline) {

        @AssertTrue(message = "salaryMax must be greater than or equal to salaryMin")
        public boolean isSalaryRangeValid() {
            return salaryMin == null || salaryMax == null || salaryMax >= salaryMin;
        }
    }

    public record JobStatusRequest(@NotNull JobStatus status) {
    }

    public record JobResponse(
            Long id,
            String title,
            String companyName,
            String location,
            JobType jobType,
            WorkMode workMode,
            int minExperience,
            Integer salaryMin,
            Integer salaryMax,
            String description,
            List<String> skills,
            int openings,
            JobStatus status,
            LocalDate deadline,
            Long recruiterId,
            String recruiterName,
            Instant createdAt,
            Long applicantCount,
            Boolean alreadyApplied) {
    }

    public record RecommendedJob(JobResponse job, int matchScore, List<String> matchedSkills,
                                 List<String> missingSkills) {
    }
}
