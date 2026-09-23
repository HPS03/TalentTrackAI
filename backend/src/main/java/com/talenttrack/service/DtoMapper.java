package com.talenttrack.service;

import com.talenttrack.dto.ApplicationDtos.*;
import com.talenttrack.dto.AuthDtos.UserDto;
import com.talenttrack.dto.CommonDtos.NotificationResponse;
import com.talenttrack.dto.JobDtos.JobResponse;
import com.talenttrack.dto.ProfileDtos.ProfileResponse;
import com.talenttrack.entity.*;

import java.util.stream.Stream;

/** Entity -> DTO conversions. Call inside a transaction (lazy associations are touched). */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserDto toUser(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getCompanyName(),
                u.isEnabled(), u.getCreatedAt());
    }

    public static JobResponse toJob(Job j, Long applicantCount, Boolean alreadyApplied) {
        return new JobResponse(j.getId(), j.getTitle(), j.getCompanyName(), j.getLocation(), j.getJobType(),
                j.getWorkMode(), j.getMinExperience(), j.getSalaryMin(), j.getSalaryMax(), j.getDescription(),
                Skills.split(j.getSkills()), j.getOpenings(), j.getStatus(), j.getDeadline(),
                j.getRecruiter().getId(), j.getRecruiter().getFullName(), j.getCreatedAt(), applicantCount,
                alreadyApplied);
    }

    public static JobResponse toJob(Job j) {
        return toJob(j, null, null);
    }

    public static ProfileResponse toProfile(User user, CandidateProfile p) {
        if (p == null) {
            return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), null, null, null, null,
                    0, null, java.util.List.of(), null, null, null, null, null, 0);
        }
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), p.getHeadline(), p.getBio(),
                p.getPhone(), p.getLocation(), p.getExperienceYears(), p.getEducation(), Skills.split(p.getSkills()),
                p.getLinkedinUrl(), p.getGithubUrl(), p.getPortfolioUrl(), p.getResumeFileName(),
                p.getResumeUploadedAt(), completeness(p));
    }

    public static int completeness(CandidateProfile p) {
        if (p == null) {
            return 0;
        }
        long filled = Stream.of(p.getHeadline(), p.getBio(), p.getPhone(), p.getLocation(), p.getEducation(),
                        p.getSkills(), firstNonBlank(p.getLinkedinUrl(), p.getGithubUrl(), p.getPortfolioUrl()),
                        p.getResumeStoredName())
                .filter(v -> v != null && !v.isBlank())
                .count();
        return Math.round(filled * 100f / 8);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    public static AtsBreakdown toAts(Application a) {
        return new AtsBreakdown(a.getAtsScore(), a.getSkillScore(), a.getSemanticScore(), a.getExperienceScore(),
                Skills.split(a.getMatchedSkills()), Skills.split(a.getMissingSkills()), a.getAiSummary());
    }

    public static BoardCard toCard(Application a, String headline) {
        return new BoardCard(a.getId(), a.getCandidate().getId(), a.getCandidate().getFullName(),
                a.getCandidate().getEmail(), headline, a.getAtsScore(), Skills.split(a.getMatchedSkills()),
                a.getRecruiterRating(), a.getBoardPosition(), a.getAppliedAt(), a.getStage().allowedTransitions());
    }

    public static ActivityResponse toActivity(ApplicationActivity a) {
        return new ActivityResponse(a.getId(), a.getType(), a.getFromStage(), a.getToStage(), a.getMessage(),
                a.getActor() == null ? "System" : a.getActor().getFullName(), a.getCreatedAt());
    }

    public static InterviewResponse toInterview(Interview i) {
        Application a = i.getApplication();
        return new InterviewResponse(i.getId(), a.getId(), a.getJob().getTitle(), a.getJob().getCompanyName(),
                a.getCandidate().getFullName(), i.getScheduledAt(), i.getDurationMin(), i.getMode(),
                i.getMeetingLink(), i.getNotes());
    }

    public static NotificationResponse toNotification(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getLink(), n.isRead(),
                n.getCreatedAt());
    }
}
