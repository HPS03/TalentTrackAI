package com.talenttrack.service;

import com.talenttrack.dto.ApplicationDtos.InterviewResponse;
import com.talenttrack.dto.DashboardDtos.*;
import com.talenttrack.entity.*;
import com.talenttrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final CandidateProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public CandidateDashboard candidate(Long candidateId) {
        List<Application> apps = applicationRepository.findByCandidateIdOrderByAppliedAtDesc(candidateId);
        Map<ApplicationStage, Long> byStage = emptyStageMap();
        apps.forEach(a -> byStage.merge(a.getStage(), 1L, Long::sum));
        CandidateProfile profile = profileRepository.findByUserId(candidateId).orElse(null);
        return new CandidateDashboard(apps.size(), byStage.get(ApplicationStage.INTERVIEW),
                byStage.get(ApplicationStage.OFFER) + byStage.get(ApplicationStage.HIRED),
                byStage.get(ApplicationStage.REJECTED), DtoMapper.completeness(profile),
                profile != null && profile.hasResume(), byStage,
                upcoming(interviewRepository.findForCandidate(candidateId)));
    }

    @Transactional(readOnly = true)
    public RecruiterDashboard recruiter(Long recruiterId) {
        Map<ApplicationStage, Long> funnel = toStageMap(applicationRepository.countGroupedByStageForRecruiter(recruiterId));
        long totalApplicants = funnel.values().stream().mapToLong(Long::longValue).sum();

        Map<Long, Long> perJob = new HashMap<>();
        applicationRepository.countPerJobForRecruiter(recruiterId).forEach(r -> perJob.put((Long) r[0], (Long) r[1]));
        List<JobPipeline> topJobs = jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId).stream()
                .map(j -> new JobPipeline(j.getId(), j.getTitle(), perJob.getOrDefault(j.getId(), 0L)))
                .sorted(Comparator.comparingLong(JobPipeline::applicants).reversed())
                .limit(6).toList();

        List<RecentApplicant> recent = applicationRepository.findRecentForRecruiter(recruiterId, PageRequest.of(0, 8))
                .stream().map(a -> new RecentApplicant(a.getId(), a.getCandidate().getFullName(), a.getJob().getTitle(),
                        a.getAtsScore(), a.getStage(), a.getAppliedAt())).toList();

        return new RecruiterDashboard(jobRepository.countByRecruiterId(recruiterId),
                jobRepository.countByRecruiterIdAndStatus(recruiterId, JobStatus.OPEN), totalApplicants,
                round1(applicationRepository.averageAtsForRecruiter(recruiterId)), funnel.get(ApplicationStage.HIRED),
                funnel, topJobs, recent, upcoming(interviewRepository.findForRecruiter(recruiterId)));
    }

    @Transactional(readOnly = true)
    public AdminDashboard admin() {
        Map<ApplicationStage, Long> byStage = toStageMap(applicationRepository.countGroupedByStage());
        long totalApps = byStage.values().stream().mapToLong(Long::longValue).sum();

        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        Map<YearMonth, Long> months = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            months.put(current.minusMonths(i), 0L);
        }
        Instant since = current.minusMonths(5).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        jobRepository.findCreatedSince(since).forEach(j ->
                months.computeIfPresent(YearMonth.from(j.getCreatedAt().atZone(ZoneOffset.UTC)), (k, v) -> v + 1));

        return new AdminDashboard(userRepository.count(), userRepository.countByRole(Role.CANDIDATE),
                userRepository.countByRole(Role.RECRUITER), userRepository.countByRole(Role.ADMIN),
                jobRepository.count(), jobRepository.countByStatus(JobStatus.OPEN), totalApps,
                round1(applicationRepository.averageAts()), byStage,
                months.entrySet().stream().map(e -> new MonthlyCount(e.getKey().toString(), e.getValue())).toList());
    }

    private static List<InterviewResponse> upcoming(List<Interview> interviews) {
        Instant now = Instant.now();
        return interviews.stream().filter(i -> i.getScheduledAt().isAfter(now)).limit(5)
                .map(DtoMapper::toInterview).toList();
    }

    private static Map<ApplicationStage, Long> emptyStageMap() {
        Map<ApplicationStage, Long> map = new EnumMap<>(ApplicationStage.class);
        for (ApplicationStage s : ApplicationStage.values()) {
            map.put(s, 0L);
        }
        return map;
    }

    private static Map<ApplicationStage, Long> toStageMap(List<Object[]> rows) {
        Map<ApplicationStage, Long> map = emptyStageMap();
        rows.forEach(r -> map.put((ApplicationStage) r[0], (Long) r[1]));
        return map;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
