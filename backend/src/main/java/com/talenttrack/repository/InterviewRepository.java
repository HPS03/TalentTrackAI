package com.talenttrack.repository;

import com.talenttrack.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationIdOrderByScheduledAtAsc(Long applicationId);

    @Query("""
            select i from Interview i
              join fetch i.application a
              join fetch a.job
            where a.candidate.id = :candidateId
            order by i.scheduledAt asc
            """)
    List<Interview> findForCandidate(@Param("candidateId") Long candidateId);

    @Query("""
            select i from Interview i
              join fetch i.application a
              join fetch a.job j
              join fetch a.candidate
            where j.recruiter.id = :recruiterId
            order by i.scheduledAt asc
            """)
    List<Interview> findForRecruiter(@Param("recruiterId") Long recruiterId);
}
