package com.talenttrack.repository;

import com.talenttrack.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByUserId(Long userId);

    List<CandidateProfile> findByUserIdIn(Collection<Long> userIds);
}
