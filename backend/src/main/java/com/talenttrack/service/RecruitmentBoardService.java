package com.talenttrack.service;

import com.talenttrack.dto.ApplicationDtos.*;
import com.talenttrack.entity.*;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Recruiter-side workflow behind the Jira-style Kanban board. */
@Service
@RequiredArgsConstructor
public class RecruitmentBoardService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationActivityRepository activityRepository;
    private final InterviewRepository interviewRepository;
    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final JobService jobService;
    private final ProfileService profileService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public BoardResponse board(Long jobId, Long recruiterId, Integer minAts, String q) {
        Job job = jobService.findOwned(jobId, recruiterId);
        List<Application> apps = applicationRepository.findByJobIdOrderByStageAscBoardPositionAsc(jobId);

        Map<Long, String> headlines = new HashMap<>();
        profileRepository.findByUserIdIn(apps.stream().map(a -> a.getCandidate().getId()).toList())
                .forEach(p -> {
                    if (p.getHeadline() != null) {
                        headlines.put(p.getUser().getId(), p.getHeadline());
                    }
                });

        Map<ApplicationStage, List<BoardCard>> grouped = new EnumMap<>(ApplicationStage.class);
        Map<ApplicationStage, Long> counts = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage stage : ApplicationStage.values()) {
            grouped.put(stage, new ArrayList<>());
            counts.put(stage, 0L);
        }
        String needle = q == null ? null : q.trim().toLowerCase(Locale.ROOT);
        for (Application a : apps) {
            counts.merge(a.getStage(), 1L, Long::sum);
            if (minAts != null && a.getAtsScore() < minAts) {
                continue;
            }
            if (needle != null && !needle.isEmpty()
                    && !a.getCandidate().getFullName().toLowerCase(Locale.ROOT).contains(needle)
                    && !Objects.toString(a.getMatchedSkills(), "").toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            grouped.get(a.getStage()).add(DtoMapper.toCard(a, headlines.get(a.getCandidate().getId())));
        }
        grouped.values().forEach(cards -> cards.sort(Comparator.comparingInt(BoardCard::position)));
        List<BoardColumn> columns = grouped.entrySet().stream()
                .map(e -> new BoardColumn(e.getKey(), e.getValue())).toList();
        return new BoardResponse(DtoMapper.toJob(job, (long) apps.size(), null), columns, counts);
    }

    /**
     * Moves a card to another column (or reorders it within the same column), validating the
     * workflow transition, recording an audit entry and notifying the candidate.
     */
    @Transactional
    public BoardCard moveStage(Long applicationId, Long recruiterId, StageChangeRequest req) {
        Application app = findOwned(applicationId, recruiterId);
        ApplicationStage from = app.getStage();
        ApplicationStage to = req.stage();
        if (!from.canMoveTo(to)) {
            throw ApiException.badRequest("Cannot move an application from " + from + " to " + to
                    + ". Allowed: " + from.allowedTransitions());
        }

        List<Application> column = new ArrayList<>(applicationRepository
                .findByJobIdOrderByStageAscBoardPositionAsc(app.getJob().getId()).stream()
                .filter(a -> a.getStage() == to && !a.getId().equals(app.getId()))
                .toList());
        int index = req.position() == null ? column.size() : Math.min(req.position(), column.size());
        app.setStage(to);
        column.add(index, app);
        for (int i = 0; i < column.size(); i++) {
            column.get(i).setBoardPosition(i);
        }

        if (from != to) {
            User actor = userRepository.getReferenceById(recruiterId);
            activityRepository.save(ApplicationActivity.builder().application(app).actor(actor)
                    .type(ActivityType.STAGE_CHANGED).fromStage(from).toStage(to)
                    .message(req.comment()).build());
            notificationService.notify(app.getCandidate(), stageTitle(to, app.getJob().getTitle()),
                    stageMessage(to, app.getJob()), "/candidate/applications");
        }
        String headline = profileRepository.findByUserId(app.getCandidate().getId())
                .map(CandidateProfile::getHeadline).orElse(null);
        return DtoMapper.toCard(app, headline);
    }

    @Transactional
    public ActivityResponse addNote(Long applicationId, Long recruiterId, NoteRequest req) {
        Application app = findOwned(applicationId, recruiterId);
        ApplicationActivity note = activityRepository.save(ApplicationActivity.builder().application(app)
                .actor(userRepository.getReferenceById(recruiterId)).type(ActivityType.NOTE)
                .message(req.message().trim()).build());
        return DtoMapper.toActivity(note);
    }

    @Transactional
    public void rate(Long applicationId, Long recruiterId, RatingRequest req) {
        Application app = findOwned(applicationId, recruiterId);
        app.setRecruiterRating(req.rating());
        activityRepository.save(ApplicationActivity.builder().application(app)
                .actor(userRepository.getReferenceById(recruiterId)).type(ActivityType.RATING)
                .message("Rated " + req.rating() + "/5").build());
    }

    @Transactional
    public InterviewResponse scheduleInterview(Long applicationId, Long recruiterId, InterviewRequest req) {
        Application app = findOwned(applicationId, recruiterId);
        if (app.getStage().isTerminal()) {
            throw ApiException.badRequest("Cannot schedule an interview for a " + app.getStage() + " application");
        }
        Interview interview = interviewRepository.save(Interview.builder().application(app)
                .scheduledAt(req.scheduledAt()).durationMin(req.durationMin()).mode(req.mode())
                .meetingLink(req.meetingLink()).notes(req.notes()).build());
        activityRepository.save(ApplicationActivity.builder().application(app)
                .actor(userRepository.getReferenceById(recruiterId)).type(ActivityType.INTERVIEW_SCHEDULED)
                .message(req.mode() + " interview on " + req.scheduledAt()).build());
        notificationService.notify(app.getCandidate(), "Interview scheduled: " + app.getJob().getTitle(),
                app.getJob().getCompanyName() + " scheduled a " + req.mode().name().toLowerCase(Locale.ROOT)
                        + " interview with you.", "/candidate/applications");
        return DtoMapper.toInterview(interview);
    }

    @Transactional(readOnly = true)
    public ApplicationDetail detail(Long applicationId, Long recruiterId) {
        Application app = findOwned(applicationId, recruiterId);
        CandidateProfile profile = profileRepository.findByUserId(app.getCandidate().getId()).orElse(null);
        return new ApplicationDetail(app.getId(), app.getStage(), app.getStage().allowedTransitions(),
                app.getCoverLetter(), DtoMapper.toAts(app), app.getRecruiterRating(), app.getAppliedAt(),
                DtoMapper.toProfile(app.getCandidate(), profile), DtoMapper.toJob(app.getJob()),
                activityRepository.findByApplicationIdOrderByCreatedAtDesc(app.getId()).stream()
                        .map(DtoMapper::toActivity).toList(),
                interviewRepository.findByApplicationIdOrderByScheduledAtAsc(app.getId()).stream()
                        .map(DtoMapper::toInterview).toList());
    }

    @Transactional(readOnly = true)
    public ProfileService.ResumeFile resume(Long applicationId, Long recruiterId) {
        Application app = findOwned(applicationId, recruiterId);
        return profileService.resumeOf(app.getCandidate().getId());
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> interviews(Long recruiterId) {
        return interviewRepository.findForRecruiter(recruiterId).stream().map(DtoMapper::toInterview).toList();
    }

    private Application findOwned(Long applicationId, Long recruiterId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Application", applicationId));
        if (!app.getJob().getRecruiter().getId().equals(recruiterId)) {
            throw ApiException.forbidden("This application belongs to another recruiter's job");
        }
        return app;
    }

    private static String stageTitle(ApplicationStage stage, String jobTitle) {
        return switch (stage) {
            case SCREENING -> "Your application is being reviewed";
            case INTERVIEW -> "Shortlisted for interview: " + jobTitle;
            case OFFER -> "You received an offer for " + jobTitle + "!";
            case HIRED -> "Welcome aboard! You're hired";
            case REJECTED -> "Update on your application for " + jobTitle;
            case APPLIED -> "Application status updated";
        };
    }

    private static String stageMessage(ApplicationStage stage, Job job) {
        return switch (stage) {
            case SCREENING -> job.getCompanyName() + " is screening your application for " + job.getTitle() + ".";
            case INTERVIEW -> job.getCompanyName() + " moved you to the interview round.";
            case OFFER -> job.getCompanyName() + " would like to make you an offer.";
            case HIRED -> "Congratulations on joining " + job.getCompanyName() + " as " + job.getTitle() + ".";
            case REJECTED -> job.getCompanyName() + " decided not to move forward at this time. Keep going!";
            case APPLIED -> "Your application for " + job.getTitle() + " is back in review.";
        };
    }
}
