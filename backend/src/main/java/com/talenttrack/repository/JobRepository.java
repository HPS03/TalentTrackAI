package com.talenttrack.repository;

import com.talenttrack.entity.Job;
import com.talenttrack.entity.JobStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    List<Job> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    List<Job> findTop200ByStatusOrderByCreatedAtDesc(JobStatus status);

    long countByStatus(JobStatus status);

    long countByRecruiterId(Long recruiterId);

    long countByRecruiterIdAndStatus(Long recruiterId, JobStatus status);

    @EntityGraph(attributePaths = "recruiter")
    @Query("select j from Job j where j.createdAt >= :since")
    List<Job> findCreatedSince(Instant since);
}
