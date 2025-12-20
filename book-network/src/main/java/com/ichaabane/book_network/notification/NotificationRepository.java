package com.ichaabane.book_network.notification;

import com.ichaabane.book_network.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findAllByUserOrderByCreatedAtDesc(User user);
}
