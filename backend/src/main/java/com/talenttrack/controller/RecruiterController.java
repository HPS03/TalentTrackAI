package com.talenttrack.controller;

import com.talenttrack.dto.ApplicationDtos.*;
import com.talenttrack.dto.CommonDtos.MessageResponse;
import com.talenttrack.dto.DashboardDtos.RecruiterDashboard;
import com.talenttrack.dto.JobDtos.*;
import com.talenttrack.security.CurrentUser;
import com.talenttrack.security.UserPrincipal;
import com.talenttrack.service.DashboardService;
import com.talenttrack.service.JobService;
import com.talenttrack.service.RecruitmentBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Recruiter")
@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final JobService jobService;
    private final RecruitmentBoardService boardService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public RecruiterDashboard dashboard(@CurrentUser UserPrincipal user) {
        return dashboardService.recruiter(user.id());
    }

    // ---- Job postings -------------------------------------------------------------------------

    @GetMapping("/jobs")
    public List<JobResponse> myJobs(@CurrentUser UserPrincipal user) {
        return jobService.recruiterJobs(user.id());
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse createJob(@CurrentUser UserPrincipal user, @Valid @RequestBody JobRequest request) {
        return jobService.create(request, user.id());
    }

    @PutMapping("/jobs/{id}")
    public JobResponse updateJob(@CurrentUser UserPrincipal user, @PathVariable Long id,
                                 @Valid @RequestBody JobRequest request) {
        return jobService.update(id, request, user.id());
    }

    @PatchMapping("/jobs/{id}/status")
    public JobResponse changeStatus(@CurrentUser UserPrincipal user, @PathVariable Long id,
                                    @Valid @RequestBody JobStatusRequest request) {
        return jobService.changeStatus(id, request.status(), user.id());
    }

    @DeleteMapping("/jobs/{id}")
    public MessageResponse deleteJob(@CurrentUser UserPrincipal user, @PathVariable Long id) {
        jobService.delete(id, user.id());
        return new MessageResponse("Job deleted");
    }

    // ---- Kanban board -------------------------------------------------------------------------

    @Operation(summary = "Kanban board for a job: applications grouped by pipeline stage")
    @GetMapping("/jobs/{jobId}/board")
    public BoardResponse board(@CurrentUser UserPrincipal user, @PathVariable Long jobId,
                               @RequestParam(required = false) Integer minAts,
                               @RequestParam(required = false) String q) {
        return boardService.board(jobId, user.id(), minAts, q);
    }

    @Operation(summary = "Move a card to another stage/position (workflow rules enforced)")
    @PatchMapping("/applications/{id}/stage")
    public BoardCard moveStage(@CurrentUser UserPrincipal user, @PathVariable Long id,
                               @Valid @RequestBody StageChangeRequest request) {
        return boardService.moveStage(id, user.id(), request);
    }

    @GetMapping("/applications/{id}")
    public ApplicationDetail detail(@CurrentUser UserPrincipal user, @PathVariable Long id) {
        return boardService.detail(id, user.id());
    }

    @PostMapping("/applications/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityResponse addNote(@CurrentUser UserPrincipal user, @PathVariable Long id,
                                    @Valid @RequestBody NoteRequest request) {
        return boardService.addNote(id, user.id(), request);
    }

    @PatchMapping("/applications/{id}/rating")
    public MessageResponse rate(@CurrentUser UserPrincipal user, @PathVariable Long id,
                                @Valid @RequestBody RatingRequest request) {
        boardService.rate(id, user.id(), request);
        return new MessageResponse("Rating saved");
    }

    @PostMapping("/applications/{id}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse scheduleInterview(@CurrentUser UserPrincipal user, @PathVariable Long id,
                                               @Valid @RequestBody InterviewRequest request) {
        return boardService.scheduleInterview(id, user.id(), request);
    }

    @GetMapping("/applications/{id}/resume")
    public ResponseEntity<Resource> resume(@CurrentUser UserPrincipal user, @PathVariable Long id) {
        return Downloads.of(boardService.resume(id, user.id()));
    }

    @GetMapping("/interviews")
    public List<InterviewResponse> interviews(@CurrentUser UserPrincipal user) {
        return boardService.interviews(user.id());
    }
}
