package com.talenttrack;

import com.talenttrack.ai.AiModels;
import com.talenttrack.ai.LocalMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMatcherTest {

    private static final String JD = "Build REST APIs with Java and Spring Boot, store data in MySQL, deploy using Docker.";

    @Test
    void strongCandidateScoresHigherThanWeakCandidate() {
        var strong = LocalMatcher.match(new AiModels.MatchRequest("Java Spring Boot developer building REST APIs on MySQL",
                List.of("Java", "Spring Boot", "MySQL", "Docker"), 3, JD,
                List.of("Java", "Spring Boot", "MySQL", "Docker"), 2));
        var weak = LocalMatcher.match(new AiModels.MatchRequest("Graphic designer using Photoshop",
                List.of("Photoshop"), 0, JD, List.of("Java", "Spring Boot", "MySQL", "Docker"), 2));

        assertThat(strong.overall()).isGreaterThan(weak.overall());
        assertThat(strong.matchedSkills()).containsExactly("Java", "Spring Boot", "MySQL", "Docker");
        assertThat(strong.skillScore()).isEqualTo(100);
        assertThat(weak.missingSkills()).hasSize(4);
        assertThat(weak.experienceScore()).isZero();
    }

    @Test
    void skillsFoundOnlyInResumeTextStillCount() {
        var r = LocalMatcher.match(new AiModels.MatchRequest("Experienced with docker and mysql", List.of(), 1, JD,
                List.of("Docker", "MySQL", "Kubernetes"), 0));
        assertThat(r.matchedSkills()).containsExactly("Docker", "MySQL");
        assertThat(r.missingSkills()).containsExactly("Kubernetes");
        assertThat(r.overall()).isBetween(0, 100);
    }
}
