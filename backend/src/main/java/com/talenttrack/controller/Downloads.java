package com.talenttrack.controller;

import com.talenttrack.service.ProfileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

final class Downloads {

    private Downloads() {
    }

    static ResponseEntity<Resource> of(ProfileService.ResumeFile file) {
        String name = file.fileName() == null ? "resume" : file.fileName();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(name, StandardCharsets.UTF_8).build().toString())
                .body(file.resource());
    }
}
