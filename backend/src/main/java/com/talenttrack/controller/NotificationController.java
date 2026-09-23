package com.talenttrack.controller;

import com.talenttrack.dto.CommonDtos.MessageResponse;
import com.talenttrack.dto.CommonDtos.NotificationResponse;
import com.talenttrack.security.CurrentUser;
import com.talenttrack.security.UserPrincipal;
import com.talenttrack.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(@CurrentUser UserPrincipal user,
                                           @RequestParam(defaultValue = "20") int limit) {
        return notificationService.latest(user.id(), limit);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@CurrentUser UserPrincipal user) {
        return Map.of("count", notificationService.unreadCount(user.id()));
    }

    @PatchMapping("/{id}/read")
    public MessageResponse markRead(@CurrentUser UserPrincipal user, @PathVariable Long id) {
        notificationService.markRead(id, user.id());
        return new MessageResponse("Marked as read");
    }

    @PatchMapping("/read-all")
    public MessageResponse markAllRead(@CurrentUser UserPrincipal user) {
        int n = notificationService.markAllRead(user.id());
        return new MessageResponse(n + " notifications marked as read");
    }
}
