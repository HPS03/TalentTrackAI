package com.talenttrack.controller;

import com.talenttrack.dto.CommonDtos.PageResponse;
import com.talenttrack.dto.JobDtos.JobResponse;
import com.talenttrack.entity.*;
import com.talenttrack.security.CurrentUser;
import com.talenttrack.security.UserPrincipal;
import com.talenttrack.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Tag(name = "Jobs (public)")
@RestController
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "Search open jobs & internships with filters and pagination")
    @GetMapping("/api/jobs")
    public PageResponse<JobResponse> search(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String location,
                                            @RequestParam(required = false) JobType jobType,
                                            @RequestParam(required = false) WorkMode workMode,
                                            @RequestParam(required = false) Integer maxExperience,
                                            @RequestParam(required = false) String skill,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size,
                                            @RequestParam(defaultValue = "newest") String sort,
                                            @CurrentUser UserPrincipal user) {
        return jobService.search(new JobService.JobSearch(q, location, jobType, workMode, maxExperience, skill,
                page, size, sort), candidateId(user));
    }

    @GetMapping("/api/jobs/{id}")
    public JobResponse get(@PathVariable Long id, @CurrentUser UserPrincipal user) {
        return jobService.get(id, candidateId(user));
    }

    @Operation(summary = "Enum values used by the UI (job types, work modes, pipeline stages)")
    @GetMapping("/api/meta/enums")
    public Map<String, List<String>> enums() {
        return Map.of(
                "jobTypes", names(JobType.values()),
                "workModes", names(WorkMode.values()),
                "stages", names(ApplicationStage.values()),
                "interviewModes", names(InterviewMode.values()));
    }

    private static Long candidateId(UserPrincipal user) {
        return user != null && user.role() == Role.CANDIDATE ? user.id() : null;
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
