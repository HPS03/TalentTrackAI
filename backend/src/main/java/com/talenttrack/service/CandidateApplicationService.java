package com.talenttrack.service;

import com.talenttrack.ai.AiGateway;
import com.talenttrack.ai.AiModels;
import com.talenttrack.dto.ApplicationDtos.*;
import com.talenttrack.dto.JobDtos.RecommendedJob;
import com.talenttrack.entity.*;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationActivityRepository activityRepository;
    private final InterviewRepository interviewRepository;
    private final JobRepository jobRepository;
    private final JobService jobService;
    private final ProfileService profileService;
    private final NotificationService notificationService;
    private final AiGateway ai;

    @Transactional
    public MyApplication apply(Long jobId, Long candidateId, ApplyRequest req) {
        Job job = jobService.find(jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw ApiException.badRequest("This job is no longer accepting applications");
        }
        if (job.getDeadline() != null && job.getDeadline().isBefore(java.time.LocalDate.now())) {
            throw ApiException.badRequest("The application deadline for this job has passed");
        }
        if (applicationRepository.existsByJobIdAndCandidateId(jobId, candidateId)) {
            throw ApiException.conflict("You have already applied to this job");
        }
        CandidateProfile profile = profileService.getOrCreate(candidateId);
        if (!profile.hasResume() && Skills.split(profile.getSkills()).isEmpty()) {
            throw ApiException.badRequest("Upload a resume or add skills to your profile before applying");
        }

        AiModels.MatchResult match = ai.match(matchRequest(profile, job.getDescription(), Skills.split(job.getSkills()),
                job.getMinExperience()));

        Application app = Application.builder()
                .job(job)
                .candidate(profile.getUser())
                .stage(ApplicationStage.APPLIED)
                .coverLetter(req == null ? null : req.coverLetter())
                .atsScore(clamp(match.overall()))
                .skillScore(clamp(match.skillScore()))
                .semanticScore(clamp(match.semanticScore()))
                .experienceScore(clamp(match.experienceScore()))
                .matchedSkills(Skills.join(match.matchedSkills()))
                .missingSkills(Skills.join(match.missingSkills()))
                .aiSummary(match.summary())
                .boardPosition(applicationRepository.maxBoardPosition(jobId, ApplicationStage.APPLIED) + 1)
                .build();
        applicationRepository.save(app);
        activityRepository.save(ApplicationActivity.builder().application(app).actor(profile.getUser())
                .type(ActivityType.APPLIED).toStage(ApplicationStage.APPLIED)
                .message("Applied with ATS score " + app.getAtsScore() + "%").build());
        notificationService.notify(job.getRecruiter(), "New applicant for " + job.getTitle(),
                profile.getUser().getFullName() + " applied (ATS " + app.getAtsScore() + "%).",
                "/recruiter/jobs/" + job.getId() + "/board");
        return toMine(app, List.of());
    }

    @Transactional(readOnly = true)
    public List<MyApplication> myApplications(Long candidateId) {
        Map<Long, List<InterviewResponse>> interviews = interviewRepository.findForCandidate(candidateId).stream()
                .collect(Collectors.groupingBy(i -> i.getApplication().getId(),
                        Collectors.mapping(DtoMapper::toInterview, Collectors.toList())));
        return applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId).stream()
                .map(a -> toMine(a, interviews.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void withdraw(Long applicationId, Long candidateId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));
        if (!app.getCandidate().getId().equals(candidateId)) {
            throw ApiException.forbidden("Not your application");
        }
        if (app.getStage() == ApplicationStage.HIRED) {
            throw ApiException.badRequest("A hired application cannot be withdrawn");
        }
        notificationService.notify(app.getJob().getRecruiter(), "Application withdrawn",
                app.getCandidate().getFullName() + " withdrew from " + app.getJob().getTitle() + ".",
                "/recruiter/jobs/" + app.getJob().getId() + "/board");
        applicationRepository.delete(app);
    }

    /** Standalone "check my resume against this JD" tool. Nothing is persisted. */
    @Transactional
    public AtsBreakdown analyze(Long candidateId, AnalyzeRequest req) {
        CandidateProfile profile = profileService.getOrCreate(candidateId);
        List<String> required = req.requiredSkills() == null ? List.of() : Skills.clean(req.requiredSkills());
        AiModels.MatchResult r = ai.match(matchRequest(profile, req.jobDescription(), required, 0));
        return new AtsBreakdown(r.overall(), r.skillScore(), r.semanticScore(), r.experienceScore(),
                r.matchedSkills(), r.missingSkills(), r.summary());
    }

    @Transactional
    public List<RecommendedJob> recommendations(Long candidateId, int limit) {
        CandidateProfile profile = profileService.getOrCreate(candidateId);
        Set<Long> applied = applicationRepository.findJobIdsAppliedBy(candidateId);
        List<Job> openJobs = jobRepository.findTop200ByStatusOrderByCreatedAtDesc(JobStatus.OPEN).stream()
                .filter(j -> !applied.contains(j.getId()))
                .toList();
        if (openJobs.isEmpty()) {
            return List.of();
        }
        Map<Long, Job> byId = openJobs.stream().collect(Collectors.toMap(Job::getId, Function.identity()));
        var request = new AiModels.RecommendRequest(candidateText(profile), Skills.split(profile.getSkills()),
                profile.getExperienceYears(),
                openJobs.stream().map(j -> new AiModels.JobDocument(j.getId(),
                        j.getTitle() + ". " + j.getDescription(), Skills.split(j.getSkills()), j.getMinExperience()))
                        .toList(),
                Math.min(Math.max(limit, 1), 20));
        return ai.recommend(request).stream()
                .filter(r -> byId.containsKey(r.jobId()))
                .map(r -> new RecommendedJob(DtoMapper.toJob(byId.get(r.jobId()), null, false), r.score(),
                        r.matchedSkills(), r.missingSkills()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> interviews(Long candidateId) {
        return interviewRepository.findForCandidate(candidateId).stream().map(DtoMapper::toInterview).toList();
    }

    private AiModels.MatchRequest matchRequest(CandidateProfile p, String jd, List<String> skills, int minExp) {
        return new AiModels.MatchRequest(candidateText(p), Skills.split(p.getSkills()), p.getExperienceYears(),
                jd, skills, minExp);
    }

    private static String candidateText(CandidateProfile p) {
        return String.join("\n", Objects.requireNonNullElse(p.getResumeText(), ""),
                Objects.requireNonNullElse(p.getHeadline(), ""), Objects.requireNonNullElse(p.getBio(), ""),
                Objects.requireNonNullElse(p.getEducation(), "")).trim();
    }

    private static MyApplication toMine(Application a, List<InterviewResponse> interviews) {
        return new MyApplication(a.getId(), DtoMapper.toJob(a.getJob()), a.getStage(), DtoMapper.toAts(a),
                a.getAppliedAt(), a.getUpdatedAt(), interviews);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
