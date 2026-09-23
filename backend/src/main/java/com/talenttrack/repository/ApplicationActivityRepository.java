package com.talenttrack.repository;

import com.talenttrack.entity.ApplicationActivity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationActivityRepository extends JpaRepository<ApplicationActivity, Long> {

    @EntityGraph(attributePaths = "actor")
    List<ApplicationActivity> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
