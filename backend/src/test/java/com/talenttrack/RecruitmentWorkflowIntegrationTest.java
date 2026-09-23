package com.talenttrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.talenttrack.ai.AiModels;
import com.talenttrack.ai.AiServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecruitmentWorkflowIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private AiServiceClient aiClient;

    private String recruiter;
    private String candidate;
    private long jobId;

    @BeforeEach
    void setUp() throws Exception {
        when(aiClient.match(any())).thenReturn(new AiModels.MatchResult(82, 90, 70, 100,
                List.of("Java", "Spring Boot"), List.of("Docker"), "Strong match"));

        recruiter = tokenFor("Rita Recruiter", "RECRUITER", "Acme");
        candidate = tokenFor("Carl Candidate", "CANDIDATE", null);

        put("/api/candidate/profile", candidate, Map.of("headline", "Java dev", "experienceYears", 2,
                "skills", List.of("Java", "Spring Boot"))).andExpect(status().isOk());

        JsonNode job = body(post("/api/recruiter/jobs", recruiter, jobRequest()).andExpect(status().isCreated()));
        jobId = job.get("id").asLong();
        assertThat(job.get("companyName").asText()).isEqualTo("Acme");
    }

    @Test
    void fullPipelineFromApplicationToKanbanMove() throws Exception {
        JsonNode app = body(post("/api/candidate/jobs/" + jobId + "/apply", candidate,
                Map.of("coverLetter", "Hire me")).andExpect(status().isCreated()));
        long appId = app.get("id").asLong();
        assertThat(app.get("ats").get("overall").asInt()).isEqualTo(82);
        assertThat(app.get("stage").asText()).isEqualTo("APPLIED");

        post("/api/candidate/jobs/" + jobId + "/apply", candidate, Map.of()).andExpect(status().isConflict());

        get("/api/recruiter/jobs/" + jobId + "/board", recruiter)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.APPLIED").value(1))
                .andExpect(jsonPath("$.columns[0].stage").value("APPLIED"))
                .andExpect(jsonPath("$.columns[0].cards[0].candidateName").value("Carl Candidate"))
                .andExpect(jsonPath("$.columns[0].cards[0].headline").value("Java dev"));

        // workflow rules: cannot jump straight to OFFER
        patch("/api/recruiter/applications/" + appId + "/stage", recruiter, Map.of("stage", "OFFER"))
                .andExpect(status().isBadRequest());

        patch("/api/recruiter/applications/" + appId + "/stage", recruiter,
                Map.of("stage", "SCREENING", "position", 0, "comment", "Looks promising"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedTransitions.length()").value(3));

        post("/api/recruiter/applications/" + appId + "/notes", recruiter, Map.of("message", "Great GitHub"))
                .andExpect(status().isCreated());
        post("/api/recruiter/applications/" + appId + "/interviews", recruiter, Map.of(
                "scheduledAt", Instant.now().plus(2, ChronoUnit.DAYS).toString(), "durationMin", 45,
                "mode", "VIDEO", "meetingLink", "https://meet.example.com/x")).andExpect(status().isCreated());

        get("/api/recruiter/applications/" + appId, recruiter)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("SCREENING"))
                .andExpect(jsonPath("$.activities.length()").value(4))
                .andExpect(jsonPath("$.interviews.length()").value(1));

        get("/api/candidate/applications", candidate)
                .andExpect(jsonPath("$[0].stage").value("SCREENING"))
                .andExpect(jsonPath("$[0].interviews.length()").value(1));
        get("/api/notifications/unread-count", candidate).andExpect(jsonPath("$.count").value(2));
        get("/api/recruiter/dashboard", recruiter)
                .andExpect(jsonPath("$.totalApplicants").value(1))
                .andExpect(jsonPath("$.funnel.SCREENING").value(1));
    }

    @Test
    void rolesAndOwnershipAreEnforced() throws Exception {
        long appId = body(post("/api/candidate/jobs/" + jobId + "/apply", candidate, Map.of())).get("id").asLong();
        String otherRecruiter = tokenFor("Other", "RECRUITER", "Globex");

        get("/api/recruiter/jobs/" + jobId + "/board", otherRecruiter).andExpect(status().isForbidden());
        patch("/api/recruiter/applications/" + appId + "/stage", otherRecruiter, Map.of("stage", "SCREENING"))
                .andExpect(status().isForbidden());
        put("/api/recruiter/jobs/" + jobId, otherRecruiter, jobRequest()).andExpect(status().isForbidden());
        get("/api/recruiter/dashboard", candidate).andExpect(status().isForbidden());
        post("/api/candidate/jobs/" + jobId + "/apply", recruiter, Map.of()).andExpect(status().isForbidden());
        get("/api/admin/dashboard", recruiter).andExpect(status().isForbidden());
    }

    @Test
    void applyingFallsBackToLocalMatcherWhenAiServiceIsDown() throws Exception {
        when(aiClient.match(any())).thenThrow(new ResourceAccessException("connection refused"));
        JsonNode app = body(post("/api/candidate/jobs/" + jobId + "/apply", candidate, Map.of())
                .andExpect(status().isCreated()));
        assertThat(app.get("ats").get("summary").asText()).contains("built-in matcher");
        assertThat(app.get("ats").get("matchedSkills").toString()).contains("Java");
    }

    @Test
    void closedJobsAreHiddenAndRejectApplications() throws Exception {
        patch("/api/recruiter/jobs/" + jobId + "/status", recruiter, Map.of("status", "CLOSED"))
                .andExpect(status().isOk());
        post("/api/candidate/jobs/" + jobId + "/apply", candidate, Map.of()).andExpect(status().isBadRequest());
        JsonNode page = body(get("/api/jobs?q=Platform%20Engineer", null));
        page.get("content").forEach(j -> assertThat(j.get("id").asLong()).isNotEqualTo(jobId));
    }

    @Test
    void jobSearchFiltersByTypeAndKeyword() throws Exception {
        get("/api/jobs?q=platform engineer&jobType=FULL_TIME", candidate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Platform Engineer"))
                .andExpect(jsonPath("$.content[0].alreadyApplied").value(false));
        get("/api/jobs?q=platform engineer&jobType=INTERNSHIP", null)
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private Map<String, Object> jobRequest() {
        return Map.ofEntries(
                Map.entry("title", "Platform Engineer"),
                Map.entry("location", "Remote"),
                Map.entry("jobType", "FULL_TIME"),
                Map.entry("workMode", "REMOTE"),
                Map.entry("minExperience", 1),
                Map.entry("salaryMin", 100),
                Map.entry("salaryMax", 200),
                Map.entry("description", "Build and run Java Spring Boot services on Docker in the cloud."),
                Map.entry("skills", List.of("Java", "Spring Boot", "Docker")),
                Map.entry("openings", 2),
                Map.entry("deadline", LocalDate.now().plusDays(30).toString()));
    }
}
