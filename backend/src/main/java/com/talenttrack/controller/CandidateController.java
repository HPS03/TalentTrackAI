package com.talenttrack.controller;

import com.talenttrack.dto.ApplicationDtos.*;
import com.talenttrack.dto.CommonDtos.MessageResponse;
import com.talenttrack.dto.DashboardDtos.CandidateDashboard;
import com.talenttrack.dto.JobDtos.RecommendedJob;
import com.talenttrack.dto.ProfileDtos.*;
import com.talenttrack.security.CurrentUser;
import com.talenttrack.security.UserPrincipal;
import com.talenttrack.service.CandidateApplicationService;
import com.talenttrack.service.DashboardService;
import com.talenttrack.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Candidate")
@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final ProfileService profileService;
    private final CandidateApplicationService applicationService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public CandidateDashboard dashboard(@CurrentUser UserPrincipal user) {
        return dashboardService.candidate(user.id());
    }

    @GetMapping("/profile")
    public ProfileResponse profile(@CurrentUser UserPrincipal user) {
        return profileService.get(user.id());
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(@CurrentUser UserPrincipal user, @Valid @RequestBody ProfileRequest request) {
        return profileService.update(user.id(), request);
    }

    @Operation(summary = "Upload a PDF/DOCX resume; the AI service extracts skills and scores its quality")
    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResumeUploadResponse uploadResume(@CurrentUser UserPrincipal user, @RequestParam("file") MultipartFile file) {
        return profileService.uploadResume(user.id(), file);
    }

    @GetMapping("/resume")
    public ResponseEntity<Resource> downloadResume(@CurrentUser UserPrincipal user) {
        return Downloads.of(profileService.resumeOf(user.id()));
    }

    @Operation(summary = "Apply to a job; an ATS score is computed against the job description")
    @PostMapping("/jobs/{jobId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public MyApplication apply(@CurrentUser UserPrincipal user, @PathVariable Long jobId,
                               @Valid @RequestBody(required = false) ApplyRequest request) {
        return applicationService.apply(jobId, user.id(), request);
    }

    @GetMapping("/applications")
    public List<MyApplication> applications(@CurrentUser UserPrincipal user) {
        return applicationService.myApplications(user.id());
    }

    @DeleteMapping("/applications/{id}")
    public MessageResponse withdraw(@CurrentUser UserPrincipal user, @PathVariable Long id) {
        applicationService.withdraw(id, user.id());
        return new MessageResponse("Application withdrawn");
    }

    @Operation(summary = "AI job recommendations ranked by resume/profile match")
    @GetMapping("/recommendations")
    public List<RecommendedJob> recommendations(@CurrentUser UserPrincipal user,
                                                @RequestParam(defaultValue = "6") int limit) {
        return applicationService.recommendations(user.id(), limit);
    }

    @Operation(summary = "Check your resume's ATS score against any job description")
    @PostMapping("/ats-check")
    public AtsBreakdown atsCheck(@CurrentUser UserPrincipal user, @Valid @RequestBody AnalyzeRequest request) {
        return applicationService.analyze(user.id(), request);
    }

    @GetMapping("/interviews")
    public List<InterviewResponse> interviews(@CurrentUser UserPrincipal user) {
        return applicationService.interviews(user.id());
    }
}
