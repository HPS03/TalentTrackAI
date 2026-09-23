package com.talenttrack.service;

import com.talenttrack.ai.AiGateway;
import com.talenttrack.ai.AiModels;
import com.talenttrack.dto.ProfileDtos.*;
import com.talenttrack.entity.CandidateProfile;
import com.talenttrack.entity.User;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.CandidateProfileRepository;
import com.talenttrack.repository.UserRepository;
import com.talenttrack.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final AiGateway ai;

    @Transactional
    public CandidateProfile getOrCreate(Long userId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User", userId));
            return profileRepository.save(CandidateProfile.builder().user(user).build());
        });
    }

    @Transactional
    public ProfileResponse get(Long userId) {
        CandidateProfile p = getOrCreate(userId);
        return DtoMapper.toProfile(p.getUser(), p);
    }

    @Transactional
    public ProfileResponse update(Long userId, ProfileRequest req) {
        CandidateProfile p = getOrCreate(userId);
        p.setHeadline(req.headline());
        p.setBio(req.bio());
        p.setPhone(req.phone());
        p.setLocation(req.location());
        p.setExperienceYears(req.experienceYears());
        p.setEducation(req.education());
        p.setSkills(Skills.join(req.skills()));
        p.setLinkedinUrl(req.linkedinUrl());
        p.setGithubUrl(req.githubUrl());
        p.setPortfolioUrl(req.portfolioUrl());
        return DtoMapper.toProfile(p.getUser(), p);
    }

    /**
     * Stores the resume, sends it to the AI service for parsing and merges the extracted
     * skills into the profile so candidates don't have to type them manually.
     */
    @Transactional
    public ResumeUploadResponse uploadResume(Long userId, MultipartFile file) {
        storage.extensionOf(file); // validate before doing any work
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        CandidateProfile p = getOrCreate(userId);
        String previous = p.getResumeStoredName();
        String stored = storage.store(file);
        p.setResumeStoredName(stored);
        p.setResumeFileName(file.getOriginalFilename());
        p.setResumeUploadedAt(Instant.now());

        List<String> extracted = List.of();
        Integer detectedYears = null;
        ResumeQuality quality = new ResumeQuality(0, List.of(),
                List.of("AI parsing is currently unavailable. Add your skills manually for accurate matching."));
        var parsed = ai.parseResume(bytes, file.getOriginalFilename());
        if (parsed.isPresent()) {
            AiModels.ParsedResume r = parsed.get();
            p.setResumeText(r.text());
            extracted = r.skills() == null ? List.of() : r.skills();
            p.setSkills(Skills.join(Skills.merge(Skills.split(p.getSkills()), extracted)));
            detectedYears = r.experienceYears();
            if (detectedYears != null && p.getExperienceYears() == 0) {
                p.setExperienceYears(detectedYears);
            }
            if (isBlank(p.getPhone()) && !isBlank(r.phone())) {
                p.setPhone(r.phone());
            }
            quality = new ResumeQuality(r.qualityScore(), r.sectionsFound(), r.tips());
        }
        storage.delete(previous);
        return new ResumeUploadResponse(DtoMapper.toProfile(p.getUser(), p), extracted, detectedYears, quality);
    }

    @Transactional(readOnly = true)
    public ResumeFile resumeOf(Long userId) {
        CandidateProfile p = profileRepository.findByUserId(userId)
                .filter(CandidateProfile::hasResume)
                .orElseThrow(() -> ApiException.notFound("Resume for user", userId));
        return new ResumeFile(storage.load(p.getResumeStoredName()), p.getResumeFileName(),
                storage.contentTypeFor(p.getResumeStoredName()));
    }

    public record ResumeFile(Resource resource, String fileName, String contentType) {
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
