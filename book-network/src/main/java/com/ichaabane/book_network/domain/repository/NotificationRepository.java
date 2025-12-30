package com.ichaabane.book_network.domain.repository;

import com.ichaabane.book_network.domain.model.Notification;
import com.ichaabane.book_network.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findAllByUserOrderByCreatedAtDesc(User user);
}
