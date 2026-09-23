package com.talenttrack.dto;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

public final class CommonDtos {

    private CommonDtos() {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    public record MessageResponse(String message) {
    }

    public record NotificationResponse(Long id, String title, String message, String link, boolean read,
                                       Instant createdAt) {
    }

    public record UserStatusRequest(boolean enabled) {
    }
}
