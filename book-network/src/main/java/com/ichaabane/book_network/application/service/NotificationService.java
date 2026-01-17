package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.application.dto.response.NotificationResponse;
import com.ichaabane.book_network.domain.enums.NotificationStatus;
import com.ichaabane.book_network.domain.model.Notification;
import com.ichaabane.book_network.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ichaabane.book_network.domain.repository.NotificationRepository;
import com.ichaabane.book_network.domain.exception.OperationNotPermittedException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Créer une notif + l'envoyer en WebSocket
     */
    public void sendNotification(User user, NotificationStatus status, String message, String bookTitle) {
        Notification notif = Notification.builder()
                .user(user)
                .status(status)
                .message(message)
                .bookTitle(bookTitle)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notif);

        log.info("Sending WS notification to user {}: {}", user.getId(), notif);

        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/notifications",
                NotificationResponse.fromEntity(notif) // on envoie un DTO au front
        );
    }

    /**
     * Récupérer toutes les notifs d'un user
     */
    public List<NotificationResponse> getUserNotifications(User user) {
        return notificationRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponse::fromEntity)
                .toList();
    }

    /**
     * Marquer une notification comme lue
     */
    public NotificationResponse markAsRead(Integer notificationId, User user) {
        Notification notif = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notif.getUser().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("Not allowed to update this notification");
        }

        notif.setRead(true);
        Notification updated = notificationRepository.save(notif);
        return NotificationResponse.fromEntity(updated);
    }

    /**
     * Marquer toutes les notifications d'un utilisateur comme lues
     */
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .filter(notif -> !notif.isRead())
                .forEach(notif -> notif.setRead(true));
    }
}
