package com.talenttrack.dto;

import com.talenttrack.entity.ApplicationStage;

import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record CandidateDashboard(long totalApplications, long inInterview, long offers, long rejected,
                                     int profileCompleteness, boolean hasResume,
                                     Map<ApplicationStage, Long> byStage,
                                     List<ApplicationDtos.InterviewResponse> upcomingInterviews) {
    }

    public record JobPipeline(Long jobId, String title, long applicants) {
    }

    public record RecentApplicant(Long applicationId, String candidateName, String jobTitle, int atsScore,
                                  ApplicationStage stage, java.time.Instant appliedAt) {
    }

    public record RecruiterDashboard(long totalJobs, long openJobs, long totalApplicants, double averageAts,
                                     long hired, Map<ApplicationStage, Long> funnel, List<JobPipeline> topJobs,
                                     List<RecentApplicant> recentApplicants,
                                     List<ApplicationDtos.InterviewResponse> upcomingInterviews) {
    }

    public record MonthlyCount(String month, long count) {
    }

    public record AdminDashboard(long totalUsers, long candidates, long recruiters, long admins,
                                 long totalJobs, long openJobs, long totalApplications, double averageAts,
                                 Map<ApplicationStage, Long> applicationsByStage,
                                 List<MonthlyCount> jobsPerMonth) {
    }
}
