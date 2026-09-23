package com.talenttrack.service;

import com.talenttrack.dto.CommonDtos.PageResponse;
import com.talenttrack.dto.JobDtos.JobRequest;
import com.talenttrack.dto.JobDtos.JobResponse;
import com.talenttrack.entity.*;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.ApplicationRepository;
import com.talenttrack.repository.JobRepository;
import com.talenttrack.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public record JobSearch(String q, String location, JobType jobType, WorkMode workMode, Integer maxExperience,
                            String skill, int page, int size, String sort) {
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> search(JobSearch s, Long candidateId) {
        Sort sort = switch (s.sort() == null ? "newest" : s.sort()) {
            case "salary" -> Sort.by(Sort.Direction.DESC, "salaryMax").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        int size = Math.min(Math.max(s.size(), 1), 50);
        Page<Job> page = jobRepository.findAll(openJobsMatching(s), PageRequest.of(Math.max(s.page(), 0), size, sort));
        Set<Long> applied = candidateId == null ? Set.of() : applicationRepository.findJobIdsAppliedBy(candidateId);
        return PageResponse.of(page, j -> DtoMapper.toJob(j, null, candidateId == null ? null : applied.contains(j.getId())));
    }

    private static Specification<Job> openJobsMatching(JobSearch s) {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("recruiter");   // avoid N+1 when mapping recruiter name
            }
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("status"), JobStatus.OPEN));
            if (notBlank(s.q())) {
                String like = "%" + s.q().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(root.get("skills")), like)));
            }
            if (notBlank(s.location())) {
                p.add(cb.like(cb.lower(root.get("location")), "%" + s.location().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (notBlank(s.skill())) {
                p.add(cb.like(cb.lower(root.get("skills")), "%" + s.skill().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (s.jobType() != null) {
                p.add(cb.equal(root.get("jobType"), s.jobType()));
            }
            if (s.workMode() != null) {
                p.add(cb.equal(root.get("workMode"), s.workMode()));
            }
            if (s.maxExperience() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("minExperience"), s.maxExperience()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public JobResponse get(Long id, Long candidateId) {
        Job job = find(id);
        Boolean applied = candidateId == null ? null
                : applicationRepository.existsByJobIdAndCandidateId(id, candidateId);
        return DtoMapper.toJob(job, applicationRepository.countByJobId(id), applied);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> recruiterJobs(Long recruiterId) {
        Map<Long, Long> counts = new HashMap<>();
        applicationRepository.countPerJobForRecruiter(recruiterId)
                .forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId).stream()
                .map(j -> DtoMapper.toJob(j, counts.getOrDefault(j.getId(), 0L), null))
                .toList();
    }

    @Transactional
    public JobResponse create(JobRequest req, Long recruiterId) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> ApiException.notFound("User", recruiterId));
        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setStatus(JobStatus.OPEN);
        apply(job, req, recruiter);
        return DtoMapper.toJob(jobRepository.save(job), 0L, null);
    }

    @Transactional
    public JobResponse update(Long id, JobRequest req, Long recruiterId) {
        Job job = findOwned(id, recruiterId);
        apply(job, req, job.getRecruiter());
        return DtoMapper.toJob(job, applicationRepository.countByJobId(id), null);
    }

    @Transactional
    public JobResponse changeStatus(Long id, JobStatus status, Long recruiterId) {
        Job job = findOwned(id, recruiterId);
        job.setStatus(status);
        return DtoMapper.toJob(job, applicationRepository.countByJobId(id), null);
    }

    @Transactional
    public void delete(Long id, Long recruiterId) {
        jobRepository.delete(findOwned(id, recruiterId));
    }

    public Job find(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> ApiException.notFound("Job", id));
    }

    public Job findOwned(Long id, Long recruiterId) {
        Job job = find(id);
        if (!job.getRecruiter().getId().equals(recruiterId)) {
            throw ApiException.forbidden("You can only manage your own job postings");
        }
        return job;
    }

    private static void apply(Job job, JobRequest req, User recruiter) {
        job.setTitle(req.title().trim());
        job.setCompanyName(notBlank(req.companyName()) ? req.companyName().trim()
                : Objects.requireNonNullElse(recruiter.getCompanyName(), recruiter.getFullName()));
        job.setLocation(req.location().trim());
        job.setJobType(req.jobType());
        job.setWorkMode(req.workMode());
        job.setMinExperience(req.minExperience());
        job.setSalaryMin(req.salaryMin());
        job.setSalaryMax(req.salaryMax());
        job.setDescription(req.description().trim());
        job.setSkills(Skills.join(req.skills()));
        job.setOpenings(req.openings());
        job.setDeadline(req.deadline());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
