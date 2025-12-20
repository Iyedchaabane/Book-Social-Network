package com.ichaabane.book_network.user;

import com.ichaabane.book_network.auth.AuthenticationService;
import com.ichaabane.book_network.notification.NotificationResponse;
import com.ichaabane.book_network.notification.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final NotificationService notificationService;

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Principal connectedUser) {
        userService.changePassword(request, connectedUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> createUser(
            @Valid @RequestBody UserRequest request
    ) throws MessagingException {
        return ResponseEntity.ok(authenticationService.createUser(request));
    }

    @GetMapping("/me/notifications")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(user);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/me/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Integer id,
                                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        NotificationResponse updated = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }
}
