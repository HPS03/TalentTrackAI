package com.talenttrack.repository;

import com.talenttrack.entity.Application;
import com.talenttrack.entity.ApplicationStage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    @EntityGraph(attributePaths = {"job", "candidate"})
    List<Application> findByCandidateIdOrderByAppliedAtDesc(Long candidateId);

    @EntityGraph(attributePaths = {"candidate"})
    List<Application> findByJobIdOrderByStageAscBoardPositionAsc(Long jobId);

    @Query("select a.job.id from Application a where a.candidate.id = :candidateId")
    Set<Long> findJobIdsAppliedBy(@Param("candidateId") Long candidateId);

    long countByJobId(Long jobId);

    long countByJobIdAndStage(Long jobId, ApplicationStage stage);

    long countByCandidateId(Long candidateId);

    long countByCandidateIdAndStage(Long candidateId, ApplicationStage stage);

    @Query("select coalesce(max(a.boardPosition), -1) from Application a where a.job.id = :jobId and a.stage = :stage")
    int maxBoardPosition(@Param("jobId") Long jobId, @Param("stage") ApplicationStage stage);

    @Query("select a.stage, count(a) from Application a group by a.stage")
    List<Object[]> countGroupedByStage();

    @Query("select a.stage, count(a) from Application a where a.job.recruiter.id = :recruiterId group by a.stage")
    List<Object[]> countGroupedByStageForRecruiter(@Param("recruiterId") Long recruiterId);

    @Query("select a.job.id, count(a) from Application a where a.job.recruiter.id = :recruiterId group by a.job.id")
    List<Object[]> countPerJobForRecruiter(@Param("recruiterId") Long recruiterId);

    @Query("select coalesce(avg(a.atsScore), 0) from Application a where a.job.recruiter.id = :recruiterId")
    double averageAtsForRecruiter(@Param("recruiterId") Long recruiterId);

    @Query("select coalesce(avg(a.atsScore), 0) from Application a")
    double averageAts();

    @EntityGraph(attributePaths = {"job", "candidate"})
    @Query("select a from Application a where a.job.recruiter.id = :recruiterId order by a.appliedAt desc")
    List<Application> findRecentForRecruiter(@Param("recruiterId") Long recruiterId,
                                             org.springframework.data.domain.Pageable pageable);
}
