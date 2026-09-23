package com.talenttrack.storage;

import com.talenttrack.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Stores resumes on local disk (mounted as a Docker volume) under random names. */
@Service
public class FileStorageService {

    public static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Path root;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir, "resumes").toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    public String extensionOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw ApiException.badRequest("Resume must be a PDF or DOCX file");
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.containsKey(ext)) {
            throw ApiException.badRequest("Resume must be a PDF or DOCX file");
        }
        return ext;
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Uploaded file is empty");
        }
        String ext = extensionOf(file);
        String storedName = UUID.randomUUID() + "." + ext;
        try {
            Files.copy(file.getInputStream(), root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store resume", e);
        }
        return storedName;
    }

    public Resource load(String storedName) {
        try {
            Path path = root.resolve(storedName).normalize();
            if (!path.startsWith(root) || !Files.exists(path)) {
                throw ApiException.notFound("Resume file", storedName);
            }
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw ApiException.notFound("Resume file", storedName);
        }
    }

    public void delete(String storedName) {
        if (storedName == null) {
            return;
        }
        try {
            Path path = root.resolve(storedName).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    public String contentTypeFor(String storedName) {
        String ext = storedName.substring(storedName.lastIndexOf('.') + 1);
        return ALLOWED_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
