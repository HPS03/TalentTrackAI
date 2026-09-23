package com.talenttrack.service;

import com.talenttrack.dto.CommonDtos.NotificationResponse;
import com.talenttrack.entity.Notification;
import com.talenttrack.entity.User;
import com.talenttrack.exception.ApiException;
import com.talenttrack.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public void notify(User user, String title, String message, String link) {
        repository.save(Notification.builder().user(user).title(title).message(message).link(link).build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> latest(Long userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Math.min(limit, 100)))
                .stream().map(DtoMapper::toNotification).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        Notification n = repository.findById(id).orElseThrow(() -> ApiException.notFound("Notification", id));
        if (!n.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Not your notification");
        }
        n.setRead(true);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllRead(userId);
    }
}
