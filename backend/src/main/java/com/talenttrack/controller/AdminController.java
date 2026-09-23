package com.talenttrack.controller;

import com.talenttrack.dto.AuthDtos.UserDto;
import com.talenttrack.dto.CommonDtos.*;
import com.talenttrack.dto.DashboardDtos.AdminDashboard;
import com.talenttrack.dto.JobDtos.JobResponse;
import com.talenttrack.entity.Role;
import com.talenttrack.security.CurrentUser;
import com.talenttrack.security.UserPrincipal;
import com.talenttrack.service.AdminService;
import com.talenttrack.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public AdminDashboard dashboard() {
        return dashboardService.admin();
    }

    @GetMapping("/users")
    public PageResponse<UserDto> users(@RequestParam(required = false) Role role,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return adminService.users(role, q, page, size);
    }

    @PatchMapping("/users/{id}/status")
    public UserDto setStatus(@CurrentUser UserPrincipal admin, @PathVariable Long id,
                             @RequestBody UserStatusRequest request) {
        return adminService.setEnabled(id, request.enabled(), admin.id());
    }

    @GetMapping("/jobs")
    public PageResponse<JobResponse> jobs(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return adminService.jobs(page, size);
    }

    @DeleteMapping("/jobs/{id}")
    public MessageResponse deleteJob(@PathVariable Long id) {
        adminService.deleteJob(id);
        return new MessageResponse("Job removed");
    }
}
