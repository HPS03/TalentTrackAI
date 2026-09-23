package com.talenttrack.service;

import com.talenttrack.dto.AuthDtos.UserDto;
import com.talenttrack.dto.CommonDtos.PageResponse;
import com.talenttrack.dto.JobDtos.JobResponse;
import com.talenttrack.entity.Role;
import com.talenttrack.entity.User;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.JobRepository;
import com.talenttrack.repository.RefreshTokenRepository;
import com.talenttrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> users(Role role, String q, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(userRepository.search(role, query,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt"))), DtoMapper::toUser);
    }

    @Transactional
    public UserDto setEnabled(Long userId, boolean enabled, Long adminId) {
        if (userId.equals(adminId)) {
            throw ApiException.badRequest("You cannot change your own account status");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.revokeAllForUser(user);
        }
        return DtoMapper.toUser(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> jobs(int page, int size) {
        return PageResponse.of(jobRepository.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"))), DtoMapper::toJob);
    }

    @Transactional
    public void deleteJob(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw ApiException.notFound("Job", jobId);
        }
        jobRepository.deleteById(jobId);
    }
}
